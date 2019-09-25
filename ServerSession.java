import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * Models an FTP session with a client. Implements methods declared by
 * FTPApplication from the perspective of an FTP server.
 * 
 * @author Arun Seelagan
 * @author Matthew Butler
 * @since Jan 23, 2007
 */
public class ServerSession extends FTPApplication {
	private static final String PROMPT = "secFTP>";

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	/**
	 * Default constructor. Sets up I/O objects associated with the socket.
	 * 
	 * @param socket
	 *            Network endpoint connected to a client.
	 * @param filePath
	 *            Path to the file base.
	 */
	//protected added
	protected ServerSession(final Socket socket, final String filePath) throws IOException {
		super(filePath);
		System.out.printf("Serving files out of %s. %n", fileBase);
		this.socket = socket;
		setUpIO();
	}

	// ----------------------------------------------------------------------
	// PROTECTED METHODS
	// ----------------------------------------------------------------------

	/**
	 * Receives the name, length, and data of the file to upload. The data
	 * received is written to a file and a status message is sent.
	 * 
	 * @param filename
	 *            the name of the file to upload.
	 */
	protected void handlePut(final String filename) {
		boolean success = false;

		try {
			System.out.printf("Receiving %s%n", filename);

			// GET DATA
			byte[] data = receiveData();

			// SAVE DATA
			if (data != null) {
				storeFile(data, filename);
				success = true;
			}

		} catch (IOException ioe) {
			System.err.printf("I/O error receiving file: %s%n", ioe);
		} catch (NumberFormatException nfe) {
			System.err.printf("Invalid length specified: %s%n", nfe);
		}

		// SEND REPLY
		System.out.printf("Sending reply ... ");
		String message = success ? "PUT OK" : "PUT FAILED";
		sendMessage(message);
		System.out.printf("done.%n");
	}

	/**
	 * Determines if the specified file exists, and sends the file's length and
	 * data.
	 * 
	 * @param filename
	 *            name of the file to transmit.
	 */
	protected void handleGet(final String filename) {
		System.out.printf("Preparing to send %s ... %n", filename);

		// CHECK EXISTANCE OF FILE, AND SEND LENGTH
		File file = new File(getFilePath(filename));
		if (!file.exists()) {
			System.err.printf("%s does not exist.%n", file);
			sendMessage(String.valueOf(ERROR));
		} else {
			// send length
			System.out.printf("Sending length ... ");
			sendMessage(String.valueOf(file.length()));
			System.out.printf("done.%n");

			// send data
			byte[] data = readFile(file);
			sendData(data);
		}
	}

	/**
	 * Determines the available files, then sends the number, followed by names
	 * of them.
	 */
	protected void handleLs() {
		System.out.printf("Listing available files.%n");

		File[] availableFiles = new File(fileBase).listFiles();

		if (availableFiles == null) {
			System.err.printf("%s is not a directory.%n", fileBase);
			sendMessage(String.valueOf(ERROR));
		} else {
			sendMessage(String.valueOf(availableFiles.length));

			/* send each file name */
			for (File file : availableFiles) {
				sendMessage(file.getName());
			}
		}
	}

	/**
	 * Models receiving an invalid or null command: ignore it.
	 * 
	 * @param invalidCmd
	 */
	protected void handleOther(final boolean invalidCmd) {
		// do nothing
	}

	/**
	 * Terminates connection to client, toggles control loop variable.
	 */
	protected void handleExit() {
		exitRecieved = true;
		terminate();
	}

	// ----------------------------------------------------------------------
	// PUBLIC METHODS
	// ----------------------------------------------------------------------

	/**
	 * Initiates the service loop that serves client requests.
	 */
	// protected added here
	protected void serveClient() {
		if (lineIn == null || lineOut == null) {
			System.err.printf("I/O has not been set up.%n");
		} else {
			try {
				String clientRequest;
				StringTokenizer args;

				// control loop, receiving client requests
				while (!exitRecieved) {
					// PRESENT PROMPT
					sendMessage(PROMPT);

					// ACCEPT & PROCESS INPUT
					clientRequest = receiveMessage();
					args = new StringTokenizer(clientRequest);
					processCommand(args);
				}

			} catch (IOException ioe) {
				System.err.printf("IO Error receiving client input: %s%n", ioe);
			}
		}
	}

}
