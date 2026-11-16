package com.mycompany.mavenproject2;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MySQL connection, small JDBC helpers, the table definitions, and demo data.
 * Settings come from kinnect.properties (db.url, db.user, db.password, demo.data),
 * found in the folder Kinnect runs from or next to kinnect.jar. Uploaded files are
 * saved inside the database too, so every computer that connects sees the same files.
 *
 * @author Ariel Soriano
 */
public final class Db {
    private static String url;
    private static Path settingsUsed;
    private static boolean urlFromFile;
    private static String user;
    private static String password;
    private static boolean demoData;
    private Db() {
    }

    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    interface Work {
        void run() throws SQLException;
    }

    static void configure() {
        disconnect();
        Properties p = new Properties();
        Path file = settingsFile();
        settingsUsed = file;
        if (file != null) {
            try {
                // Notepad can save an invisible mark at the start of the file, which would hide db.url.
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (text.startsWith("\uFEFF")) {
                    text = text.substring(1);
                }
                p.load(new StringReader(text));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read kinnect.properties: " + e.getMessage());
            }
        }
        urlFromFile = p.getProperty("db.url") != null;
        url = setting(p, "db.url", "jdbc:mysql://localhost:3306/kinnect?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true");
        user = setting(p, "db.user", "root");
        password = setting(p, "db.password", "");
        demoData = Boolean.parseBoolean(setting(p, "demo.data", "true"));
    }

    /** True when db.url came from kinnect.properties instead of the XAMPP default. */
    static boolean urlFromFile() {
        return urlFromFile;
    }

    /** The kinnect.properties file in use, or null when Kinnect runs on the XAMPP defaults. */
    static Path settingsUsed() {
        return settingsUsed;
    }

    /**
     * The folders Kinnect checks for kinnect.properties, in order: the folder it runs from,
     * then the folder that holds kinnect.jar (the "app" folder inside Kinnect.exe's folder).
     */
    static List<Path> settingsFolders() {
        List<Path> folders = new ArrayList<>();
        folders.add(Paths.get("").toAbsolutePath());
        try {
            Path code = Paths.get(Db.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path beside = code.toAbsolutePath().getParent();
            if (beside != null && !folders.contains(beside)) {
                folders.add(beside);
            }
        } catch (Exception ignored) {
            // Only the current folder can be checked.
        }
        return folders;
    }

    private static Path settingsFile() {
        for (Path folder : settingsFolders()) {
            Path file = folder.resolve("kinnect.properties");
            if (Files.isRegularFile(file)) {
                return file;
            }
        }
        return null;
    }

    private static String setting(Properties p, String key, String fallback) {
        // Spaces copied in by accident around a value would break the address or password.
        return System.getProperty("kinnect." + key, p.getProperty(key, fallback)).strip();
    }

    static String url() {
        return url;
    }

    // ---------- one shared connection ----------
    // Opening a connection to an online server is slow, so Kinnect opens one and keeps
    // reusing it. If it drops (Wi-Fi changes, the server restarts), it reconnects once.

    private static Connection shared;
    private static long lastUsed;
    private static int txDepth;

    interface Call<T> {
        T run(Connection c) throws SQLException;
    }

    static Connection connect() throws SQLException {
        boolean idle = System.currentTimeMillis() - lastUsed > 30_000;
        if (shared == null || shared.isClosed() || (txDepth == 0 && idle && !shared.isValid(5))) {
            disconnect();
            shared = DriverManager.getConnection(url, user, password);
            useThisComputersClock(shared);
        }
        lastUsed = System.currentTimeMillis();
        return shared;
    }

    /**
     * Online database servers usually run on UTC, which would show "sent", "joined", and
     * "paid" times 8 hours early in the Philippines. This makes the server use the time
     * zone of the computer Kinnect runs on. On XAMPP nothing changes.
     */
    private static void useThisComputersClock(Connection c) throws SQLException {
        if (!url.startsWith("jdbc:mysql:")) {
            return;
        }
        int total = ZonedDateTime.now().getOffset().getTotalSeconds();
        int seconds = Math.abs(total);
        String offset = String.format("%s%02d:%02d", total < 0 ? "-" : "+", seconds / 3600, seconds / 60 % 60);
        try (Statement st = c.createStatement()) {
            st.execute("SET time_zone = '" + offset + "'");
        }
    }

    static void disconnect() {
        if (shared != null) {
            try {
                shared.close();
            } catch (SQLException ignored) {
                // already gone
            }
            shared = null;
        }
    }

    private static boolean connectionLost(SQLException e) {
        String state = e.getSQLState();
        return e instanceof java.sql.SQLRecoverableException || (state != null && state.startsWith("08"));
    }

    private static <T> T run(Call<T> call) {
        try {
            return call.run(connect());
        } catch (SQLException e) {
            if (txDepth == 0 && connectionLost(e)) {
                disconnect();
                try {
                    return call.run(connect());
                } catch (SQLException again) {
                    throw new IllegalStateException("Database error: " + again.getMessage(), again);
                }
            }
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    static <T> List<T> query(String sql, RowMapper<T> mapper, Object... args) {
        return run(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, args);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                    return out;
                }
            }
        });
    }

    static <T> T one(String sql, RowMapper<T> mapper, Object... args) {
        List<T> rows = query(sql, mapper, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    static long count(String sql, Object... args) {
        Long n = one(sql, rs -> rs.getLong(1), args);
        return n == null ? 0 : n;
    }

    static int update(String sql, Object... args) {
        return run(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, args);
                return ps.executeUpdate();
            }
        });
    }

    static long insert(String sql, Object... args) {
        return run(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, args);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    return keys.next() ? keys.getLong(1) : -1L;
                }
            }
        });
    }

    /** Runs several statements as one transaction: all are saved, or none are. */
    static void transaction(Work work) {
        try {
            if (txDepth > 0) {
                work.run();
                return;
            }
            Connection c = connect();
            c.setAutoCommit(false);
            txDepth++;
            try {
                work.run();
                c.commit();
            } catch (RuntimeException | SQLException e) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                    // the connection is gone; nothing was saved
                }
                throw e;
            } finally {
                txDepth--;
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    disconnect();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS users (
          user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          full_name VARCHAR(120) NOT NULL,
          email VARCHAR(160) NOT NULL,
          password_hash VARCHAR(255) NOT NULL,
          account_role VARCHAR(12) NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT uq_user_email UNIQUE (email))""",
        """
        CREATE TABLE IF NOT EXISTS classrooms (
          class_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          professor_id BIGINT NOT NULL,
          class_name VARCHAR(120) NOT NULL,
          description VARCHAR(255),
          class_code VARCHAR(10) NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT uq_class_code UNIQUE (class_code),
          CONSTRAINT fk_class_prof FOREIGN KEY (professor_id) REFERENCES users(user_id))""",
        """
        CREATE TABLE IF NOT EXISTS class_enrollments (
          enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          class_id BIGINT NOT NULL,
          student_id BIGINT NOT NULL,
          joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT uq_enrollment UNIQUE (class_id, student_id),
          CONSTRAINT fk_enr_class FOREIGN KEY (class_id) REFERENCES classrooms(class_id) ON DELETE CASCADE,
          CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES users(user_id))""",
        """
        CREATE TABLE IF NOT EXISTS project_groups (
          group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          class_id BIGINT NOT NULL,
          group_name VARCHAR(80) NOT NULL,
          description VARCHAR(255),
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_group_class FOREIGN KEY (class_id) REFERENCES classrooms(class_id) ON DELETE CASCADE)""",
        """
        CREATE TABLE IF NOT EXISTS group_members (
          group_member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          enrollment_id BIGINT NOT NULL,
          group_role VARCHAR(10) NOT NULL,
          joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT uq_one_group_per_enrollment UNIQUE (enrollment_id),
          CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_gm_enr FOREIGN KEY (enrollment_id) REFERENCES class_enrollments(enrollment_id) ON DELETE CASCADE)""",
        """
        CREATE TABLE IF NOT EXISTS tasks (
          task_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          title VARCHAR(150) NOT NULL,
          description TEXT,
          deadline DATE NOT NULL,
          status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
          assignee_id BIGINT NOT NULL,
          creator_id BIGINT NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_task_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES group_members(group_member_id),
          CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES group_members(group_member_id))""",
        """
        CREATE TABLE IF NOT EXISTS group_files (
          file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          file_name VARCHAR(255) NOT NULL,
          storage_path VARCHAR(500) NOT NULL,
          file_type VARCHAR(10) NOT NULL,
          file_size BIGINT NOT NULL,
          uploader_id BIGINT NOT NULL,
          uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_file_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES group_members(group_member_id))""",
        """
        CREATE TABLE IF NOT EXISTS file_chunks (
          file_id BIGINT NOT NULL,
          chunk_no INT NOT NULL,
          data MEDIUMBLOB NOT NULL,
          PRIMARY KEY (file_id, chunk_no),
          CONSTRAINT fk_chunk_file FOREIGN KEY (file_id) REFERENCES group_files(file_id) ON DELETE CASCADE)""",
        """
        CREATE TABLE IF NOT EXISTS calendar_events (
          event_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          event_title VARCHAR(150) NOT NULL,
          description TEXT,
          start_date_time DATETIME NOT NULL,
          end_date_time DATETIME NOT NULL,
          creator_id BIGINT NOT NULL,
          CONSTRAINT fk_event_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_event_creator FOREIGN KEY (creator_id) REFERENCES group_members(group_member_id))""",
        """
        CREATE TABLE IF NOT EXISTS messages (
          message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          sender_id BIGINT NOT NULL,
          content TEXT NOT NULL,
          sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_msg_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(user_id))""",
        """
        CREATE TABLE IF NOT EXISTS shared_documents (
          document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          document_title VARCHAR(150) NOT NULL,
          document_url VARCHAR(500) NOT NULL,
          contributor_id BIGINT NOT NULL,
          added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_doc_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_doc_contrib FOREIGN KEY (contributor_id) REFERENCES group_members(group_member_id))""",
        """
        CREATE TABLE IF NOT EXISTS expenses (
          expense_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_id BIGINT NOT NULL,
          item VARCHAR(150) NOT NULL,
          amount DECIMAL(10,2) NOT NULL,
          status VARCHAR(10) NOT NULL,
          bought_by BIGINT,
          bought_on DATE,
          CONSTRAINT fk_exp_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_exp_buyer FOREIGN KEY (bought_by) REFERENCES group_members(group_member_id))""",
        """
        CREATE TABLE IF NOT EXISTS payments (
          payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
          group_member_id BIGINT NOT NULL,
          amount DECIMAL(10,2) NOT NULL,
          paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_pay_member FOREIGN KEY (group_member_id) REFERENCES group_members(group_member_id) ON DELETE CASCADE)""",
        """
        CREATE TABLE IF NOT EXISTS member_shares (
          group_member_id BIGINT PRIMARY KEY,
          amount DECIMAL(10,2) NOT NULL,
          CONSTRAINT fk_share_member FOREIGN KEY (group_member_id) REFERENCES group_members(group_member_id) ON DELETE CASCADE)""",
        """
        CREATE TABLE IF NOT EXISTS group_name_locks (
          group_id BIGINT PRIMARY KEY,
          locked_by BIGINT NOT NULL,
          locked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          CONSTRAINT fk_lock_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE,
          CONSTRAINT fk_lock_user FOREIGN KEY (locked_by) REFERENCES users(user_id))""",
        """
        CREATE TABLE IF NOT EXISTS kinnect_meta (
          meta_key VARCHAR(40) PRIMARY KEY,
          meta_value VARCHAR(255) NOT NULL)""",
        """
        CREATE TABLE IF NOT EXISTS budget_settings (
          group_id BIGINT PRIMARY KEY,
          due_date DATE,
          CONSTRAINT fk_budget_group FOREIGN KEY (group_id) REFERENCES project_groups(group_id) ON DELETE CASCADE)"""
    };

    static final int SCHEMA_VERSION = 2;

    /** Creates any missing tables, updates older Kinnect databases, then adds demo data the first time. */
    static void init() {
        run(c -> {
            try (Statement st = c.createStatement()) {
                for (String sql : SCHEMA) {
                    st.execute(sql);
                }
            }
            return null;
        });
        upgrade();
        moveLocalFiles();
        if (demoData && count("SELECT COUNT(*) FROM users") == 0) {
            DemoData.load();
        }
    }

    /**
     * Version 1 kept one payment per member in a "contributions" table and saved uploads in a
     * local folder. Version 2 allows several payments (partial payments) in "payments" and custom
     * shares in "member_shares". This runs once.
     */
    private static void upgrade() {
        String v = one("SELECT meta_value FROM kinnect_meta WHERE meta_key = 'schema_version'", rs -> rs.getString(1));
        int version = v == null ? 1 : Integer.parseInt(v);
        if (version >= SCHEMA_VERSION) {
            return;
        }
        boolean oldTable;
        try {
            count("SELECT COUNT(*) FROM contributions");
            oldTable = true;
        } catch (IllegalStateException e) {
            oldTable = false;
        }
        boolean copy = oldTable;
        transaction(() -> {
            if (copy) {
                update("""
                        INSERT INTO payments (group_member_id, amount, paid_at)
                        SELECT group_member_id, amount_paid, paid_at FROM contributions""");
            }
            update("DELETE FROM kinnect_meta WHERE meta_key = 'schema_version'");
            update("INSERT INTO kinnect_meta (meta_key, meta_value) VALUES ('schema_version', ?)",
                    String.valueOf(SCHEMA_VERSION));
        });
        if (oldTable) {
            update("DROP TABLE contributions");
        }
    }

    /**
     * Older versions saved uploads in a folder on one computer. Move any of those into the
     * database so they still open on another computer or online. Runs on every start, so a
     * file that couldn't be moved before is tried again.
     */
    private static void moveLocalFiles() {
        record OldFile(long id, String path) {
        }
        for (OldFile f : query("SELECT file_id, storage_path FROM group_files WHERE storage_path <> ?",
                rs -> new OldFile(rs.getLong(1), rs.getString(2)), CollabService.IN_DATABASE)) {
            try {
                Path path = Paths.get(f.path());
                if (Files.isRegularFile(path)) {
                    byte[] bytes = Files.readAllBytes(path);
                    transaction(() -> {
                        update("DELETE FROM file_chunks WHERE file_id = ?", f.id());
                        CollabService.storeChunks(f.id(), bytes);
                        update("UPDATE group_files SET storage_path = ? WHERE file_id = ?", CollabService.IN_DATABASE, f.id());
                    });
                }
            } catch (IOException | RuntimeException e) {
                System.err.println("Kinnect: could not move file " + f.id() + " into the database: " + e.getMessage());
            }
        }
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }
}
