package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class DashboardFrame extends JFrame {

    private String username;
    private File rootFolder;
    private File currentFolder;

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTextArea activityLog;

    private JProgressBar storageBar;
    private final long MAX_STORAGE = 50L * 1024 * 1024; // 50 MB

    private JPanel previewPanel; // for internal preview

    public DashboardFrame(String username) {
        this.username = username;

        // ===== STORAGE SETUP =====
        String basePath = System.getProperty("user.dir") + "/storage/";
        rootFolder = new File(basePath + username);
        if (!rootFolder.exists()) rootFolder.mkdirs();
        currentFolder = rootFolder;

        setTitle("MiniDrive - " + username);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== STORAGE BAR =====
        storageBar = new JProgressBar(0, 100);
        storageBar.setStringPainted(true);
        storageBar.setForeground(new Color(70, 130, 180)); // blue
        storageBar.setBackground(new Color(10, 25, 50)); // dark blue
        updateStorageBar();
        add(storageBar, BorderLayout.NORTH);

        // ===== TABLE =====
        tableModel = new DefaultTableModel(new String[]{"Name", "Type", "Size"}, 0);
        table = new JTable(tableModel);
        table.setBackground(new Color(10, 25, 50));
        table.setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 140, 0));
        table.setSelectionForeground(Color.BLACK);
        JScrollPane tableScroll = new JScrollPane(table);

        // ===== PREVIEW PANEL =====
        previewPanel = new JPanel();
        previewPanel.setPreferredSize(new Dimension(300, 0));
        previewPanel.setBackground(new Color(20, 30, 50));
        add(previewPanel, BorderLayout.EAST);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row == -1) return;

                    String name = tableModel.getValueAt(row, 0).toString();
                    String type = tableModel.getValueAt(row, 1).toString();
                    File target = new File(currentFolder, name);

                    if (type.equals("Folder")) {
                        currentFolder = target;
                        loadFiles();
                        logActivity("Opened folder: " + name);
                        previewPanel.removeAll();
                        previewPanel.repaint();
                    } else {
                        previewFile(target); // ✅ internal preview
                    }
                }
            }
        });

        // ===== SEARCH =====
        searchField = new JTextField();
        searchField.setColumns(20);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterFiles(searchField.getText());
            }
        });

        // ===== BUTTONS =====
        JButton uploadBtn = new JButton("Upload File");
        JButton folderBtn = new JButton("Create Folder");
        JButton deleteBtn = new JButton("Delete");
        JButton backBtn = new JButton("⬅ Back");
        JButton downloadBtn = new JButton("Download File");

        Color buttonColor = new Color(255, 140, 0);
        uploadBtn.setBackground(buttonColor);
        folderBtn.setBackground(buttonColor);
        deleteBtn.setBackground(buttonColor);
        backBtn.setBackground(buttonColor);
        downloadBtn.setBackground(buttonColor);

        uploadBtn.setForeground(Color.BLACK);
        folderBtn.setForeground(Color.BLACK);
        deleteBtn.setForeground(Color.BLACK);
        backBtn.setForeground(Color.BLACK);
        downloadBtn.setForeground(Color.BLACK);

        uploadBtn.addActionListener(e -> uploadFile());
        folderBtn.addActionListener(e -> createFolder());
        deleteBtn.addActionListener(e -> deleteItem());
        backBtn.addActionListener(e -> goBack());
        downloadBtn.addActionListener(e -> downloadFile());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(10, 25, 50));
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(Color.WHITE);
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        topPanel.add(uploadBtn);
        topPanel.add(folderBtn);
        topPanel.add(deleteBtn);
        topPanel.add(downloadBtn);
        topPanel.add(backBtn);

        add(topPanel, BorderLayout.PAGE_START);
        add(tableScroll, BorderLayout.CENTER);

        // ===== ACTIVITY LOG =====
        activityLog = new JTextArea(5, 20);
        activityLog.setEditable(false);
        activityLog.setBackground(new Color(10, 25, 50));
        activityLog.setForeground(Color.WHITE);
        JScrollPane logScroll = new JScrollPane(activityLog);
        add(logScroll, BorderLayout.SOUTH);

        loadFiles();
        setVisible(true);
    }

    // ===== INTERNAL PREVIEW =====
    private void previewFile(File file) {
        previewPanel.removeAll();
        previewPanel.repaint();

        try {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".txt")) {
                JTextArea textArea = new JTextArea();
                textArea.setEditable(false);
                textArea.setBackground(new Color(20, 30, 50));
                textArea.setForeground(Color.WHITE);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.read(new FileReader(file), null);
                previewPanel.setLayout(new BorderLayout());
                previewPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
                previewPanel.revalidate();
            } else if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")) {
                ImageIcon img = new ImageIcon(ImageIO.read(file));
                JLabel imgLabel = new JLabel();
                imgLabel.setIcon(new ImageIcon(img.getImage().getScaledInstance(250, 250, Image.SCALE_SMOOTH)));
                previewPanel.setLayout(new BorderLayout());
                previewPanel.add(imgLabel, BorderLayout.CENTER);
                previewPanel.revalidate();
            } else {
                // Other files open externally
                Desktop.getDesktop().open(file);
            }
            logActivity("Opened file: " + file.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot open file");
        }
    }

    // ===== UPLOAD FILE =====
    private void uploadFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File src = chooser.getSelectedFile();
            File dest = new File(currentFolder, src.getName());

            try {
                if (getUsedStorage() + src.length() > MAX_STORAGE) {
                    JOptionPane.showMessageDialog(this, "Storage limit reached!");
                    return;
                }
                Files.copy(src.toPath(), dest.toPath());
                loadFiles();
                updateStorageBar();
                logActivity("Uploaded file: " + src.getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Upload failed");
            }
        }
    }

    // ===== CREATE FOLDER =====
    private void createFolder() {
        String name = JOptionPane.showInputDialog(this, "Folder name:");
        if (name == null || name.isEmpty()) return;

        File folder = new File(currentFolder, name);
        if (folder.mkdir()) {
            loadFiles();
            logActivity("Created folder: " + name);
        } else {
            JOptionPane.showMessageDialog(this, "Cannot create folder");
        }
    }

    // ===== DELETE =====
    private void deleteItem() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        String name = tableModel.getValueAt(row, 0).toString();
        File file = new File(currentFolder, name);

        deleteRecursively(file);
        loadFiles();
        updateStorageBar();
        logActivity("Deleted: " + name);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) deleteRecursively(f);
        }
        file.delete();
    }

    // ===== BACK =====
    private void goBack() {
        if (!currentFolder.equals(rootFolder)) {
            currentFolder = currentFolder.getParentFile();
            loadFiles();
            logActivity("Went back");
            previewPanel.removeAll();
            previewPanel.repaint();
        }
    }

    // ===== DOWNLOAD =====
    private void downloadFile() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        String name = tableModel.getValueAt(row, 0).toString();
        File file = new File(currentFolder, name);

        if (file.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Cannot download folder!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(name));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            try {
                Files.copy(file.toPath(), dest.toPath());
                logActivity("Downloaded file: " + name);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Download failed");
            }
        }
    }

    // ===== LOAD FILES =====
    private void loadFiles() {
        tableModel.setRowCount(0);
        File[] files = currentFolder.listFiles();
        if (files == null) return;

        for (File f : files) {
            tableModel.addRow(new Object[]{
                    f.getName(),
                    f.isDirectory() ? "Folder" : "File",
                    f.isDirectory() ? "-" : (f.length() / 1024) + " KB"
            });
        }
    }

    // ===== STORAGE =====
    private long getUsedStorage() {
        return calculateSize(rootFolder);
    }

    private long calculateSize(File file) {
        if (file.isFile()) return file.length();
        long size = 0;
        File[] files = file.listFiles();
        if (files != null)
            for (File f : files) size += calculateSize(f);
        return size;
    }

    private void updateStorageBar() {
        long used = getUsedStorage();
        int percent = (int) ((used * 100) / MAX_STORAGE);
        storageBar.setValue(percent);
        storageBar.setString((used / 1024 / 1024) + " MB / 50 MB used");
    }

    // ===== ACTIVITY LOG =====
    private void logActivity(String msg) {
        activityLog.append(msg + "\n");
    }

    // ===== SEARCH =====
    private void filterFiles(String query) {
        tableModel.setRowCount(0);
        File[] files = currentFolder.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.getName().toLowerCase().contains(query.toLowerCase())) {
                tableModel.addRow(new Object[]{
                        f.getName(),
                        f.isDirectory() ? "Folder" : "File",
                        f.isDirectory() ? "-" : (f.length() / 1024) + " KB"
                });
            }
        }
    }
}
