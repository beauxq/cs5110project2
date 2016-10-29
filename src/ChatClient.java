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

    private final static String PROMPT = "message to send (\"" + ENDING_MESSAGE + "\" to end): ";
    private final static String DEFAULT_SERVER_ADDRESS = "localhost";
    private final String serverAddress;

    // sockets and streams
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private ConsoleReader consoleReader;
    private ReceiveFromServerThread.BottomConsoleLineInformation bottomLineInfo;

    private final Lock mutex;

    private ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;
        this.mutex = new ReentrantLock();
        this.bottomLineInfo = new ReceiveFromServerThread.BottomConsoleLineInformation();
    }

    private static class ReceiveFromServerThread extends Thread {
        private BufferedReader inFromServer;
        private final Lock mutex;

        private static class BottomConsoleLineInformation {
            String bottomLine;
            int previousBottomLineLength;

            BottomConsoleLineInformation() {
                bottomLine = "";
                previousBottomLineLength = 0;
            }
        }
        private BottomConsoleLineInformation bottomLineInfo;

        ReceiveFromServerThread(BufferedReader inFromServer,
                                Lock mutex,
                                BottomConsoleLineInformation bottomLineInfo) {
            this.inFromServer = inFromServer;
            this.mutex = mutex;
            this.bottomLineInfo = bottomLineInfo;
        }

        public void run() {
            String receivedLine;
            while (true) {
                try {
                    receivedLine = inFromServer.readLine();
                    if (receivedLine == null) {
                        throw new IOException("reading from the server returned null");
                    }
                    insertLine(receivedLine);
                } catch (IOException | NullPointerException e) {
                    System.out.println("\nconnection closed");
                    return;
                }
            }
        }

        /**
         * puts output to the console above the current line
         * @param output the message to be put on the screen
         * @throws IOException exception
         */
        private void insertLine(String output) throws IOException {
            mutex.lock();

            // cursor to beginning of line
            System.out.print('\r');

            // print what we want to print
            System.out.print(output);

            // erase any extra stuff at the end of the line
            for (int i = output.length();
                 i < Math.max(bottomLineInfo.bottomLine.length(), bottomLineInfo.previousBottomLineLength);
                 ++i) {
                System.out.print(' ');
            }

            // move to the next line
            System.out.print('\n');

            // print the bottom line
            System.out.print(bottomLineInfo.bottomLine);

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

        // run console input loop
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
            mutex.lock();
            bottomLineInfo.bottomLine = PROMPT;
            System.out.print('\r');
            System.out.print(bottomLineInfo.bottomLine);
            mutex.unlock();

            while (bottomLineInfo.bottomLine.charAt(bottomLineInfo.bottomLine.length() - 1) != '\n') {
                int charRead = consoleReader.readVirtualKey();

                mutex.lock();
                if (charRead == 10) {  // enter key
                    bottomLineInfo.bottomLine += '\n';
                }
                else if (charRead == 8) {  // backspace
                    if (bottomLineInfo.bottomLine.length() > PROMPT.length()) {
                        bottomLineInfo.bottomLine =
                                bottomLineInfo.bottomLine.substring(0, bottomLineInfo.bottomLine.length() - 1);

                        // erase the character that is there
                        System.out.print('\r');
                        System.out.print(bottomLineInfo.bottomLine);

                        // put the cursor at the end of the line
                        System.out.print(" \r");
                        System.out.print(bottomLineInfo.bottomLine);
                    }
                }
                else if (charRead >= 32) {  // printable character
                    // TODO: either restrict the length to one line, or handle multiple lines
                    bottomLineInfo.bottomLine += (char)charRead;
                    System.out.print((char)charRead);
                }
                mutex.unlock();
            }

            mutex.lock();
            stringToSend = bottomLineInfo.bottomLine.substring(PROMPT.length());  // This has '\n' at end
            bottomLineInfo.previousBottomLineLength = bottomLineInfo.bottomLine.length();

            try {
                outToServer.writeBytes(stringToSend);
                mutex.unlock();
            }
            catch (SocketException e) {
                clientSocket.close();
                System.out.println("Unable to send data to server: " + e.getMessage());
                mutex.unlock();
                return;
            }

            // remove '\n' from the end
            stringToSend = stringToSend.substring(0, stringToSend.length() - 1);
        }

        clientSocket.close();
    }

    private void runReceivingThread() {
        new ReceiveFromServerThread(inFromServer, mutex, bottomLineInfo).start();
    }
}
