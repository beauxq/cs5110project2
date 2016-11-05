import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {
    // protocol constants
    private final static int PORT = 8888;

    private static class OutToAllClients extends Thread {
        LinkedBlockingQueue<String> messageQueue;
        List<ConnectionThread> clients;
        Lock listLock;
        OutToAllClients(LinkedBlockingQueue<String> messageQueue, List<ConnectionThread> clients, Lock listLock) {
            this.messageQueue = messageQueue;
            this.clients = clients;
            this.listLock = listLock;
        }

        public void run() {
            while (true) {
                String message = null;
                try {
                    message = messageQueue.poll(9999, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                System.out.print(message);
                listLock.lock();
                ListIterator<ConnectionThread> clientIterator = clients.listIterator();
                while (clientIterator.hasNext()) {
                    if (! clientIterator.next().sendMessageToClient(message)) {
                        // if there was an error sending this message,
                        // remove this client from the list of clients
                        clientIterator.remove();
                    }
                }
                listLock.unlock();
            }
        }
    }

    private static class ConnectionThread extends Thread {
        // chat protocol constants
        private final static String ENDING_MESSAGE = "exit";

        Socket socket;
        Queue<String> chatMessageQueue;
        DataOutputStream outToClient;

        ConnectionThread(Socket clientSocket, Queue<String> chatMessageQueue) {
            this.socket = clientSocket;
            this.chatMessageQueue = chatMessageQueue;
            this.outToClient = new DataOutputStream(System.out);
            // messages received between construction and initialization will go to System.out
        }

        /**
         * send a message to the client that this thread is connected to
         * returns whether the message was sent without error
         * @param messageToSend the message to be sent to the client
         * @return whether this client can receive messages
         */
        boolean sendMessageToClient(String messageToSend) {
            try {
                outToClient.writeBytes(messageToSend);
                return true;
            } catch (IOException e) {
                // this client doesn't get messages anymore
                return false;
            }
        }

        public void run() {
            InputStream inputStreamFromClient;
            BufferedReader bufferedReaderInFromClient;
            try {
                inputStreamFromClient = socket.getInputStream();
                bufferedReaderInFromClient = new BufferedReader(new InputStreamReader(inputStreamFromClient));
                outToClient = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                System.out.println("Error: opening streams to client: " + e.getMessage());
                return;
            }
            chatMessageQueue.add(socket.getInetAddress() + " connected\n");

            String inputLineFromClient;
            while (true) {
                try {
                    inputLineFromClient = bufferedReaderInFromClient.readLine();
                    if ((inputLineFromClient == null) || inputLineFromClient.equalsIgnoreCase(ENDING_MESSAGE)) {
                        String displayString = socket.getInetAddress() + ": client exited " + '\n';
                        chatMessageQueue.add(displayString);
                        return;
                    } else {
                        String displayString = socket.getInetAddress() + ": " + inputLineFromClient + '\n';
                        chatMessageQueue.add(displayString);
                        //outToClient.writeBytes(displayString);
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving message from client: " + e.getMessage());
                    return;
                }
            }
        }
    }


    public static void main(String argv[]) throws Exception {

        Lock listLock = new ReentrantLock();
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

        listLock.lock();
        List<ConnectionThread> clients = new LinkedList<>();
        listLock.unlock();
        LinkedBlockingQueue<String> chatMessageQueue = new LinkedBlockingQueue<>();

        new OutToAllClients(chatMessageQueue, clients, listLock).start();

        // wait for connection
        while (true) {
            try {
                connectionSocket = serverSocket.accept();
            }
            catch (IOException e) {
                System.out.println("Error connecting to client: " + e.getMessage());
            }
            ConnectionThread connectionThread = new ConnectionThread(connectionSocket, chatMessageQueue);
            connectionThread.start();
            listLock.lock();
            clients.add(connectionThread);
            listLock.unlock();
            // connectionThread.join();  // wait for connection to end
            // break;  // stop after first connection ends - for part A
        }

        // serverSocket.close(); // sever will keep one if one client exits
    }
}
