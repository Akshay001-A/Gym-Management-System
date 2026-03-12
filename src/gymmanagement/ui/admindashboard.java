package gymmanagement.ui;

import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class admindashboard extends JFrame {
    private String user;

    public admindashboard(String user) {
        this.user = user;
        setTitle("Gym Management - Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Start maximized

        // North Panel (Logo and Heading)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(33, 150, 243));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel logoLabel = new JLabel();
        try {
            Image logo = ImageIO.read(new File("assets/gym_logo.png"))
                    .getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(logo));
        } catch (IOException e) {
            logoLabel.setText("GYM");
            logoLabel.setFont(new Font("Arial", Font.BOLD, 36));
            logoLabel.setForeground(Color.WHITE);
        }
        JLabel heading = new JLabel("Admin Dashboard", JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 36));
        heading.setForeground(Color.WHITE);
        northPanel.add(logoLabel, BorderLayout.WEST);
        northPanel.add(heading, BorderLayout.CENTER);
        northPanel.add(new JLabel("Welcome, " + user + "  "), BorderLayout.EAST);

        // Side Navigation Panel
        JPanel sideNav = new JPanel();
        sideNav.setLayout(new BoxLayout(sideNav, BoxLayout.Y_AXIS));
        sideNav.setPreferredSize(new Dimension(240, 0));
        sideNav.setBackground(new Color(245, 250, 255));
        sideNav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(33, 150, 243)));

        // Admin photo/logo at the top of the sideNav
        JLabel photoLabel = new JLabel();
        photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            Image photo = ImageIO.read(new File("assets/admin.jpg"))
                    .getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            photoLabel.setIcon(new ImageIcon(photo));
        } catch (IOException e) {
            photoLabel.setText("Admin");
            photoLabel.setFont(new Font("Arial", Font.BOLD, 28));
            photoLabel.setForeground(new Color(33, 150, 243));
        }
        sideNav.add(Box.createVerticalStrut(30));
        sideNav.add(photoLabel);
        sideNav.add(Box.createVerticalStrut(20));

        // Navigation buttons
        String[] navItems = {"Add Member", "Update Member", "Delete Member", "List Members", "Payment Notification", "Logout", "Exit"};
        String[] icons = {
            "assets/addmembers.png", "assets/updatemembers.png", "assets/deletemembers.png",
            "assets/checklist.png", "assets/payment.png.png", "assets/logout.png", "assets/exit.png"
        };
        for (int i = 0; i < navItems.length; i++) {
            String item = navItems[i];
            JButton btn = new JButton(item, getIcon(icons[i], 28, 28));
            btn.setMaximumSize(new Dimension(200, 48));
            btn.setBackground(new Color(33, 150, 243));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 17));
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 2, true),
                BorderFactory.createEmptyBorder(8, 24, 8, 16)
            ));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setHorizontalTextPosition(SwingConstants.RIGHT);
            btn.setIconTextGap(18);
            btn.setOpaque(true);
            addHoverEffect(btn);
            btn.addActionListener(e -> handleNav(item));
            sideNav.add(Box.createVerticalStrut(12));
            sideNav.add(btn);
        }
        sideNav.add(Box.createVerticalGlue());

        // Center Panel with background image
        Image bgImg = null;
        try {
            bgImg = ImageIO.read(new File("assets/center.jpg")); // Gym or admin background image
        } catch (IOException e) {
            // fallback or log error
        }
        JPanel centerPanel = new BackgroundPanel(bgImg);
        centerPanel.setLayout(new BorderLayout());
        centerPanel.setOpaque(false);

        JLabel demoMsg = new JLabel(
            "<html><center><br>Welcome to the Modern Gym Management Admin Portal!<br><br>Select an option from the left menu.</center></html>",
            JLabel.CENTER
        );
        demoMsg.setFont(new Font("Arial", Font.PLAIN, 24));
        demoMsg.setForeground(Color.WHITE);
        demoMsg.setOpaque(false);
        centerPanel.add(demoMsg, BorderLayout.CENTER);

        // Layout
        setLayout(new BorderLayout());
        add(northPanel, BorderLayout.NORTH);
        add(sideNav, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void handleNav(String item) {
        switch (item) {
            case "Add Member":
                new addmemberpage().setVisible(true);
                break;
            case "Update Member":
                new updatememberpage().setVisible(true);
                break;
            case "Delete Member":
                new deletememberpage().setVisible(true);
                break;
            case "List Members":
                new listmemberpage().setVisible(true);
                break;
            case "Payment Notification":
                new adminpaymentpage().setVisible(true);
                break;
            case "Logout":
                int confirm = JOptionPane.showConfirmDialog(this, "Log out?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    new loginpage().setVisible(true);
                    dispose();
                }
                break;
            case "Exit":
                int confirmExit = JOptionPane.showConfirmDialog(this, "Exit application?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirmExit == JOptionPane.YES_OPTION) System.exit(0);
                break;
        }
    }

    private void addHoverEffect(JButton button) {
        Color original = button.getBackground();
        Color hover = new Color(30, 136, 229);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(original);
            }
        });
    }

    private ImageIcon getIcon(String path, int w, int h) {
        try {
            Image img = new ImageIcon(path).getImage();
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }

    // Background panel for the admin background image
    static class BackgroundPanel extends JPanel {
        private final Image bgImage;

        public BackgroundPanel(Image bgImage) {
            this.bgImage = bgImage;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bgImage != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}