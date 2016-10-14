package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.LoginProto;

public class Login extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private String username;
    private String password;

    /**
     * Serialization constructor
     */
    public Login() {
    }

    public Login(String username, String password) {
        this.setUsername(username);
        this.setPassword(password);
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public Login(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof LoginProto.Login) {
            LoginProto.Login proto = (LoginProto.Login)mesg;
            this.setUsername(proto.getUsername());
            this.setPassword(proto.getPassword());
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        LoginProto.Login.Builder builder = LoginProto.Login.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setUsername(this.getUsername());
        builder.setPassword(this.getPassword());
        return builder.build();
    }
}
