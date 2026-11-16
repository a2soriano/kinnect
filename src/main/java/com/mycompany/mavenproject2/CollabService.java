package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.CalendarEvent;
import com.mycompany.mavenproject2.Models.CalendarItem;
import com.mycompany.mavenproject2.Models.GroupFile;
import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.Message;
import com.mycompany.mavenproject2.Models.SharedDocument;
import com.mycompany.mavenproject2.Models.Task;
import com.mycompany.mavenproject2.Models.TaskStatus;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Files, calendar, messages, and shared documents of a group.
 *
 * @author Ariel Soriano
 */
public final class CollabService {
    static final Set<String> FILE_TYPES = Set.of("pdf", "docx", "doc", "pptx", "xlsx", "png", "jpg", "jpeg",
            "txt", "zip");
    static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
    static final String IN_DATABASE = "database";
    private static final int CHUNK = 128 * 1024;
    private static final String MEMBER_NAME = """
            JOIN group_members m ON m.group_member_id = %s
            JOIN class_enrollments e ON e.enrollment_id = m.enrollment_id
            JOIN users u ON u.user_id = e.student_id
            """;

    private CollabService() {
    }

    static String extension(String fileName) { //Files
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static GroupFile uploadFile(long groupId, long userId, Path source) {
        Member me = GroupService.requireMember(groupId, userId);
        if (source == null || !Files.isRegularFile(source)) {
            throw new AppException("Choose a file to upload.");
        }
        String name = source.getFileName().toString();
        String type = extension(name);
        if (!FILE_TYPES.contains(type)) {
            throw new AppException("Kinnect accepts PDF, Word, PowerPoint, Excel, image, text, and ZIP files only.");
        }
        try {
            long size = Files.size(source);
            if (size == 0) {
                throw new AppException("That file is empty.");
            }
            if (size > MAX_FILE_SIZE) {
                throw new AppException("Files must be 25 MB or smaller.");
            }
            byte[] bytes = Files.readAllBytes(source);
            long[] id = new long[1];
            Db.transaction(() -> {
                id[0] = Db.insert("""
                        INSERT INTO group_files (group_id, file_name, storage_path, file_type, file_size, uploader_id)
                        VALUES (?, ?, ?, ?, ?, ?)""", groupId, name, IN_DATABASE, type, size, me.id());
                storeChunks(id[0], bytes);
            });
            return files(groupId).stream().filter(f -> f.id() == id[0]).findFirst().orElseThrow();
        } catch (IOException e) {
            throw new AppException("Could not save the file: " + e.getMessage());
        }
    }

    static List<GroupFile> files(long groupId) {
        return Db.query("""
                SELECT f.file_id, f.file_name, f.storage_path, f.file_type, f.file_size, u.full_name, f.uploaded_at
                FROM group_files f
                """ + MEMBER_NAME.formatted("f.uploader_id") + """
                WHERE f.group_id = ? ORDER BY f.uploaded_at DESC, f.file_id DESC""",
                rs -> new GroupFile(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getLong(5), rs.getString(6), rs.getObject(7, LocalDateTime.class)), groupId);
    }

    /**
     * Saves the file's bytes in the database in 128 KB pieces. The MySQL driver sends each
     * piece at about twice its size, and XAMPP accepts at most 1 MB per request, so small
     * pieces always fit. Keeping files in the database means everyone connected to the same
     * server sees the same files.
     */
    static void storeChunks(long fileId, byte[] bytes) {
        int n = 0;
        for (int from = 0; from < bytes.length; from += CHUNK) {
            byte[] piece = Arrays.copyOfRange(bytes, from, Math.min(bytes.length, from + CHUNK));
            Db.insert("INSERT INTO file_chunks (file_id, chunk_no, data) VALUES (?, ?, ?)", fileId, n++, piece);
        }
    }

    /** Writes the file to target, reading it from the database (or from an older local copy). */
    static Path copyOut(GroupFile file, Path target) {
        try {
            Path old = file.storagePath() == null || file.storagePath().equals(IN_DATABASE) ? null
                    : Path.of(file.storagePath());
            if (old != null && Files.exists(old)) {
                return Files.copy(old, target, StandardCopyOption.REPLACE_EXISTING);
            }
            List<byte[]> pieces = Db.query("SELECT data FROM file_chunks WHERE file_id = ? ORDER BY chunk_no",
                    rs -> rs.getBytes(1), file.id());
            if (pieces.isEmpty()) {
                throw new AppException(file.fileName() + " is missing from the database.");
            }
            try (OutputStream out = Files.newOutputStream(target)) {
                for (byte[] piece : pieces) {
                    out.write(piece);
                }
            }
            return target;
        } catch (IOException e) {
            throw new AppException("Could not save the file: " + e.getMessage());
        }
    }

    /** A copy of the file in a temporary folder, so it can be opened with its usual app. */
    static Path openableCopy(GroupFile file) {
        try {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "Kinnect", "file-" + file.id());
            Files.createDirectories(dir);
            return copyOut(file, dir.resolve(file.fileName()));
        } catch (IOException e) {
            throw new AppException("Could not open the file: " + e.getMessage());
        }
    }

    static CalendarEvent addEvent(long groupId, long userId, String title, String description,
            LocalDateTime start, LocalDateTime end) {
        Member me = GroupService.requireMember(groupId, userId);
        String name = AuthService.clean(title);
        if (name.isEmpty()) {
            throw new AppException("Enter an event title.");
        }
        if (start == null || end == null) {
            throw new AppException("Pick the date, start time, and end time.");
        }
        if (!end.isAfter(start)) {
            throw new AppException("The end time must be after the start time.");
        }
        long id = Db.insert("""
                INSERT INTO calendar_events (group_id, event_title, description, start_date_time, end_date_time, creator_id)
                VALUES (?, ?, ?, ?, ?, ?)""", groupId, name, description == null ? "" : description.trim(),
                start, end, me.id());
        return events(groupId).stream().filter(ev -> ev.id() == id).findFirst().orElseThrow();
    }

    static List<CalendarEvent> events(long groupId) {
        return Db.query("""
                SELECT c.event_id, c.event_title, c.description, c.start_date_time, c.end_date_time, u.full_name
                FROM calendar_events c
                """ + MEMBER_NAME.formatted("c.creator_id") + """
                WHERE c.group_id = ? ORDER BY c.start_date_time""",
                rs -> new CalendarEvent(rs.getLong(1), rs.getString(2), rs.getString(3),
                        rs.getObject(4, LocalDateTime.class), rs.getObject(5, LocalDateTime.class),
                        rs.getString(6)), groupId);
    }

    /** Task deadlines and group events together, sorted by date. */
    static List<CalendarItem> calendarItems(long groupId, LocalDate from, LocalDate to) {
        List<CalendarItem> items = new ArrayList<>();
        for (Task t : TaskService.tasks(groupId)) {
            if (!t.deadline().isBefore(from) && !t.deadline().isAfter(to)) {
                String detail = t.status() == TaskStatus.COMPLETED
                        ? "Completed by " + Ui.shortName(t.assigneeName())
                        : "Task deadline, " + Ui.shortName(t.assigneeName());
                items.add(new CalendarItem(t.deadline(), t.title(), detail, false, null));
            }
        }
        for (CalendarEvent ev : events(groupId)) {
            LocalDate d = ev.start().toLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                items.add(new CalendarItem(d, ev.title(), "Event, " + Ui.timeRange(ev.start(), ev.end()), true, ev));
            }
        }
        items.sort(Comparator.comparing(CalendarItem::date)
                .thenComparing(i -> i.isEvent() ? i.event().start().toLocalTime().toString() : "99"));
        return items;
    }

    static List<CalendarItem> calendarMonth(long groupId, YearMonth month) {
        return calendarItems(groupId, month.atDay(1).minusDays(7), month.atEndOfMonth().plusDays(14));
    }

    // ---------- messages ----------

    static Message sendMessage(long groupId, long userId, String content) {
        boolean allowed = GroupService.memberOf(groupId, userId) != null
                || GroupService.isProfessorOf(groupId, userId);
        if (!allowed) {
            throw new AppException("You cannot send messages in this group.");
        }
        String text = content == null ? "" : content.strip();
        if (text.isEmpty()) {
            throw new AppException("Type a message first.");
        }
        if (text.length() > 1000) {
            throw new AppException("Messages can be up to 1,000 characters.");
        }
        long id = Db.insert("INSERT INTO messages (group_id, sender_id, content, sent_at) VALUES (?, ?, ?, ?)",
                groupId, userId, text, LocalDateTime.now());
        return messagesAfter(groupId, id - 1).stream().filter(m -> m.id() == id).findFirst().orElseThrow();
    }

    static List<Message> messages(long groupId) {
        return messagesAfter(groupId, 0);
    }

    static List<Message> messagesAfter(long groupId, long lastId) {
        return Db.query("""
                SELECT ms.message_id, ms.sender_id, u.full_name, ms.content, ms.sent_at
                FROM messages ms JOIN users u ON u.user_id = ms.sender_id
                WHERE ms.group_id = ? AND ms.message_id > ? ORDER BY ms.message_id""",
                rs -> new Message(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                        rs.getObject(5, LocalDateTime.class)), groupId, lastId);
    }

    // ---------- shared documents ----------

    static SharedDocument addDocument(long groupId, long userId, String title, String link) {
        Member me = GroupService.requireMember(groupId, userId);
        String name = AuthService.clean(title);
        String url = link == null ? "" : link.trim();
        if (name.isEmpty()) {
            throw new AppException("Enter a title for the document.");
        }
        if (!isValidLink(url)) {
            throw new AppException("Paste a full link that starts with https://");
        }
        long id = Db.insert("""
                INSERT INTO shared_documents (group_id, document_title, document_url, contributor_id)
                VALUES (?, ?, ?, ?)""", groupId, name, url, me.id());
        return documents(groupId).stream().filter(d -> d.id() == id).findFirst().orElseThrow();
    }

    static boolean isValidLink(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getHost().contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    static List<SharedDocument> documents(long groupId) {
        return Db.query("""
                SELECT d.document_id, d.document_title, d.document_url, u.full_name, d.added_at
                FROM shared_documents d
                """ + MEMBER_NAME.formatted("d.contributor_id") + """
                WHERE d.group_id = ? ORDER BY d.added_at DESC, d.document_id DESC""",
                rs -> new SharedDocument(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getObject(5, LocalDateTime.class)), groupId);
    }
}
