package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;

import java.nio.file.Path;
import java.util.logging.Logger;

public enum ArchiveMode {

    FULL_BACKUP("archive.full_backup") {
        @Override
        public FileArchiver getInstance(Folder f) {
            Path archive = f.getSystemSubDir().resolve(
                    ConfigurationEntry.ARCHIVE_DIRECTORY_NAME.getValue(f.getController()));
            return new FileArchiver(archive, f.getController().getMySelf()
                    .getInfo());
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
        return Translation.get(key);
    }

    public abstract FileArchiver getInstance(Folder f);
}
