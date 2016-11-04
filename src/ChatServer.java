import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    // protocol constants
    private final static int PORT = 8888;

    private static class ConnectionThread extends Thread {
        // chat protocol constants
        private final static String ENDING_MESSAGE = "exit";

        Socket socket;

        ConnectionThread(Socket clientSocket) {
            this.socket = clientSocket;
        }

        public void run() {
            InputStream inputStreamFromClient;
            BufferedReader bufferedReaderInFromClient;
            DataOutputStream outToClient;
            try {
                inputStreamFromClient = socket.getInputStream();
                bufferedReaderInFromClient = new BufferedReader(new InputStreamReader(inputStreamFromClient));
                outToClient = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("Error: opening streams to client: " + e.getMessage());
                return;
            }
            System.out.println(socket.getInetAddress() + " connected");

            String inputLineFromClient;
            while (true) {
                try {
                    inputLineFromClient = bufferedReaderInFromClient.readLine();
                    if ((inputLineFromClient == null) || inputLineFromClient.equalsIgnoreCase(ENDING_MESSAGE)) {
                        String displayString = socket.getInetAddress() + ": client exited " + '\n';
                        System.out.print(displayString);
                        return;
                    } else {
                        String displayString = socket.getInetAddress() + ": " + inputLineFromClient + '\n';
                        System.out.print(displayString);
                        outToClient.writeBytes(displayString);
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving message from client: " + e.getMessage());
                    return;
                }
            }
        }
    }


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
            // connectionThread.join();  // wait for connection to end
            // break;  // stop after first connection ends - for part A
        }

        // serverSocket.close(); // sever will keep one if one client exits
    }
}
