package common;
import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private byte[] fileData;
    private String fileName;

    public Message(MessageType type, String sender, String receiver, String content) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public Message(MessageType type, String sender, String receiver, String content, String fileName, byte[] fileData) {
        this(type, sender, receiver, content);
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public MessageType getType() {
        return type;
    }
    public void setType(MessageType type) {
        this.type = type;
    }


    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }
    

    public String getReceiver() {
        return receiver;
    }
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }


    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public String getFileName() {
        return fileName;
    }

}
