package gymmanagement.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import gymmanagement.util.dbconnection;

public class addmemberpage extends JFrame {
    private JTextField txtName, txtAge, txtMobile, txtEmail, txtAadhaar, txtFather;
    private JComboBox<String> cbGender, cbGymTime;
    private JLabel lblStatus;
    private final int AMOUNT = 6000;

    public addmemberpage() {
        setTitle("Add New Member");
        setSize(420, 570);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 3, true),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));
        mainPanel.setBackground(new Color(245, 250, 255));

        JLabel heading = new JLabel("Add Member");
        heading.setFont(new Font("Arial", Font.BOLD, 26));
        heading.setForeground(new Color(33, 150, 243));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        txtName = new JTextField();
        txtAge = new JTextField();
        txtMobile = new JTextField();
        cbGymTime = new JComboBox<>(new String[]{"6:00 am to 8:00 am", "5:00 pm to 7:00 pm"});
        txtEmail = new JTextField();
        txtAadhaar = new JTextField();
        cbGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        txtFather = new JTextField();

        addField(formPanel, gbc, 0,"Name:", txtName);
        addField(formPanel, gbc, 1,"Age:", txtAge);
        addField(formPanel, gbc, 2,"Mobile:", txtMobile);
        addField(formPanel, gbc, 3,"Gym Time:", cbGymTime);
        addField(formPanel, gbc, 4,"Email:", txtEmail);
        addField(formPanel, gbc, 5,"Aadhaar:", txtAadhaar);
        addField(formPanel, gbc, 6,"Gender:", cbGender);
        addField(formPanel, gbc, 7,"Father's Name:", txtFather);

        gbc.gridy = 8; gbc.gridx = 0;
        formPanel.add(new JLabel("Amount (per month):"), gbc);
        gbc.gridx = 1; formPanel.add(new JLabel("₹" + AMOUNT), gbc);

        lblStatus = new JLabel(" ");
        lblStatus.setForeground(Color.RED);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);

        JButton btnSave = new JButton("Save Member");
        btnSave.setBackground(new Color(33, 150, 243));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Arial", Font.BOLD, 16));
        btnSave.setFocusPainted(false);
        btnSave.setAlignmentX(CENTER_ALIGNMENT);
        btnSave.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        btnSave.addActionListener(e -> saveMember());

        mainPanel.add(heading);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(lblStatus);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(btnSave);

        add(mainPanel);
    }

    private void addField(JPanel formPanel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridy = row; gbc.gridx = 0; formPanel.add(new JLabel(label), gbc);
        gbc.gridx = 1; field.setPreferredSize(new Dimension(160, 28)); formPanel.add(field, gbc);
    }

    private void saveMember() {
        String name = txtName.getText().trim();
        String mobile = txtMobile.getText().trim();
        String aadhaar = txtAadhaar.getText().trim();
        String email = txtEmail.getText().trim();

        if (!name.matches("[a-zA-Z ]+")) {
            lblStatus.setText("Name: only letters and spaces allowed.");
            txtName.requestFocus(); return;
        }
        if (!mobile.matches("\\d{10}")) {
            lblStatus.setText("Enter a valid 10-digit mobile number.");
            txtMobile.requestFocus(); return;
        }
        // Allow any valid email format, not just gmail
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$")) {
            lblStatus.setText("Enter a valid email address.");
            txtEmail.requestFocus(); return;
        }
        if (!aadhaar.matches("\\d{12}")) {
            lblStatus.setText("Enter a valid 12-digit Aadhaar number.");
            txtAadhaar.requestFocus(); return;
        }
        if (!txtAge.getText().matches("\\d+")) {
            lblStatus.setText("Age must be a number."); txtAge.requestFocus(); return;
        }

        lblStatus.setText(" ");
        try (Connection con = dbconnection.getConnection()) {
            String sql = "INSERT INTO members (name, age, mobile, gym_time, email, aadhaar, gender, father_name, amount) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setString(1, name);
            pst.setInt(2, Integer.parseInt(txtAge.getText()));
            pst.setString(3, mobile);
            pst.setString(4, (String) cbGymTime.getSelectedItem());
            pst.setString(5, txtEmail.getText());
            pst.setString(6, aadhaar);
            pst.setString(7, (String) cbGender.getSelectedItem());
            pst.setString(8, txtFather.getText());
            pst.setInt(9, AMOUNT);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: "+ex.getMessage());
        }
    }
}