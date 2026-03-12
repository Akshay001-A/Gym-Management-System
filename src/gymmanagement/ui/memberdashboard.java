package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class memberdashboard extends JFrame {
    private final int memberId;
    private final String username;

    public memberdashboard(int memberId, String username) {
        this.memberId = memberId;
        this.username = username;

        try { FlatIntelliJLaf.setup(); } catch (Exception ignored) {}

        setTitle("Member Dashboard");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ---- Center Panel with background image and overlayed welcome text ----
        Image bgImg = null;
        try {
            bgImg = ImageIO.read(new File("assets/center.jpg"));
        } catch (IOException e) {
            System.out.println("Background image not found! " + e.getMessage());
        }
        JPanel centerPanel = new BackgroundPanel(bgImg, "Welcome to your Member Dashboard, " + username + "!");

        // ---- Side Navigation Panel ----
        JPanel sideNav = new JPanel();
        sideNav.setLayout(new BoxLayout(sideNav, BoxLayout.Y_AXIS));
        sideNav.setPreferredSize(new Dimension(240, 0));
        sideNav.setBackground(new Color(245, 250, 255));
        sideNav.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(33, 150, 243)));

        // Member photo/logo at top
        JLabel photoLabel = new JLabel();
        photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            Image photo = ImageIO.read(new File("assets/member.jpg"))
                    .getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            photoLabel.setIcon(new ImageIcon(photo));
        } catch (IOException e) {
            photoLabel.setText("Member");
            photoLabel.setFont(new Font("Arial", Font.BOLD, 28));
            photoLabel.setForeground(new Color(33, 150, 243));
        }
        sideNav.add(Box.createVerticalStrut(30));
        sideNav.add(photoLabel);
        sideNav.add(Box.createVerticalStrut(20));

        // --- Navigation buttons (functional) ---
        String[] buttonNames = {
                "View Schedule",
                "Pay Bill",
                "Notifications",
                "Sign Out",
                "Exit"
        };
        String[] iconPaths = {
                "assets/schedule.png.png",
                "assets/payment.png.png",
                "assets/notification.png.png",
                "assets/logout.png",
                "assets/exit.png"
        };

        JButton btnViewSchedule = createNavButton(buttonNames[0], iconPaths[0]);
        JButton btnPayBill = createNavButton(buttonNames[1], iconPaths[1]);
        JButton btnNotifications = createNavButton(buttonNames[2], iconPaths[2]);
        JButton btnSignOut = createNavButton(buttonNames[3], iconPaths[3]);
        JButton btnExit = createNavButton(buttonNames[4], iconPaths[4]);

        // Add listeners for actions, passing memberId and username where needed
        btnViewSchedule.addActionListener(e -> openSchedule());
        btnPayBill.addActionListener(e -> openPayment());
        btnNotifications.addActionListener(e -> openNotification());
        btnSignOut.addActionListener(e -> signOut());
        btnExit.addActionListener(e -> System.exit(0));

        // Add to sidebar
        JButton[] buttons = { btnViewSchedule, btnPayBill, btnNotifications, btnSignOut, btnExit };
        for (JButton btn : buttons) {
            sideNav.add(Box.createVerticalStrut(12));
            sideNav.add(btn);
        }
        sideNav.add(Box.createVerticalGlue());

        // ---- Layout ----
        setLayout(new BorderLayout());
        add(sideNav, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }

    // Helper to create a styled nav button with hover effect
    private JButton createNavButton(String text, String iconPath) {
        JButton btn = new JButton(text, getIcon(iconPath, 28, 28));
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
        return btn;
    }

    private void addHoverEffect(JButton button) {
        Color original = button.getBackground();
        Color hover = new Color(30, 136, 229);
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }
            public void mouseExited(MouseEvent e) {
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

    // --- BUTTON ACTIONS (full functionality, pass both memberId and username) ---

    private void openSchedule() {
        new memberschedulepage(memberId).setVisible(true);
    }

    private void openPayment() {
        new memberpaymentpage(memberId).setVisible(true);
    }

    private void openNotification() {
        new membernotificationpage(memberId).setVisible(true);
    }

    private void signOut() {
        int confirm = JOptionPane.showConfirmDialog(this, "Sign Out?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new memberloginpage().setVisible(true);
            dispose();
        }
    }

    // Background panel for the gym room image and overlayed welcome text
    static class BackgroundPanel extends JPanel {
        private final Image bgImage;
        private final String overlayText;

        public BackgroundPanel(Image bgImage, String overlayText) {
            this.bgImage = bgImage;
            this.overlayText = overlayText;
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
            if (overlayText != null) {
                g.setFont(new Font("Segoe UI", Font.BOLD, 36));
                g.setColor(new Color(255, 255, 255, 230));
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(overlayText);
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() / 2) + (fm.getAscent() / 2);
                int padding = 30;
                g.setColor(new Color(0,0,0,120));
                g.fillRoundRect(
                        x - padding/2, y - fm.getAscent(),
                        textWidth + padding, fm.getHeight() + 10, 30, 30
                );
                g.setColor(new Color(255,255,255,230));
                g.drawString(overlayText, x, y);
            }
        }
    }
}