/* $Id: RConManager.java,v 1.10 2006/04/29 08:35:14 schaatser Exp $
 */
package de.dal33t.powerfolder;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Util;

/**
 * The remote command processor is responsible for binding on a socket and
 * receive and process any remote control commands. e.g. Load invitation file,
 * process powerfolder links or exit powerfolder.
 * <p>
 * Supported links:
 * <p>
 * <code>
 * Folder links:
 * PowerFolder://|folder|<foldername>|<P or S>|<folderid>|<size>|<numFiles>
 * <P or S> P = public, S = secret
 * PowerFolder://|folder|test|S|[test-AAgwZXFLgigj222]|99900000|1000
 * 
 * File links:
 * PowerFolder://|file|<foldername>|<P or S>|<folderid>|<fullpath_filename>
 * <P or S> P = public, S = secret
 * PowerFolder://|folder|test|S|[test-AAgwZXFLgigj222]|/test/New_text_docuement.txt
 * </code>
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.10 $
 */
public class RConManager extends PFComponent implements Runnable {
    // The logger
    private static final Logger LOG = Logger.getLogger(RConManager.class);
    // The default port to listen for remote commands
    private static final int DEFAULT_RCON_PORT = 1338;
    // The default prefix for all rcon commands
    private static final String RCON_PREFIX = "PowerFolder_RCON_COMMAND";
    // The default enconding
    private static final String RCON_ENCODING = "UTF8";
    // The prefix for pf links
    private static final String POWERFOLDER_LINK_PREFIX = "powerfolder://";

    // All possible commands
    public static final String QUIT = "QUIT";
    public static final String OPEN = "OPEN;";

    // Private vars
    private ServerSocket serverSocket;
    private Thread myThread;

    /**
     * Initalization
     * 
     * @param controller
     */
    public RConManager(Controller controller) {
        super(controller);
    }

    /**
     * Checks if there is a running instance of PowerFolder
     * 
     * @return
     */
    public static boolean hasRunningInstance() {
        ServerSocket testSocket = null;
        try {
            //Only bind to localhost
            testSocket = new ServerSocket(DEFAULT_RCON_PORT, 0, InetAddress
                .getByName("127.0.0.1"));

            // Server socket can be opend, no instance of PowerFolder running
            return false;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        } finally {
            if (testSocket != null) {
                try {
                    testSocket.close();
                } catch (IOException e) {
                    LOG.error("Unable to close already running test socket. "
                        + testSocket, e);
                }
            }
        }
        return true;
    }

    /**
     * Sends a remote command to a running instance of PowerFolder
     * 
     * @param command
     *            the command
     * @return true if succeeded, otherwise false
     */
    public static boolean sendCommand(String command) {
        try {
            LOG.debug("Sending remote command '" + command + "'");
            Socket socket = new Socket("127.0.0.1", 1338);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket
                .getOutputStream(), RCON_ENCODING));

            writer.println(RCON_PREFIX + ";" + command);
            writer.flush();
            writer.close();
            socket.close();

            return true;
        } catch (IOException e) {
            LOG.error("Unable to send remote command", e);
        }
        return false;
    }

    /**
     * Starts the rcon processor
     */
    public void start() {
        try {
            //Only bind to localhost
            serverSocket = new ServerSocket(DEFAULT_RCON_PORT, 0, InetAddress
                .getByName("127.0.0.1"));

            // Start thread
            myThread = new Thread(this, "RCon Manager");
            myThread.start();
        } catch (UnknownHostException e) {
            log().warn(
                "Unable to open rcon manager on port " + DEFAULT_RCON_PORT, e);
        } catch (IOException e) {
            log().warn(
                "Unable to open rcon manager on port " + DEFAULT_RCON_PORT, e);
        }
    }

    /**
     * Shuts down the rcon manager
     */
    public void shutdown() {
        if (myThread != null) {
            myThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log().verbose("Unable to close rcon socket", e);
            }
        }
    }

    public void run() {
        log().info(
            "Listening for remote commands on port "
                + serverSocket.getLocalPort());
        while (!Thread.currentThread().isInterrupted()) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                log().verbose("Rcon socket closed, stopping", e);
                break;
            }

            log().verbose("Remote command from " + socket);
            try {
                String address = socket.getInetAddress().getHostAddress();
                if (address.equals("localhost") || address.equals("127.0.0.1"))
                {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(),
                            RCON_ENCODING));
                    String line = reader.readLine();
                    if (line.startsWith(RCON_PREFIX)) {
                        processCommand(line.substring(RCON_PREFIX.length() + 1));
                    }
                }
                socket.close();
            } catch (IOException e) {
                log().warn("Problems parsing remote command from " + socket);
            }
        }
    }

    /**
     * Processes a remote command
     * 
     * @param command
     */
    private void processCommand(String command) {
        if (StringUtils.isBlank(command)) {
            log().error("Received a empty remote command");
            return;
        }
        log().debug("Received remote command: '" + command + "'");
        if (QUIT.equalsIgnoreCase(command)) {
            getController().exit(0);
        } else if (command.startsWith(OPEN)) {
            // Open files
            String fileStr = command.substring(OPEN.length());

            // Open all files in remote command
            StringTokenizer nizer = new StringTokenizer(fileStr, ";");
            while (nizer.hasMoreTokens()) {
                String token = nizer.nextToken().toLowerCase();                
                if (token.startsWith(POWERFOLDER_LINK_PREFIX)) {
                    // We got a link
                    openLink(token);
                } else {
                    // Must be a file
                    File file = new File(token);
                    openFile(file);
                }

            }
        } else {
            log().warn("Remote command not recognizable '" + command + "'");
        }
    }

    /**
     * Opens a powerfolder link and executes it
     * 
     * @param link
     */
    private void openLink(String link) {
        String plainLink = link.substring(POWERFOLDER_LINK_PREFIX.length());
        log().warn("Got plain link: " + plainLink);

        // Chop off ending /
        if (plainLink.endsWith("/")) {
            plainLink = plainLink.substring(1, plainLink.length() - 1);
        }

        try {
            // Parse link
            StringTokenizer nizer = new StringTokenizer(plainLink, "|");
            // Get type
            String type = nizer.nextToken();

            if ("folder".equalsIgnoreCase(type)) {
                // Decode the url form
                String name = Util.decodeFromURL(nizer.nextToken());
                boolean secret = nizer.nextToken().equalsIgnoreCase("s");
                String id = Util.decodeFromURL(nizer.nextToken());
                FolderInfo folder = new FolderInfo(name, id, secret);

                // Parse optional folder infos
                if (nizer.hasMoreElements()) {
                    try {
                        folder.bytesTotal = Long.parseLong(nizer.nextToken());
                        if (nizer.hasMoreElements()) {
                            folder.filesCount = Integer.parseInt(nizer
                                .nextToken());
                        }
                    } catch (NumberFormatException e) {
                        log().verbose(
                            "Unable to parse additonal folder info from link. "
                                + link);
                    }
                }

                Invitation invitation = new Invitation(folder, null);
                getController().getFolderRepository().invitationReceived(
                    invitation, false, true);
            } else if ("file".equalsIgnoreCase(type)) {
                // Decode the url form
                String name = Util.decodeFromURL(nizer.nextToken());
                boolean secret = nizer.nextToken().equalsIgnoreCase("s");
                String id = Util.decodeFromURL(nizer.nextToken());
                FolderInfo folder = new FolderInfo(name, id, secret);

                String filename = Util.decodeFromURL(nizer.nextToken());
                FileInfo fInfo = new FileInfo(folder, filename);

                // FIXME: Show warning/join panel if not on folder
                
                // Enqueue for download
                getController().getTransferManager().downloadNewestVersion(
                    fInfo);
            }
        } catch (NoSuchElementException e) {
            log().error("Illegal link '" + link + "'");
        }
    }

    /**
     * Opens a file and processes its content
     * 
     * @param file
     */
    private void openFile(File file) {
        if (!file.exists()) {
            log().warn("File not found " + file.getAbsolutePath());
            return;
        }

        if (file.getName().endsWith(".invitation")) {
            // Load invitation file
            Invitation invitation = Util.loadInvitation(file);
            if (invitation != null) {
                getController().getFolderRepository().invitationReceived(
                    invitation, false, true);
            }
        } else if (file.getName().endsWith(".nodes")) {
            // Load nodes file
            MemberInfo[] nodes = loadNodesFile(file);
            // Enqueue new nodes
            if (nodes != null) {
                getController().getNodeManager().queueNewNodes(nodes);
            }
        }
    }

    /**
     * Tries to load a list of nodes from a nodes file. Returns null if wasn't
     * able to read the file
     * 
     * @param filename
     * @return
     */
    private MemberInfo[] loadNodesFile(File file) {
        try {
            InputStream fIn = new BufferedInputStream(new FileInputStream(file));
            ObjectInputStream oIn = new ObjectInputStream(fIn);
            // Load nodes
            List nodes = (List) oIn.readObject();

            log().warn("Loaded " + nodes.size() + " nodes");
            MemberInfo[] nodesArrary = new MemberInfo[nodes.size()];
            nodes.toArray(nodesArrary);

            return nodesArrary;
        } catch (IOException e) {
            log().error("Unable to load nodes from file '" + file + "'.", e);
        } catch (ClassCastException e) {
            log().error("Illegal format of nodes file '" + file + "'.", e);
        } catch (ClassNotFoundException e) {
            log().error("Illegal format of nodes file '" + file + "'.", e);
        }

        return null;
    }
}

