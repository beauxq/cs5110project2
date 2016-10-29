import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jline.ConsoleReader;

public class ChatClient {
    // protocol constants
    private final static int PORT = 8888;
    private final static String ENDING_MESSAGE = "exit";
    private final static int MESSAGE_ACKNOWLEDGEMENT = 1;

    private final static String DEFAULT_SERVER_ADDRESS = "localhost";
    private final String serverAddress;

    // sockets and streams
    private Socket clientSocket;
    private DataOutputStream outToServer;

    private BufferedReader inFromServer;
    //private BufferedReader inFromConsole;

    private ConsoleReader consoleReader;
    private String bottomConsoleLine;

    private final Lock mutex;

    private ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;
        mutex = new ReentrantLock();
    }

    private static class ReceiveFromServerThread extends Thread {
        private BufferedReader inFromServer;
        private final Lock mutex;
        private ConsoleReader consoleReader;

        ReceiveFromServerThread(BufferedReader inFromServer, Lock mutex, ConsoleReader consoleReader) {
            this.inFromServer = inFromServer;
            this.mutex = mutex;
            this.consoleReader = consoleReader;
        }

        public void run() {
            String receivedLine;
            while (true) {  // TODO: how to exit this?
                try {
                    receivedLine = inFromServer.readLine();
                    insertLine(receivedLine);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        /**
         * puts output to the console above the current line
         * @param output
         * @throws IOException
         */
        private void insertLine(String output) throws IOException {
            mutex.lock();
            System.out.print('\r' + output + '\n');
            consoleReader.redrawLine();
            mutex.unlock();
        }
    }

    public static void main(String argv[]) throws Exception
    {
        // get server address from command line arguments
        String serverAddress;
        if (argv.length > 0) {
            serverAddress = argv[0];
        }
        else {  // no commandline arguments
            serverAddress = DEFAULT_SERVER_ADDRESS;
        }

        // create instance of client
        ChatClient chatClient = new ChatClient(serverAddress);

        // connect
        chatClient.connectSocketAndStream();

        // run receiving thread
        chatClient.runReceivingThread();

        // run
        chatClient.inputLoop();
    }

    private void connectSocketAndStream() throws IOException {
        // make socket connection to server
        try {
            clientSocket = new Socket(serverAddress, PORT);
        }
        catch (ConnectException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            throw e;
        }

        // make input and output streams
        outToServer = new DataOutputStream(clientSocket.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        consoleReader = new ConsoleReader();
        consoleReader.setInput(System.in);

        System.out.println("Connection established to: " + serverAddress + ":" + PORT);
    }

    private void inputLoop() throws IOException {
        String stringToSend = "";
        while (!Objects.equals(stringToSend, ENDING_MESSAGE)) {
            bottomConsoleLine = "message to send (\"" + ENDING_MESSAGE + "\" to end): ";
            // TODO: System.out.print();
            /*
            while (bottomConsoleLine.charAt(bottomConsoleLine.length() - 1) != '\n') {
                char charRead = consoleReader.getEchoCharacter();
            }
            */

            System.out.println("before readLine");
            stringToSend = consoleReader.readLine(bottomConsoleLine, 'g');
            System.out.println("after readLine");

            try {
                outToServer.writeBytes(stringToSend + '\n');
            }
            catch (SocketException e) {
                clientSocket.close();
                System.out.println("Unable to send data to server: " + e.getMessage());
                return;
            }
        }

        clientSocket.close();
    }

    private void runReceivingThread() {
        new ReceiveFromServerThread(inFromServer, mutex, consoleReader).start();
    }
}
