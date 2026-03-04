package FTP.Admin;

import FTP.Server.SqliteUserStore;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Panel de administración de usuarios FTP (estilo retro, misma estética que ClientGUI).
 * Lista, añade, edita y activa/desactiva usuarios en la base SQLite.
 * Lee la ruta de la base desde server.properties (ftp.users.database) o permite indicarla manualmente.
 *
 * @author Eduardo Díaz Sánchez
 * @version 1.0
 */
public class AdminGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final Color RETRO_BG = new Color(20, 20, 35);
    private static final Color RETRO_FG = new Color(255, 200, 100);
    private static final Color RETRO_HEADER_BG = new Color(35, 35, 55);
    private static final Color RETRO_BUTTON_BG = new Color(50, 50, 80);
    private static final Color RETRO_SUCCESS = new Color(100, 255, 100);
    private static final Color RETRO_ERROR = new Color(255, 100, 100);

    private static final String TABLE = SqliteUserStore.TABLE;
    private static final String CREATE_TABLE = SqliteUserStore.CREATE_TABLE;

    private JTextField dbPathField;
    private JButton loadDbButton;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JTextPane logArea;
    private JButton refreshButton;
    private JButton addButton;
    private JButton editButton;
    private JButton toggleButton;
    private String currentDbPath;

    public AdminGUI() {
        setTitle("FTP Admin - Usuarios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(RETRO_BG);
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        main.add(createTopPanel(), BorderLayout.NORTH);
        main.add(createCenterPanel(), BorderLayout.CENTER);
        main.add(createSouthPanel(), BorderLayout.SOUTH);
        add(main);

        loadDbPathFromConfig();
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(RETRO_HEADER_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        Font font = new Font("Consolas", Font.PLAIN, 12);
        JLabel label = new JLabel("Base de datos:");
        label.setForeground(RETRO_FG);
        label.setFont(font);
        panel.add(label);
        dbPathField = new JTextField(40);
        styleTextField(dbPathField, font);
        panel.add(dbPathField);
        loadDbButton = createStyledButton("Cargar / Abrir", font);
        loadDbButton.addActionListener(e -> openDatabase());
        panel.add(loadDbButton);
        return panel;
    }

    private JPanel createCenterPanel() {
        String[] columns = {"Usuario", "Perfil", "Estado", "Fecha creación"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleTable(userTable);
        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
        scroll.getViewport().setBackground(RETRO_BG);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttons.setBackground(RETRO_BG);
        Font btnFont = new Font("Consolas", Font.BOLD, 11);
        refreshButton = createStyledButton("Actualizar", btnFont);
        refreshButton.addActionListener(e -> loadUsers());
        addButton = createStyledButton("Añadir usuario", btnFont);
        addButton.addActionListener(e -> addUser());
        editButton = createStyledButton("Editar", btnFont);
        editButton.addActionListener(e -> editUser());
        toggleButton = createStyledButton("Activar/Desactivar", btnFont);
        toggleButton.addActionListener(e -> toggleEnabled());
        buttons.add(refreshButton);
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(toggleButton);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(RETRO_BG);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1), "Usuarios FTP",
            0, 0, new Font("Consolas", Font.BOLD, 12), RETRO_FG
        ));
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSouthPanel() {
        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setBackground(RETRO_BG);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
        logArea.setPreferredSize(new Dimension(600, 80));
        JScrollPane scroll = new JScrollPane(logArea);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(RETRO_HEADER_BG);
        panel.setBorder(new EmptyBorder(5, 0, 0, 0));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleTextField(JTextField f, Font font) {
        f.setBackground(RETRO_BG);
        f.setForeground(RETRO_FG);
        f.setCaretColor(RETRO_FG);
        f.setFont(font);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1),
            new EmptyBorder(3, 5, 3, 5)
        ));
    }

    private JButton createStyledButton(String text, Font font) {
        JButton b = new JButton(text);
        b.setBackground(RETRO_BUTTON_BG);
        b.setForeground(RETRO_FG);
        b.setFont(font);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(RETRO_FG, 1),
            new EmptyBorder(5, 10, 5, 10)
        ));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(RETRO_FG);
                    b.setForeground(RETRO_BG);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(RETRO_BUTTON_BG);
                b.setForeground(RETRO_FG);
            }
        });
        return b;
    }

    private void styleTable(JTable t) {
        t.setBackground(RETRO_BG);
        t.setForeground(RETRO_FG);
        t.setGridColor(RETRO_BUTTON_BG);
        t.setFont(new Font("Consolas", Font.PLAIN, 11));
        t.setRowHeight(22);
        t.setSelectionBackground(RETRO_BUTTON_BG);
        t.setSelectionForeground(RETRO_SUCCESS);
        t.getTableHeader().setBackground(RETRO_HEADER_BG);
        t.getTableHeader().setForeground(RETRO_FG);
        t.getTableHeader().setBorder(BorderFactory.createLineBorder(RETRO_FG, 1));
    }

    private void log(String msg, Color c) {
        javax.swing.text.StyledDocument doc = logArea.getStyledDocument();
        javax.swing.text.SimpleAttributeSet attr = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setForeground(attr, c);
        javax.swing.text.StyleConstants.setFontFamily(attr, "Consolas");
        javax.swing.text.StyleConstants.setFontSize(attr, 11);
        try {
            doc.insertString(doc.getLength(), msg + "\n", attr);
        } catch (javax.swing.text.BadLocationException ignored) { }
        logArea.setCaretPosition(doc.getLength());
    }

    private void loadDbPathFromConfig() {
        try {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream("server.properties")) {
                p.load(fis);
            }
            String db = p.getProperty("ftp.users.database", "").trim();
            if (!db.isEmpty()) {
                dbPathField.setText(db);
                currentDbPath = db;
                loadUsers();
            }
        } catch (IOException ignored) {
            // no server.properties or not readable
        }
    }

    private void openDatabase() {
        String path = dbPathField.getText().trim();
        if (path.isEmpty()) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Seleccionar base de datos SQLite");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                path = fc.getSelectedFile().getAbsolutePath();
                dbPathField.setText(path);
            } else {
                return;
            }
        }
        currentDbPath = path;
        loadUsers();
        log("Base cargada: " + path, RETRO_SUCCESS);
    }

    private Connection getConnection() throws SQLException {
        if (currentDbPath == null || currentDbPath.isEmpty()) {
            throw new SQLException("Indique la ruta de la base de datos y pulse Cargar.");
        }
        File f = new File(currentDbPath);
        return DriverManager.getConnection("jdbc:sqlite:" + f.getAbsolutePath());
    }

    /** Crea la tabla ftp_users si no existe (permite usar una base nueva sin haber arrancado el servidor). */
    private void ensureSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(CREATE_TABLE);
        }
    }

    private void loadUsers() {
        if (currentDbPath == null || currentDbPath.isEmpty()) {
            log("Primero cargue la base de datos (ruta + Cargar).", RETRO_ERROR);
            return;
        }
        tableModel.setRowCount(0);
        try (Connection conn = getConnection()) {
            ensureSchema(conn);
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT username, profile, enabled, created_at FROM " + TABLE + " ORDER BY username")) {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                while (rs.next()) {
                    String user = rs.getString(1);
                    String profile = rs.getString(2);
                    int en = rs.getInt(3);
                    String state = en != 0 ? "Activo" : "Desactivado";
                    String created = rs.getString(4);
                    if (created != null && !created.isEmpty()) {
                        try {
                            long ts = Long.parseLong(created);
                            created = fmt.format(new Date(ts));
                        } catch (NumberFormatException ignored) {
                            // keep as string
                        }
                    } else {
                        created = "-";
                    }
                    tableModel.addRow(new Object[]{user, profile, state, created});
                }
                log("Listados " + tableModel.getRowCount() + " usuarios.", RETRO_SUCCESS);
            }
        } catch (SQLException e) {
            log("Error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void addUser() {
        if (currentDbPath == null || currentDbPath.isEmpty()) {
            log("Cargue la base de datos primero.", RETRO_ERROR);
            return;
        }
        JTextField userField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);
        JComboBox<String> profileCombo = new JComboBox<>(new String[]{"BASICO", "INTERMEDIO", "ADMINISTRADOR"});
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Usuario:"));
        p.add(userField);
        p.add(new JLabel("Contraseña:"));
        p.add(passField);
        p.add(new JLabel("Perfil:"));
        p.add(profileCombo);
        int r = JOptionPane.showConfirmDialog(this, p, "Añadir usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());
        String profile = (String) profileCombo.getSelectedItem();
        if (username.isEmpty() || password.isEmpty()) {
            log("Usuario y contraseña obligatorios.", RETRO_ERROR);
            return;
        }
        if (username.contains(":")) {
            log("El usuario no puede contener ':'", RETRO_ERROR);
            return;
        }
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        long now = System.currentTimeMillis();
        try (Connection conn = getConnection()) {
            ensureSchema(conn);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + TABLE + " (username, password_hash, profile, enabled, created_at) VALUES (?,?,?,1,?)")) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.setString(3, profile);
                ps.setString(4, String.valueOf(now));
                ps.executeUpdate();
            }
            log("Usuario '" + username + "' añadido.", RETRO_SUCCESS);
            loadUsers();
        } catch (SQLException e) {
            log("Error al añadir: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void editUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            log("Seleccione un usuario.", RETRO_ERROR);
            return;
        }
        String username = (String) tableModel.getValueAt(row, 0);
        String currentProfile = (String) tableModel.getValueAt(row, 1);
        JPasswordField passField = new JPasswordField(20);
        JComboBox<String> profileCombo = new JComboBox<>(new String[]{"BASICO", "INTERMEDIO", "ADMINISTRADOR"});
        profileCombo.setSelectedItem(currentProfile);
        JPanel p = new JPanel(new GridLayout(0, 1, 5, 5));
        p.add(new JLabel("Usuario (solo lectura): " + username));
        p.add(new JLabel("Nueva contraseña (dejar vacío para no cambiar):"));
        p.add(passField);
        p.add(new JLabel("Perfil:"));
        p.add(profileCombo);
        int r = JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        String password = new String(passField.getPassword());
        String profile = (String) profileCombo.getSelectedItem();
        try (Connection conn = getConnection()) {
            if (!password.isEmpty()) {
                String hash = BCrypt.hashpw(password, BCrypt.gensalt());
                try (PreparedStatement ps = conn.prepareStatement("UPDATE " + TABLE + " SET password_hash=?, profile=? WHERE username=?")) {
                    ps.setString(1, hash);
                    ps.setString(2, profile);
                    ps.setString(3, username);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE " + TABLE + " SET profile=? WHERE username=?")) {
                    ps.setString(1, profile);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }
            }
            log("Usuario '" + username + "' actualizado.", RETRO_SUCCESS);
            loadUsers();
        } catch (SQLException e) {
            log("Error al editar: " + e.getMessage(), RETRO_ERROR);
        }
    }

    private void toggleEnabled() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            log("Seleccione un usuario.", RETRO_ERROR);
            return;
        }
        String username = (String) tableModel.getValueAt(row, 0);
        String state = (String) tableModel.getValueAt(row, 2);
        int newEnabled = "Activo".equals(state) ? 0 : 1;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE " + TABLE + " SET enabled=? WHERE username=?")) {
            ps.setInt(1, newEnabled);
            ps.setString(2, username);
            ps.executeUpdate();
            log("Usuario '" + username + "' " + (newEnabled != 0 ? "activado" : "desactivado") + ".", RETRO_SUCCESS);
            loadUsers();
        } catch (SQLException e) {
            log("Error: " + e.getMessage(), RETRO_ERROR);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminGUI gui = new AdminGUI();
            gui.setVisible(true);
        });
    }
}
