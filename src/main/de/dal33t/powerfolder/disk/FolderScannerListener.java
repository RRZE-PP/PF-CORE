/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: FolderScanner.java 18828 2012-05-10 01:24:49Z tot $
 */
package de.dal33t.powerfolder.disk;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * This listener is called everytime a file was scanned by the FolderScanner.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public interface FolderScannerListener {

    /**
     * Called after FolderScanner#scanDiskItem() is finished. This is called for
     * every FileInfo the FolderScanner found.
     * 
     * @param fInfo
     *            The scanned file.
     */
    void notify(FileInfo fInfo);
}
