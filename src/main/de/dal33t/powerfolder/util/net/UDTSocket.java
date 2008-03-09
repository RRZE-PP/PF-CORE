package de.dal33t.powerfolder.util.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.os.OSUtil;

/**
 * Basic wrapper for a UDT socket in java.
 * Currently it's not possible to adjust the buffer sizes used in the native implementation in java.
 * For this reason, expect alot of memory usage from creating such a socket. <br>
 * Actually, the amount used should be around 22MB per socket (if one looks at the default buffer sizes).
 * @author Dennis "Bytekeeper" Waldherr
 *
 */
public class UDTSocket {
	private InputStream in;
	private OutputStream out;
	private static Logger LOG = Logger.getLogger(UDTSocket.class);
	
	// Used in native code!
	@SuppressWarnings("unused")
	private int sock = -1;
	
	private volatile boolean connected = false;
	private volatile boolean closed = false;
	
	private static boolean supported = false;
	
	static {
		if (OSUtil.loadLibrary(LOG, "UDT") && OSUtil.loadLibrary(LOG, "UDT4J")) {
			initIDs();
			supported = true;
		}
	}
	
	public UDTSocket() {
		sock = socket();
	}
	
	// Used in native code!
	@SuppressWarnings("unused")
	private UDTSocket(int sock) {
		this.sock = sock;
		connected = true;
	}

	/**
	 * Returns true if UDTSockets are supported. 
	 */
	public static boolean isSupported() {
		return supported;
	}

	
	public InputStream getInputStream() throws IOException {
		if (in == null) {
			in = new UDTInputStream();
		}
		return in;
	}
	
	public OutputStream getOutputStream() throws IOException {
		if (out == null) {
			out = new UDTOutputStream();
		}
		return out;
	}

	/**
	 * FIXME
	 * Returns true if the socket is closed
	 * @return
	 */
	public boolean isClosed() {
		return closed;
	}
	
	/**
	 * FIXME
	 * Returns true if the socket is connected
	 * @return
	 */
	public boolean isConnected() {
		return connected;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (sock != -1) {
			close();
		}
	}

	public void connect(InetSocketAddress endPoint) throws IOException {
		connectImpl(endPoint);
		// If no exception occurred, we're now connected
		connected = true;
	}
	
	public void close() throws IOException {
		// Whatever happens in the actual native code - we can't use this object anymore
		closed = true;
		connected = false;
		closeImpl();
	}
	
	@Override
	public String toString() {
	    return "{UDTSocket: closed: " + closed + ", connected:" + connected + ", fd:" + sock + "}";
	}
	
	public native UDTSocket accept() throws IOException;
	public native void listen(int backlog) throws IOException;
	public native void bind(InetSocketAddress bindPoint) throws IOException;
	public native InetSocketAddress getLocalAddress();
	public native InetSocketAddress getRemoteAddress();
	public native boolean getSoRendezvous();
	public native void setSoRendezvous(boolean enabled);
	
	private native void closeImpl() throws IOException;
	private native void connectImpl(InetSocketAddress endPoint) throws IOException;
	private native int recv(byte[] buffer, int off, int len) throws IOException;
	private native void send(byte[] buffer, int off, int len) throws IOException;

	/**
	 * Initializes access IDs in JNI wrapper
	 */
	private native static void initIDs();
	
	/**
	 * Allocates a new UDT Socket.
	 */
	private native static int socket();

	private class UDTInputStream extends InputStream {

		@Override
		public int read() throws IOException {
			byte b[] = new byte[1];
			try {
				return recv(b, 0, 1);
			} catch (IOException e) {
				connected = false;
				throw e;
			}
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException(b + " from:" + off + " len:" + len);
			}
			try {
				return recv(b, off, len);
			} catch (IOException e) {
				connected = false;
				throw e;
			}
		}
	}
	
	private class UDTOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			byte buf[] = new byte[] { (byte) b };
			try {
				send(buf, 0, 1);
			} catch (IOException e) {
				connected = false;
				throw e;
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (off < 0 || len < 0 || len > b.length - off) {
				throw new IndexOutOfBoundsException(b + " from:" + off + " len:" + len);
			}
			try {
				send(b, off, len);
			} catch (IOException e) {
				connected = false;
				throw e;
			}
		}
		
	}
}
