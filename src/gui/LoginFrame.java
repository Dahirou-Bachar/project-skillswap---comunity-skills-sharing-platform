package gui;

import javax.swing.*;
import java.awt.*;
import service.AuthService;

public class LoginFrame extends JFrame {

    public LoginFrame() {
        setTitle("MiniDrive - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ===== PANEL =====
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(10, 25, 50)); // dark blue
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ===== LABELS =====
        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        userLabel.setForeground(Color.WHITE);
        passLabel.setForeground(Color.WHITE);

        JTextField userField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);

        // ===== LOGIN BUTTON =====
        JButton loginBtn = new JButton("Login");
        loginBtn.setForeground(Color.BLACK);
        loginBtn.setBackground(new Color(255, 140, 0)); // orange

        loginBtn.addActionListener(e -> {
            if (AuthService.login(userField.getText(),
                    new String(passField.getPassword()))) {
                dispose();
                new DashboardFrame(userField.getText());
            } else {
                JOptionPane.showMessageDialog(this, "Invalid login", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // ===== LAYOUT =====
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(userLabel, gbc);
        gbc.gridx = 1;
        panel.add(userField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(loginBtn, gbc);

        add(panel);
        setVisible(true);
    }
}
