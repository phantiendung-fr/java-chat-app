package common;

import java.io.File;

public class AppPaths {

    private static final File APP_FOLDER = new File(System.getProperty("user.home"), ".lan-chat");

    public static String getPath(String name) {
        APP_FOLDER.mkdirs();
        return new File(APP_FOLDER, name).getPath();
    }
    
}
