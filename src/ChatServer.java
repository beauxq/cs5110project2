import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class ChatServer {
    // protocol constants
    private final static int PORT = 8888;
    private final static String ENDING_MESSAGE = "exit";

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
            new ConnectionThread(connectionSocket).start();
            break;
        }

        /* old code
        // input and output streams
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
        DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

        System.out.println("Connection received from: " + connectionSocket.getInetAddress());

        while(!Objects.equals(stringFromClient, ENDING_MESSAGE)) {

            stringFromClient = inFromClient.readLine();

            System.out.println(stringFromClient + " from Client-Doug");

            responseString = stringFromClient + '\n';

            outToClient.writeBytes(responseString);
        }
        System.out.println("Connection ended");

        connectionSocket.close();
        serverSocket.close();
        */
    }
}
