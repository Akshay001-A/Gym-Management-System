package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import gymmanagement.util.dbconnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class listmemberpage extends JFrame {
    public listmemberpage() {
        try { FlatIntelliJLaf.setup(); } catch(Exception ignored) {}

        setTitle("List of Members");
        setSize(900, 400);
        setLocationRelativeTo(null);

        String[] columns = {"ID", "Name", "Age", "Mobile", "Gym Time", "Email", "Aadhaar", "Gender", "Father's Name", "Amount"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try (Connection con = dbconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM members")) {
            while (rs.next()) {
                Object[] row = new Object[10];
                for (int i = 0; i < 10; i++)
                    row[i] = rs.getObject(i + 1);
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        JScrollPane scrollPane = new JScrollPane(table);

        JLabel heading = new JLabel("List of Members", JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(heading, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }
}