package com.mycompany.mavenproject2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;

/**
 * Sample classes, groups, and work so Kinnect can be demonstrated right away.
 * Runs once, only when the users table is empty. Every demo password is kinnect123.
 *
 * @author Ariel Soriano
 */
final class DemoData {

    static final String PASSWORD = "kinnect123";

    private DemoData() {
    }

    static void load() {
        String hash = Security.hash(PASSWORD);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Db.transaction(() -> {
            long prof = user("Prof. Elena Cruz", "elena.cruz@school.edu", hash, "PROFESSOR");
            long mika = user("Mika Reyes", "mika.reyes@school.edu", hash, "STUDENT");
            long andrea = user("Andrea Lim", "andrea.lim@school.edu", hash, "STUDENT");
            long jomar = user("Jomar Cruz", "jomar.cruz@school.edu", hash, "STUDENT");
            long paolo = user("Paolo Santos", "paolo.santos@school.edu", hash, "STUDENT");
            long bea = user("Bea Torres", "bea.torres@school.edu", hash, "STUDENT");
            long kevin = user("Kevin Tan", "kevin.tan@school.edu", hash, "STUDENT");
            long lea = user("Lea Santos", "lea.santos@school.edu", hash, "STUDENT");
            long carla = user("Carla Mendoza", "carla.mendoza@school.edu", hash, "STUDENT");
            long diego = user("Diego Ramos", "diego.ramos@school.edu", hash, "STUDENT");
            long nina = user("Nina Garcia", "nina.garcia@school.edu", hash, "STUDENT");
            long rafael = user("Rafael Uy", "rafael.uy@school.edu", hash, "STUDENT");

            long sdB = Db.insert("INSERT INTO classrooms (professor_id, class_name, description, class_code) VALUES (?, ?, ?, ?)",
                    prof, "Software Design, Section B", "MWF 1:00 to 2:30 PM", "KQ7-4MX");
            Db.insert("INSERT INTO classrooms (professor_id, class_name, description, class_code) VALUES (?, ?, ?, ?)",
                    prof, "Software Design, Section A", "MWF 9:00 to 10:30 AM", "PT2-9LW");
            Db.insert("INSERT INTO classrooms (professor_id, class_name, description, class_code) VALUES (?, ?, ?, ?)",
                    prof, "Information Management, Section D", "TTh 3:00 to 4:30 PM", "HR8-6ZT");

            long[] students = {mika, andrea, jomar, paolo, bea, kevin, lea, carla, diego, nina, rafael};
            long[] e = new long[students.length];
            for (int i = 0; i < students.length; i++) {
                e[i] = Db.insert("INSERT INTO class_enrollments (class_id, student_id) VALUES (?, ?)", sdB, students[i]);
            }

            long g1 = group(sdB, "Group 1", "Campus lost and found app");
            long g2 = group(sdB, "Group 2", "Clinic queue tracker");
            long g3 = group(sdB, "Group 3", "Water quality monitor app");

            long gmMika = member(g3, e[0], "LEADER");
            long gmAndrea = member(g3, e[1], "MEMBER");
            long gmJomar = member(g3, e[2], "MEMBER");
            long gmPaolo = member(g3, e[3], "MEMBER");
            long gmBea = member(g3, e[4], "MEMBER");
            long gmCarla = member(g1, e[7], "LEADER");
            long gmNina = member(g1, e[9], "MEMBER");
            long gmDiego = member(g2, e[8], "LEADER");
            long gmRafael = member(g2, e[10], "MEMBER");

            // Group 3: 5 completed, 3 ongoing, 2 pending (50%), same as the Figma screens
            task(g3, "Write the project proposal", "Problem, objectives, scope, and timeline.", today.minusDays(9), "COMPLETED", gmMika, gmMika);
            task(g3, "Run the user survey", "Ask barangay health workers how water samples are checked today.", today.minusDays(10), "COMPLETED", gmBea, gmMika);
            task(g3, "Draw the ERD", "Tables, keys, and relationships.", today.minusDays(11), "COMPLETED", gmAndrea, gmMika);
            task(g3, "Make the app wireframes", "Low fidelity first, then clean them up.", today.minusDays(7), "COMPLETED", gmJomar, gmMika);
            task(g3, "Draft the system flowchart", "Cover every role and every decision.", today.minusDays(6), "COMPLETED", gmAndrea, gmMika);
            task(g3, "Build the sensor dashboard UI", "Chart the last 24 hours of pH and TDS readings.", today.plusDays(4), "ONGOING", gmJomar, gmMika);
            task(g3, "Write the database schema", "Turn the ERD into CREATE TABLE statements.", today.plusDays(6), "ONGOING", gmPaolo, gmMika);
            task(g3, "Set up the sensor data API", "Read values from the Arduino every minute and save them.", today.plusDays(8), "ONGOING", gmAndrea, gmMika);
            task(g3, "Prepare the user test script", "At least two test cases per feature.", today.plusDays(9), "PENDING", gmMika, gmMika);
            task(g3, "Record the demo video", "Three minutes, showing every role.", today.plusDays(12), "PENDING", gmBea, gmMika);

            // Group 1: 80%
            task(g1, "Write the project proposal", "", today.minusDays(14), "COMPLETED", gmCarla, gmCarla);
            task(g1, "Draw the ERD", "", today.minusDays(10), "COMPLETED", gmNina, gmCarla);
            task(g1, "Make the app wireframes", "", today.minusDays(6), "COMPLETED", gmCarla, gmCarla);
            task(g1, "Build the item report screen", "", today.minusDays(2), "COMPLETED", gmNina, gmCarla);
            task(g1, "Final testing", "", today.plusDays(5), "ONGOING", gmNina, gmCarla);
            // Group 2: 40%
            task(g2, "Write the project proposal", "", today.minusDays(15), "COMPLETED", gmDiego, gmDiego);
            task(g2, "Draft the system flowchart", "", today.minusDays(8), "COMPLETED", gmRafael, gmDiego);
            task(g2, "Build the queue screen", "", today.plusDays(3), "ONGOING", gmRafael, gmDiego);
            task(g2, "Build the admin screen", "", today.plusDays(7), "PENDING", gmDiego, gmDiego);
            task(g2, "Prepare the user test script", "", today.plusDays(10), "PENDING", gmRafael, gmDiego);

            event(g3, "Group meeting", "Plan the demo and split the last tasks before the deadline.",
                    today.plusDays(7).atTime(15, 0), today.plusDays(7).atTime(16, 0), gmMika);
            event(g3, "Practice run", "Full run of the presentation with the hardware.",
                    today.plusDays(11).atTime(10, 0), today.plusDays(11).atTime(11, 30), gmAndrea);

            LocalDate chatDay = now.getHour() >= 12 ? today : today.minusDays(1);
            LocalDateTime morning = chatDay.atTime(9, 5);
            message(g3, mika, "Morning! Reminder that the sensor dashboard UI is due Thursday.", morning);
            message(g3, paolo, "Schema is almost done. I'll push it tonight so Andrea can start on the API.", chatDay.atTime(9, 12));
            message(g3, jomar, "First version of the sensor dashboard is up. Can someone check the chart colors?", chatDay.atTime(10, 42));
            message(g3, andrea, "Looks good. I'll test it with the sample sensor data tonight.", chatDay.atTime(10, 55));
            message(g3, prof, "Nice progress, Group 3. Please add your test plan to Shared documents before Friday.", chatDay.atTime(11, 20));
            message(g3, bea, "On it. I'll link the draft there today.", chatDay.atTime(11, 31));

            doc(g3, "Sprint task board", "https://trello.com/b/kinnect-group-3", gmMika, now.minusDays(8));
            doc(g3, "Sensor data sheet", "https://docs.google.com/spreadsheets/d/kinnect-sensor-data", gmPaolo, now.minusDays(6));
            doc(g3, "Defense slides", "https://docs.google.com/presentation/d/kinnect-defense", gmMika, now.minusDays(3));
            doc(g3, "Final paper draft", "https://docs.google.com/document/d/kinnect-final-paper", gmAndrea, now.minusDays(2));
            doc(g3, "Test plan draft", "https://docs.google.com/document/d/kinnect-test-plan", gmBea, now.minusHours(5));

            seedFile(g3, "System flowchart v3.pdf", "pdf", pdf(), gmAndrea, now.minusDays(1));
            seedFile(g3, "Dashboard wireframe.png", "png", wireframe(), gmJomar, now.minusDays(2));

            expense(g3, "Arduino Uno and jumper wires", "650", true, gmMika, today.minusDays(5));
            expense(g3, "pH and TDS water sensors", "800", true, gmPaolo, today.minusDays(2));
            expense(g3, "Printing and binding of the paper", "600", false, null, null);
            expense(g3, "Demo day materials", "450", false, null, null);
            paid(gmMika, now.minusDays(6));
            paid(gmAndrea, now.minusDays(4));
            paid(gmPaolo, now.minusDays(3));
            Db.insert("INSERT INTO budget_settings (group_id, due_date) VALUES (?, ?)", g3, today.plusDays(3));
        });
    }

    private static long user(String name, String email, String hash, String role) {
        return Db.insert("INSERT INTO users (full_name, email, password_hash, account_role) VALUES (?, ?, ?, ?)",
                name, email, hash, role);
    }

    private static long group(long classId, String name, String details) {
        return Db.insert("INSERT INTO project_groups (class_id, group_name, description) VALUES (?, ?, ?)",
                classId, name, details);
    }

    private static long member(long groupId, long enrollmentId, String role) {
        return Db.insert("INSERT INTO group_members (group_id, enrollment_id, group_role) VALUES (?, ?, ?)",
                groupId, enrollmentId, role);
    }

    private static void task(long groupId, String title, String desc, LocalDate deadline, String status,
            long assignee, long creator) {
        Db.insert("""
                INSERT INTO tasks (group_id, title, description, deadline, status, assignee_id, creator_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", groupId, title, desc, deadline, status, assignee, creator);
    }

    private static void event(long groupId, String title, String desc, LocalDateTime start, LocalDateTime end,
            long creator) {
        Db.insert("""
                INSERT INTO calendar_events (group_id, event_title, description, start_date_time, end_date_time, creator_id)
                VALUES (?, ?, ?, ?, ?, ?)""", groupId, title, desc, start, end, creator);
    }

    private static void message(long groupId, long userId, String text, LocalDateTime at) {
        Db.insert("INSERT INTO messages (group_id, sender_id, content, sent_at) VALUES (?, ?, ?, ?)",
                groupId, userId, text, at);
    }

    private static void doc(long groupId, String title, String url, long member, LocalDateTime at) {
        Db.insert("""
                INSERT INTO shared_documents (group_id, document_title, document_url, contributor_id, added_at)
                VALUES (?, ?, ?, ?, ?)""", groupId, title, url, member, at);
    }

    private static void expense(long groupId, String item, String amount, boolean bought, Long buyer, LocalDate on) {
        Db.insert("INSERT INTO expenses (group_id, item, amount, status, bought_by, bought_on) VALUES (?, ?, ?, ?, ?, ?)",
                groupId, item, new BigDecimal(amount), bought ? "BOUGHT" : "PLANNED", buyer, on);
    }

    private static void paid(long member, LocalDateTime at) {
        Db.insert("INSERT INTO payments (group_member_id, amount, paid_at) VALUES (?, ?, ?)",
                member, new BigDecimal("500.00"), at);
    }

    private static void seedFile(long groupId, String name, String type, byte[] bytes, long member, LocalDateTime at) {
        long id = Db.insert("""
                INSERT INTO group_files (group_id, file_name, storage_path, file_type, file_size, uploader_id, uploaded_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", groupId, name, CollabService.IN_DATABASE, type, (long) bytes.length,
                member, at);
        CollabService.storeChunks(id, bytes);
    }

    /** A one page PDF that says what the sample file is. */
    private static byte[] pdf() {
        String text = "Water Quality Monitor App: System Flowchart v3 (Kinnect demo file)";
        String stream = "BT /F1 16 Tf 60 760 Td (" + text + ") Tj ET";
        String[] objs = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>",
            "<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        };
        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objs.length];
        for (int i = 0; i < objs.length; i++) {
            offsets[i] = sb.length();
            sb.append(i + 1).append(" 0 obj\n").append(objs[i]).append("\nendobj\n");
        }
        int xref = sb.length();
        sb.append("xref\n0 ").append(objs.length + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) {
            sb.append(String.format("%010d 00000 n \n", off));
        }
        sb.append("trailer\n<< /Size ").append(objs.length + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return Db.ascii(sb.toString());
    }

    /** A simple wireframe picture drawn in code. */
    private static byte[] wireframe() {
        BufferedImage img = new BufferedImage(800, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xF6F3EC));
        g.fillRect(0, 0, 800, 500);
        g.setColor(new Color(0x1E6B52));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(30, 30, 740, 440, 18, 18);
        g.fillRoundRect(30, 30, 180, 440, 18, 18);
        g.setColor(new Color(0xC9D9CF));
        for (int i = 0; i < 6; i++) {
            g.fillRoundRect(52, 80 + i * 50, 136, 22, 10, 10);
        }
        g.setColor(new Color(0x1E6B52));
        g.drawRoundRect(240, 60, 240, 150, 14, 14);
        g.drawRoundRect(500, 60, 240, 150, 14, 14);
        g.drawRoundRect(240, 230, 500, 210, 14, 14);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int[] xs = {270, 330, 390, 450, 520, 590, 650, 710};
        int[] ys = {400, 350, 370, 300, 320, 270, 290, 250};
        g.drawPolyline(xs, ys, xs.length);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            return new byte[0];
        }
    }
}
