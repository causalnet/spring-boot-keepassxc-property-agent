package au.net.causal.springboot.keepassxcpropertyagent.connection;

import org.purejava.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

import static au.net.causal.springboot.keepassxcpropertyagent.logging.Logging.log;

/**
 * Stores credentials as a serialized credentials object.
 */
public class StandardKeepassCredentialsStore implements KeepassCredentialsStore
{
    private final Path storeFile;

    public StandardKeepassCredentialsStore(Path storeFile)
    {
        this.storeFile = Objects.requireNonNull(storeFile);
    }

    @Override
    public void saveCredentials(Credentials credentials)
    throws IOException
    {
        Files.createDirectories(storeFile.getParent());
        Path tmpPath;
        try
        {
            tmpPath = Files.createTempFile(storeFile.getParent(), storeFile.getFileName().toString(), ".tmp",
                                           PosixFilePermissions.asFileAttribute(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
        }
        catch (IOException | UnsupportedOperationException e)
        {
            //log.debug("Failed to set POSIX permissions on store file: " + e, e);

            //Posix attributes may not be supported on this file system, or it just failed for some reason, fall back to not trying to set permissions
            tmpPath = Files.createTempFile(storeFile.getParent(), storeFile.getFileName().toString(), ".tmp");
        }
        try (ObjectOutputStream os = new ObjectOutputStream(Files.newOutputStream(tmpPath)))
        {
            os.writeObject(credentials);
        }
        Files.move(tmpPath, storeFile, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Credentials loadCredentials()
    throws IOException
    {
        if (Files.notExists(storeFile))
            return null;

        try (ObjectInputStream is = new ObjectInputStream(Files.newInputStream(storeFile)))
        {
            return (Credentials)is.readObject();
        }
        catch (ObjectStreamException | ClassNotFoundException e)
        {
            //If the file is corrupted (empty or bad data) log a warning and just re-pair with Keepass
            log("KeepassXC property agent credentials file corrupted - will attempt recreation and repair with KeepassXC: " + e, e);
            return null;
        }
        //Normal IO exception will fail like normal - a more serious data reading issue
    }
}
