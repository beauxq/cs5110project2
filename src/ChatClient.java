import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

public class ChatClient {
    private final static String SERVER_ADDRESS = "localhost";

    // protocol constants
    private final static int PORT = 8888;
    private final static String ENDING_MESSAGE = "exit";
    private final static int MESSAGE_ACKNOWLEDGEMENT = 1;

    // sockets and streams
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private BufferedReader inFromConsole;

    public static void main(String argv[]) throws Exception
    {
        ChatClient chatClient = new ChatClient();

        chatClient.connectSocketAndStream();

        chatClient.inputLoop();
    }

    private void connectSocketAndStream() throws IOException {
        // make socket connection to server
        try {
            clientSocket = new Socket(SERVER_ADDRESS, PORT);
        }
        catch (ConnectException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            return;
        }

        // make input and output streams
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        inFromConsole = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Connection established to: " + SERVER_ADDRESS + ":" + PORT);
    }

    private void inputLoop() throws IOException {
        String stringToSend = "";
        while (!Objects.equals(stringToSend, ENDING_MESSAGE)) {
            System.out.print("message to send (\"" + ENDING_MESSAGE + "\" to end): ");
            stringToSend = inFromConsole.readLine();

            try {
                outToServer.writeBytes(stringToSend + '\n');
            }
            catch (SocketException e) {
                System.out.println("Unable to send data to server: " + e.getMessage());
                return;
            }

            if (!Objects.equals(stringToSend, ENDING_MESSAGE) &&
                    inFromServer.read() != MESSAGE_ACKNOWLEDGEMENT) {
                System.out.println("Error: acknowledgement not received");
            }
        }

        clientSocket.close();
    }
}
