*OOPSSS FINAL FINAL FINAL CODE*
package rims;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/* ===========================
   Generic Utility
   =========================== */
class OperationResult<T> {
    public final boolean success;
    public final String message;
    public final T data;

    public OperationResult(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> OperationResult<T> ok(T data, String msg) {
        return new OperationResult<>(true, msg, data);
    }

    public static <T> OperationResult<T> fail(String msg) {
        return new OperationResult<>(false, msg, null);
    }
}

/* ===========================
   UserRole Interface
   =========================== */
interface UserRole {
    void showMenu();
    void viewProperties();
    default void viewFeedback() {}
}

/* ===========================
   ADMIN / OWNER CLASS
   =========================== */
class Admin implements UserRole {
    protected static final String DB_URL  = "jdbc:mysql://localhost:3306/rims";
    protected static final String DB_USER = "root";
    protected static final String DB_PASS = "root";

    protected static Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // --- Validation helpers available to subclasses ---
    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    public static LocalDate parseValidDate(String s) throws DateTimeParseException {
        LocalDate d = LocalDate.parse(s);
        int y = d.getYear();
        if (y < 2000 || y > 2100) throw new IllegalArgumentException("Year must be between 2000 and 2100.");
        return d;
    }

    @Override
    public void viewProperties() {
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT property_id, name, type, location, price_per_month, availability_status, sharing FROM property")) {

            System.out.println("\nID | Name | Type | Location | Price | Status | Sharing");
            boolean any = false;
            while (rs.next()) {
                any = true;
                Object sharingObj = rs.getObject("sharing");
                String sharing = sharingObj == null ? "-" : String.valueOf(rs.getInt("sharing"));
                System.out.printf("%d | %s | %s | %s | %.2f | %s | %s%n",
                        rs.getInt("property_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("location"),
                        rs.getDouble("price_per_month"),
                        rs.getString("availability_status"),
                        sharing);
            }
            if (!any) System.out.println("(No properties found)");
        } catch (Exception ex) {
            System.out.println("View failed: " + ex.getMessage());
        }
    }

    // Add property — asks for sharing when type is PG. (Assumes property table has 'sharing' column)
    protected void addProperty(Scanner sc) {
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO property (name, type, location, price_per_month, availability_status, sharing) VALUES (?,?,?,?,?,?)")) {

            System.out.print("Property Name: ");
            String name = sc.nextLine().trim();
            System.out.print("Type (PG/Apartment/House): ");
            String type = sc.nextLine().trim();
            System.out.print("Location: ");
            String location = sc.nextLine().trim();

            double price;
            while (true) {
                try {
                    System.out.print("Price per Month: ");
                    price = Double.parseDouble(sc.nextLine().trim());
                    if (price < 0) { System.out.println("Enter non-negative price."); continue; }
                    break;
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid number. Try again.");
                }
            }

            Integer sharing = null;
            if ("PG".equalsIgnoreCase(type)) {
                while (true) {
                    System.out.print("Sharing count (number of people sharing): ");
                    String s = sc.nextLine().trim();
                    try {
                        int sh = Integer.parseInt(s);
                        if (sh <= 0) { System.out.println("Enter positive integer."); continue; }
                        sharing = sh;
                        break;
                    } catch (NumberFormatException nfe) {
                        System.out.println("Invalid integer; try again.");
                    }
                }
            }

            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, location);
            ps.setDouble(4, price);
            ps.setString(5, "Available");
            if (sharing == null) ps.setNull(6, java.sql.Types.INTEGER);
            else ps.setInt(6, sharing);

            ps.executeUpdate();
            System.out.println("Property added!");
        } catch (Exception ex) {
            System.out.println("Add failed: " + ex.getMessage());
        }
    }

    protected void changePropertyAvailability(Scanner sc) {
        viewProperties();
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE property SET availability_status=? WHERE property_id=?")) {

            System.out.print("Enter Property ID to change status: ");
            int pid = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Enter new status (Available/Booked/Not Available): ");
            String status = sc.nextLine().trim();

            if (!status.equalsIgnoreCase("Available") &&
                !status.equalsIgnoreCase("Booked") &&
                !status.equalsIgnoreCase("Not Available")) {
                System.out.println("Invalid status. Use Available / Booked / Not Available.");
                return;
            }

            ps.setString(1, status);
            ps.setInt(2, pid);
            int updated = ps.executeUpdate();
            if (updated > 0) System.out.println("Status updated!");
            else System.out.println("No property found with that ID.");
        } catch (Exception ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    protected void deleteProperty(Scanner sc) {
        viewProperties();
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("DELETE FROM property WHERE property_id=?")) {
            System.out.print("Enter Property ID to delete: ");
            int pid = Integer.parseInt(sc.nextLine().trim());
            ps.setInt(1, pid);
            int del = ps.executeUpdate();
            if (del > 0) System.out.println("Property deleted.");
            else System.out.println("No property found.");
        } catch (SQLIntegrityConstraintViolationException tie) {
            System.out.println("Cannot delete: property is referenced by bookings or residents.");
        } catch (Exception ex) {
            System.out.println("Delete failed: " + ex.getMessage());
        }
    }

    /* Admin: change booking status */
    protected void changeBookingStatus(Scanner sc) {
        try (Connection con = connect();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT b.booking_id, u.name AS user, p.name AS property, b.status " +
                     "FROM booking b JOIN user u ON b.user_id=u.user_id " +
                     "JOIN property p ON b.property_id=p.property_id")) {

            System.out.println("\nBooking ID | User | Property | Status");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.printf("%d | %s | %s | %s%n",
                        rs.getInt("booking_id"),
                        rs.getString("user"),
                        rs.getString("property"),
                        rs.getString("status"));
            }
            if (!any) {
                System.out.println("(No bookings found)");
                return;
            }
        } catch (Exception ex) {
            System.out.println("Error fetching bookings: " + ex.getMessage());
            return;
        }

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement("UPDATE booking SET status=? WHERE booking_id=?")) {

            System.out.print("Enter Booking ID to update: ");
            int bid = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Enter new status (Active / Cancelled / Completed): ");
            String newStatus = sc.nextLine().trim();

            if (!newStatus.equalsIgnoreCase("Active")
                && !newStatus.equalsIgnoreCase("Cancelled")
                && !newStatus.equalsIgnoreCase("Completed")) {
                System.out.println("Invalid status.");
                return;
            }

            ps.setString(1, newStatus);
            ps.setInt(2, bid);
            int updated = ps.executeUpdate();
            if (updated > 0) System.out.println("Booking status updated successfully!");
            else System.out.println("No booking found with that ID.");
        } catch (Exception ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    @Override
    public void showMenu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n==== Owner Menu ====");
            System.out.println("1. Add Property");
            System.out.println("2. View Properties");
            System.out.println("3. Change Property Availability");
            System.out.println("4. Delete Property");
            System.out.println("5. Change Booking Status");
            System.out.println("6. Logout");
            System.out.print("Choice: ");
            String ch = sc.nextLine();
            switch (ch) {
                case "1": addProperty(sc); break;
                case "2": viewProperties(); break;
                case "3": changePropertyAvailability(sc); break;
                case "4": deleteProperty(sc); break;
                case "5": changeBookingStatus(sc); break;
                case "6": return;
                default: System.out.println("Invalid Option.");
            }
        }
    }
}

/* ===========================
   REGISTERED USER
   =========================== */
class RegisteredUser extends Admin {
    private final int userId;

    public RegisteredUser(int userId) { this.userId = userId; }

    @Override
    public void viewProperties() {
        try (Connection con = connect();
             PreparedStatement st = con.prepareStatement(
                     "SELECT property_id, name, type, location, price_per_month, sharing FROM property WHERE availability_status='Available'");
             ResultSet rs = st.executeQuery()) {

            System.out.println("\nID | Name | Type | Location | Price | Sharing");
            boolean any = false;
            while (rs.next()) {
                any = true;
                Object sharingObj = rs.getObject("sharing");
                String sharing = sharingObj == null ? "-" : String.valueOf(rs.getInt("sharing"));
                System.out.printf("%d | %s | %s | %s | %.2f | %s%n",
                        rs.getInt("property_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("location"),
                        rs.getDouble("price_per_month"),
                        sharing);
            }
            if (!any) System.out.println("(No available properties)");
        } catch (Exception ex) {
            System.out.println("View failed: " + ex.getMessage());
        }
    }

    // --- Verify credentials for payment (uses same Scanner) ---
    private OperationResult<Integer> verifyCredentials(Connection con, Scanner sc) {
        try (PreparedStatement ps = con.prepareStatement("SELECT user_id FROM user WHERE email=? AND password=?")) {
            System.out.print("Enter email: ");
            String e = sc.nextLine().trim();
            System.out.print("Enter password: ");
            String p = sc.nextLine();
            ps.setString(1, e);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return OperationResult.ok(rs.getInt("user_id"), "Verified");
                return OperationResult.fail("Invalid credentials");
            }
        } catch (Exception ex) {
            return OperationResult.fail("Verification error: " + ex.getMessage());
        }
    }

    // payment methods that accept Scanner (no nested scanners)
    private void pay(Connection con, int bookingId, double amount, Scanner sc) throws Exception {
        pay(con, bookingId, amount, sc, "Cash");
    }

    private void pay(Connection con, int bookingId, double amount, Scanner sc, String method) throws Exception {
        System.out.println("\nProceed to payment:");
        System.out.print("Pay now? (y/n): ");
        String yn = sc.nextLine().trim();

        String status = "Pending";
        if (yn.equalsIgnoreCase("y")) {
            OperationResult<Integer> verify = verifyCredentials(con, sc);
            if (verify.success) {
                status = "Paid";
                System.out.println("Verification success. Payment marked PAID.");
            } else {
                System.out.println("Verification failed. Payment will be PENDING.");
            }
        } else {
            System.out.println("Skipped payment. Payment will be PENDING.");
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO payment(booking_id, amount, method, status, date) VALUES (?,?,?,?,CURDATE())")) {
            ps.setInt(1, bookingId);
            ps.setBigDecimal(2, new java.math.BigDecimal(String.format("%.2f", amount)));
            ps.setString(3, method);
            ps.setString(4, status);
            ps.executeUpdate();
        }
        System.out.println("Payment recorded: " + status);
    }

    // Full booking flow (uses single Scanner, date validation, updates availability, resident insert, payment)
    private void bookProperty(Scanner sc) {
        viewProperties();
        try (Connection con = connect()) {
            System.out.print("Enter Property ID to book: ");
            int pid = Integer.parseInt(sc.nextLine().trim());

            // Check availability and price
            double price = 0.0;
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT availability_status, price_per_month FROM property WHERE property_id=?")) {
                chk.setInt(1, pid);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Invalid Property ID.");
                        return;
                    }
                    if (!"Available".equalsIgnoreCase(rs.getString("availability_status"))) {
                        System.out.println("Property not available.");
                        return;
                    }
                    price = rs.getDouble("price_per_month");
                }
            }

            System.out.print("Start date (YYYY-MM-DD): ");
            String s1 = sc.nextLine().trim();
            System.out.print("End date (YYYY-MM-DD): ");
            String s2 = sc.nextLine().trim();

            LocalDate start, end;
            try {
                start = parseValidDate(s1);
                end = parseValidDate(s2);
                LocalDate today = LocalDate.now();
                if (start.isBefore(today)) {
                    System.out.println("Start date cannot be before today.");
                    return;
                }
                if (end.isBefore(start)) {
                    System.out.println("End date cannot be before start date.");
                    return;
                }
            } catch (DateTimeParseException dt) {
                System.out.println("Booking failed: invalid date format.");
                return;
            } catch (IllegalArgumentException ia) {
                System.out.println("Booking failed: " + ia.getMessage());
                return;
            }

            con.setAutoCommit(false);
            try {
                int bookingId;
                // Insert booking
                try (PreparedStatement insB = con.prepareStatement(
                        "INSERT INTO booking(user_id, property_id, start_date, end_date, status) VALUES (?,?,?,?, 'Active')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insB.setInt(1, userId);
                    insB.setInt(2, pid);
                    insB.setDate(3, java.sql.Date.valueOf(start));
                    insB.setDate(4, java.sql.Date.valueOf(end));
                    insB.executeUpdate();
                    try (ResultSet keys = insB.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Failed to get booking_id");
                        bookingId = keys.getInt(1);
                    }
                }

                // Insert resident
                try (PreparedStatement insR = con.prepareStatement(
                        "INSERT INTO resident(user_id, property_id) VALUES (?,?)")) {
                    insR.setInt(1, userId);
                    insR.setInt(2, pid);
                    insR.executeUpdate();
                }

                // Update property availability
                try (PreparedStatement updP = con.prepareStatement(
                        "UPDATE property SET availability_status='Booked' WHERE property_id=?")) {
                    updP.setInt(1, pid);
                    updP.executeUpdate();
                }

                // Payment
                System.out.print("Payment method (UPI/Card/Cash) [default Cash]: ");
                String method = sc.nextLine().trim();
                if (method.isEmpty()) pay(con, bookingId, price, sc);
                else pay(con, bookingId, price, sc, method);

                con.commit();
                System.out.println("Booking successful! Booking ID: " + bookingId);
            } catch (Exception inner) {
                con.rollback();
                System.out.println("Booking failed: " + inner.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception ex) {
            System.out.println("Booking failed: " + ex.getMessage());
        }
    }

    // Cancel booking (user)
    private void cancelBooking(Scanner sc) {
        try (Connection con = connect();
             PreparedStatement st = con.prepareStatement(
                     "SELECT b.booking_id, p.name, b.status FROM booking b JOIN property p ON b.property_id=p.property_id WHERE b.user_id=?")) {

            st.setInt(1, userId);
            try (ResultSet rs = st.executeQuery()) {
                System.out.println("\nYour Bookings:");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("Booking ID: %d | Property: %s | Status: %s%n",
                            rs.getInt("booking_id"),
                            rs.getString("name"),
                            rs.getString("status"));
                }
                if (!any) {
                    System.out.println("(No bookings found)");
                    return;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error fetching bookings: " + ex.getMessage());
            return;
        }

        System.out.print("Enter Booking ID to cancel: ");
        int bid;
        try {
            bid = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException nfe) {
            System.out.println("Invalid Booking ID.");
            return;
        }

        try (Connection con = connect()) {
            con.setAutoCommit(false);
            try {
                int pid = -1;
                try (PreparedStatement chk = con.prepareStatement(
                        "SELECT property_id, status FROM booking WHERE booking_id=? AND user_id=?")) {
                    chk.setInt(1, bid);
                    chk.setInt(2, userId);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("No such booking found for your account.");
                            con.rollback();
                            return;
                        }
                        String stt = rs.getString("status");
                        if ("Cancelled".equalsIgnoreCase(stt)) {
                            System.out.println("This booking is already cancelled.");
                            con.rollback();
                            return;
                        }
                        pid = rs.getInt("property_id");
                    }
                }

                try (PreparedStatement updB = con.prepareStatement(
                        "UPDATE booking SET status='Cancelled' WHERE booking_id=?")) {
                    updB.setInt(1, bid);
                    updB.executeUpdate();
                }

                try (PreparedStatement delR = con.prepareStatement(
                        "DELETE FROM resident WHERE user_id=? AND property_id=?")) {
                    delR.setInt(1, userId);
                    delR.setInt(2, pid);
                    delR.executeUpdate();
                }

                try (PreparedStatement updP = con.prepareStatement(
                        "UPDATE property SET availability_status='Available' WHERE property_id=?")) {
                    updP.setInt(1, pid);
                    updP.executeUpdate();
                }

                con.commit();
                System.out.println("✅ Booking cancelled successfully!");
            } catch (Exception inner) {
                con.rollback();
                System.out.println("Cancel failed: " + inner.getMessage());
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception ex) {
            System.out.println("Error cancelling booking: " + ex.getMessage());
        }
    }

    // View previous bookings (Cancelled or Completed)
    private void viewPreviousBookings() {
        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT b.booking_id, p.name, b.status, b.start_date, b.end_date " +
                             "FROM booking b JOIN property p ON b.property_id=p.property_id " +
                             "WHERE b.user_id=? AND (b.status='Cancelled' OR b.status='Completed')")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nYour Previous Bookings:");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("Booking ID: %d | Property: %s | Status: %s | %s → %s%n",
                            rs.getInt("booking_id"),
                            rs.getString("name"),
                            rs.getString("status"),
                            rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "-",
                            rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "-");
                }
                if (!any) System.out.println("(No previous bookings found)");
            }
        } catch (Exception ex) {
            System.out.println("Error fetching previous bookings: " + ex.getMessage());
        }
    }

    @Override
    public void showMenu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n==== User Menu ====");
            System.out.println("1. View Available Properties");
            System.out.println("2. Book Property");
            System.out.println("3. Cancel Booking");
            System.out.println("4. View Previous Bookings");
            System.out.println("5. Logout");
            System.out.print("Choice: ");
            String ch = sc.nextLine();
            switch (ch) {
                case "1": viewProperties(); break;
                case "2": bookProperty(sc); break;
                case "3": cancelBooking(sc); break;
                case "4": viewPreviousBookings(); break;
                case "5": return;
                default: System.out.println("Invalid Option.");
            }
        }
    }
}

/* ===========================
   LOOKER
   =========================== */
class Sightseer extends Admin {
    @Override
    public void showMenu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n==== Looker Menu ====");
            System.out.println("1. View Available Properties");
            System.out.println("2. Exit");
            System.out.print("Choice: ");
            String ch = sc.nextLine();
            if (ch.equals("1")) viewProperties();
            else if (ch.equals("2")) return;
            else System.out.println("Invalid Option.");
        }
    }
}

/* ===========================
   MAIN APPLICATION (unchanged structure)
   =========================== */
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n==== Welcome to RIMS ====");
            System.out.println("1. Login as Owner");
            System.out.println("2. Register as Owner");
            System.out.println("3. Login/Register as User");
            System.out.println("4. Continue as Looker");
            System.out.println("5. Exit");
            System.out.print("Choose option: ");
            String option = sc.nextLine();

            UserRole role = null;
            switch (option) {
                case "1":
                    if (ownerLogin(sc)) role = new Admin();
                    break;
                case "2":
                    ownerRegister(sc);
                    break;
                case "3":
                    Integer uid = userLoginOrRegister(sc);
                    if (uid != null) role = new RegisteredUser(uid);
                    break;
                case "4":
                    role = new Sightseer();
                    break;
                case "5":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid Option.");
            }

            if (role != null) role.showMenu();
        }
    }

    // Owner login (renamed to Owner)
    private static boolean ownerLogin(Scanner sc) {
        System.out.print("Owner username: ");
        String u = sc.nextLine();
        System.out.print("Owner password: ");
        String p = sc.nextLine();
        try (Connection con = Admin.connect();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM admin WHERE username=? AND password=?")) {
            ps.setString(1, u);
            ps.setString(2, p);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Owner login successful!\n");
                    return true;
                } else {
                    System.out.println("Owner login failed.");
                    return false;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }

    // Owner registration (renamed)
    private static void ownerRegister(Scanner sc) {
        System.out.print("Choose owner username: ");
        String u = sc.nextLine().trim();
        System.out.print("Choose owner password: ");
        String p = sc.nextLine();
        try (Connection con = Admin.connect();
             PreparedStatement chk = con.prepareStatement("SELECT admin_id FROM admin WHERE username=?")) {
            chk.setString(1, u);
            try (ResultSet rs = chk.executeQuery()) {
                if (rs.next()) { System.out.println("Username already taken."); return; }
            }

            try (PreparedStatement ps = con.prepareStatement("INSERT INTO admin(username,password) VALUES(?,?)")) {
                ps.setString(1, u);
                ps.setString(2, p);
                ps.executeUpdate();
                System.out.println("Owner registered successfully!");
            }
        } catch (Exception ex) {
            System.out.println("Registration failed: " + ex.getMessage());
        }
    }

    // User login/register with email validation, phone validation, uniqueness check
    private static Integer userLoginOrRegister(Scanner sc) {
        System.out.println("1. Login\n2. Register");
        String ch = sc.nextLine();
        if (ch.equals("1")) {
            System.out.print("Email: ");
            String e = sc.nextLine().trim();
            System.out.print("Password: ");
            String p = sc.nextLine();
            try (Connection con = Admin.connect();
                 PreparedStatement ps = con.prepareStatement(
                         "SELECT user_id, name FROM user WHERE email=? AND password=?")) {
                ps.setString(1, e);
                ps.setString(2, p);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Welcome, " + rs.getString("name"));
                        return rs.getInt("user_id");
                    } else {
                        System.out.println("Login failed.");
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        } else if (ch.equals("2")) {
            try (Connection con = Admin.connect()) {
                System.out.print("Name: ");
                String n = sc.nextLine().trim();

                String e;
                while (true) {
                    System.out.print("Email: ");
                    e = sc.nextLine().trim();
                    if (!Admin.isValidEmail(e)) { System.out.println("Invalid email format."); continue; }
                    try (PreparedStatement chk = con.prepareStatement("SELECT user_id FROM user WHERE email=?")) {
                        chk.setString(1, e);
                        try (ResultSet rs = chk.executeQuery()) {
                            if (rs.next()) { System.out.println("Email already registered."); return null; }
                        }
                    }
                    break;
                }

                System.out.print("Password: ");
                String p = sc.nextLine();

                String ph;
                while (true) {
                    System.out.print("Phone (10 digits): ");
                    ph = sc.nextLine().trim();
                    if (!Admin.isValidPhone(ph)) { System.out.println("Invalid phone. Enter exactly 10 digits."); continue; }
                    break;
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO user(name,email,password,phone) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, n);
                    ps.setString(2, e);
                    ps.setString(3, p);
                    ps.setString(4, ph);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            int uid = rs.getInt(1);
                            System.out.println("Registration successful! Your user ID: " + uid);
                            return uid;
                        }
                    }
                } catch (SQLIntegrityConstraintViolationException tie) {
                    System.out.println("Email already exists.");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
        return null;
    }
}