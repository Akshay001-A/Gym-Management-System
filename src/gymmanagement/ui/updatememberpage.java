package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class updatememberpage extends JFrame {
    private JTextField txtId, txtName, txtAge, txtMobile, txtGymTime, txtEmail, txtAadhaar, txtGender, txtFather;
    private JButton btnSearch, btnUpdate;
    private JLabel lblStatus;

    public updatememberpage() {
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}

        setTitle("Update Member");
        setSize(470, 570);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 3, true),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));
        mainPanel.setBackground(new Color(245, 250, 255));

        JLabel heading = new JLabel("Update Member");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(33, 150, 243));
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
            formPanel.add(fields[i], gbc);
        }

        btnSearch = new JButton("Search");
        btnSearch.setBackground(new Color(33, 150, 243));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridy = 0; gbc.gridx = 2;
        formPanel.add(btnSearch, gbc);

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(33, 150, 243));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setFont(new Font("Arial", Font.BOLD, 14));

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(mainPanel.getBackground());
        btnPanel.add(btnUpdate);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
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
        btnUpdate.addActionListener(e -> updateMember());

        // Only allow editing fields after search
        setFieldsEditable(false);
        txtId.setEditable(true);
    }

    private void setFieldsEditable(boolean editable) {
        txtName.setEditable(editable);
        txtAge.setEditable(editable);
        txtMobile.setEditable(editable);
        txtGymTime.setEditable(editable);
        txtEmail.setEditable(editable);
        txtAadhaar.setEditable(editable);
        txtGender.setEditable(editable);
        txtFather.setEditable(editable);
    }

    private void searchMember() {
        lblStatus.setText(" ");
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT * FROM members WHERE id=?";
            PreparedStatement pst = con.prepareStatement(sql);
            String idText = txtId.getText().trim();
            if (!idText.matches("\\d+")) {
                lblStatus.setText("Enter a valid Member ID.");
                setFieldsEditable(false);
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
                lblStatus.setForeground(new Color(33, 150, 243));
                lblStatus.setText("Member found. You can update details.");
                setFieldsEditable(true);
            } else {
                lblStatus.setForeground(Color.RED);
                lblStatus.setText("Member not found!");
                setFieldsEditable(false);
                clearFieldsExceptId();
            }
        } catch (SQLException ex) {
            lblStatus.setForeground(Color.RED);
            lblStatus.setText("Database Error: " + ex.getMessage());
            setFieldsEditable(false);
        }
    }

    private void updateMember() {
        lblStatus.setForeground(Color.RED);
        if (!validateFields()) return;

        try (Connection con = dbconnection.getConnection()) {
            String sql = "UPDATE members SET name=?, age=?, mobile=?, gym_time=?, email=?, aadhaar=?, gender=?, father_name=? WHERE id=?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, txtName.getText().trim());
            pst.setInt(2, Integer.parseInt(txtAge.getText().trim()));
            pst.setString(3, txtMobile.getText().trim());
            pst.setString(4, txtGymTime.getText().trim());
            pst.setString(5, txtEmail.getText().trim());
            pst.setString(6, txtAadhaar.getText().trim());
            pst.setString(7, txtGender.getText().trim());
            pst.setString(8, txtFather.getText().trim());
            pst.setInt(9, Integer.parseInt(txtId.getText().trim()));
            int affected = pst.executeUpdate();
            if (affected > 0) {
                lblStatus.setForeground(new Color(0, 128, 0));
                lblStatus.setText("Member updated successfully!");
            } else {
                lblStatus.setText("Update failed!");
            }
        } catch (SQLException ex) {
            lblStatus.setText("Database Error: " + ex.getMessage());
        }
    }

    private boolean validateFields() {
        if (txtName.getText().trim().isEmpty() ||
            txtAge.getText().trim().isEmpty() ||
            txtMobile.getText().trim().isEmpty() ||
            txtGymTime.getText().trim().isEmpty() ||
            txtEmail.getText().trim().isEmpty() ||
            txtAadhaar.getText().trim().isEmpty() ||
            txtGender.getText().trim().isEmpty()) {
            lblStatus.setText("All fields must be filled.");
            return false;
        }
        if (!txtAge.getText().trim().matches("\\d+")) {
            lblStatus.setText("Age must be numeric.");
            return false;
        }
        if (!txtMobile.getText().trim().matches("\\d{10}")) {
            lblStatus.setText("Mobile must be 10 digits.");
            return false;
        }
        if (!txtEmail.getText().trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            lblStatus.setText("Invalid email.");
            return false;
        }
        if (!txtAadhaar.getText().trim().matches("\\d{12}")) {
            lblStatus.setText("Aadhaar must be 12 digits.");
            return false;
        }
        return true;
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