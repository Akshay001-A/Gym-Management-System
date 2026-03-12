package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class membernotificationpage extends JFrame {
    private int memberId;
    private JTextArea txtNotifications;
    private JLabel lblStatus;

    public membernotificationpage(int memberId) {
        this.memberId = memberId;
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}

        setTitle("My Notifications");
        setSize(480, 410);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 250, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 193, 7), 3, true),
                BorderFactory.createEmptyBorder(22, 32, 22, 32)
        ));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("My Notifications", getIcon("assets/notification.png", 32, 32), JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setForeground(new Color(255, 193, 7));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtNotifications = new JTextArea();
        txtNotifications.setEditable(false);
        txtNotifications.setFont(new Font("Arial", Font.PLAIN, 15));
        txtNotifications.setLineWrap(true);
        txtNotifications.setWrapStyleWord(true);
        txtNotifications.setBackground(new Color(255, 249, 196));
        txtNotifications.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(txtNotifications);
        scrollPane.setPreferredSize(new Dimension(400, 190));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 13));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(heading);
        panel.add(Box.createVerticalStrut(14));
        panel.add(scrollPane);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblStatus);

        setContentPane(panel);

        loadNotifications();
    }

    private ImageIcon getIcon(String path, int w, int h) {
        try {
            Image img = new ImageIcon(path).getImage();
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }

    private void loadNotifications() {
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT message, DATE_FORMAT(date, '%d-%b-%Y') as date_sent FROM notifications WHERE member_id=? ORDER BY date DESC";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, memberId);
            ResultSet rs = pst.executeQuery();
            StringBuilder sb = new StringBuilder();
            int count = 0;
            while (rs.next()) {
                count++;
                sb.append("• [").append(rs.getString("date_sent")).append("] ")
                  .append(rs.getString("message")).append("\n\n");
            }
            txtNotifications.setText(count > 0 ? sb.toString() : "No notifications at this time.");
            lblStatus.setText(" ");
        } catch (SQLException ex) {
            lblStatus.setText("Error loading notifications: " + ex.getMessage());
        }
    }
}