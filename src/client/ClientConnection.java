package client;

import common.Message;
import java.io.*;
import java.net.Socket;

public class ClientConnection {

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void send(Message message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message receive() {
        try {
            return (Message)input.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}