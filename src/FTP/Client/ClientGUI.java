package FTP.Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Interfaz gráfica retro para el cliente FTP.
 * <p>
 * Proporciona una interfaz visual con estilo retro (terminal ámbar/verde)
 * para interactuar con servidores FTP de forma intuitiva.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class ClientGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    // Colores estilo retro
    private static final Color RETRO_BG = new Color(20, 20, 35);          // Azul oscuro casi negro
    private static final Color RETRO_FG = new Color(255, 200, 100);       // Ámbar
    private static final Color RETRO_HEADER_BG = new Color(35, 35, 55);   // Azul oscuro
    private static final Color RETRO_BUTTON_BG = new Color(50, 50, 80);   // Azul medio
    private static final Color RETRO_SUCCESS = new Color(100, 255, 100);  // Verde brillante
    private static final Color RETRO_ERROR = new Color(255, 100, 100);    // Rojo suave

    // Componentes de la GUI
    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JComboBox<String> modeCombo;

    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel currentDirLabel;

    // Botones de operaciones
    private JButton refreshButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton deleteButton;
    private JButton mkdirButton;
    private JButton rmdirButton;
    private JButton renameButton;
    private JButton cdButton;
    private JButton cdupButton;
    private JButton pwdButton;

    // Cliente FTP
    private FTPClient ftpClient;
    private boolean connected = false;

    /**
     * Constructor que inicializa la interfaz gráfica del cliente.
     */
    public ClientGUI() {
        initComponents();
        ftpClient = new FTPClient();
    }

    /**
     * Inicializa todos los componentes visuales de la interfaz.
     */
    private void initComponents() {
        setTitle("█▓▒░ JAVA FTP CLIENT ░▒▓█");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(RETRO_BG);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel superior - Conexión
        JPanel connectionPanel = createConnectionPanel();
        mainPanel.add(connectionPanel, BorderLayout.NORTH);

        // Panel central - Split entre archivos y log
        JSplitPane splitPane = createCenterPanel();
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Panel inferior - Estado
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Deshabilitar botones de operaciones inicialmente
        setOperationButtonsEnabled(false);
    }

    /**
     * Crea el panel de conexión al servidor.
     *
     * @return Panel de conexión configurado
     */
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(RETRO_HEADER_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 2),
            new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Consolas", Font.BOLD, 12);
        Font fieldFont = new Font("Consolas", Font.PLAIN, 12);

        // Host
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel hostLabel = createStyledLabel("HOST:", labelFont);
        panel.add(hostLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        hostField = createStyledTextField("localhost", fieldFont);
        panel.add(hostField, gbc);

        // Puerto
        gbc.gridx = 2; gbc.weightx = 0;
        JLabel portLabel = createStyledLabel("PORT:", labelFont);
        panel.add(portLabel, gbc);

        gbc.gridx = 3; gbc.weightx = 0.3;
        portField = createStyledTextField("21", fieldFont);
        panel.add(portField, gbc);

        // Usuario
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel userLabel = createStyledLabel("USER:", labelFont);
        panel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        usernameField = createStyledTextField("", fieldFont);
        panel.add(usernameField, gbc);

        // Contraseña
        gbc.gridx = 2; gbc.weightx = 0;
        JLabel passLabel = createStyledLabel("PASS:", labelFont);
        panel.add(passLabel, gbc);

        gbc.gridx = 3; gbc.weightx = 0.3;
        passwordField = createStyledPasswordField(fieldFont);
        panel.add(passwordField, gbc);

        // Modo
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel modeLabel = createStyledLabel("MODE:", labelFont);
        panel.add(modeLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        String[] modes = {"PASSIVE", "ACTIVE"};
        modeCombo = new JComboBox<>(modes);
        styleComboBox(modeCombo, fieldFont);
        panel.add(modeCombo, gbc);

        // Botones
        gbc.gridx = 2; gbc.gridwidth = 2; gbc.weightx = 0;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(RETRO_HEADER_BG);

        connectButton = createStyledButton("▶ CONNECT", labelFont);
        connectButton.addActionListener(e -> connect());

        disconnectButton = createStyledButton("■ DISCONNECT", labelFont);
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());

        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        panel.add(buttonPanel, gbc);

        return panel;
    }

    /**
     * Crea el panel central con tabla de archivos y área de log.
     *
     * @return JSplitPane configurado
     */
    private JSplitPane createCenterPanel() {
        // Panel izquierdo - Archivos
        JPanel filesPanel = createFilesPanel();

        // Panel derecho - Log
        JPanel logPanel = createLogPanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, filesPanel, logPanel);
        splitPane.setDividerLocation(600);
        splitPane.setBackground(RETRO_BG);

        return splitPane;
    }

    /**
     * Crea el panel de archivos con tabla y botones de operaciones.
     *
     * @return Panel de archivos configurado
     */
    private JPanel createFilesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(RETRO_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(RETRO_FG, 2),
            " ▼ FILES ",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Consolas", Font.BOLD, 12),
            RETRO_FG
        );
        panel.setBorder(border);

        // Directorio actual
        currentDirLabel = new JLabel(" Current: /");
        currentDirLabel.setForeground(RETRO_SUCCESS);
        currentDirLabel.setFont(new Font("Consolas", Font.BOLD, 11));
        currentDirLabel.setOpaque(true);
        currentDirLabel.setBackground(RETRO_HEADER_BG);
        currentDirLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(currentDirLabel, BorderLayout.NORTH);

        // Tabla de archivos
        String[] columns = {"Type", "Name", "Size", "Modified"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fileTable = new JTable(tableModel);
        styleTable(fileTable);

        // Habilitar selección múltiple
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Doble clic para cambiar directorio
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && fileTable.getSelectedRow() != -1) {
                    int row = fileTable.getSelectedRow();
                    String type = (String) tableModel.getValueAt(row, 0);
                    if ("<DIR>".equals(type)) {
                        String dirname = (String) tableModel.getValueAt(row, 1);
                        try {
                            boolean success = ftpClient.changeWorkingDirectory(dirname);
                            if (success) {
                                log("[SUCCESS] Changed to: " + dirname, RETRO_SUCCESS);
                                listFiles();
                            } else {
                                log("[ERROR] CD failed", RETRO_ERROR);
                            }
                        } catch (IOException ex) {
                            log("[ERROR] CD error: " + ex.getMessage(), RETRO_ERROR);
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
        scrollPane.getViewport().setBackground(RETRO_BG);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonsPanel = createOperationButtonsPanel();
        panel.add(buttonsPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Crea el panel con botones de operaciones.
     *
     * @return Panel de botones configurado
     */
    private JPanel createOperationButtonsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 4, 5, 5));
        panel.setBackground(RETRO_BG);
        panel.setBorder(new EmptyBorder(10, 5, 5, 5));

        Font btnFont = new Font("Consolas", Font.BOLD, 10);

        refreshButton = createStyledButton("↻ REFRESH", btnFont);
        uploadButton = createStyledButton("↑ UPLOAD", btnFont);
        downloadButton = createStyledButton("↓ DOWNLOAD", btnFont);
        deleteButton = createStyledButton("✖ DELETE", btnFont);
        mkdirButton = createStyledButton("+ MKD", btnFont);
        rmdirButton = createStyledButton("- RMD", btnFont);
        renameButton = createStyledButton("✎ RENAME", btnFont);
        cdButton = createStyledButton("→ CD", btnFont);
        cdupButton = createStyledButton("← CDUP", btnFont);
        pwdButton = createStyledButton("? PWD", btnFont);

        refreshButton.addActionListener(e -> listFiles());
        uploadButton.addActionListener(e -> uploadFile());
        downloadButton.addActionListener(e -> downloadFile());
        deleteButton.addActionListener(e -> deleteFile());
        mkdirButton.addActionListener(e -> makeDirectory());
        rmdirButton.addActionListener(e -> removeDirectory());
        renameButton.addActionListener(e -> renameFile());
        cdButton.addActionListener(e -> changeDirectory());
        cdupButton.addActionListener(e -> changeToParentDirectory());
        pwdButton.addActionListener(e -> printWorkingDirectory());

        panel.add(refreshButton);
        panel.add(uploadButton);
        panel.add(downloadButton);
        panel.add(deleteButton);
        panel.add(mkdirButton);
        panel.add(rmdirButton);
        panel.add(renameButton);
        panel.add(cdButton);
        panel.add(cdupButton);
        panel.add(pwdButton);

        return panel;
    }

    /**
     * Crea el panel de log.
     *
     * @return Panel de log configurado
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(RETRO_BG);

        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(RETRO_FG, 2),
            " ▼ LOG ",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Consolas", Font.BOLD, 12),
            RETRO_FG
        );
        panel.setBorder(border);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(RETRO_BG);
        logArea.setForeground(RETRO_FG);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea el panel de estado.
     *
     * @return Panel de estado configurado
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(RETRO_HEADER_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 2),
            new EmptyBorder(8, 15, 8, 15)
        ));

        statusLabel = new JLabel("● STATUS: DISCONNECTED");
        statusLabel.setForeground(RETRO_ERROR);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 13));

        JLabel authorLabel = new JLabel("By RGiskard ©");
        authorLabel.setForeground(RETRO_FG);
        authorLabel.setFont(new Font("Consolas", Font.ITALIC, 11));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(authorLabel, BorderLayout.EAST);

        return panel;
    }

    // Métodos auxiliares para crear componentes estilizados

    private JLabel createStyledLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setForeground(RETRO_FG);
        label.setFont(font);
        return label;
    }

    private JTextField createStyledTextField(String text, Font font) {
        JTextField field = new JTextField(text);
        field.setBackground(RETRO_BG);
        field.setForeground(RETRO_FG);
        field.setCaretColor(RETRO_FG);
        field.setFont(font);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1),
            new EmptyBorder(3, 5, 3, 5)
        ));
        return field;
    }

    private JPasswordField createStyledPasswordField(Font font) {
        JPasswordField field = new JPasswordField();
        field.setBackground(RETRO_BG);
        field.setForeground(RETRO_FG);
        field.setCaretColor(RETRO_FG);
        field.setFont(font);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1),
            new EmptyBorder(3, 5, 3, 5)
        ));
        return field;
    }

    private void styleComboBox(JComboBox<String> combo, Font font) {
        combo.setBackground(RETRO_BG);
        combo.setForeground(RETRO_FG);
        combo.setFont(font);
        combo.setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
    }

    private JButton createStyledButton(String text, Font font) {
        JButton button = new JButton(text);
        button.setBackground(RETRO_BUTTON_BG);
        button.setForeground(RETRO_FG);
        button.setFont(font);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1),
            new EmptyBorder(5, 10, 5, 10)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(RETRO_FG);
                    button.setForeground(RETRO_BG);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(RETRO_BUTTON_BG);
                button.setForeground(RETRO_FG);
            }
        });

        return button;
    }

    private void styleTable(JTable table) {
        table.setBackground(RETRO_BG);
        table.setForeground(RETRO_FG);
        table.setGridColor(RETRO_BUTTON_BG);
        table.setFont(new Font("Consolas", Font.PLAIN, 11));
        table.setRowHeight(25);
        table.setSelectionBackground(RETRO_BUTTON_BG);
        table.setSelectionForeground(RETRO_SUCCESS);

        table.getTableHeader().setBackground(RETRO_HEADER_BG);
        table.getTableHeader().setForeground(RETRO_FG);
        table.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
    }

    // Métodos de operaciones FTP

    private void connect() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty()) {
            log("[ERROR] Host, port, and username are required", RETRO_ERROR);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            log("[INFO] Connecting to " + host + ":" + port + "...", RETRO_FG);

            // Configurar encoding UTF-8 para soportar acentos, ñ, etc.
            ftpClient.setControlEncoding("UTF-8");

            ftpClient.connect(host, port);
            int reply = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log("[ERROR] FTP server refused connection.", RETRO_ERROR);
                return;
            }

            log("[SUCCESS] Connected to server", RETRO_SUCCESS);
            log("[INFO] Logging in as " + username + "...", RETRO_FG);

            boolean login = ftpClient.login(username, password);
            if (!login) {
                log("[ERROR] Login failed", RETRO_ERROR);
                ftpClient.disconnect();
                return;
            }

            log("[SUCCESS] Login successful", RETRO_SUCCESS);

            // Configurar modo binario (crítico para PDFs, ZIPs, imágenes)
            ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            log("[INFO] Binary transfer mode enabled", RETRO_FG);

            // Configurar modo
            String mode = (String) modeCombo.getSelectedItem();
            if ("PASSIVE".equals(mode)) {
                ftpClient.enterLocalPassiveMode();
                log("[INFO] Passive mode enabled", RETRO_FG);
            } else {
                ftpClient.enterLocalActiveMode();
                log("[INFO] Active mode enabled", RETRO_FG);
            }

            connected = true;

            statusLabel.setText("● STATUS: CONNECTED");
            statusLabel.setForeground(RETRO_SUCCESS);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            setOperationButtonsEnabled(true);

            listFiles();

        } catch (NumberFormatException e) {
            log("[ERROR] Invalid port number", RETRO_ERROR);
        } catch (IOException e) {
            log("[ERROR] Connection error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            log("[INFO] Disconnected from server", RETRO_FG);

            connected = false;
            statusLabel.setText("● STATUS: DISCONNECTED");
            statusLabel.setForeground(RETRO_ERROR);
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            setOperationButtonsEnabled(false);

            tableModel.setRowCount(0);
            currentDirLabel.setText(" Current: /");

        } catch (IOException e) {
            log("[ERROR] Disconnect error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void listFiles() {
        if (!connected) return;

        try {
            tableModel.setRowCount(0);
            FTPFile[] files = ftpClient.listFiles();

            for (FTPFile file : files) {
                String type = file.isDirectory() ? "<DIR>" : "FILE";
                String name = file.getName();
                String size = file.isDirectory() ? "-" : String.format("%,d", file.getSize());
                String date = file.getTimestamp() != null ?
                             String.format("%tF %<tT", file.getTimestamp()) : "Unknown";

                tableModel.addRow(new Object[]{type, name, size, date});
            }

            log("[INFO] Listed " + files.length + " items", RETRO_SUCCESS);
            updateCurrentDirectory();

        } catch (IOException e) {
            log("[ERROR] Failed to list files: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void uploadFile() {
        if (!connected) return;

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String remoteName = JOptionPane.showInputDialog(this,
                "Remote filename:", file.getName());

            if (remoteName != null && !remoteName.isEmpty()) {
                // Crear diálogo de progreso
                JDialog progressDialog = new JDialog(this, "Uploading...", true);
                JProgressBar progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                progressBar.setForeground(RETRO_SUCCESS);
                progressBar.setBackground(RETRO_BG);

                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBackground(RETRO_BG);
                panel.setBorder(new EmptyBorder(20, 20, 20, 20));
                panel.add(new JLabel("Uploading " + remoteName + "...") {{
                    setForeground(RETRO_FG);
                    setFont(new Font("Consolas", Font.BOLD, 12));
                }}, BorderLayout.NORTH);
                panel.add(progressBar, BorderLayout.CENTER);
                progressDialog.add(panel);
                progressDialog.setSize(400, 120);
                progressDialog.setLocationRelativeTo(this);

                // Ejecutar upload en thread separado
                new Thread(() -> {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        final long fileSize = file.length();

                        ftpClient.setCopyStreamListener(new org.apache.commons.net.io.CopyStreamListener() {
                            @Override
                            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                                int percent = (int)((totalBytesTransferred * 100) / fileSize);
                                SwingUtilities.invokeLater(() -> progressBar.setValue(percent));
                            }

                            @Override
                            public void bytesTransferred(org.apache.commons.net.io.CopyStreamEvent event) {
                                bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
                            }
                        });

                        boolean success = ftpClient.storeFile(remoteName, fis);

                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            if (success) {
                                log("[SUCCESS] File uploaded: " + remoteName, RETRO_SUCCESS);
                                listFiles();
                            } else {
                                log("[ERROR] Upload failed", RETRO_ERROR);
                            }
                        });
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> {
                            progressDialog.dispose();
                            log("[ERROR] Upload error: " + e.getMessage(), RETRO_ERROR);
                        });
                    }
                }).start();

                progressDialog.setVisible(true);
            }
        }
    }

    private void downloadFile() {
        if (!connected) return;

        int row = fileTable.getSelectedRow();
        if (row < 0) {
            log("[ERROR] Select a file to download", RETRO_ERROR);
            return;
        }

        String filename = (String) tableModel.getValueAt(row, 1);
        String type = (String) tableModel.getValueAt(row, 0);
        String sizeStr = (String) tableModel.getValueAt(row, 2);

        if ("<DIR>".equals(type)) {
            log("[ERROR] Cannot download a directory", RETRO_ERROR);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(filename));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File localFile = chooser.getSelectedFile();

            // Crear diálogo de progreso
            JDialog progressDialog = new JDialog(this, "Downloading...", true);
            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setForeground(RETRO_SUCCESS);
            progressBar.setBackground(RETRO_BG);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(RETRO_BG);
            panel.setBorder(new EmptyBorder(20, 20, 20, 20));
            panel.add(new JLabel("Downloading " + filename + "...") {{
                setForeground(RETRO_FG);
                setFont(new Font("Consolas", Font.BOLD, 12));
            }}, BorderLayout.NORTH);
            panel.add(progressBar, BorderLayout.CENTER);
            progressDialog.add(panel);
            progressDialog.setSize(400, 120);
            progressDialog.setLocationRelativeTo(this);

            // Ejecutar download en thread separado
            new Thread(() -> {
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    // Obtener tamaño del archivo remoto
                    long fileSize = 0;
                    try {
                        fileSize = Long.parseLong(sizeStr.replace(",", ""));
                    } catch (NumberFormatException e) {
                        // Si no se puede parsear, usar estimación
                        fileSize = 1000000; // 1MB default
                    }

                    final long finalFileSize = fileSize;

                    ftpClient.setCopyStreamListener(new org.apache.commons.net.io.CopyStreamListener() {
                        @Override
                        public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                            int percent = finalFileSize > 0 ? (int)((totalBytesTransferred * 100) / finalFileSize) : 0;
                            SwingUtilities.invokeLater(() -> progressBar.setValue(Math.min(percent, 100)));
                        }

                        @Override
                        public void bytesTransferred(org.apache.commons.net.io.CopyStreamEvent event) {
                            bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
                        }
                    });

                    boolean success = ftpClient.retrieveFile(filename, fos);

                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        if (success) {
                            log("[SUCCESS] File downloaded: " + filename, RETRO_SUCCESS);
                        } else {
                            log("[ERROR] Download failed", RETRO_ERROR);
                        }
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        log("[ERROR] Download error: " + e.getMessage(), RETRO_ERROR);
                    });
                }
            }).start();

            progressDialog.setVisible(true);
        }
    }

    private void deleteFile() {
        if (!connected) return;

        int row = fileTable.getSelectedRow();
        if (row < 0) {
            log("[ERROR] Select a file to delete", RETRO_ERROR);
            return;
        }

        String filename = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + filename + "?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = ftpClient.deleteFile(filename);
                if (success) {
                    log("[SUCCESS] File deleted: " + filename, RETRO_SUCCESS);
                    listFiles();
                } else {
                    log("[ERROR] Delete failed", RETRO_ERROR);
                }
            } catch (IOException e) {
                log("[ERROR] Delete error: " + e.getMessage(), RETRO_ERROR);
            }
        }
    }

    private void makeDirectory() {
        if (!connected) return;

        String dirname = JOptionPane.showInputDialog(this, "Directory name:");
        if (dirname != null && !dirname.isEmpty()) {
            try {
                boolean success = ftpClient.makeDirectory(dirname);
                if (success) {
                    log("[SUCCESS] Directory created: " + dirname, RETRO_SUCCESS);
                    listFiles();
                } else {
                    log("[ERROR] MKD failed", RETRO_ERROR);
                }
            } catch (IOException e) {
                log("[ERROR] MKD error: " + e.getMessage(), RETRO_ERROR);
            }
        }
    }

    private void removeDirectory() {
        if (!connected) return;

        int row = fileTable.getSelectedRow();
        if (row < 0) {
            log("[ERROR] Select a directory to remove", RETRO_ERROR);
            return;
        }

        String dirname = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove directory " + dirname + "?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = ftpClient.removeDirectory(dirname);
                if (success) {
                    log("[SUCCESS] Directory removed: " + dirname, RETRO_SUCCESS);
                    listFiles();
                } else {
                    log("[ERROR] RMD failed", RETRO_ERROR);
                }
            } catch (IOException e) {
                log("[ERROR] RMD error: " + e.getMessage(), RETRO_ERROR);
            }
        }
    }

    private void renameFile() {
        if (!connected) return;

        int row = fileTable.getSelectedRow();
        if (row < 0) {
            log("[ERROR] Select a file/directory to rename", RETRO_ERROR);
            return;
        }

        String oldName = (String) tableModel.getValueAt(row, 1);
        String newName = JOptionPane.showInputDialog(this, "New name:", oldName);

        if (newName != null && !newName.isEmpty() && !newName.equals(oldName)) {
            try {
                boolean success = ftpClient.rename(oldName, newName);
                if (success) {
                    log("[SUCCESS] Renamed: " + oldName + " → " + newName, RETRO_SUCCESS);
                    listFiles();
                } else {
                    log("[ERROR] Rename failed", RETRO_ERROR);
                }
            } catch (IOException e) {
                log("[ERROR] Rename error: " + e.getMessage(), RETRO_ERROR);
            }
        }
    }

    private void changeDirectory() {
        if (!connected) return;

        int row = fileTable.getSelectedRow();
        String dirname = null;

        if (row >= 0) {
            String type = (String) tableModel.getValueAt(row, 0);
            if ("<DIR>".equals(type)) {
                dirname = (String) tableModel.getValueAt(row, 1);
            }
        }

        if (dirname == null) {
            dirname = JOptionPane.showInputDialog(this, "Directory:");
        }

        if (dirname != null && !dirname.isEmpty()) {
            try {
                boolean success = ftpClient.changeWorkingDirectory(dirname);
                if (success) {
                    log("[SUCCESS] Changed to: " + dirname, RETRO_SUCCESS);
                    listFiles();
                } else {
                    log("[ERROR] CD failed", RETRO_ERROR);
                }
            } catch (IOException e) {
                log("[ERROR] CD error: " + e.getMessage(), RETRO_ERROR);
            }
        }
    }

    private void changeToParentDirectory() {
        if (!connected) return;

        try {
            boolean success = ftpClient.changeToParentDirectory();
            if (success) {
                log("[SUCCESS] Changed to parent directory", RETRO_SUCCESS);
                listFiles();
            } else {
                log("[ERROR] CDUP failed", RETRO_ERROR);
            }
        } catch (IOException e) {
            log("[ERROR] CDUP error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void printWorkingDirectory() {
        if (!connected) return;

        try {
            String pwd = ftpClient.printWorkingDirectory();
            log("[INFO] Current directory: " + pwd, RETRO_SUCCESS);
            currentDirLabel.setText(" Current: " + pwd);
        } catch (IOException e) {
            log("[ERROR] PWD error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void updateCurrentDirectory() {
        try {
            String pwd = ftpClient.printWorkingDirectory();
            if (pwd != null) {
                currentDirLabel.setText(" Current: " + pwd);
            }
        } catch (IOException e) {
            // Ignorar silenciosamente
        }
    }

    private void setOperationButtonsEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        uploadButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
        mkdirButton.setEnabled(enabled);
        rmdirButton.setEnabled(enabled);
        renameButton.setEnabled(enabled);
        cdButton.setEnabled(enabled);
        cdupButton.setEnabled(enabled);
        pwdButton.setEnabled(enabled);
    }

    private void log(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Punto de entrada para ejecutar la GUI del cliente.
     *
     * @param args Argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI();
            gui.setVisible(true);
        });
    }
}
