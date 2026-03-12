package gymmanagement.ui;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class loginpage extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;

    public loginpage() {
        setTitle("Gym Management - Login");
        setSize(440, 350);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 3, true),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));
        panel.setBackground(new Color(245, 250, 255));

        JLabel logoLabel = new JLabel();
        try {
            Image logo = ImageIO.read(new File("assets/gym_logo.png")).getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(logo));
        } catch (IOException e) {
            logoLabel.setText("GYM");
            logoLabel.setFont(new Font("Arial", Font.BOLD, 32));
            logoLabel.setForeground(new Color(33, 150, 243));
        }
        logoLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Login as");
        heading.setFont(new Font("Arial", Font.BOLD, 28));
        heading.setForeground(new Color(33, 150, 243));
        heading.setAlignmentX(CENTER_ALIGNMENT);

        JButton adminBtn = new JButton("Admin Login");
        adminBtn.setBackground(new Color(33, 150, 243));
        adminBtn.setForeground(Color.WHITE);
        adminBtn.setFont(new Font("Arial", Font.BOLD, 18));
        adminBtn.setFocusPainted(false);
        adminBtn.setAlignmentX(CENTER_ALIGNMENT);
        adminBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        adminBtn.addActionListener(e -> {
            new adminloginpage().setVisible(true);
            dispose();
        });

        JButton memberBtn = new JButton("Member Login");
        memberBtn.setBackground(new Color(76, 175, 80));
        memberBtn.setForeground(Color.WHITE);
        memberBtn.setFont(new Font("Arial", Font.BOLD, 18));
        memberBtn.setFocusPainted(false);
        memberBtn.setAlignmentX(CENTER_ALIGNMENT);
        memberBtn.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2, true));
        memberBtn.addActionListener(e -> {
            new memberloginpage().setVisible(true);
            dispose();
        });

        panel.add(logoLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(18));
        panel.add(adminBtn);
        panel.add(Box.createVerticalStrut(14));
        panel.add(memberBtn);

        setContentPane(panel);
    }
}