/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.d2d.*;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.FolderListProto;
import de.dal33t.powerfolder.util.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * List of available folders
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderList extends Message implements D2DObject, D2DEvent
{
    private static final long serialVersionUID = 101L;
    private static Logger LOG = Logger.getLogger(FolderList.class.getName());

    /** List of public folders. */
    public FolderInfo[] folders = new FolderInfo[0];

    /** Secret folders, Folder IDs are encrypted with magic Id */
    @Deprecated
    public FolderInfo[] secretFolders;

    /**
     * Boolean to indicate that the source also has joined matching meta
     * folders.
     */
    public boolean joinedMetaFolders;

    public FolderList() {
        // Serialisation constructor
    }

    public FolderList(Collection<FolderInfo> folderInfos) {
        this.folders = folderInfos.toArray(new FolderInfo[0]);
        this.secretFolders = new FolderInfo[0];
        this.joinedMetaFolders = true;
    }

    /**
     * Constructor which splits up public and secret folder into own array.
     * Folder Ids of secret folders will be encrypted with magic Id sent by
     * remote node
     *
     * @param allFolders
     * @param remoteMagicId
     *            the magic id which was sent by the remote side
     */
    @Deprecated
    public FolderList(Collection<FolderInfo> allFolders, String remoteMagicId) {
        Reject.ifBlank(remoteMagicId, "Remote magic id is blank");
        // Split folderlist into secret and public list
        // Encrypt secret folder ids with magic id
        List<FolderInfo> secretFos = new ArrayList<FolderInfo>(
            allFolders.size());
        for (FolderInfo folderInfo : allFolders) {
            // Send secret folder infos if magic id is not empty
            // Clone folderinfo
            String secureId = folderInfo.calculateSecureId(remoteMagicId);
            // Set Id to secure Id
            FolderInfo secretFolder = new FolderInfo(folderInfo.getName(),
                secureId);
            // Secret folder, encrypt folder id with magic id
            secretFos.add(secretFolder);
        }
        this.secretFolders = new FolderInfo[secretFos.size()];
        this.joinedMetaFolders = true;
        secretFos.toArray(secretFolders);
    }

    public boolean contains(FolderInfo foInfo, Member node) {
        ConnectionHandler peer = node.getPeer();
        if (peer == null) {
            return false;
        }
        if (peer.getMember().getProtocolVersion() < Identity.PROTOCOL_VERSION_112) {
            String magicId = peer.getMyMagicId();
            if (StringUtils.isBlank(magicId)) {
                return false;
            }
            return contains(foInfo, magicId);
        } else {
            return contains(foInfo);
        }
    }

    public boolean contains(FolderInfo folderInfo) {
        return folders != null && Arrays.asList(folders).contains(folderInfo);
    }

    @Deprecated
    public boolean contains(FolderInfo foInfo, String magicId) {
        String secureId = foInfo.calculateSecureId(magicId);
        for (FolderInfo folder : secretFolders) {
            if (folder.id.equals(secureId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param member
     *            the {@link Member} to store this {@link FolderList} relative
     *            to.
     * @return true if this {@link FolderList} could be stored.
     * @see #load(Member)
     * @deprecated #2569
     */
    @Deprecated
    public synchronized boolean store(Member member) {
        return store(getMemberFile(member));
    }

    /**
     * @param file
     *            the file to store this {@link FolderList} in.
     * @return true if this {@link FolderList} could be stored to that file.
     * @see #load(Path)
     * @deprecated #2569
     */
    @Deprecated
    public synchronized boolean store(Path file) {
        Reject.ifNull(file, "File");
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            byte[] buf = ByteSerializer.serializeStatic(this, false);
            PathUtils.copyFromStreamToFile(new ByteArrayInputStream(buf), file);
            return true;
        } catch (Exception e) {
            LOG.warning("Unable to store to " + file + ". " + e + ". " + this);
            return false;
        }
    }

    /**
     * @param member
     *            the {@link Member} to load a {@link FolderList} previously
     *            stored with {@link #store(Member)} from
     * @return the loaded {@link FolderList} or null if failed or not existing.
     * @deprecated #2569
     */
    @Deprecated
    public static FolderList load(Member member) {
        return load(getMemberFile(member));
    }

    /**
     * @param file
     *            the file to load a {@link FolderList} previously stored with
     *            {@link #store(Path)} from
     * @return the loaded {@link FolderList} or null if failed or not existing.
     * @deprecated #2569
     */
    @Deprecated
    public static FolderList load(Path file) {
        Reject.ifNull(file, "File");
        if (Files.notExists(file)) {
            return null;
        }
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buf = StreamUtils.readIntoByteArray(in);
            return (FolderList) ByteSerializer.deserializeStatic(buf, false);
        } catch (Exception e) {
            LOG.warning("Unable to load to " + file + ". " + e);
            return null;
        }
    }

    public static void removeMemberFiles(Controller controller) {
        try {
            PathUtils.recursiveDelete(Controller
                .getMiscFilesLocation().resolve(controller.getConfigName()
                + ".temp/nodes"));
        } catch (IOException e) {
            LOG.severe("Unable to deleted FolderList temporary files. " + e);
        }
    }

    private static Path getMemberFile(Member member) {
        String idPath = new String(Util.encodeHex(Util.md5(member.getId()
            .getBytes(Convert.UTF8))));
        return Controller.getMiscFilesLocation().resolve(member
            .getController().getConfigName()
            + ".temp/nodes/"
            + idPath
            + ".FolderList");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (joinedMetaFolders ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(folders);
        result = prime * result + Arrays.hashCode(secretFolders);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderList other = (FolderList) obj;
        if (joinedMetaFolders != other.joinedMetaFolders)
            return false;
        if (!Arrays.equals(folders, other.folders))
            return false;
        if (!Arrays.equals(secretFolders, other.secretFolders))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FolderList: " + ((folders != null ? folders.length : 0) + (secretFolders != null ? secretFolders.length : 0)) + " folders";
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     * @author Christoph Kappel <kappel@powerfolder.com>
     **/
    @Override
    public void
    initFromD2D(AbstractMessage message) {
        if (message instanceof FolderListProto.FolderList) {
            FolderListProto.FolderList proto = (FolderListProto.FolderList) message;
            int i = 0;
            this.folders = new FolderInfo[proto.getFolderInfosCount()];
            for (FolderInfoProto.FolderInfo folderInfo : proto.getFolderInfosList()) {
                this.folders[i++] = new FolderInfo(folderInfo);
            }
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     **/
    @Override
    public AbstractMessage
    toD2D() {
        FolderListProto.FolderList.Builder builder = FolderListProto.FolderList.newBuilder();
        builder.setClazzName("FolderList");
        for (FolderInfo folderInfo : this.folders) {
            builder.addFolderInfos((FolderInfoProto.FolderInfo) folderInfo.toD2D());
        }
        return builder.build();
    }

    @Override
    public void handle(Member node) {
        node.processFolderListD2D(this);
        // Execute additional code during handshake phase
        D2DSocketConnectionHandler d2DSocketConnectionHandler = ((D2DSocketConnectionHandler)node.getPeer());
        if (d2DSocketConnectionHandler == null) {
            return;
        }

        if (d2DSocketConnectionHandler.getNodeStateMachine().getCurrentState() == NodeState.OPEN_FOLDER_LIST_WAIT || d2DSocketConnectionHandler.getNodeStateMachine().getCurrentState() == NodeState.OPEN_LOGIN_REQUEST_WAIT) {
            node.handshakeFolderList();
        }
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.FOLDER_LIST;
    }
}
