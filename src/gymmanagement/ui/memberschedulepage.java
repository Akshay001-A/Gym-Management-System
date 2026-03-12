package gymmanagement.ui;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import java.awt.*;

public class memberschedulepage extends JFrame {
    private JTextArea txtSchedule;

    // Improved default schedule with workouts and diets
    private static final String DEFAULT_SCHEDULE =
            "----- Weekly Gym Schedule (Workout + Diet) -----\n\n" +
            "Monday\n" +
            "  Workout : Chest + Triceps\n" +
            "  Diet    : Eggs, oats, banana smoothie\n\n" +

            "Tuesday\n" +
            "  Workout : Back + Biceps\n" +
            "  Diet    : Brown rice, chicken, salad\n\n" +

            "Wednesday\n" +
            "  Workout : Legs + Core\n" +
            "  Diet    : Sweet potatoes, lentils, fruits\n\n" +

            "Thursday\n" +
            "  Workout : Shoulders + Triceps\n" +
            "  Diet    : Paneer, multigrain roti, curd\n\n" +

            "Friday\n" +
            "  Workout : Full body HIIT\n" +
            "  Diet    : Eggs, quinoa, green veggies\n\n" +

            "Saturday\n" +
            "  Workout : Cardio + Abs\n" +
            "  Diet    : Light protein shake, nuts, fruits\n\n" +

            "Sunday\n" +
            "  Rest Day (Stretching / Yoga)\n" +
            "  Diet    : Balanced meals, hydrate well\n\n" +
            "----------------------------------------------\n" +
            "Note: Consult trainers for personal modifications.";

    public memberschedulepage(int memberId) {
        try { FlatIntelliJLaf.setup(); } catch (Exception ignored) {}

        setTitle("My Schedule");
        setSize(500, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 250, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 3, true),
                BorderFactory.createEmptyBorder(22, 32, 22, 32)
        ));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("My Weekly Schedule", getIcon("assets/schedule.png", 32, 32), JLabel.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 22));
        heading.setForeground(new Color(33, 150, 243));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtSchedule = new JTextArea(DEFAULT_SCHEDULE);
        txtSchedule.setEditable(false);
        txtSchedule.setFont(new Font("Monospaced", Font.PLAIN, 15));
        txtSchedule.setLineWrap(true);
        txtSchedule.setWrapStyleWord(true);
        txtSchedule.setBackground(new Color(235, 245, 255));
        txtSchedule.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(txtSchedule);
        scrollPane.setPreferredSize(new Dimension(420, 260));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(heading);
        panel.add(Box.createVerticalStrut(14));
        panel.add(scrollPane);
        panel.add(Box.createVerticalStrut(10));

        setContentPane(panel);
    }

    private ImageIcon getIcon(String path, int w, int h) {
        try {
            Image img = new ImageIcon(path).getImage();
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            return null;
        }
    }
}
