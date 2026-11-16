
package backend;

/**
 * Represents one user from the users table along with their role.
 */
public class User {

    private final int userId;
    private final String fullName;
    private final String username;
    private final String email;
    private final boolean active;
    private final String role;

    public User(int userId, String fullName, String username,
                String email, boolean active, String role) {

        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.active = active;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public String getRole() {
        return role;
    }
}
