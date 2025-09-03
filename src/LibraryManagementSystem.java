import java.sql.*;
import java.util.Scanner;

public class LibraryManagementSystem {
    private static final String URL = "jdbc:mysql://localhost:3306/librarydb";
    private static final String USER = "root"; // your MySQL username
    private static final String PASSWORD = "sony"; // your MySQL password

    private Connection conn;
    private Scanner scanner;

    public LibraryManagementSystem() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            scanner = new Scanner(System.in);
            System.out.println("Connected to Database Successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void menu() {
        while (true) {
            System.out.println("\n===== Library Management System =====");
            System.out.println("1. Add Book");
            System.out.println("2. View Books");
            System.out.println("3. Borrow Book");
            System.out.println("4. Return Book");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> addBook();
                case 2 -> viewBooks();
                case 3 -> borrowBook();
                case 4 -> returnBook();
                case 5 -> {
                    System.out.println("Exiting...");
                    close();
                    return;
                }
                default -> System.out.println("Invalid choice, try again.");
            }
        }
    }

    private void addBook() {
        try {
            System.out.print("Enter book title: ");
            String title = scanner.nextLine();
            System.out.print("Enter author: ");
            String author = scanner.nextLine();

            String sql = "INSERT INTO books (title, author, available) VALUES (?, ?, 1)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.executeUpdate();
            System.out.println("Book added successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void viewBooks() {
        try {
            String sql = "SELECT * FROM books";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            System.out.println("\n--- Book List ---");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s\n",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getBoolean("available") ? "Available" : "Borrowed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void borrowBook() {
        try {
            System.out.print("Enter book ID to borrow: ");
            int id = scanner.nextInt();
            scanner.nextLine();

            String checkSql = "SELECT available FROM books WHERE id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getBoolean("available")) {
                String updateSql = "UPDATE books SET available = 0 WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setInt(1, id);
                updateStmt.executeUpdate();
                System.out.println("Book borrowed successfully.");
            } else {
                System.out.println("Book not available.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void returnBook() {
        try {
            System.out.print("Enter book ID to return: ");
            int id = scanner.nextInt();
            scanner.nextLine();

            String updateSql = "UPDATE books SET available = 1 WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(updateSql);
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Book returned successfully.");
            } else {
                System.out.println("Invalid book ID.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            if (conn != null) conn.close();
            if (scanner != null) scanner.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new LibraryManagementSystem().menu();
    }
}
