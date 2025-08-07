import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ChatServer class is responsible for setting up the server,
 * accepting client connections, and managing multiple client threads.
 * It broadcasts messages received from one client to all other connected clients.
 */
public class ChatServer {
    private static final int PORT = 12345; // Port number for the server to listen on
    // Use ConcurrentHashMap for thread-safe access to client handlers
    private static Map<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private static int clientCounter = 0; // Counter for unique client IDs

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat Server started on port " + PORT);

            // Continuously listen for new client connections
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Blocks until a client connects
                clientCounter++;
                String clientId = "User" + clientCounter;
                System.out.println("New client connected: " + clientId + " from " + clientSocket.getInetAddress().getHostAddress());

                // Create a new thread to handle the connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                clientHandlers.put(clientId, clientHandler); // Add handler to the map
                clientHandler.start(); // Start the client handler thread
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message from one client to all other connected clients.
     *
     * @param senderId The ID of the client who sent the message.
     * @param message The message to be broadcasted.
     */
    public static void broadcastMessage(String senderId, String message) {
        String formattedMessage = "[" + senderId + "]: " + message;
        System.out.println("Broadcasting: " + formattedMessage); // Server log

        // Iterate over all client handlers and send the message
        for (ClientHandler handler : clientHandlers.values()) {
            // Do not send the message back to the sender
            if (!handler.getClientId().equals(senderId)) {
                handler.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * Removes a disconnected client handler from the map.
     * This method is called when a client disconnects.
     *
     * @param clientId The ID of the client to be removed.
     */
    public static void removeClient(String clientId) {
        clientHandlers.remove(clientId);
        System.out.println("Client disconnected: " + clientId);
        broadcastMessage("SERVER", clientId + " has left the chat.");
    }

    /**
     * The ClientHandler class represents a thread that manages communication
     * with a single connected client. It reads messages from the client
     * and sends messages to the client.
     */
    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String clientId;

        public ClientHandler(Socket socket, String clientId) {
            this.clientSocket = socket;
            this.clientId = clientId;
            try {
                // Get input and output streams for the client socket
                this.writer = new PrintWriter(clientSocket.getOutputStream(), true); // true for auto-flush
                this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error setting up streams for client " + clientId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        public String getClientId() {
            return clientId;
        }

        /**
         * Sends a message to this specific client.
         *
         * @param message The message to send.
         */
        public void sendMessage(String message) {
            writer.println(message);
        }

        @Override
        public void run() {
            try {
                // Notify all clients that a new user has joined
                broadcastMessage("SERVER", clientId + " has joined the chat.");

                String clientMessage;
                // Continuously read messages from the client
                while ((clientMessage = reader.readLine()) != null) {
                    System.out.println("Received from " + clientId + ": " + clientMessage);
                    // Broadcast the received message to all other clients
                    broadcastMessage(clientId, clientMessage);
                }
            } catch (IOException e) {
                // This usually means the client disconnected unexpectedly
                System.err.println("Client " + clientId + " disconnected unexpectedly: " + e.getMessage());
            } finally {
                // Clean up resources when the client disconnects
                try {
                    if (reader != null) reader.close();
                    if (writer != null) writer.close();
                    if (clientSocket != null) clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing resources for client " + clientId + ": " + e.getMessage());
                }
                // Remove the client from the active handlers list
                ChatServer.removeClient(clientId); // This is the line that was causing the error
            }
        }
    }
}

