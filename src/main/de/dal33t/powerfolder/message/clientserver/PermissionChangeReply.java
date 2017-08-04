package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.PermissionChangeReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class PermissionChangeReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;

    /**
     * Serialization constructor
     */
    public PermissionChangeReply() {
    }

    public PermissionChangeReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * Init from D2D message
     *
     * @param mesg Message to use data from
     **/
    public PermissionChangeReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(String replyCode) {
        this.replyCode = replyCode;
    }

    public ReplyStatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(ReplyStatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
    }

    /**
     * initFromD2DMessage
     * Init from D2D message
     *
     * @param mesg Message to use data from
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if (mesg instanceof PermissionChangeReplyProto.PermissionChangeReply) {
            PermissionChangeReplyProto.PermissionChangeReply proto = (PermissionChangeReplyProto.PermissionChangeReply) mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
        }
    }

    /**
     * toD2D
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public AbstractMessage toD2D() {
        PermissionChangeReplyProto.PermissionChangeReply.Builder builder = PermissionChangeReplyProto.PermissionChangeReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null)
            builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        return builder.build();
    }
}
