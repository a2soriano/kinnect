package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AccountRole;
import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.User;
import java.util.regex.Pattern;

/**
 * Sign up and log in.
 *
 * @author Ariel Soriano
 */
public final class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AuthService() {
    }

    static User logIn(String email, String password) {
        String mail = clean(email).toLowerCase();
        if (mail.isEmpty() || password == null || password.isEmpty()) {
            throw new AppException("Enter your email and password.");
        }
        record Row(User user, String hash) {
        }
        Row row = Db.one("SELECT user_id, full_name, email, account_role, password_hash FROM users WHERE email = ?",
                rs -> new Row(new User(rs.getLong(1), rs.getString(2), rs.getString(3),
                        AccountRole.valueOf(rs.getString(4))), rs.getString(5)), mail);
        if (row == null || !Security.matches(password, row.hash())) {
            throw new AppException("Incorrect email or password.");
        }
        Session.start(row.user());
        return row.user();
    }

    static User signUp(String fullName, String email, String password, String confirm, AccountRole role) {
        String name = clean(fullName);
        String mail = clean(email).toLowerCase();
        if (name.length() < 2) {
            throw new AppException("Enter your full name.");
        }
        if (!EMAIL.matcher(mail).matches()) {
            throw new AppException("Enter a valid email address.");
        }
        if (password == null || password.length() < 8) {
            throw new AppException("Password must be at least 8 characters.");
        }
        if (!password.equals(confirm)) {
            throw new AppException("Passwords do not match.");
        }
        if (role == null) {
            throw new AppException("Choose whether you are a student or a professor.");
        }
        if (Db.count("SELECT COUNT(*) FROM users WHERE email = ?", mail) > 0) {
            throw new AppException("An account with that email already exists.");
        }
        long id = Db.insert("INSERT INTO users (full_name, email, password_hash, account_role) VALUES (?, ?, ?, ?)",
                name, mail, Security.hash(password), role.name());
        User user = new User(id, name, mail, role);
        Session.start(user);
        return user;
    }

    static void logOut() {
        Session.end();
    }

    static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
