package server;

import common.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerFrame extends JFrame {

    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private DefaultListModel<String> onlineModel;
    private JList<String> onlineList;
    private ServerSocket serverSocket;
    private boolean running;
    private final Map<String, ClientHandler> onlineClients;
    private final UserStore userStore;

    public ServerFrame() {

        onlineClients = new ConcurrentHashMap<>();
        userStore = new UserStore("data/users.txt");
        running = false;

        setTitle("Chat Server");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
    }

    private void initUI() {
        
        JPanel topPanel = new JPanel(new FlowLayout());
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("12345", 10);
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        topPanel.add(portLabel);
        topPanel.add(portField);
        topPanel.add(startButton);
        topPanel.add(stopButton);

        logArea = new JTextArea();
        logArea.setEditable(false);
        
        JScrollPane logScroll = new JScrollPane(logArea);
        onlineModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineModel);

        JScrollPane onlineScroll = new JScrollPane(onlineList);
        onlineScroll.setPreferredSize(new Dimension(200, 0));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JLabel("Online Users", SwingConstants.CENTER), BorderLayout.NORTH);
        rightPanel.add(onlineScroll, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
    }

    private void startServer() {
        try {

            int port = Integer.parseInt(portField.getText().trim());
            serverSocket = new ServerSocket(port);
            running = true;
            
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            portField.setEnabled(false);

            log("Server started on port " + port);
            
            Thread acceptThread = new Thread(() -> {
                while (running) {
                    try {
                        
                        Socket socket = serverSocket.accept();

                        log("New client connected: " + socket.getInetAddress().getHostAddress());

                        ClientHandler handler = new ClientHandler(socket, this, userStore);
                        handler.start();

                    } catch (IOException e) {
                        if (running) {
                            log("Accept client error: " + e.getMessage());
                        }
                    }
                }
            });

            acceptThread.start();

        } catch (Exception e) {
            log("Cannot start server: " + e.getMessage());
        }
    }

    private void stopServer() {
        try {

            running = false;
            if (serverSocket != null) {
                serverSocket.close();
            }

            for (ClientHandler handler : onlineClients.values()) {
                handler.send(new Message(
                        MessageType.SERVER_MESSAGE,
                        "SERVER",
                        "ALL",
                        "Server đã đóng"
                ));
            }

            onlineClients.clear();
            updateOnlineList();

            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            portField.setEnabled(true);

            log("Server stopped");

        } catch (IOException e) {
            log("Stop server error: " + e.getMessage());
        }
    }

    public void addClient(String username, ClientHandler handler) {
        onlineClients.put(username, handler);
        updateOnlineList();
    }

    public void removeClient(String username) {
        onlineClients.remove(username);
        updateOnlineList();
    }

    public boolean isUserOnline(String username) {
        return onlineClients.containsKey(username);
    }

    public ClientHandler getClient(String username) {
        return onlineClients.get(username);
    }

    public void broadcastOnlineUsers() {

        String users = String.join(",", onlineClients.keySet());

        Message message = new Message(
                MessageType.ONLINE_USERS,
                "SERVER",
                "ALL",
                users
        );

        for (ClientHandler handler : onlineClients.values()) {
            handler.send(message);
        }
    }

    private void updateOnlineList() {
        SwingUtilities.invokeLater(() -> {
            onlineModel.clear();
            for (String username : onlineClients.keySet()) {
                onlineModel.addElement(username);
            }
        });
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}