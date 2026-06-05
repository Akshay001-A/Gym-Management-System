package gymmanagement.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.FileInputStream;

public class dbconnection {

    public static Connection getConnection() throws SQLException {

        try {

            System.out.println("Looking for config file at:");
            System.out.println(new java.io.File("config.properties").getAbsolutePath());

            Properties prop = new Properties();
            FileInputStream fis = new FileInputStream("config.properties");
            prop.load(fis);

            String url = prop.getProperty("db.url");
            String user = prop.getProperty("db.user");
            String password = prop.getProperty("db.password");

            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            System.out.println("MySQL Driver Loaded Successfully");

            return DriverManager.getConnection(url, user, password);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}