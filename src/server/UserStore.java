package server;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class UserStore {
    private final File userFile;
    private final Map<String, String> users;

    private void loadUsers() {
        try {

            File parentFolder = userFile.getParentFile();

            if (parentFolder != null) {
                parentFolder.mkdirs();
            }
            if (!userFile.exists()) {
                userFile.createNewFile();
            }

            BufferedReader reader = new BufferedReader(new FileReader(userFile));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];

                    users.put(username, password);
                }
            }

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public UserStore(String filePath) {
        userFile = new File(filePath);
        users = new HashMap<>();

        loadUsers();
    }

    private void saveUsers() {

        try {

            BufferedWriter writer = new BufferedWriter( new FileWriter(userFile));
            for (String username : users.keySet()) {
                String password = users.get(username);
                writer.write(username + "|" + password);
                writer.newLine();
            }

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean register(String username, String password) {

        if (users.containsKey(username)) {
            return false;
        }
        
        users.put(username, password);
        saveUsers();

        return true;
    }

    public synchronized boolean login(String username, String password) {
        
        if (!users.containsKey(username)) {
            return false;
        }
        
        return users.get(username).equals(password);

    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}
