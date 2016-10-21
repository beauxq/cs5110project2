import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    // protocol constants
    private final static int PORT = 8888;

    public static void main(String argv[]) throws Exception {

        ServerSocket serverSocket;
        Socket connectionSocket = null;

        try {
            serverSocket = new ServerSocket(PORT);
        }
        catch (BindException e) {
            System.out.println("Unable to start server: " + e.getMessage());
            return;
        }
        System.out.println("Server started on port: " + PORT);

        // wait for connection
        while (true) {
            try {
                connectionSocket = serverSocket.accept();
            }
            catch (IOException e) {
                System.out.println("Error connecting to client: " + e.getMessage());
            }
            ConnectionThread connectionThread = new ConnectionThread(connectionSocket);
            connectionThread.start();
            connectionThread.join();  // wait for connection to end
            break;  // stop after first connection ends - for part A
        }

        serverSocket.close();
    }
}
