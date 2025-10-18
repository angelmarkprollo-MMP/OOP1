/**
 * Person.java
 * Base class representing a person in the system.
 */

public class Person {
    protected String id;
    protected String name;

    public Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Polymorphic method to display info. Overridden by subclasses.
    public void displayInfo() {
        System.out.println("Person ID: " + id);
        System.out.println("Name: " + name);
    }

    // getters
    public String getId() { return id; }
    public String getName() { return name; }

    // setters
    public void setName(String name) { this.name = name; }
}