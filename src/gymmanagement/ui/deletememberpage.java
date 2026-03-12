package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class deletememberpage extends JFrame {
    private JTextField txtId, txtName, txtAge, txtMobile, txtGymTime, txtEmail, txtAadhaar, txtGender, txtFather;
    private JButton btnSearch, btnDelete;
    private JLabel lblStatus;

    public deletememberpage() {
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}

        setTitle("Delete Member");
        setSize(470, 570);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 53, 69), 3, true),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));
        mainPanel.setBackground(new Color(255, 245, 245));

        JLabel heading = new JLabel("Delete Member");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(220, 53, 69));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtId = new JTextField();
        txtName = new JTextField(); txtAge = new JTextField();
        txtMobile = new JTextField(); txtGymTime = new JTextField();
        txtEmail = new JTextField(); txtAadhaar = new JTextField();
        txtGender = new JTextField(); txtFather = new JTextField();

        String[] labels = {"Member ID:", "Name:", "Age:", "Mobile:", "Gym Time:", "Email:", "Aadhaar:", "Gender:", "Father's Name:"};
        JTextField[] fields = {txtId, txtName, txtAge, txtMobile, txtGymTime, txtEmail, txtAadhaar, txtGender, txtFather};
        for (int i = 0; i < labels.length; i++) {
            gbc.gridy = i; gbc.gridx = 0;
            formPanel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            fields[i].setPreferredSize(new Dimension(180, 28));
            fields[i].setEditable(false); // Always view-only
            formPanel.add(fields[i], gbc);
        }

        btnSearch = new JButton("Search");
        btnSearch.setBackground(new Color(220, 53, 69));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridy = 0; gbc.gridx = 2;
        formPanel.add(btnSearch, gbc);

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(220, 53, 69));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(mainPanel.getBackground());
        btnPanel.add(btnDelete);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(new Color(220, 53, 69));
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        mainPanel.add(heading);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(lblStatus);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(btnPanel);

        setContentPane(mainPanel);

        btnSearch.addActionListener(e -> searchMember());
        btnDelete.addActionListener(e -> deleteMember());

        txtId.setEditable(true);
    }

    private void searchMember() {
        lblStatus.setText(" ");
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT * FROM members WHERE id=?";
            PreparedStatement pst = con.prepareStatement(sql);
            String idText = txtId.getText().trim();
            if (!idText.matches("\\d+")) {
                lblStatus.setText("Enter a valid Member ID.");
                clearFieldsExceptId();
                return;
            }
            pst.setInt(1, Integer.parseInt(idText));
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                txtName.setText(rs.getString("name"));
                txtAge.setText(rs.getString("age"));
                txtMobile.setText(rs.getString("mobile"));
                txtGymTime.setText(rs.getString("gym_time"));
                txtEmail.setText(rs.getString("email"));
                txtAadhaar.setText(rs.getString("aadhaar"));
                txtGender.setText(rs.getString("gender"));
                txtFather.setText(rs.getString("father_name"));
                lblStatus.setForeground(new Color(220, 53, 69));
                lblStatus.setText("Member found. Please confirm delete.");
            } else {
                lblStatus.setForeground(new Color(220, 53, 69));
                lblStatus.setText("Member not found!");
                clearFieldsExceptId();
            }
        } catch (SQLException ex) {
            lblStatus.setText("Database Error: " + ex.getMessage());
        }
    }

    private void deleteMember() {
        String idText = txtId.getText().trim();
        if (!idText.matches("\\d+")) {
            lblStatus.setText("Enter a valid Member ID.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this member?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = dbconnection.getConnection()) {
            String sql = "DELETE FROM members WHERE id=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(idText));
            int affected = pst.executeUpdate();
            if (affected > 0) {
                lblStatus.setForeground(new Color(0, 128, 0));
                lblStatus.setText("Member deleted successfully!");
                clearFieldsExceptId();
            } else {
                lblStatus.setForeground(new Color(220, 53, 69));
                lblStatus.setText("Delete failed!");
            }
        } catch (SQLException ex) {
            lblStatus.setForeground(new Color(220, 53, 69));
            lblStatus.setText("Database Error: " + ex.getMessage());
        }
    }

    private void clearFieldsExceptId() {
        txtName.setText("");
        txtAge.setText("");
        txtMobile.setText("");
        txtGymTime.setText("");
        txtEmail.setText("");
        txtAadhaar.setText("");
        txtGender.setText("");
        txtFather.setText("");
    }
}