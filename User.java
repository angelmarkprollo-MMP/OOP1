import java.util.ArrayList;

/**
 * User.java
 * Subclass of Person. Holds login data, role and borrowed books list.
 */

public class User extends Person {
    private String password;
    private String role; // "admin" or "user"
    private ArrayList<String> borrowedBooks; // list of BookIDs

    public User(String id, String name, String password, String role) {
        super(id, name);
        this.password = password;
        this.role = role;
        this.borrowedBooks = new ArrayList<>();
    }

    // Override displayInfo to show user-specific details (Polymorphism example)
    @Override
    public void displayInfo() {
        System.out.println("User ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Role: " + role);
        System.out.println("Borrowed books: " + borrowedBooks);
    }

    // Basic CRUD-like methods can be implemented in LibrarySystem; these are helpers.
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public ArrayList<String> getBorrowedBooks() { return borrowedBooks; }

    public void addBorrowedBook(String bookId) {
        borrowedBooks.add(bookId);
    }

    public void removeBorrowedBook(String bookId) {
        borrowedBooks.remove(bookId);
    }

    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}