package client;

import common.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class ChatFrame extends JFrame {
    
    private ClientConnection connection;
    private String username;
    private DefaultListModel<String> onlineModel;
    private JList<String> onlineList;
    private JTextArea inputArea;
    private JButton sendButton;
    private JLabel chattingWithLabel;
    private String selectedUser;
    private Map<String, List<Message>> privateMessages;
    private Map<String, JTextArea> chatAreas;
    private JTabbedPane chatTabs;
    private JCheckBox enterToSendBox;
    private String serverInfo;

    public ChatFrame(ClientConnection connection, String username, String serverInfo) {

        this.connection = connection;
        this.username = username;
        this.selectedUser = null;
        this.privateMessages = new HashMap<>();
        this.chatAreas = new HashMap<>();
        this.serverInfo = serverInfo;

        setTitle("Chat - " + username + " | Server: " + serverInfo);
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initUI();
        startReceiverThread();

    }

    private void initUI() {

        onlineModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineModel);

        JScrollPane onlineScroll = new JScrollPane(onlineList);
        onlineScroll.setPreferredSize(new Dimension(180, 0));
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JLabel("Online Users", SwingConstants.CENTER), BorderLayout.NORTH);
        leftPanel.add(onlineScroll, BorderLayout.CENTER);

        chattingWithLabel = new JLabel("Chưa chọn người chat", SwingConstants.CENTER);

        chatTabs = new JTabbedPane();
        JPanel centerPanel = new JPanel(new BorderLayout());

        JPanel infoPanel = new JPanel(new GridLayout(2, 1));
        infoPanel.add(chattingWithLabel);
        infoPanel.add(new JLabel("Server đang kết nối: " + serverInfo, SwingConstants.CENTER));
        centerPanel.add(infoPanel, BorderLayout.NORTH);
        centerPanel.add(chatTabs, BorderLayout.CENTER);

        inputArea = new JTextArea(4, 20);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);

        sendButton = new JButton("Send");
        JButton fileButton = new JButton("Gửi file");
        JButton groupButton = new JButton("Tạo group");
        enterToSendBox = new JCheckBox("Enter để gửi", true);

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] emojis = {"😀", "😂", "😍", "😢", "😡", "👍", "❤️"};
        for (String emoji : emojis) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.addActionListener(e -> insertEmoji(emoji));
            emojiPanel.add(emojiButton);
        }
        emojiPanel.add(fileButton);
        emojiPanel.add(groupButton);
        emojiPanel.add(enterToSendBox);

        bottomPanel.add(emojiPanel, BorderLayout.NORTH);
        bottomPanel.add(inputScroll, BorderLayout.CENTER);

        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        onlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedUser = onlineList.getSelectedValue();
                if (selectedUser != null) {
                    chattingWithLabel.setText("Đang chat với: " + selectedUser);
                    openChatTab(selectedUser);
                    renderConversation(selectedUser);
                }
            }
        });

        sendButton.addActionListener(e -> sendPrivateMessage());
        fileButton.addActionListener(e -> sendFile());
        groupButton.addActionListener(e -> createGroup());

        chatTabs.addChangeListener(e -> {
            int index = chatTabs.getSelectedIndex();
            if (index >= 0) {
                selectedUser = chatTabs.getTitleAt(index);
                chattingWithLabel.setText("Đang chat với: " + selectedUser);
            }
        });

        setupEnterToSend();

    }

    private void createGroup() {
        String users = JOptionPane.showInputDialog(this, "Nhập username trong group, cách nhau bằng dấu phẩy:");
        if (users == null || users.trim().isEmpty()) {
            return;
        }

        selectedUser = "Group: " + users.trim();
        openChatTab(selectedUser);

    }

    private void sendFile() {

        if (selectedUser == null || selectedUser.startsWith("Group: ")) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một user để gửi file.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();

        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            connection.send(new Message(MessageType.FILE, username, selectedUser, "Đã gửi file", file.getName(), data));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không đọc được file.");
        }

    }

    private void insertEmoji(String emoji) {
        inputArea.replaceSelection(emoji);
        inputArea.requestFocusInWindow();
    }

    private void setupEnterToSend() {

        InputMap inputMap = inputArea.getInputMap();
        ActionMap actionMap = inputArea.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
        actionMap.put("sendMessage", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (enterToSendBox.isSelected()) {
                    sendPrivateMessage();
                } else {
                    inputArea.replaceSelection("\n");
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insertLineBreak");
        actionMap.put("insertLineBreak", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputArea.replaceSelection("\n");
            }
        });
    
    }

    private void sendPrivateMessage() {

        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn user online để chat.");
            return;
        }

        if (selectedUser.equals(username)) {
            JOptionPane.showMessageDialog(this, "Không thể chat với chính mình.");
            return;
        }

        String content = inputArea.getText();
        if (content.trim().isEmpty()) {
            return;
        }

        MessageType type = selectedUser.startsWith("Group: ")
                ? MessageType.GROUP_CHAT : MessageType.PRIVATE_CHAT;
        String receiver = type == MessageType.GROUP_CHAT
                ? selectedUser.substring("Group: ".length()) : selectedUser;
        Message message = new Message(type, username, receiver, content);
        connection.send(message);
        inputArea.setText("");

    }

    private void startReceiverThread() {
        
        Thread thread = new Thread(() -> {
            while (connection.isConnected()) {
                Message message = connection.receive();
                if (message == null) {
                    break;
                }

                handleIncomingMessage(message);
            }
        });

        thread.start();

    }

    private void handleIncomingMessage(Message message) {
        if (message.getType() == MessageType.ONLINE_USERS) {
            updateOnlineUsers(message.getContent());
        } else if (message.getType() == MessageType.PRIVATE_CHAT) {
            savePrivateMessage(message);
        } else if (message.getType() == MessageType.GROUP_CHAT) {
            saveGroupMessage(message);
        } else if (message.getType() == MessageType.FILE) {
            receiveFile(message);
        } else if (message.getType() == MessageType.SERVER_MESSAGE) {
            appendSystemMessage("[SERVER] " + message.getContent());
        } else if (message.getType() == MessageType.ERROR) {
            appendSystemMessage("[ERROR] " + message.getContent());
        }
    }

    private void saveGroupMessage(Message message) {
        String group = "Group: " + message.getReceiver();
        privateMessages.putIfAbsent(group, new ArrayList<>());
        privateMessages.get(group).add(message);
        openChatTab(group);
        renderConversation(group);
    }

    private void receiveFile(Message message) {
        File folder = new File(AppPaths.getPath("downloads"));
        folder.mkdirs();
        File file = new File(folder, message.getFileName());
        try {
            java.nio.file.Files.write(file.toPath(), message.getFileData());
            appendSystemMessage("[FILE] " + message.getSender() + ": " + file.getPath());
        } catch (IOException e) {
            appendSystemMessage("[ERROR] Không lưu được file " + message.getFileName());
        }
    }

    private void savePrivateMessage(Message message) {

        String otherUser;
        if (message.getSender().equals(username)) {
            otherUser = message.getReceiver();
        } else {
            otherUser = message.getSender();
        }

        privateMessages.putIfAbsent(otherUser, new ArrayList<>());
        privateMessages.get(otherUser).add(message);

        openChatTab(otherUser);
        renderConversation(otherUser);

    }

    private void renderConversation(String otherUser) {
        SwingUtilities.invokeLater(() -> {

            JTextArea area = chatAreas.get(otherUser);
            if (area == null) {
                return;
            }

            area.setText("");
            List<Message> messages = privateMessages.get(otherUser);
            if (messages == null) {
                return;
            }

            for (Message msg : messages) {
                area.append(msg.getSender() + ":\n");
                area.append(msg.getContent() + "\n\n");
            }

            area.setCaretPosition(area.getDocument().getLength());

        });
    }

    private void openChatTab(String name) {

        Runnable openTab = () -> {

            JTextArea area = chatAreas.get(name);
            if (area == null) {
                area = new JTextArea();
                area.setEditable(false);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                chatAreas.put(name, area);
                chatTabs.addTab(name, new JScrollPane(area));
            }

            chatTabs.setSelectedComponent(area.getParent().getParent());
            selectedUser = name;
            chattingWithLabel.setText("Đang chat với: " + name);

        };

        if (SwingUtilities.isEventDispatchThread()) {
            openTab.run();
        } else {
            SwingUtilities.invokeLater(openTab);
        }

    }

    private void updateOnlineUsers(String usersText) {

        SwingUtilities.invokeLater(() -> {
            String oldSelectedUser = selectedUser;
            onlineModel.clear();
            
            if (usersText == null || usersText.trim().isEmpty()) {
                return;
            }

            String[] users = usersText.split(",");
            for (String user : users) {
                String cleanUser = user.trim();
                if (!cleanUser.isEmpty()) {
                    onlineModel.addElement(cleanUser);
                }
            }

            if (oldSelectedUser != null) {
                onlineList.setSelectedValue(oldSelectedUser, true);
            }

        });
    }

    private void appendSystemMessage(String text) {
        
        SwingUtilities.invokeLater(() -> {
            if (selectedUser == null) {
                return;
            }
            openChatTab(selectedUser);
            JTextArea area = chatAreas.get(selectedUser);
            area.append(text + "\n");
            area.setCaretPosition(area.getDocument().getLength());
        });
    }
    
}
