package gymmanagement.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import gymmanagement.util.dbconnection;

public class adminpaymentpage extends JFrame {
    private JTextField txtMemberId;
    private JLabel lblMemberName, lblAmountDue, lblStatus;
    private int foundMemberId = -1;

    public adminpaymentpage() {
        setTitle("Send Payment Notification");
        setSize(440, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 3, true),
                BorderFactory.createEmptyBorder(28, 44, 28, 44)
        ));
        panel.setBackground(new Color(245, 250, 255));

        JLabel heading = new JLabel("Send Payment Notification");
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setForeground(new Color(33, 150, 243));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        txtMemberId = new JTextField();
        txtMemberId.setMaximumSize(new Dimension(200, 28));
        txtMemberId.setToolTipText("Enter Member ID");
        txtMemberId.setFont(new Font("Arial", Font.PLAIN, 15));
        txtMemberId.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnSearch = new JButton("Search Member");
        btnSearch.setBackground(new Color(33, 150, 243));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Arial", Font.BOLD, 15));
        btnSearch.setFocusPainted(false);
        btnSearch.setAlignmentX(CENTER_ALIGNMENT);
        btnSearch.addActionListener(e -> searchMember());

        lblMemberName = new JLabel(" ");
        lblMemberName.setFont(new Font("Arial", Font.BOLD, 16));
        lblMemberName.setForeground(new Color(33, 150, 243));
        lblMemberName.setAlignmentX(CENTER_ALIGNMENT);

        lblAmountDue = new JLabel("Amount Due: ");
        lblAmountDue.setFont(new Font("Arial", Font.BOLD, 15));
        lblAmountDue.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnNotify = new JButton("Send Notification");
        btnNotify.setBackground(new Color(33, 150, 243));
        btnNotify.setForeground(Color.WHITE);
        btnNotify.setFont(new Font("Arial", Font.BOLD, 16));
        btnNotify.setFocusPainted(false);
        btnNotify.setAlignmentX(CENTER_ALIGNMENT);
        btnNotify.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        btnNotify.addActionListener(e -> sendNotification());

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        panel.add(heading);
        panel.add(Box.createVerticalStrut(16));
        panel.add(new JLabel("Enter Member ID:"));
        panel.add(txtMemberId);
        panel.add(Box.createVerticalStrut(8));
        panel.add(btnSearch);
        panel.add(Box.createVerticalStrut(12));
        panel.add(lblMemberName);
        panel.add(Box.createVerticalStrut(6));
        panel.add(lblAmountDue);
        panel.add(Box.createVerticalStrut(14));
        panel.add(btnNotify);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblStatus);

        setContentPane(panel);
    }

    private void searchMember() {
        lblStatus.setText(" ");
        lblMemberName.setText(" ");
        lblAmountDue.setText("Amount Due: ");
        foundMemberId = -1;

        String idStr = txtMemberId.getText().trim();
        if (!idStr.matches("\\d+")) {
            lblStatus.setText("Please enter a valid numeric Member ID.");
            return;
        }
        int memberId = Integer.parseInt(idStr);

        try (Connection con = dbconnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement(
                    "SELECT name, amount FROM members WHERE id=?");
            pst.setInt(1, memberId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                foundMemberId = memberId;
                String name = rs.getString("name");
                int amount = rs.getInt("amount");
                lblMemberName.setText("Member Name: " + name);

                // Check if paid this month
                PreparedStatement payPst = con.prepareStatement(
                        "SELECT * FROM payments WHERE member_id=? AND MONTH(date)=MONTH(CURRENT_DATE())");
                payPst.setInt(1, memberId);
                ResultSet payRs = payPst.executeQuery();
                if (payRs.next()) {
                    lblAmountDue.setText("Amount Due: ₹0 (Paid)");
                } else {
                    lblAmountDue.setText("Amount Due: ₹" + amount + " (Pending)");
                }
            } else {
                lblStatus.setText("Member not found.");
            }
        } catch (SQLException ex) {
            lblStatus.setText("DB Error: " + ex.getMessage());
        }
    }

    private void sendNotification() {
        lblStatus.setForeground(Color.RED);
        if (foundMemberId == -1) {
            lblStatus.setText("Search for a valid Member ID first.");
            return;
        }

        // Check if already sent notification this month
        try (Connection con = dbconnection.getConnection()) {
            String checkSql = "SELECT * FROM notifications WHERE member_id=? AND MONTH(date)=MONTH(CURRENT_DATE())";
            PreparedStatement checkPst = con.prepareStatement(checkSql);
            checkPst.setInt(1, foundMemberId);
            ResultSet rs = checkPst.executeQuery();
            if (rs.next()) {
                lblStatus.setText("Notification already sent to this member for this month.");
                return;
            }

            // Send notification
            String sql = "INSERT INTO notifications (member_id, message, date) VALUES (?, ?, NOW())";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, foundMemberId);
            pst.setString(2, "Please pay your gym bill for this month.");
            pst.executeUpdate();

            lblStatus.setForeground(new Color(0,120,0));
            lblStatus.setText("Notification sent!");
        } catch (SQLException ex) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Error: " + ex.getMessage());
        }
    }
}