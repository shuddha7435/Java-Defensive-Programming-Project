import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A pseudo FTP server. Accepts client connection and initiates a session.
 * 
 * @author Arun Seelagan
 * @author Matthew Butler
 * @since Jan 18, 2007
 */
// change here
public class FTPServer{
	protected int port;
	protected String fileBase;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	/**
	 * Default constructor.
	 * 
	 * @param port
	 *            Port which the server will listen on.
	 * @param fileBase
	 *            The path to the directory from which files will be served
	 */
	// Change here - protected
	protected FTPServer(final int port, final String fileBase) {
		this.port = port;
		this.fileBase = fileBase;
	}

	// ----------------------------------------------------------------------
	// PRIVATE METHODS
	// ----------------------------------------------------------------------

	/**
	 * Prints usage message to the console.
	 */
	//final keyword is added here
	final protected static void printUsage() {
		System.out.printf("Usage: FTPServer -p <port> [options] %n%n");
		System.out.printf("\tWhere valid options include: %n");
		System.out.printf("\t\t -h \t Prints usage %n");
		System.out.printf("\t\t -d \t File directory %n");
	}

	// ----------------------------------------------------------------------
	// PUBLIC METHODS
	// ----------------------------------------------------------------------

	/**
	 * Performs a passive open and accepts a client. ServerSession objects are
	 * instantiated to serve client requests.
	 */
	//protected added here
	protected void acceptClients() {
		try {
			// initiate server socket
			System.out.printf("Creating socket ... ");
			ServerSocket serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(FTPApplication.SOCKET_TIMEOUT);
			System.out.printf("Bound to %s:%s %n", InetAddress.getLocalHost(),
					serverSocket.getLocalPort());

			// listen for clients
			System.out.printf("Listening for connections ... ");
			Socket clientSocket = serverSocket.accept();
			System.out.printf("Accepted client from %s%n", clientSocket);

			// SERVE THE CLIENT
			ServerSession session = new ServerSession(clientSocket, fileBase);
			session.serveClient();

			// clean up
			System.out.printf("Closing socket ... ");
			serverSocket.close();
			System.out.printf("done.");

		} catch (IOException e) {
			System.err.printf("%s Error with listening%n", e);
		}
	}

	/**
	 * Driver for the FTPServer.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		int port = -1;
		String directory = System.getProperty(FTPApplication.DEFAULT_FILEBASE);

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
						File path = new File(args[++index]);
						if (path.exists()) {
							directory = args[index];
						} else {
							System.err.println("Invalid directory");
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
		if (helpRequested || port == 0) {
			printUsage();
		} else {
			FTPServer server = new FTPServer(port, directory);
			server.acceptClients();
		}
	}

}
