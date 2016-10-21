import java.io.*;
import java.net.Socket;

public class ConnectionThread extends Thread {
    // chat protocol constants
    private final static String ENDING_MESSAGE = "exit";
    private final static int MESSAGE_ACKNOWLEDGEMENT = 1;

    protected Socket socket;

    public ConnectionThread(Socket clientSocket) {
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
                    socket.close();
                    return;
                } else {
                    System.out.println(socket.getInetAddress() + ": " + inputLineFromClient);
                    outToClient.writeByte(MESSAGE_ACKNOWLEDGEMENT);
                    outToClient.flush();
                }
            } catch (IOException e) {
                System.out.println("Error receiving message from client: " + e.getMessage());
                return;
            }
        }
    }
}
