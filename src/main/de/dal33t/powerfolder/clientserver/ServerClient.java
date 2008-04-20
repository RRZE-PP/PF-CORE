package de.dal33t.powerfolder.clientserver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderException;
import de.dal33t.powerfolder.disk.FolderSettings;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.event.NodeManagerListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.clientserver.AccountDetails;
import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.security.FolderAdminPermission;
import de.dal33t.powerfolder.security.InvalidAccount;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;

/**
 * Client to a server.
 * <p>
 * TODO Finalize Request <-> Response code.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class ServerClient extends PFComponent {
    private Member server;
    private AccountDetails accountDetails;
    private UserService userService;
    private FolderService folderService;
    private boolean tringToConnect;

    public ServerClient(Controller controller, Member serverNode) {
        super(controller);
        Reject.ifNull(serverNode, "Server node is null");
        this.server = serverNode;
        userService = (UserService) ServiceProvider.createRemoteStub(
            controller, UserService.SERVICE_ID, UserService.class, serverNode);
        folderService = (FolderService) ServiceProvider.createRemoteStub(
            controller, FolderService.SERVICE_ID, FolderService.class,
            serverNode);
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
        this.accountDetails = new AccountDetails(new InvalidAccount(), 0, 0);
    }

    public void start() {
        getController().scheduleAndRepeat(new OnlineStorageConnectTask(), 0,
            1000L * 20);
    }

    public Member getServer() {
        return server;
    }

    /**
     * Logs into the server and saves the identity as my login.
     * 
     * @param username
     * @param password
     * @return the identity with this username or <code>NullAccount</code> if
     *         login failed.
     */
    public Account login(String username, String password) {
        String salt = IdGenerator.makeId() + IdGenerator.makeId();
        String mix = salt + password + salt;
        String passwordMD5;
        try {
            passwordMD5 = new String(Util.md5(mix.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found", e);
        }
        boolean loginOk = userService.login(username, passwordMD5, salt);
        if (!loginOk) {
            log().warn("Login to server (" + username + ") failed!");
            accountDetails = new AccountDetails(new InvalidAccount(), 0, 0);
            return accountDetails.getAccount();
        }
        AccountDetails newAccountDetails = userService.getAccountDetails();
        log().warn(
            "Login to server (" + username + ") result: " + accountDetails);
        if (newAccountDetails != null) {
            accountDetails = newAccountDetails;
        } else {
            accountDetails = new AccountDetails(new InvalidAccount(), 0, 0);
        }
        return accountDetails.getAccount();
    }

    /**
     * @return the user/account of the last login.
     */
    public Account getAccount() {
        return accountDetails != null ? accountDetails.getAccount() : null;
    }

    public AccountDetails getAccountDetails() {
        return accountDetails;
    }

    // Services ***************************************************************

    public UserService getUserService() {
        return userService;
    }

    public FolderService getFolderService() {
        return folderService;
    }

    // Conviniece *************************************************************

    /**
     * @return the joined folders by the OS.
     */
    public List<Folder> getJoinedFolders() {
        List<Folder> mirroredFolders = new ArrayList<Folder>();
        for (Folder folder : getController().getFolderRepository()
            .getFoldersAsCollection())
        {
            if (hasJoined(folder)) {
                mirroredFolders.add(folder);
            }
        }
        return mirroredFolders;
    }

    /**
     * @param folder
     *            the folder to check.
     * @return true if the server has joined the folder.
     */
    public boolean hasJoined(Folder folder) {
        return folder.hasMember(server);
    }

    /**
     * @return true if the last attempt to login to the online storage was ok.
     *         false if not or no login tried yet.
     */
    public boolean isLastLoginOK() {
        return accountDetails != null
            && (!(accountDetails.getAccount() instanceof InvalidAccount));
    }

    /**
     * @param node
     * @return true if the node is the server.
     */
    public boolean isServer(Member node) {
        return server.equals(node);
    }

    /**
     * @return if the server is connected
     */
    public boolean isConnected() {
        return server.isCompleteyConnected();
    }

    /**
     * @return true if the account data has been set
     */
    public boolean isAccountSet() {
        return !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
            .getValue(getController()))
            && !StringUtils.isEmpty(ConfigurationEntry.WEBSERVICE_USERNAME
                .getValue(getController()));
    }

    /**
     * Syncs the folder memberships with the FolderAdminPermissions on the
     * server.
     */
    public void syncFolderRights() {
        Reject.ifFalse(isLastLoginOK(), "Last login not ok");
        FolderInfo[] myFolders = getController().getFolderRepository()
            .getJoinedFolderInfos();

        if (logWarn) {
            log().warn(
                "Granting admin permission on: " + Arrays.asList(myFolders));
        }
        getFolderService().grantAdmin(myFolders);

        log().warn("Rights: " + getAccount().getPermissions().size());
        Collection<FolderInfo> foInfos = FolderAdminPermission
            .filter(getAccount());
        log().warn("Rights on: " + foInfos);
        for (FolderInfo foInfo : foInfos) {
            log().warn("Checking: " + foInfo);
            if (getController().getFolderRepository().hasJoinedFolder(foInfo)) {
                continue;
            }
            FolderSettings settings = new FolderSettings(new File("."),
                SyncProfile.SYNCHRONIZE_PCS, true, true, true);
            try {
                log().warn("Adding as preview: " + foInfo);
                getController().getFolderRepository().createPreviewFolder(
                    foInfo, settings);
            } catch (FolderException e1) {
                log().error(e1);
            }
        }
    }

    // Inner classes **********************************************************

    /**
     * This listener violates the rule "Listener/Event usage". Reason: Even when
     * a ServerClient is a true core-component there might be multiple
     * ClientServer objects that dynamically change.
     * <p>
     * http://dev.powerfolder.com/projects/powerfolder/wiki/GeneralDevelopRules
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeConnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {
                if (isAccountSet()) {
                    String username = ConfigurationEntry.WEBSERVICE_USERNAME
                        .getValue(getController());
                    String password = ConfigurationEntry.WEBSERVICE_PASSWORD
                        .getValue(getController());
                    login(username, password);
                }
            }
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            if (isServer(e.getNode())) {
                // Invalidate account.
                // TODO: Why no cache until reconnect?
                // myAccount = null;
            }
        }

        public void friendAdded(NodeManagerEvent e) {
        }

        public void friendRemoved(NodeManagerEvent e) {
        }

        public void nodeAdded(NodeManagerEvent e) {
        }

        public void nodeRemoved(NodeManagerEvent e) {
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        public void startStop(NodeManagerEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return false;
        }
    }

    // Internal classes *******************************************************

    private class OnlineStorageConnectTask extends TimerTask {
        @Override
        public void run() {
            if (isConnected()) {
                return;
            }
            if (isServer(getController().getMySelf())) {
                return;
            }
            if (getController().isLanOnly()) {
                return;
            }
            if (tringToConnect) {
                return;
            }
            if (!ConfigurationEntry.AUTO_CONNECT
                .getValueBoolean(getController()))
            {
                return;
            }
            tringToConnect = true;
            Runnable connector = new Runnable() {
                public void run() {
                    try {
                        log().debug(
                            "Triing to connect to Online Storage ("
                                + Constants.ONLINE_STORAGE_ADDRESS + ")");
                        ConnectionHandler conHan = getController()
                            .getIOProvider().getConnectionHandlerFactory()
                            .tryToConnect(Constants.ONLINE_STORAGE_ADDRESS);
                        getController().getNodeManager().acceptConnection(
                            conHan);
                    } catch (ConnectionException e) {
                        log().warn("Unable to connect to online storage", e);
                    } finally {
                        tringToConnect = false;
                    }
                }
            };
            getController().getIOProvider().startIO(connector);
        }
    }
}
