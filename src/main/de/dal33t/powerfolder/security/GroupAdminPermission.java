/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
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
 * $Id: FolderRepository.java 20999 2013-03-11 13:19:11Z glasgow $
 */
package de.dal33t.powerfolder.security;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.PermissionInfoProto;
import de.dal33t.powerfolder.protocol.PermissionTypeProto;
import de.dal33t.powerfolder.protocol.StringMessageProto;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
public class GroupAdminPermission implements Permission, D2DObject {

    private static final Logger LOG = Logger.getLogger(GroupAdminPermission.class.getName());
    private static final long serialVersionUID = 100L;
    public static final String ID_SEPARATOR = "_GP_";
    private String groupOID;
    // PFS-1888: For Backward compatibility. Remove after major distribution of 10.4:
    private Group group;

    public GroupAdminPermission(Group group) {
        Reject.ifNull(group, "group is null");
        this.groupOID = group.getOID();
        this.group = group;
    }

    public GroupAdminPermission(String groupOID) {
        Reject.ifBlank(groupOID, "GroupID is blank");
        this.groupOID = groupOID;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public GroupAdminPermission(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public boolean implies(Permission impliedPermision) {
        return false;
    }

    public String getId() {
        return groupOID + ID_SEPARATOR + getClass().getSimpleName();
    }

    public String getGroupOID() {
        return groupOID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((groupOID == null) ? 0 : groupOID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Permission))
            return false;
        Permission other = (Permission) obj;
        return Util.equals(getId(), other.getId());
    }
  
    // Serialization compatibility ********************************************

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        if (groupOID == null && group != null) {
            groupOID = group.getOID();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (group == null && groupOID != null) {
            group = new Group(groupOID, "-unknown-");
        }
        out.defaultWriteObject();
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof PermissionInfoProto.PermissionInfo) {
            PermissionInfoProto.PermissionInfo proto = (PermissionInfoProto.PermissionInfo) mesg;
            if (proto.getObjectsList().size() == 1) {
                try {
                    // Objects can be any message so they need to be unpacked from com.google.protobuf.Any
                    com.google.protobuf.Any object = proto.getObjects(0);
                    String clazzName = object.getTypeUrl().split("/")[1];
                    if (clazzName.equals("StringMessage")) {
                        StringMessageProto.StringMessage stringMessage = object.unpack(StringMessageProto.StringMessage.class);
                        this.groupOID = stringMessage.getValue();
                    }
                } catch (InvalidProtocolBufferException | NullPointerException e) {
                    LOG.severe("Cannot unpack message: " + e);
                }
            }
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        PermissionInfoProto.PermissionInfo.Builder builder = PermissionInfoProto.PermissionInfo.newBuilder();
        builder.setClazzName("PermissionInfo");
        StringMessageProto.StringMessage.Builder stringMessageBuilder = StringMessageProto.StringMessage.newBuilder();
        stringMessageBuilder.setClazzName("StringMessage");
        stringMessageBuilder.setValue(this.groupOID);
        // Objects can be any message so they need to be packed to com.google.protobuf.Any
        builder.addObjects(com.google.protobuf.Any.pack(stringMessageBuilder.build()));
        // Set permission enum
        builder.setPermissionType(PermissionTypeProto.PermissionType.GROUP_ADMIN);
        return builder.build();
    }
}
