package au.net.causal.springboot.keepassxcpropertyagent.connection;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static au.net.causal.springboot.keepassxcpropertyagent.logging.Logging.log;

/**
 * Type-safe settings that are sourced from the configuration of the decryptor which comes from the settings-security.xml configurations section.
 */
public class KeepassExtensionSettings
{
    private static final String CONFIG_KEY_CREDENTIALS_STORE_FILE = "credentialsStoreFile";
    private static final String CONFIG_KEY_UNLOCK_MAX_WAIT_TIME = "unlockMaxWaitTime";
    private static final String CONFIG_KEY_UNLOCK_MESSAGE_REPEAT_TIME = "unlockMessageRepeatTime";

    private Path credentialsStoreFile = Path.of("spring-boot-keepassxc-property-agent-credentials");
    private Duration unlockMaxWaitTime = Duration.ofMinutes(2L);
    private Duration unlockMessageRepeatTime = Duration.ofSeconds(5L);

    /**
     * Configures this settings object from configuration passed in to a decryptor which is sourced from that decryptor's configuration
     * in settings-security.xml's configuration section.
     *
     * @param config configuration map.  Actually has string keys and value.
     */
    public void configure(Map<?, ?> config)
    {
        Path credentialsStoreFile = pathFromMapKey(config, CONFIG_KEY_CREDENTIALS_STORE_FILE);
        if (credentialsStoreFile != null)
            setCredentialsStoreFile(credentialsStoreFile);

        Duration unlockMaxWaitTime = durationFromMapKey(config, CONFIG_KEY_UNLOCK_MAX_WAIT_TIME);
        if (unlockMaxWaitTime != null)
            setUnlockMaxWaitTime(unlockMaxWaitTime);

        Duration unlockMessageRepeatTime = durationFromMapKey(config, CONFIG_KEY_UNLOCK_MESSAGE_REPEAT_TIME);
        if (unlockMessageRepeatTime != null)
            setUnlockMessageRepeatTime(unlockMessageRepeatTime);
    }

    /**
     * Reads a string value from a map.
     *
     * @param map the map to read the value from.
     * @param key the key to read the value with @return the value converted to string, or null if no entry for the specified key exists in the map.
     */
    private static String stringFromMapKey(Map<?, ?> map, String key)
    {
        Object value = map.get(key);
        if (value == null)
            return null;
        else
            return value.toString();
    }

    /**
     * Reads a Path value from a map.
     *
     * @param map the map to read the value from.
     * @param key the key to read the value with.
     *
     * @return the value converted to a Path, or null if no entry for the specified key exists in the map.
     */
    private static Path pathFromMapKey(Map<?, ?> map, String key)
    {
        String sValue = stringFromMapKey(map, key);
        if (sValue == null)
            return null;
        else
            return Path.of(sValue);
    }

    /**
     * Reads a Duration value from a map.
     *
     * @param map the map to read the value from.
     * @param key the key to read the value with.
     *
     * @return the value converted to a Duration, or null if no entry for the specified key exists in the map or the value could not be parsed.
     */
    private static Duration durationFromMapKey(Map<?, ?> map, String key)
    {
        String sValue = stringFromMapKey(map, key);
        if (sValue == null)
            return null;
        else
        {
            try
            {
                return Duration.parse(sValue);
            }
            catch (DateTimeParseException e)
            {
                log("Error parsing Keepass extension configuration option '" + key + "' (" + sValue + "): " + e, e);
                return null;
            }
        }
    }

    /**
     * Reads an enum value from a map.
     *
     * @param map the map to read the value from.
     * @param key the key to read the value with.
     * @param enumType the enum type.
     *
     * @return the value converted to an enum value, or null if no entry for the specified key exists in the map or the value could not be parsed.
     */
    private static <E extends Enum<E>> E enumFromMapKey(Map<?, ?> map, String key, Class<E> enumType)
    {
        String sValue = stringFromMapKey(map, key);
        if (sValue == null)
            return null;
        else
        {
            try
            {
                return Enum.valueOf(enumType, sValue);
            }
            catch (IllegalArgumentException e)
            {
                log("Error parsing Keepass extension configuration option '" + key + "' (" + sValue + "): " + e, e);
                return null;
            }
        }

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
}
