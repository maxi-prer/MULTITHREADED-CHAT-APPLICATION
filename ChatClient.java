import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField, usernameField;
    private JButton sendButton, connectButton, quitButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ChatClient() {
        frame = new JFrame("Client Chat");
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Top panel: username entry and buttons
        JPanel topPanel = new JPanel(new BorderLayout());
        usernameField = new JTextField("User1");
        connectButton = new JButton("Connect");
        quitButton = new JButton("Quit");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(connectButton);
        buttonPanel.add(quitButton);

        topPanel.add(new JLabel("Username:"), BorderLayout.WEST);
        topPanel.add(usernameField, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // Center panel: chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        // Bottom panel: input and send button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Add panels to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        connectButton.addActionListener(e -> connect());
        quitButton.addActionListener(e -> {
            disconnect();
            System.exit(0);
        });
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        frame.setVisible(true);
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            username = usernameField.getText().trim();
            if (username.isEmpty()) {
                username = "Anonymous";
            }

            out.println(username);  // Send username to server
            chatArea.append("[Client]: Connected as " + username + "\n");

            // Disable editing of username after connecting
            usernameField.setEditable(false);
            connectButton.setEnabled(false);

            // Thread to receive messages
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        chatArea.append(msg + "\n");
                    }
                } catch (IOException ex) {
                    chatArea.append("[Client]: Disconnected from server.\n");
                }
            }).start();

        } catch (IOException ex) {
            chatArea.append("[Client]: Error connecting to server.\n");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}
