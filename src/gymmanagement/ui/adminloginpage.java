package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class adminloginpage extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblStatus;

    public adminloginpage() {
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}
        setTitle("Admin Login");
        setSize(360, 260);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLUE, 3),
            BorderFactory.createEmptyBorder(16, 32, 16, 32)
        ));
        panel.setBackground(new Color(225,240,255));

        JLabel heading = new JLabel("Admin Login");
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        txtUsername = new JTextField();
        txtUsername.setToolTipText("Admin username");
        txtPassword = new JPasswordField();
        txtPassword.setToolTipText("Admin password");

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnLogin = new JButton("Login");
        btnLogin.setBackground(new Color(33, 150, 243));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Arial", Font.BOLD, 16));
        btnLogin.setFocusPainted(false);
        btnLogin.setAlignmentX(CENTER_ALIGNMENT);
        btnLogin.addActionListener(e -> login());

        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Username:"));
        panel.add(txtUsername);
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Password:"));
        panel.add(txtPassword);
        panel.add(Box.createVerticalStrut(8));
        panel.add(lblStatus);
        panel.add(Box.createVerticalStrut(8));
        panel.add(btnLogin);

        setContentPane(panel);
    }

    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Please enter username and password.");
            return;
        }
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE username=? AND password=? AND role='admin'";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                new admindashboard(username).setVisible(true);
                dispose();
            } else {
                lblStatus.setText("Invalid credentials or not admin.");
            }
        } catch (SQLException ex) {
            lblStatus.setText("DB Error: " + ex.getMessage());
        }
    }
}