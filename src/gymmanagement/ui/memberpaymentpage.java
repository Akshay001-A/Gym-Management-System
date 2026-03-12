package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class memberpaymentpage extends JFrame {
    private int memberId;
    private JLabel lblStatus, lblPaid;
    
    // Utility to load and scale icons
    private ImageIcon getIcon(String path, int w, int h) {
        try {
            Image img = new ImageIcon(path).getImage();
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }

    public memberpaymentpage(int memberId) {
        this.memberId = memberId;

        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}

        setTitle("Member - Make Payment");
        setSize(400, 270);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0,120,0), 3),
            BorderFactory.createEmptyBorder(20, 34, 20, 34)
        ));
        panel.setBackground(new Color(235,255,235));

        // Payment icon at the top
        JLabel iconLabel = new JLabel(getIcon("assets/payment.png", 54, 54));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Pay Membership Fee");
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        lblPaid = new JLabel(paymentStatus());
        lblPaid.setFont(new Font("Arial", Font.BOLD, 15));
        lblPaid.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnPay = new JButton("Pay Now", getIcon("assets/paynow.png.png", 22, 22));
        btnPay.setBackground(new Color(76, 175, 80));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(new Font("Arial", Font.BOLD, 16));
        btnPay.setFocusPainted(false);
        btnPay.setAlignmentX(CENTER_ALIGNMENT);
        btnPay.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnPay.setIconTextGap(10);
        btnPay.addActionListener(e -> pay());

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 13));
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(14));
        panel.add(lblPaid);
        panel.add(Box.createVerticalStrut(16));
        panel.add(btnPay);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblStatus);

        setContentPane(panel);
    }

    private String paymentStatus() {
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT * FROM payments WHERE member_id=? AND MONTH(date)=MONTH(CURRENT_DATE())";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, memberId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return "<html><span style='color:#4caf50;'>Status: Paid for this month. &#10004;</span></html>";
            }
        } catch (SQLException ex) {
            return "<html><span style='color:#e53935;'>Error fetching payment.</span></html>";
        }
        return "<html><span style='color:#e65100;'>Status: Payment pending! &#9888;</span></html>";
    }

    private void pay() {
        if (paymentStatus().contains("Paid")) {
            lblStatus.setForeground(new Color(0,120,0));
            lblStatus.setText("Already paid for this month.");
            return;
        }
        try (Connection con = dbconnection.getConnection()) {
            // Insert payment
            String sql = "INSERT INTO payments (member_id, amount, date) VALUES (?, 6000, NOW())";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, memberId);
            pst.executeUpdate();

            // DELETE notification for this member for this month
            String deleteNotifSql = "DELETE FROM notifications WHERE member_id=? AND MONTH(date)=MONTH(CURRENT_DATE())";
            PreparedStatement delNotifPst = con.prepareStatement(deleteNotifSql);
            delNotifPst.setInt(1, memberId);
            delNotifPst.executeUpdate();

            lblStatus.setForeground(new Color(0,120,0));
            lblStatus.setText("Payment successful! Any notifications cleared.");
            lblPaid.setText(paymentStatus());
        } catch (SQLException ex) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Error: " + ex.getMessage());
        }
    }
}