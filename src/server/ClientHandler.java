package server;

import common.*;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    
    private final Socket socket;
    private final ServerFrame serverFrame;
    private final UserStore userStore;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;

    public ClientHandler(Socket socket, ServerFrame serverFrame, UserStore userStore) {
        this.socket = socket;
        this.serverFrame = serverFrame;
        this.userStore = userStore;
        this.username = null;
    }

    @Override
    public void run() {
        try {
            
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            
            while (true) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }

        } catch (Exception e) {
            disconnect();
        }
    }

    private void handleMessage(Message message) {

        if (message == null) {
            return;
        }

        MessageType type = message.getType();
        if (type == MessageType.REGISTER) {
            handleRegister(message);
        } else if (type == MessageType.LOGIN) {
            handleLogin(message);
        } else if (type == MessageType.LOGOUT) {
            disconnect();
        } else if (type == MessageType.PRIVATE_CHAT) {
            handlePrivateChat(message);
        } else if (type == MessageType.GROUP_CHAT) {
            handleGroupChat(message);
        } else {
            send(new Message(MessageType.ERROR, "SERVER", message.getSender(), "Loại tin nhắn chưa được hỗ trợ"));
        }

    }

    private void handleRegister(Message message) {
        
        String newUsername = message.getSender();
        String password = message.getContent();

        if (newUsername == null || newUsername.trim().isEmpty()) {
            send(new Message(MessageType.ERROR, "SERVER", newUsername, "Username không được rỗng"));
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            send(new Message(MessageType.ERROR, "SERVER", newUsername,  "Password không được rỗng"));
            return;
        }

        boolean success = userStore.register(newUsername, password);
        if (success) {
            send(new Message(MessageType.SERVER_MESSAGE, "SERVER", newUsername, "Đăng ký thành công"));
            serverFrame.log("Registered new user: " + newUsername);
        } else {
            send(new Message(MessageType.ERROR, "SERVER", newUsername, "Username đã tồn tại"));
        }
        
    }

    private void handleLogin(Message message) {
        
        String loginUsername = message.getSender();
        String password = message.getContent();
        boolean valid = userStore.login(loginUsername, password);
        if (!valid) {
            send(new Message(MessageType.ERROR, "SERVER", loginUsername, "Sai username hoặc password"));
            return;
        }

        if (serverFrame.isUserOnline(loginUsername)) {
            send(new Message(MessageType.ERROR, "SERVER", loginUsername, "User này đang online"));
            return;
        }

        username = loginUsername;
        serverFrame.addClient(username, this);
        send(new Message(MessageType.SERVER_MESSAGE, "SERVER", username, "Đăng nhập thành công"));
        serverFrame.broadcastOnlineUsers();
        serverFrame.log(username + " logged in");
    }

    private void handlePrivateChat(Message message) {
        
        if (username == null) {
            send(new Message(MessageType.ERROR, "SERVER", "", "Bạn chưa đăng nhập"));
            return;
        }

        String receiver = message.getReceiver();
        ClientHandler receiverHandler = serverFrame.getClient(receiver);
        if (receiverHandler == null) {
            send(new Message(MessageType.ERROR, "SERVER", username, "User không online"));
            return;
        }
        receiverHandler.send(message);

        send(message);
        serverFrame.log(message.getSender() + " -> " + message.getReceiver() + ": " + message.getContent());
    }

    private void handleGroupChat(Message message) {
        if (username == null) {
            send(new Message(MessageType.ERROR, "SERVER", "", "Bạn chưa đăng nhập"));
            return;
        }

        String receivers = message.getReceiver();
        if (receivers == null || receivers.trim().isEmpty()) {
            send(new Message(MessageType.ERROR, "SERVER", username, "Group không có thành viên" ));
            return;
        }

        String[] userList = receivers.split(",");
        for (String user : userList) {
            String targetUser = user.trim();
            ClientHandler handler = serverFrame.getClient(targetUser);
            if (handler != null) {
                handler.send(message);
            }
        }

        send(message);
        serverFrame.log("[GROUP] " + message.getSender() + ": " + message.getContent());
    }

    public void send(Message message) {
        try {
            if (output != null) {
                output.writeObject(message);
                output.flush();
            }
        } catch (IOException e) {
            disconnect();
        }
    }

    private void disconnect() {
        try {

            if (username != null) {
                serverFrame.removeClient(username);
                serverFrame.broadcastOnlineUsers();
                serverFrame.log(username + " disconnected");
                username = null;
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}