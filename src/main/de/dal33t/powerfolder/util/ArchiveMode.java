package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;

public enum ArchiveMode {
    FULL_BACKUP("archive.full_backup") {

        @Override
        public FileArchiver getInstance(Folder f) {
            Path archive = f.getSystemSubDir().resolve(
                (String)ConfigurationEntry.ARCHIVE_DIRECTORY_NAME.getValue(f
                    .getController()));
            if (!f.checkIfDeviceDisconnected() && Files.notExists(archive)) {
                try {
                    Files.createDirectories(archive);
                } catch (IOException ioe) {
                    log.warning("Failed to create archive directory in system subdirectory: "
                        + archive);
                }
            }
            return new FileArchiver(archive, f.getController()
                .getMySelf().getInfo());
        }

    };

    private static Logger log = Logger.getLogger(ArchiveMode.class.getName());
    private final String key;

    ArchiveMode(String key) {
        assert StringUtils.isNotEmpty(key);
        this.key = key;
    }

    /**
     * Simplifies usage in GUI
     */
    @Override
    public String toString() {
        return Translation.getTranslation(key);
    }

    public abstract FileArchiver getInstance(Folder f);
}
