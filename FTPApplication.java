import java.io.*;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.StringTokenizer;

/**
 * Models FTP commands in a modular, extendible way. This abstract class
 * provides several methods used by both the client and server.
 *
 * @author Arun Seelagan
 * @author Matthew Butler
 * @since Jan 23, 2007
 */
     abstract public class FTPApplication {
	/* constants common to the client and server */
	protected static final int SOCKET_TIMEOUT = 300000; // change-protected
	protected static final int ERROR = -1;  // change-protected
	protected static final String DEFAULT_FILEBASE = "user.dir"; // change-protected

	/* FTP commands */
	protected static final String PUT = "put"; // change-protected
	protected static final String GET = "get"; // change-protected
	protected static final String LS = "ls"; // change-protected
	protected static final String EXIT = "exit"; // change-protected

	/* Path to the directory from which to serve or store files */
	protected static  String fileBase;

	/* Network I/O objects used for sending/receiving data */
	protected static Socket socket;
	protected static BufferedReader lineIn;
	protected static PrintWriter lineOut;

	/* Flag used to break session loop */
	protected static boolean exitRecieved = false;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	/**
	 * Default constructor. Establishes file base.
	 *
	 * @param filebase
	 *            The directory from which to read & write files.
	 */
	/**
	 final - before String
	 Private - Access Modifiers
	 */

	/**
	 protected added before FTPApplication
	 */
	protected  FTPApplication(final String filebase) {
		try {
			final File path = new File(filebase);
			this.fileBase = path.getCanonicalPath() + File.separator;
		} catch (IOException ioe) {
			this.fileBase = "";
			System.err.printf("Could not access %s: %s", filebase, ioe);
		}
	}

	// ----------------------------------------------------------------------
	// PROTECTED METHODS
	// ----------------------------------------------------------------------

	/**
	 * Sets a timeout on the socket and instantiates I/O objects.
	 *
	 * @throws IOException
	 */
	protected void setUpIO() throws IOException {
		if (socket != null) {
			socket.setSoTimeout(SOCKET_TIMEOUT);
			final InputStreamReader  input = new InputStreamReader(
					socket.getInputStream(),"UTF-8");
			lineIn = new BufferedReader(input);
			lineOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")
			, true);
		} else {
			System.err.println("Cannot set up IO, socket is null.%n");
		}
	}

	/**
	 * Closes the socket and its associated I/O objects.
	 */

	/**
	 * Final - Should add or not?
	 */
	protected void terminate() {
		try {
			System.out.printf("Terminating session ... ");
			if (lineIn != null) {
				lineIn.close();
			}

			if (lineOut != null) {
				lineOut.flush();
				lineOut.close();
			}

			if (socket != null) {
				socket.close();
			}

			System.out.printf("done.%n");

		} catch (IOException ioe) {
			System.err.printf("I/O Error terminating session: %s%n", ioe);
		}
	}

	/**
	 * Calls the proper method based on user input.
	 *
	 * @param args
	 *            User arguments
	 */
	protected void processCommand(final StringTokenizer args) {
		if (args.hasMoreTokens()) {
			final String command = args.nextToken();
			if (command.equalsIgnoreCase(PUT)) {
				final String file = args.hasMoreTokens() ? args.nextToken() : null;
				handlePut(file);
			} else if (command.equalsIgnoreCase(GET)) {
				final String file = args.hasMoreTokens() ? args.nextToken() : null;
				handleGet(file);
			} else if (command.equalsIgnoreCase(LS)) {
				handleLs();
			} else if (command.equalsIgnoreCase(EXIT)) {
				handleExit();
			} else {
				handleOther(true);
			}
		} else {
			handleOther(false);
		}
	}

	/**
	 * Returns the path to the given file by prepending the filebase.
	 *
	 * @param file
	 *            target filename
	 * @return full local path to file
	 */
	final protected String getFilePath(final String file) {
		return MessageFormat.format("{0}{1}", fileBase, file);
	}

	/**
	 * Read the given file from disk into a byte array.
	 *
	 * @param file
	 *            the target file
	 * @return byte array containing file contents
	 */
	 protected byte[] readFile(final File file) {
		 byte[] data = new byte[(int) file.length()];

		 FileInputStream fis=null;
		try {
			try {
				fis = new FileInputStream(file);
				System.out.printf("Loading %s ... ", file);

				int amt = fis.read(data, 0, data.length);
				System.out.printf("done (%d bytes).%n", amt);
			}finally {
				fis.close();
				System.err.printf("Connecting is closing");
			}

		} catch (IOException ioe) {
			System.err.printf("Error reading file: %s%n", ioe);
		}

		return data;
	}

	/**
	 * Writes a file to the filebase.
	 *
	 * @param data
	 *            Raw bytes of a file
	 * @param filename
	 *            the name to give the new file.
	 * @throws IOException
	 */
	protected void storeFile(final byte[] data, final String filename) throws IOException {
		final String toWrite = getFilePath(filename);
		System.out.printf("Storing file at %s... ", toWrite);
		final FileOutputStream fileOut = new FileOutputStream(toWrite);
		fileOut.write(data);
		fileOut.flush();
		fileOut.close();
		System.out.printf("done.%n");
	}

	/**
	 * Receives a byte[] as individual ints.
	 * 
	 * @return a byte[] value
	 */
	protected byte[] receiveData() throws IOException {
		// get amount of bytes expected
		int byteAmt =  Integer.parseInt(lineIn.readLine());
		byte[] toReturn = new byte[byteAmt];

		// receive the bytes
		for (int i = 0; i < byteAmt; i++) {
			toReturn[i] = Integer.valueOf(lineIn.readLine()).byteValue();
		}

		return toReturn;
	}

	/**
	 * Transmits bytes one by one as ints.
	 * 
	 * @param bytes
	 *            a byte[] value
	 */
	protected void sendData(final byte[] bytes) {
		// tell receiver # of bytes to expect
		lineOut.println(bytes.length);

		// send each byte as ints, one-by-one
		for (final byte aB : bytes) {
			lineOut.println(Byte.valueOf(aB).intValue());
		}
	}

	/**
	 * Sends the string to over the socket.
	 * 
	 * @param message
	 */
	protected final void  sendMessage(final String message) {
		try{
			sendData(message.getBytes("UTF-8"));
		}catch (UnsupportedEncodingException e )
		{

		}

	}

	/**
	 * Receives a string from the socket.
	 * 
	 * @return string received
	 */
	protected String receiveMessage() throws IOException {
		return new String(receiveData(),"UTF-8");
	}

	// ----------------------------------------------------------------------
	// ABSTRACT METHODS - inherited & implemented by client and server
	// ----------------------------------------------------------------------

	/**
	 * Uploads a client-side file to the server.
	 * 
	 * @param filename
	 */
	abstract protected void handlePut(final String filename);

	/**
	 * Downloads a file from the server
	 * 
	 * @param filename
	 */
	abstract protected void handleGet(final String filename);

	/**
	 * Lists the files available on the server.
	 */
	abstract protected void handleLs();

	/**
	 * Ends an FTP session, tearing down the server-client connection.
	 */
	abstract protected void handleExit();

	/**
	 * Models receiving bad input.
	 * 
	 * @param invalidCmd
	 */
	abstract protected void handleOther(final boolean invalidCmd);

}
