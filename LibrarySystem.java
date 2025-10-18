import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;


public class LibrarySystem {
    private List<Book> books;
    private List<User> users;
    private List<Transaction> transactions;
    private User loggedInUser;
    private Scanner scanner;

    // File names (can be moved to constants or config)
    private static final String USERS_FILE = "users.txt";
    private static final String BOOKS_FILE = "books.txt";
    private static final String TRANSACTIONS_FILE = "transactions.txt";

    public LibrarySystem() {
        books = new ArrayList<>();
        users = new ArrayList<>();
        transactions = new ArrayList<>();
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        LibrarySystem ls = new LibrarySystem();
        try {
            ls.loadData();
            ls.start();
            ls.saveData();
        } catch (FileNotFoundException fnfe) {
            System.err.println("One or more required files were not found: " + fnfe.getMessage());
        } catch (IOException ioe) {
            System.err.println("An I/O error occurred: " + ioe.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Load books, users, transactions. Reconstruct borrowedBooks from transactions.
    private void loadData() throws IOException {
        loadBooks();
        loadUsers();
        loadTransactions();
        reconstructBorrowedBooksFromTransactions();
    }

    private void loadBooks() throws IOException {
        Path p = Paths.get(BOOKS_FILE);
        if (!Files.exists(p)) throw new FileNotFoundException(BOOKS_FILE);
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Format: B001,The Great Gatsby,F. Scott Fitzgerald,true
                String[] parts = line.split(",", 4);
                if (parts.length < 4) continue;
                String id = parts[0].trim();
                String title = parts[1].trim();
                String author = parts[2].trim();
                boolean available = Boolean.parseBoolean(parts[3].trim());
                books.add(new Book(id, title, author, available));
            }
        }
    }

    private void loadUsers() throws IOException {
        Path p = Paths.get(USERS_FILE);
        if (!Files.exists(p)) throw new FileNotFoundException(USERS_FILE);
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Format: U001,John Doe,pass123,user
                String[] parts = line.split(",", 4);
                if (parts.length < 4) continue;
                String id = parts[0].trim();
                String name = parts[1].trim();
                String password = parts[2].trim();
                String role = parts[3].trim();
                users.add(new User(id, name, password, role));
            }
        }
    }

    private void loadTransactions() throws IOException {
        Path p = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(p)) {
            // If file missing, create empty file. But throw FileNotFound per specs? We'll create empty to proceed.
            Files.createFile(p);
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Format: T001,U001,B002,2025-10-14,null
                String[] parts = line.split(",", 5);
                if (parts.length < 5) continue;
                String tid = parts[0].trim();
                String uid = parts[1].trim();
                String bid = parts[2].trim();
                LocalDate borrowed = parseDateOrNull(parts[3].trim());
                LocalDate returned = parseDateOrNull(parts[4].trim());
                transactions.add(new Transaction(tid, uid, bid, borrowed, returned));
            }
        }
    }

    private LocalDate parseDateOrNull(String s) {
        if (s.equalsIgnoreCase("null") || s.isEmpty()) return null;
        return LocalDate.parse(s);
    }

    // After loading transactions, populate user.borrowedBooks for unreturned transactions
    private void reconstructBorrowedBooksFromTransactions() {
        // Clear any existing (should be empty)
        for (User u : users) u.getBorrowedBooks().clear();

        for (Transaction t : transactions) {
            if (t.getDateReturned() == null) {
                // find user
                User u = findUserById(t.getUserId());
                if (u != null) {
                    u.addBorrowedBook(t.getBookId());
                }
            }
        }
    }

    private void start() {
        System.out.println("Welcome to the Library Management System");
        System.out.println("----------------------------------------");
        System.out.println("Please log in to continue.");
        boolean success = login();

        if (!success) {
            System.out.println("Too many failed attempts. Exiting.");
            return;
        }

        boolean exit = false;
        while (!exit) {
            displayMenu();
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        viewAllBooks();
                        break;
                    case "2":
                        borrowBook();
                        break;
                    case "3":
                        returnBook();
                        break;
                    case "4":
                        searchBooks();
                        break;
                    case "5":
                        if (loggedInUser.getRole().equalsIgnoreCase("admin")) {
                            adminMenu();
                        } else {
                            System.out.println("Invalid choice.");
                        }
                        break;
                    case "0":
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (NullPointerException npe) {
                System.err.println("Unexpected missing data: " + npe.getMessage());
            } catch (IOException ioe) {
                System.err.println("I/O error occurred while processing your request: " + ioe.getMessage());
            }
            System.out.println();
        }

        System.out.println("Exiting... Saving data.");
    }

    private boolean login() {
        int attempts = 3;
        while (attempts > 0) {
            System.out.print("Username (Name): ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            // Find user by name (the sample flow uses "John Doe" as username)
            Optional<User> optUser = users.stream()
                    .filter(u -> u.getName().equalsIgnoreCase(username) && u.getPassword().equals(password))
                    .findFirst();

            if (optUser.isPresent()) {
                loggedInUser = optUser.get();
                System.out.println("\nLogin successful! Welcome, " + loggedInUser.getName() + ".");
                System.out.println();
                return true;
            } else {
                attempts--;
                if (attempts > 0) {
                    System.out.println("Invalid username or password. Try again.");
                    System.out.println("(Attempts left: " + attempts + ")");
                }
            }
        }
        return false;
    }

    private void displayMenu() {
        System.out.println("1. View All Books");
        System.out.println("2. Borrow Book");
        System.out.println("3. Return Book");
        System.out.println("4. Search Books (by title/author)");
        if (loggedInUser.getRole().equalsIgnoreCase("admin")) {
            System.out.println("5. Admin Menu (Users, Books, Transactions)");
        }
        System.out.println("0. Exit");
    }

    private void viewAllBooks() {
        System.out.println("BookID | Title | Author | Available");
        System.out.println("------------------------------------------------------");
        for (Book b : books) {
            b.displayBookDetails();
        }
    }

    private void borrowBook() throws IOException {
        // Check borrow limit (bonus)
        if (loggedInUser.getBorrowedBooks().size() >= 3) {
            System.out.println("You have reached the maximum of 3 borrowed books. Return a book before borrowing another.");
            return;
        }
        System.out.print("Enter Book ID to borrow: ");
        String bookId = scanner.nextLine().trim();
        Book book = findBookById(bookId);
        if (book == null) {
            System.out.println("Book not found.");
            return;
        }
        if (!book.isAvailable()) {
            System.out.println("Sorry, the book is currently not available.");
            return;
        }

        // Mark book unavailable
        book.setAvailable(false);
        // Create transaction
        String newTid = generateNextTransactionId();
        Transaction t = new Transaction(newTid, loggedInUser.getId(), bookId, LocalDate.now(), null);
        transactions.add(t);
        loggedInUser.addBorrowedBook(bookId);
        System.out.println("Book borrowed successfully! Transaction ID: " + newTid);
    }

    private void returnBook() {
        System.out.print("Enter Book ID to return: ");
        String bookId = scanner.nextLine().trim();
        // Find an active transaction for this user & book (dateReturned == null)
        Optional<Transaction> opt = transactions.stream()
                .filter(tr -> tr.getUserId().equals(loggedInUser.getId())
                        && tr.getBookId().equals(bookId)
                        && tr.getDateReturned() == null)
                .findFirst();
        if (!opt.isPresent()) {
            System.out.println("No borrowing record found for this book under your account.");
            return;
        }
        Transaction tr = opt.get();
        tr.setDateReturned(LocalDate.now());
        // Mark book available
        Book b = findBookById(bookId);
        if (b != null) {
            b.setAvailable(true);
        }
        loggedInUser.removeBorrowedBook(bookId);
        System.out.println("Book returned successfully. Transaction updated: " + tr.getTransactionId());
    }

    private void searchBooks() {
        System.out.print("Enter title or author to search: ");
        String q = scanner.nextLine().trim().toLowerCase();
        boolean found = false;
        System.out.println("Search results:");
        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(q) || b.getAuthor().toLowerCase().contains(q)) {
                b.displayBookDetails();
                found = true;
            }
        }
        if (!found) {
            System.out.println("No books matched your search.");
        }
    }

    // ADMIN MENU: accessible only to admins
    private void adminMenu() throws IOException {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Manage Users");
            System.out.println("2. Manage Books");
            System.out.println("3. View Transactions");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    manageUsers();
                    break;
                case "2":
                    manageBooks();
                    break;
                case "3":
                    viewTransactionsMenu();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // Admin: Manage Users (Add, Update, Delete, Display)
    private void manageUsers() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Manage Users ---");
            System.out.println("1. Add User");
            System.out.println("2. Update User");
            System.out.println("3. Delete User");
            System.out.println("4. Display Users");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    addUser();
                    break;
                case "2":
                    updateUser();
                    break;
                case "3":
                    deleteUser();
                    break;
                case "4":
                    displayUsers();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void addUser() {
        System.out.print("Enter new User ID (e.g., U003): ");
        String id = scanner.nextLine().trim();
        if (findUserById(id) != null) {
            System.out.println("User ID already exists.");
            return;
        }
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter password: ");
        String pass = scanner.nextLine().trim();
        System.out.print("Enter role (user/admin): ");
        String role = scanner.nextLine().trim();
        if (!role.equalsIgnoreCase("user") && !role.equalsIgnoreCase("admin")) {
            System.out.println("Invalid role. Defaulting to 'user'.");
            role = "user";
        }
        users.add(new User(id, name, pass, role));
        System.out.println("User added.");
    }

    private void updateUser() {
        System.out.print("Enter User ID to update: ");
        String id = scanner.nextLine().trim();
        User u = findUserById(id);
        if (u == null) {
            System.out.println("User not found.");
            return;
        }
        System.out.print("Enter new name (leave blank to keep): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) u.setName(name);
        System.out.print("Enter new password (leave blank to keep): ");
        String pass = scanner.nextLine().trim();
        if (!pass.isEmpty()) u.setPassword(pass);
        System.out.print("Enter new role (user/admin) (leave blank to keep): ");
        String role = scanner.nextLine().trim();
        if (!role.isEmpty()) u.setRole(role);
        System.out.println("User updated.");
    }

    private void deleteUser() {
        System.out.print("Enter User ID to delete: ");
        String id = scanner.nextLine().trim();
        User u = findUserById(id);
        if (u == null) {
            System.out.println("User not found.");
            return;
        }
        // Prevent deleting the logged-in admin accidentally
        if (u.getId().equals(loggedInUser.getId())) {
            System.out.println("Cannot delete the currently logged-in user.");
            return;
        }
        users.remove(u);
        System.out.println("User deleted.");
    }

    private void displayUsers() {
        System.out.println("Users:");
        for (User u : users) {
            u.displayInfo();
            System.out.println("---------------------------------");
        }
    }

    // Admin: Manage Books (Add, Update, Delete, Display)
    private void manageBooks() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Manage Books ---");
            System.out.println("1. Add Book");
            System.out.println("2. Update Book");
            System.out.println("3. Delete Book");
            System.out.println("4. Display Books");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    addBook();
                    break;
                case "2":
                    updateBook();
                    break;
                case "3":
                    deleteBook();
                    break;
                case "4":
                    viewAllBooks();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void addBook() {
        System.out.print("Enter new Book ID (e.g., B004): ");
        String id = scanner.nextLine().trim();
        if (findBookById(id) != null) {
            System.out.println("Book ID already exists.");
            return;
        }
        System.out.print("Enter title: ");
        String title = scanner.nextLine().trim();
        System.out.print("Enter author: ");
        String author = scanner.nextLine().trim();
        books.add(new Book(id, title, author, true));
        System.out.println("Book added.");
    }

    private void updateBook() {
        System.out.print("Enter Book ID to update: ");
        String id = scanner.nextLine().trim();
        Book b = findBookById(id);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        System.out.print("Enter new title (leave blank to keep): ");
        String title = scanner.nextLine().trim();
        if (!title.isEmpty()) b.setTitle(title);
        System.out.print("Enter new author (leave blank to keep): ");
        String author = scanner.nextLine().trim();
        if (!author.isEmpty()) b.setAuthor(author);
        System.out.print("Set availability (true/false) or leave blank to keep: ");
        String av = scanner.nextLine().trim();
        if (!av.isEmpty()) b.setAvailable(Boolean.parseBoolean(av));
        System.out.println("Book updated.");
    }

    private void deleteBook() {
        System.out.print("Enter Book ID to delete: ");
        String id = scanner.nextLine().trim();
        Book b = findBookById(id);
        if (b == null) {
            System.out.println("Book not found.");
            return;
        }
        // Ensure book is not currently borrowed
        boolean currentlyBorrowed = transactions.stream()
                .anyMatch(tr -> tr.getBookId().equals(id) && tr.getDateReturned() == null);
        if (currentlyBorrowed) {
            System.out.println("Cannot delete a book that is currently borrowed.");
            return;
        }
        books.remove(b);
        System.out.println("Book deleted.");
    }

    // Admin: Transactions viewing
    private void viewTransactionsMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Transactions ---");
            System.out.println("1. View all transactions");
            System.out.println("2. View by user");
            System.out.println("3. View by book");
            System.out.println("0. Back");
            System.out.print("Choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    viewAllTransactions();
                    break;
                case "2":
                    viewTransactionsByUser();
                    break;
                case "3":
                    viewTransactionsByBook();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private void viewAllTransactions() {
        for (Transaction t : transactions) {
            t.displayTransaction();
        }
    }

    private void viewTransactionsByUser() {
        System.out.print("Enter User ID (e.g., U001): ");
        String uid = scanner.nextLine().trim();
        boolean found = false;
        for (Transaction t : transactions) {
            if (t.getUserId().equals(uid)) {
                t.displayTransaction();
                found = true;
            }
        }
        if (!found) System.out.println("No transactions for that user.");
    }

    private void viewTransactionsByBook() {
        System.out.print("Enter Book ID (e.g., B001): ");
        String bid = scanner.nextLine().trim();
        boolean found = false;
        for (Transaction t : transactions) {
            if (t.getBookId().equals(bid)) {
                t.displayTransaction();
                found = true;
            }
        }
        if (!found) System.out.println("No transactions for that book.");
    }

    // Utilities
    private Book findBookById(String bookId) {
        for (Book b : books) {
            if (b.getBookId().equalsIgnoreCase(bookId)) return b;
        }
        return null;
    }

    private User findUserById(String userId) {
        for (User u : users) {
            if (u.getId().equalsIgnoreCase(userId)) return u;
        }
        return null;
    }

    // Generate next transaction id automatically (e.g., T001 -> T002)
    private String generateNextTransactionId() {
        int max = 0;
        for (Transaction t : transactions) {
            String tid = t.getTransactionId().replaceAll("[^0-9]", "");
            try {
                int n = Integer.parseInt(tid);
                if (n > max) max = n;
            } catch (NumberFormatException ignore) {}
        }
        int next = max + 1;
        return String.format("T%03d", next);
    }

    // Save all data back to files. Use try-catch to surface IOExceptions.
    private void saveData() throws IOException {
        saveUsers();
        saveBooks();
        saveTransactions();
        System.out.println("All data saved successfully.");
    }

    private void saveUsers() throws IOException {
        Path p = Paths.get(USERS_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            for (User u : users) {
                // Note: borrowedBooks are not persisted in users.txt by spec.
                bw.write(String.format("%s,%s,%s,%s", u.getId(), u.getName(), u.getPassword(), u.getRole()));
                bw.newLine();
            }
        }
    }

    private void saveBooks() throws IOException {
        Path p = Paths.get(BOOKS_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            for (Book b : books) {
                bw.write(String.format("%s,%s,%s,%s", b.getBookId(), b.getTitle(), b.getAuthor(), b.isAvailable()));
                bw.newLine();
            }
        }
    }

    private void saveTransactions() throws IOException {
        Path p = Paths.get(TRANSACTIONS_FILE);
        try (BufferedWriter bw = Files.newBufferedWriter(p)) {
            for (Transaction t : transactions) {
                String borrowed = t.getDateBorrowed() != null ? t.getDateBorrowed().toString() : "null";
                String returned = t.getDateReturned() != null ? t.getDateReturned().toString() : "null";
                bw.write(String.format("%s,%s,%s,%s,%s", t.getTransactionId(), t.getUserId(), t.getBookId(), borrowed, returned));
                bw.newLine();
            }
        }
    }
}