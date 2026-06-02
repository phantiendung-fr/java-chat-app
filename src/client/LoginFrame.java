package client;

import common.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;

public class LoginFrame extends JFrame {

    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton loginButton;
    private JButton registerButton;
    private JTextArea statusArea;
    private ClientConnection connection;
    private DefaultListModel<String> serverModel;
    private JList<String> serverList;
    private static final String SERVER_FILE = "data/servers.txt";

    public LoginFrame() {
        connection = new ClientConnection();
        setTitle("Chat Client - Login");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
    }

    private void initUI() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        serverModel = new DefaultListModel<>();
        serverList = new JList<>(serverModel);

        loadServers();

        serverList.addListSelectionListener(e -> selectServer());

        JPanel serverPanel = new JPanel(new BorderLayout());
        serverPanel.setBorder(BorderFactory.createTitledBorder("Danh sách server"));
        serverPanel.add(new JScrollPane(serverList), BorderLayout.CENTER);

        JPanel managePanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Thêm");
        JButton editButton = new JButton("Sửa");
        JButton deleteButton = new JButton("Xóa");

        managePanel.add(addButton);
        managePanel.add(editButton);
        managePanel.add(deleteButton);
        serverPanel.add(managePanel, BorderLayout.SOUTH);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        formPanel.add(new JLabel("Server IP:"));

        hostField = new JTextField("127.0.0.1");
        formPanel.add(hostField);

        formPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345");
        formPanel.add(portField);

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        connectButton = new JButton("Connect");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);

        buttonPanel.add(connectButton);
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        statusArea = new JTextArea(10, 40);
        statusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(statusArea);

        mainPanel.add(serverPanel, BorderLayout.WEST);
        mainPanel.add(formPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        add(mainPanel);

        connectButton.addActionListener(e -> connectToServer());
        registerButton.addActionListener(e -> register());
        loginButton.addActionListener(e -> login());
        addButton.addActionListener(e -> addServer());
        editButton.addActionListener(e -> editServer());
        deleteButton.addActionListener(e -> deleteServer());

    }

    private void loadServers() {

        File file = new File(SERVER_FILE);
        if (!file.exists()) {
            serverModel.addElement("127.0.0.1:12345");
            saveServers();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    serverModel.addElement(line.trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Không đọc được file server config.");
        }

    }

    private void saveServers() {

        File file = new File(SERVER_FILE);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < serverModel.size(); i++) {
                writer.write(serverModel.get(i));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Không lưu được file server config.");
        }

    }

    private void selectServer() {

        String server = serverList.getSelectedValue();
        if (server == null) {
            return;
        }

        String[] parts = server.split(":");
        if (parts.length == 2) {
            hostField.setText(parts[0]);
            portField.setText(parts[1]);
        }

    }

    private String inputServer(String oldValue) {
        return JOptionPane.showInputDialog(this, "Nhập server dạng IP:Port", oldValue);
    }

    private void addServer() {
        String server = inputServer("127.0.0.1:12345");
        if (server != null && server.contains(":")) {
            serverModel.addElement(server.trim());
            saveServers();
        }
    }

    private void editServer() {

        int index = serverList.getSelectedIndex();
        if (index < 0) {
            return;
        }

        String server = inputServer(serverModel.get(index));
        if (server != null && server.contains(":")) {
            serverModel.set(index, server.trim());
            saveServers();
        }

    }

    private void deleteServer() {
        int index = serverList.getSelectedIndex();
        if (index >= 0) {
            serverModel.remove(index);
            saveServers();
        }
    }

    private void connectToServer() {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        boolean success = connection.connect(host, port);
        if (success) {

            appendStatus("Kết nối server thành công.");

            connectButton.setEnabled(false);
            hostField.setEnabled(false);
            portField.setEnabled(false);
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);

        } else {
            appendStatus("Không kết nối được server. Hãy mở Server trước.");
        }
    }

    private void register() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Message message = new Message(
                MessageType.REGISTER,
                username,
                "",
                password
        );
        
        connection.send(message);
        Message response = connection.receive();
        appendStatus(response.getContent());

    }

    private void login() {
        
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        Message message = new Message(
                MessageType.LOGIN,
                username,
                "",
                password
        );
        
        connection.send(message);
        Message response = connection.receive();
        appendStatus(response.getContent());

        if (response.getType() == MessageType.SERVER_MESSAGE) {
            ChatFrame chatFrame = new ChatFrame(connection, username,
                    hostField.getText().trim() + ":" + portField.getText().trim());
            chatFrame.setVisible(true);
            this.dispose();
        }
        
    }

    private void appendStatus(String text) {
        statusArea.append(text + "\n");
    }

}
