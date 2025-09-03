import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;

public class LibraryGUI extends JFrame {
    // --- DB configuration (edit password) ---
    public static final String DB_URL  = "jdbc:mysql://localhost:3306/librarydb";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "sony"; // <<--- replace

    private DefaultTableModel tableModel;
    private JTable booksTable;

    public LibraryGUI() {
        setTitle("Library Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 420);
        setLocationRelativeTo(null);
        initUI();
        loadAllBooks(); // initial load
    }

    private void initUI() {
        // Left button panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(8, 1, 8, 8));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(180, 0));

        JButton addBtn    = new JButton("Add Book");
        JButton viewBtn   = new JButton("Refresh (View)");
        JButton borrowBtn = new JButton("Borrow Book");
        JButton returnBtn = new JButton("Return Book");
        JButton searchBtn = new JButton("Search Book");
        JButton exportBtn = new JButton("Export CSV");
        JButton logoutBtn = new JButton("Logout");
        JButton exitBtn   = new JButton("Exit");

        leftPanel.add(addBtn);
        leftPanel.add(viewBtn);
        leftPanel.add(borrowBtn);
        leftPanel.add(returnBtn);
        leftPanel.add(searchBtn);
        leftPanel.add(exportBtn);
        leftPanel.add(logoutBtn);
        leftPanel.add(exitBtn);

        // Center table area
        String[] cols = {"ID", "Title", "Author", "Available"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        booksTable = new JTable(tableModel);
        JScrollPane centerScroll = new JScrollPane(booksTable);

        // Layout
        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(leftPanel, BorderLayout.WEST);
        getContentPane().add(centerScroll, BorderLayout.CENTER);

        // Button actions
        addBtn.addActionListener(e -> openAddBookDialog());
        viewBtn.addActionListener(e -> loadAllBooks());
        borrowBtn.addActionListener(e -> openBorrowDialog());
        returnBtn.addActionListener(e -> openReturnDialog());
        searchBtn.addActionListener(e -> openSearchDialog());
        exportBtn.addActionListener(e -> exportToCSV());
        logoutBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
        exitBtn.addActionListener(e -> System.exit(0));
    }

    // ---------- DB helpers ----------
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void loadAllBooks() {
        tableModel.setRowCount(0);
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books ORDER BY id")) {

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("available") ? "Yes" : "No"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- Add Book ----------
    private void openAddBookDialog() {
        JPanel panel = new JPanel(new GridLayout(2,2,6,6));
        JTextField titleField  = new JTextField();
        JTextField authorField = new JTextField();
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Author:"));
        panel.add(authorField);

        int option = JOptionPane.showConfirmDialog(this, panel, "Add Book", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) return;

        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        if (title.isEmpty() || author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Both Title and Author are required.");
            return;
        }

        try (Connection c = getConnection()) {
            PreparedStatement check = c.prepareStatement("SELECT 1 FROM books WHERE title = ?");
            check.setString(1, title);
            ResultSet rs = check.executeQuery();
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "A book with this title already exists.");
                return;
            }
            PreparedStatement insert = c.prepareStatement("INSERT INTO books (title, author, available) VALUES (?, ?, TRUE)");
            insert.setString(1, title);
            insert.setString(2, author);
            insert.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book added.");
            loadAllBooks();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding book: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- Borrow Book ----------
    private void openBorrowDialog() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Book ID to borrow:");
        if (idStr == null) return;
        try {
            int id = Integer.parseInt(idStr.trim());
            try (Connection c = getConnection()) {
                PreparedStatement check = c.prepareStatement("SELECT available FROM books WHERE id = ?");
                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Book ID not found.");
                    return;
                }
                if (!rs.getBoolean("available")) {
                    JOptionPane.showMessageDialog(this, "Book is already borrowed.");
                    return;
                }
                PreparedStatement upd = c.prepareStatement("UPDATE books SET available = FALSE WHERE id = ?");
                upd.setInt(1, id);
                upd.executeUpdate();
                JOptionPane.showMessageDialog(this, "Book borrowed successfully.");
                loadAllBooks();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- Return Book ----------
    private void openReturnDialog() {
        String idStr = JOptionPane.showInputDialog(this, "Enter Book ID to return:");
        if (idStr == null) return;
        try {
            int id = Integer.parseInt(idStr.trim());
            try (Connection c = getConnection()) {
                PreparedStatement check = c.prepareStatement("SELECT available FROM books WHERE id = ?");
                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Book ID not found.");
                    return;
                }
                if (rs.getBoolean("available")) {
                    JOptionPane.showMessageDialog(this, "Book is not currently borrowed.");
                    return;
                }
                PreparedStatement upd = c.prepareStatement("UPDATE books SET available = TRUE WHERE id = ?");
                upd.setInt(1, id);
                upd.executeUpdate();
                JOptionPane.showMessageDialog(this, "Book returned successfully.");
                loadAllBooks();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric ID.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- Search ----------
    private void openSearchDialog() {
        String keyword = JOptionPane.showInputDialog(this, "Enter title or author to search:");
        if (keyword == null) return;
        keyword = keyword.trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.");
            return;
        }

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Title","Author","Available"}, 0);
        try (Connection c = getConnection()) {
            PreparedStatement st = c.prepareStatement("SELECT * FROM books WHERE title LIKE ? OR author LIKE ? ORDER BY id");
            String like = "%" + keyword + "%";
            st.setString(1, like);
            st.setString(2, like);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("available") ? "Yes" : "No"
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error searching: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        JDialog dlg = new JDialog(this, "Search Results", true);
        dlg.setSize(520, 320);
        JTable t = new JTable(model);
        dlg.add(new JScrollPane(t));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ---------- Export to CSV ----------
    private void exportToCSV() {
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books ORDER BY id")) {

            FileWriter fw = new FileWriter("books_export.csv");
            fw.append("ID,Title,Author,Available\n");
            while (rs.next()) {
                fw.append(rs.getInt("id") + ",");
                fw.append("\"" + rs.getString("title").replace("\"","\"\"") + "\",");
                fw.append("\"" + rs.getString("author").replace("\"","\"\"") + "\",");
                fw.append(rs.getBoolean("available") ? "Yes" : "No");
                fw.append("\n");
            }
            fw.flush();
            fw.close();
            JOptionPane.showMessageDialog(this, "Exported to books_export.csv");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exporting: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---------- Login Frame ----------
    public static class LoginFrame extends JFrame {
        private JTextField userField;
        private JPasswordField passField;

        public LoginFrame() {
            setTitle("Login - Library");
            setSize(320, 180);
            setLayout(new GridLayout(4,2,6,6));
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            add(new JLabel("Username:"));
            userField = new JTextField();
            add(userField);

            add(new JLabel("Password:"));
            passField = new JPasswordField();
            add(passField);

            JButton loginBtn = new JButton("Login");
            JButton signupBtn = new JButton("Signup");
            JButton quitBtn  = new JButton("Quit");
            add(loginBtn);
            add(signupBtn);
            add(quitBtn);

            loginBtn.addActionListener(e -> attemptLogin());
            signupBtn.addActionListener(e -> {
                dispose();
                SwingUtilities.invokeLater(() -> new SignupFrame().setVisible(true));
            });
            quitBtn.addActionListener(e -> System.exit(0));
        }

        private void attemptLogin() {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username & password.");
                return;
            }
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement st = c.prepareStatement("SELECT id FROM users WHERE username=? AND password=?")) {
                st.setString(1, u);
                st.setString(2, p);
                ResultSet rs = st.executeQuery();
                if (rs.next()) {
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        LibraryGUI gui = new LibraryGUI();
                        gui.setVisible(true);
                    });
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ---------- Signup Frame ----------
    public static class SignupFrame extends JFrame {
        private JTextField userField;
        private JPasswordField passField;

        public SignupFrame() {
            setTitle("Signup - Library");
            setSize(320, 180);
            setLayout(new GridLayout(3,2,6,6));
            setLocationRelativeTo(null);
            setDefaultCloseOperation(EXIT_ON_CLOSE);

            add(new JLabel("Username:"));
            userField = new JTextField();
            add(userField);

            add(new JLabel("Password:"));
            passField = new JPasswordField();
            add(passField);

            JButton registerBtn = new JButton("Register");
            JButton backBtn     = new JButton("Back");
            add(registerBtn);
            add(backBtn);

            registerBtn.addActionListener(e -> registerUser());
            backBtn.addActionListener(e -> {
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            });
        }

        private void registerUser() {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.");
                return;
            }

            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement st = c.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
                st.setString(1, u);
                st.setString(2, p);
                st.executeUpdate();
                JOptionPane.showMessageDialog(this, "Signup successful!");
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            } catch (SQLIntegrityConstraintViolationException ex) {
                JOptionPane.showMessageDialog(this, "Username already exists!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Signup error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // ---------- main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
