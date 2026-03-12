package gymmanagement.util;
import java.sql.*;

public class dbconnection {
    private static final String URL = "jdbc:mysql://localhost:3306/gymdb2";
    private static final String USER = "root";
    private static final String PASSWORD = "Akshay07@123"; // change as needed

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}