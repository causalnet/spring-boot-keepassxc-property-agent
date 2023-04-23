package au.net.causal.springboot.keepassxcpropertyagent;

import au.net.causal.springboot.keepassxcpropertyagent.connection.KeepassCredentialsStore;
import au.net.causal.springboot.keepassxcpropertyagent.connection.KeepassProxy;
import au.net.causal.springboot.keepassxcpropertyagent.connection.StandardKeepassCredentialsStore;
import org.purejava.KeepassProxyAccessException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static au.net.causal.springboot.keepassxcpropertyagent.logging.Logging.*;

public class KeepassXCPropertyReader
{
    private static final Path CREDENTIALS_STORE_BASE_DIRECTORY = Path.of(System.getProperty("user.home"), ".spring-boot-keepassxc-property-agent");

    private final Clock clock = Clock.systemUTC();

    private final AgentConfiguration settings;

    public KeepassXCPropertyReader(AgentConfiguration settings)
    {
        this.settings = Objects.requireNonNull(settings);
    }

    private KeepassProxy connectKeepassProxy(KeepassCredentialsStore credentialsStore)
    throws IOException
    {
        KeepassProxy kpa = new KeepassProxy(credentialsStore);

        tryRepeat(settings,
                  "Agent needs to read passwords from KeepassXC, please start KeepassXC, ensure the 'Browser Extensions' option is enabled and open your database",
                  "Failed to connect to KeepassXC",
                  kpa::connect);

        boolean connected = kpa.connectionAvailable();
        if (!connected)
            connected = kpa.associate();

        tryRepeat(settings,
                  "Agent needs to read passwords from KeepassXC, please unlock your database",
                  "Failed to connect to KeepassXC - database remained locked",
                  () ->
        {
            boolean iConnected = kpa.connectionAvailable();
            if (!iConnected)
                throw new IOException("Could not connect to KeepassXC");
        });

        return kpa;
    }

    /**
     * Runs a block of code multiple times until it succeeds or the unlock timeout it hit.  Non-success for the block is when it fails
     * with an IOException.
     * <p>
     *
     * This method returns normally if, either initially or during a repeat, the code block succeeds.  If it times out, a
     * IOException is thrown.
     *
     * @param settings Keepass settings used to determine the timeout time and the message repeat time.
     * @param failMessage message to display and possibly repeat to the user when the code block fails.
     * @param timeoutMessage message to display on timeout.
     * @param block the code block to execute, possibly multiple times.
     */
    private void tryRepeat(AgentConfiguration settings, String failMessage, String timeoutMessage, RepeatBlock block)
    throws IOException
    {
        IOException failureException = null;

        //Staggered
        Instant connectionStartTime = Instant.now(clock);
        Instant connectionMaxTime = connectionStartTime.plus(settings.getUnlockMaxWaitTime());
        Instant lastMessageTime = Instant.EPOCH;
        while (failureException == null || Instant.now(clock).isBefore(connectionMaxTime))
        {
            try
            {
                block.call();

                //If we succeed we are finished
                return;
            }
            catch (IOException e)
            {
                failureException = e;
            }

            //If we get here we failed to connect
            Instant now = Instant.now(clock);
            Duration remainingTime = Duration.between(now, connectionMaxTime).truncatedTo(ChronoUnit.SECONDS); //truncate to seconds for a nicer message
            if (lastMessageTime.plus(settings.getUnlockMessageRepeatTime()).isBefore(now))
            {
                log(failMessage + " (timeout in " + remainingTime + ")...");
                lastMessageTime = now;
            }

            try
            {
                Thread.sleep(500L);
            }
            catch (InterruptedException e)
            {
                InterruptedIOException ex = new InterruptedIOException("Interrupted while waiting for KeepassXC");
                ex.initCause(e);
                throw ex;
            }
        }

        String msg = timeoutMessage + " (within " + settings.getUnlockMaxWaitTime() + ")";
        log(msg);
        throw new IOException(msg);
    }

    /**
     * Loads the credentials/pairing store for our KeepassXC client.  The credentials from this store are used for pairing with KeepassXC as a client.
     *
     * @return the store.
     */
    protected KeepassCredentialsStore createCredentialsStore()
    {
        //May be absolute, but if relative resolve from the .m2 directory
        Path credentialsStoreFile = CREDENTIALS_STORE_BASE_DIRECTORY.resolve(settings.getCredentialsStoreFile());

        return new StandardKeepassCredentialsStore(credentialsStoreFile);
    }

    public void readProperties(String entryName, Map<String, Object> valueMap)
    throws IOException
    {
        KeepassCredentialsStore credentialsStore = createCredentialsStore();

        try (KeepassProxy kpa = connectKeepassProxy(credentialsStore))
        {
            log("Reading properties from KeePassXC entry: " + entryName);
            Map<String, ?> results = kpa.getLogins(entryName, null, true, List.of(kpa.exportConnection()));
            if (results == null)
            {
                log("Entry not found for " + entryName);
                return;
            }

            Object entriesObj = results.get("entries");
            if (!(entriesObj instanceof Collection<?>))
            {
                //log.debug("No entries value for " + entryName);
                return;
            }

            Collection<?> rawEntries = (Collection<?>)entriesObj;
            List<KeepassEntry> entries = new ArrayList<>(rawEntries.size());
            for (Object rawEntry : rawEntries)
            {
                if (rawEntry instanceof Map<?, ?>)
                    entries.add(KeepassEntry.parse((Map<?, ?>)rawEntry));
            }

            for (KeepassEntry entry : entries)
            {
                entry.getStringFields().forEach((k, v) ->
                {
                    if (k.startsWith(settings.getPropertyPrefix()))
                    {
                        String key = k.substring(settings.getPropertyPrefix().length()).trim();
                        Object value = v;
                        if (value instanceof String)
                            value = value.toString().trim();

                        if (value != null)
                            valueMap.put(key, value);
                    }
                });
            }
        }
        catch (KeepassProxyAccessException e)
        {
            throw new IOException("Error getting entry for " + entryName + ": " + e, e);
        }
    }

    /**
     * An entry returned from KeepassXC.
     *
     * See:
     * <ul>
     *     <li><a href="https://github.com/keepassxreboot/keepassxc-browser/blob/develop/keepassxc-protocol.md#get-logins">KeepassXC protocol documentation</a></li>
     *     <li><a href="https://github.com/keepassxreboot/keepassxc/blob/2.7.4/src/browser/BrowserAction.cpp#L234">BrowserAction::handleGetLogins</a></li>
     *     <li><a href="https://github.com/keepassxreboot/keepassxc/blob/2.7.4/src/browser/BrowserService.cpp#L920">BrowserService::prepareEntry</a></li>
     * </ul>
     */
    private static class KeepassEntry
    {
        private final String name;
        private final String login;
        private final String password;
        private final String group;
        private final Map<String, String> stringFields;

        public KeepassEntry(String name, String login, String password, String group, Map<String, String> stringFields)
        {
            this.name = name;
            this.login = login;
            this.password = password;
            this.group = group;
            this.stringFields = Map.copyOf(stringFields);
        }

        /**
         * Converts object to string, keeping null as null.
         */
        private static String stringValue(Object raw)
        {
            if (raw == null)
                return null;
            else
                return raw.toString();
        }

        /**
         * Parses an entry from JSON returned from a KeepassXC connection's getLogin call.
         *
         * @param json raw JSON in map form.  Nested maps, strings and primitives.
         *
         * @return the parsed entry.
         */
        public static KeepassEntry parse(Map<?, ?> json)
        {
            String name = stringValue(json.get("name"));
            String login = stringValue(json.get("login"));
            String password = stringValue(json.get("password"));
            String group = stringValue(json.get("group"));

            Object rawStringFields = json.get("stringFields");
            Map<String, String> stringFields = new LinkedHashMap<>();
            if (rawStringFields instanceof Collection<?>)
            {
                Collection<?> stringFieldsList = (Collection<?>)rawStringFields;
                for (Object rawStringFieldEntry : stringFieldsList)
                {
                    if (rawStringFieldEntry instanceof Map<?, ?>)
                    {
                        Map<?, ?> stringFieldEntry = (Map<?, ?>)rawStringFieldEntry;
                        for (Map.Entry<?, ?> e : stringFieldEntry.entrySet())
                        {
                            if (e.getKey() != null && e.getValue() != null)
                                stringFields.put(e.getKey().toString(), e.getValue().toString());
                        }
                    }
                }
            }

            return new KeepassEntry(name, login, password, group, stringFields);
        }

        public String getName()
        {
            return name;
        }

        public String getLogin()
        {
            return login;
        }

        public String getPassword()
        {
            return password;
        }

        public String getGroup()
        {
            return group;
        }

        public Map<String, ?> getStringFields()
        {
            return stringFields;
        }
    }

    /**
     * Piece of KeypassXC connection code that can potentially be repeated if it fails.
     */
    @FunctionalInterface
    private static interface RepeatBlock
    {
        /**
         * Executes the code block.  Returns normally when successful, or throws a IOException on failure.
         *
         * @throws IOException on failure.
         */
        public void call()
        throws IOException;
    }
}
