import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.StringTokenizer;

/**
 * A pseudo-FTP client. Connects to server and enters a control loop which
 * enables it to query the server.
 * 
 * @author Arun Seelagan
 * @author Matthew Butler
 * @since Jan 24, 2007
 */
public class FTPClient extends FTPApplication {
	protected final int port;
	protected final InetAddress ip;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	/**
	 * Default constructor.
	 * 
	 * @param port
	 *            The port to connect to on the server.
	 * @param ip
	 *            The server's IP address
	 * @param fileBase
	 *            The directory from which the client will read and write.
	 */
	// Protected added here instead of public 
	
	protected FTPClient(final int port, final InetAddress ip, final String fileBase) {
		super(fileBase);
		this.port = port;
		this.ip = ip;
		System.out.printf("Working out of %s%n", this.fileBase);
	}

	// ----------------------------------------------------------------------
	// PRIVATE METHODS
	// ----------------------------------------------------------------------

	/**
	 * Prints usage message to the console.
	 */
	private static void printUsage() {
		System.out.printf("Usage: FTPClient -i <ip> -p <port> [options]%n%n");
		System.out.printf("\tWhere valid options include: %n");
		System.out.printf("\t\t -h \t Prints usage %n");
		System.out.printf("\t\t -d \t File directory %n");
	}

	// ----------------------------------------------------------------------
	// INHERITED, PROTECTED METHODS
	// ----------------------------------------------------------------------

	/**
	 * Checks that the specified file is valid, then transmits the file's name,
	 * size, and data. After transmission, the client waits for a response from
	 * the server.
	 * 
	 * @param filename
	 *            Name of the file to send.
	 */
	protected void handlePut(final String filename) {
		// confirm existence of file
		final File file = new File(getFilePath(filename));
		if (!file.exists()) {
			System.err.printf("%s does not exist.%n", file);
			handleOther(false);
		} else {
			try {
				// send filename
				System.out.printf("Sending filename ... ");
				sendMessage(MessageFormat.format("{0} {1}", PUT, filename));
				System.out.printf("done.%n");

				// send data
				final byte[] data = readFile(file);
				sendData(data);

				// await reply
				System.out.printf("Server reply: ");
				String reply = receiveMessage();
				System.out.printf("%s%n", reply);
			} catch (IOException ioe) {
				System.err.printf("%s No reply from server%n", ioe); // change made here
			}
		}
	}

	/**
	 * Sends the request to the server, receives the length of the file (if it
	 * exists), and receives the file's data.
	 * 
	 * @param filename
	 *            Name of the file to receive.
	 */
	protected void handleGet(final String filename) {
		try {
			// request file
			sendMessage(MessageFormat.format("{0} {1}", GET, filename));

			// receive file length
			System.out.printf("Receiving length ... ");
			final String input = receiveMessage();
			//final long length = (input != null) ? Long.parseLong(input) : ERROR;
			System.out.printf("done.%n");

			// in case the file doesn't exist on the server side

				// GET DATA
				final byte[] data = receiveData();

				// SAVE DATA
				storeFile(data, filename);


		} catch (IOException e) {
			System.err.printf("%s File does not exist%n", e);
		}
	}

	/**
	 * Sends the listing request to the server, receives the amount of files
	 * there are, and receives each filename.
	 */
	protected void handleLs() {
		try {
			// request listing
			sendMessage(LS);

			// receive number of files
			final String input = receiveMessage();
			//final int fileAmt = input != null ? Integer.parseInt(input) : ERROR;

			// receive and print names of files
			if (input != null) {
				for (int i = 0; i < Integer.parseInt(input); i++) {
					System.out.printf("\t%s%n", receiveMessage());
				}
			}

		} catch (IOException e) {
			System.err.printf("%s Error with request listening%n", e);
			//e.printStackTrace();
		}
	}

	/**
	 * Models receiving a null or invalid command
	 * 
	 * @param invalidCmd
	 *            If true, prints message stating that an invalid cmd was
	 *            received.
	 */
	final protected void handleOther(final boolean invalidCmd) {
		sendMessage("%n");
		if (invalidCmd) {
			System.err.printf("Invalid command.%n");
		}
	}

	/**
	 * Sends an exit signal to the server and terminates the connection.
	 */
	final protected void handleExit() {
		sendMessage(EXIT);
		exitRecieved = true;
		terminate();
	}

	// ----------------------------------------------------------------------
	// PUBLIC METHODS
	// ----------------------------------------------------------------------

	/**
	 * Establish connection with server and create I/O objects.
	 * 
	 * @return true if connection was established successfully.
	 */
	// Private added instead of public 
	final private boolean connect() {
		boolean success = false;

		try {
			System.out.printf("Connecting to %s:%d ... ", ip, port);
			socket  =  new Socket(ip, port);
			setUpIO();
			success = true;
			System.out.printf("Established.%n");

		} catch (IOException e) {
			System.err.printf("%s printStactTrace%n", e);
		}

		return success;
	}

	/**
	 * Begins loop that allows client to send commands to the server.
	 */
	// Private added instead of public 
	
	final private void queryServer() {
		if (lineIn == null || lineOut == null) {
			System.err.printf("Cannot query server, IO has not been set up%n");
		} else {
			try {
				String serverReply;
				String userInput;
				StringTokenizer args;
				final InputStreamReader input = new InputStreamReader(System.in,"UTF-8");
				final BufferedReader console = new BufferedReader(input);

				// client-side control loop
				while (!exitRecieved) {
					// Wait for PROMPT
					serverReply = receiveMessage();
					System.out.printf("%s", serverReply);

					// ACCEPT & PROCESS USER INPUT
					userInput = console.readLine();
					userInput = userInput == null ? "" : userInput;
					args = new StringTokenizer(userInput);
					processCommand(args);
				}

				// clean up
				console.close();
				input.close();

			} catch (IOException e) {
				System.err.printf("%s File not found%n", e);
			}
		}
	}

	/**
	 * Driver for FTPClient
	 * 
	 * @param args
	 *            Command-line arguments
	 */
	// Protected added instead of public 
	
	public static void main(final String[] args) {
		int port = -1;
		String directory = System.getProperty(FTPApplication.DEFAULT_FILEBASE); 
		InetAddress ip = null;

		// Process arguments
		boolean helpRequested = false;
		for (int index = 0; !helpRequested && index < args.length; index++) {
			if (args[index].length() == 2 && args[index].charAt(0) == '-') {
				switch (args[index].charAt(1)) {
				case 'p':
					if (index + 1 >= args.length) {
						System.err.println("Port number expected.");
						helpRequested = true;
					} else {
						port = Integer.parseInt(args[++index]);
					}
					break;
				case 'h':
					helpRequested = true;
					break;
				case 'd':
					if (index + 1 >= args.length) {
						System.err.println("Directory expected.");
						helpRequested = true;
					} else {
						final File path = new File(args[++index]);
						if (path.exists()) {
							directory = args[index];
						} else {
							System.err.println("Invalid directory");
							helpRequested = true;
						}
					}
					break;
				case 'i':
					if (index + 1 >= args.length) {
						System.err.println("IP address expected.");
						helpRequested = true;
					} else {
						try {
							ip = InetAddress.getByName(args[++index]);
						} catch (UnknownHostException uhe) {
							System.err.printf("Bad address: %s%n", uhe);
							helpRequested = true;
						}
					}
					break;
				default:
					helpRequested = true;
					break;
				}
			} else {
				helpRequested = true;
			}
		}

		// Begin execution
		if (helpRequested || port == 0 || ip == null) {
			printUsage();
		} else {
			final FTPClient client = new FTPClient(port, ip, directory);
			if (client.connect()) {
				client.queryServer();
			}
		}
	}

}
