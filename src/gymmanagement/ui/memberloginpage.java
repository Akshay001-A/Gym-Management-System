package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class memberloginpage extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblStatus;

    // Utility method for loading and scaling icons
    private ImageIcon getIcon(String path, int width, int height) {
        try {
            Image img = new ImageIcon(path).getImage();
            return new ImageIcon(img.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }

    public memberloginpage() {
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}
        setTitle("Member Login");
        setSize(390, 340);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0,120,0), 3),
            BorderFactory.createEmptyBorder(22, 34, 22, 34)
        ));
        panel.setBackground(new Color(235,255,235));

        // Header with user icon
        JPanel headingPanel = new JPanel();
        headingPanel.setOpaque(false);
        headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));

        JLabel iconLabel = new JLabel(getIcon("assets/user_login.png.png", 56, 56));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Member Login");
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        headingPanel.add(iconLabel);
        headingPanel.add(Box.createVerticalStrut(4));
        headingPanel.add(heading);

        txtUsername = new JTextField();
        txtUsername.setToolTipText("Member username");
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 15));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        txtPassword = new JPasswordField();
        txtPassword.setToolTipText("Member password");
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 15));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 13));
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnLogin = new JButton("Login", getIcon("assets/login_button.png.png", 22, 22));
        btnLogin.setBackground(new Color(76, 175, 80));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFont(new Font("Arial", Font.BOLD, 16));
        btnLogin.setFocusPainted(false);
        btnLogin.setAlignmentX(CENTER_ALIGNMENT);
        btnLogin.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnLogin.setIconTextGap(10);
        btnLogin.addActionListener(e -> login());

        panel.add(headingPanel);
        panel.add(Box.createVerticalStrut(14));
        panel.add(new JLabel("Username:"));
        panel.add(txtUsername);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Password:"));
        panel.add(txtPassword);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblStatus);
        panel.add(Box.createVerticalStrut(12));
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
            String sql = "SELECT * FROM users WHERE username=? AND password=? AND role='member'";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int memberId = rs.getInt("member_id");
                new memberdashboard(memberId, username).setVisible(true);
                dispose();
            } else {
                lblStatus.setText("Invalid credentials or not member.");
            }
        } catch (SQLException ex) {
            lblStatus.setText("DB Error: " + ex.getMessage());
        }
    }
}