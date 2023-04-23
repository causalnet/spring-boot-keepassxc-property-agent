package au.net.causal.springboot.keepassxcpropertyagent;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static au.net.causal.springboot.keepassxcpropertyagent.logging.Logging.*;

public class AgentConfiguration
{
    private static final String CONFIG_KEY_ENTRY_URI = "entryUri";
    private static final String CONFIG_KEY_CREDENTIALS_STORE_FILE = "credentialsStoreFile";
    private static final String CONFIG_KEY_UNLOCK_MAX_WAIT_TIME = "unlockMaxWaitTime";
    private static final String CONFIG_KEY_UNLOCK_MESSAGE_REPEAT_TIME = "unlockMessageRepeatTime";
    private static final String CONFIG_KEY_PROPERTY_PREFIX = "propertyPrefix";

    private static final String DEFAULT_ENTRY_URI = "spring://app";

    private final List<String> entryUris = new ArrayList<>();

    private Path credentialsStoreFile = Path.of("spring-boot-keepassxc-property-agent-credentials");
    private Duration unlockMaxWaitTime = Duration.ofMinutes(2L);
    private Duration unlockMessageRepeatTime = Duration.ofSeconds(5L);
    private String propertyPrefix = "KPH: spring:";

    public static AgentConfiguration parse(String argsString)
    {
        if (argsString == null)
        {
            argsString = "";
        }

        AgentConfiguration args = new AgentConfiguration();
        for (String argSegment : argsString.split(Pattern.quote(",")))
        {
            String[] argSplit = argSegment.split(Pattern.quote("="), 2);
            if (argSplit.length > 1)
            {
                String key = argSplit[0];
                String value = argSplit[1];

                try
                {
                    switch (key)
                    {
                        case CONFIG_KEY_ENTRY_URI -> args.addEntryUri(value);
                        case CONFIG_KEY_CREDENTIALS_STORE_FILE -> args.setCredentialsStoreFile(Path.of(value));
                        case CONFIG_KEY_UNLOCK_MAX_WAIT_TIME -> args.setUnlockMaxWaitTime(Duration.parse(value));
                        case CONFIG_KEY_UNLOCK_MESSAGE_REPEAT_TIME -> args.setUnlockMessageRepeatTime(Duration.parse(value));
                        case CONFIG_KEY_PROPERTY_PREFIX -> args.setPropertyPrefix(value);
                    }
                }
                catch (DateTimeParseException e)
                {
                    log("Error parsing Keepass agent configuration option '" + key + "' (" + value + "): " + e, e);
                }
            }
        }


        if (args.getEntryUris().isEmpty())
            args.addEntryUri(DEFAULT_ENTRY_URI);

        return args;
    }

    /**
     * @return a list of URIs for KeepassXC entries that will be used for Spring properties.
     *
     * @see #addEntryUri(String)
     */
    public List<String> getEntryUris()
    {
        return entryUris;
    }

    /**
     * Adds a KeepassXC entry URI for finding entries that will be used for Spring properties.
     *
     * @param entryUri the entry URI to add.
     *
     * @see #getEntryUris()
     */
    public void addEntryUri(String entryUri)
    {
        entryUris.add(entryUri);
    }

    /**
     * @return the credentials store file that is used for pairing with KeepassXC as a client.  May be a relative path.
     *
     * @see #setCredentialsStoreFile(Path)
     */
    public Path getCredentialsStoreFile()
    {
        return credentialsStoreFile;
    }

    /**
     * Sets the credentials store file.
     **
     * @see #getCredentialsStoreFile()
     */
    public void setCredentialsStoreFile(Path credentialsStoreFile)
    {
        this.credentialsStoreFile = credentialsStoreFile;
    }

    /***
     * @return the maximum amount of time to wait for the user to unlock their database in KeepassXC before giving up and failing.
     *
     * @see #setUnlockMaxWaitTime(Duration)
     */
    public Duration getUnlockMaxWaitTime()
    {
        return unlockMaxWaitTime;
    }

    /**
     * Sets the maximum amount of time to wait for the user to unlock their database in KeepassXC before giving up and failing.
     *
     * @see #getUnlockMaxWaitTime()
     */
    public void setUnlockMaxWaitTime(Duration unlockMaxWaitTime)
    {
        this.unlockMaxWaitTime = unlockMaxWaitTime;
    }

    /**
     * @return the interval for repeating the 'you should unlock the database' message to the user in the console.
     *
     * @see #setUnlockMessageRepeatTime(Duration)
     */
    public Duration getUnlockMessageRepeatTime()
    {
        return unlockMessageRepeatTime;
    }

    /**
     * Sets the interval for repeating the unlock message.
     *
     * @see #getUnlockMessageRepeatTime()
     */
    public void setUnlockMessageRepeatTime(Duration unlockMessageRepeatTime)
    {
        this.unlockMessageRepeatTime = unlockMessageRepeatTime;
    }

    /**
     * @return the prefix for KeepassXC entry property names that will be used by the agent.
     *
     * @see #setPropertyPrefix(String)
     */
    public String getPropertyPrefix()
    {
        return propertyPrefix;
    }

    /**
     * Sets the prefix for KeepassXC entry property names that will be used by the agent.
     *
     * @param propertyPrefix the property prefix to set.
     *
     * @see #getPropertyPrefix()
     */
    public void setPropertyPrefix(String propertyPrefix)
    {
        this.propertyPrefix = propertyPrefix;
    }
}
