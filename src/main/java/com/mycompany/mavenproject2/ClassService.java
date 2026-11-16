package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.ClassRoom;
import com.mycompany.mavenproject2.Models.Enrollment;
import com.mycompany.mavenproject2.Models.GroupRole;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import com.mycompany.mavenproject2.Models.StudentClass;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Classes: the professor creates them, students join with the class code.
 *
 * @author Ariel Soriano
 */
public final class ClassService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String CLASS_SELECT = """
            SELECT c.class_id, c.professor_id, u.full_name, c.class_name, c.description, c.class_code,
              (SELECT COUNT(*) FROM class_enrollments e WHERE e.class_id = c.class_id) AS students,
              (SELECT COUNT(*) FROM project_groups g WHERE g.class_id = c.class_id) AS groupcount
            FROM classrooms c JOIN users u ON u.user_id = c.professor_id
            """;

    private ClassService() {
    }

    static ClassRoom mapClass(ResultSet rs) throws SQLException {
        return new ClassRoom(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getInt(7), rs.getInt(8));
    }

    static ClassRoom createClass(long professorId, String className, String details) {
        String name = AuthService.clean(className);
        if (name.isEmpty()) {
            throw new AppException("Enter a class name.");
        }
        if (name.length() > 120) {
            throw new AppException("Class name is too long.");
        }
        String code = newCode();
        long id = Db.insert("INSERT INTO classrooms (professor_id, class_name, description, class_code) VALUES (?, ?, ?, ?)",
                professorId, name, AuthService.clean(details), code);
        return get(id);
    }

    static String newCode() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i == 3) {
                    sb.append('-');
                }
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (Db.count("SELECT COUNT(*) FROM classrooms WHERE class_code = ?", code) == 0) {
                return code;
            }
        }
    }

    static ClassRoom get(long classId) {
        return Db.one(CLASS_SELECT + " WHERE c.class_id = ?", ClassService::mapClass, classId);
    }

    static List<ClassRoom> classesOf(long professorId) {
        return Db.query(CLASS_SELECT + " WHERE c.professor_id = ? ORDER BY c.class_id DESC",
                ClassService::mapClass, professorId);
    }

    static ClassRoom joinClass(long studentId, String classCode) {
        String code = AuthService.clean(classCode).toUpperCase().replace(" ", "");
        if (code.length() == 6 && !code.contains("-")) {
            code = code.substring(0, 3) + "-" + code.substring(3);
        }
        if (code.isEmpty()) {
            throw new AppException("Enter the class code from your professor.");
        }
        ClassRoom room = Db.one(CLASS_SELECT + " WHERE c.class_code = ?", ClassService::mapClass, code);
        if (room == null) {
            throw new AppException("That class code does not exist. Check it with your professor.");
        }
        if (Db.count("SELECT COUNT(*) FROM class_enrollments WHERE class_id = ? AND student_id = ?",
                room.id(), studentId) > 0) {
            throw new AppException("You already joined " + room.name() + ".");
        }
        Db.insert("INSERT INTO class_enrollments (class_id, student_id) VALUES (?, ?)", room.id(), studentId);
        return get(room.id());
    }

    /** Every student in the class with the group they are in, if any. */
    static List<Enrollment> students(long classId) {
        return Db.query("""
                SELECT e.enrollment_id, e.class_id, u.user_id, u.full_name, u.email, g.group_id, g.group_name
                FROM class_enrollments e
                JOIN users u ON u.user_id = e.student_id
                LEFT JOIN group_members m ON m.enrollment_id = e.enrollment_id
                LEFT JOIN project_groups g ON g.group_id = m.group_id
                WHERE e.class_id = ?
                ORDER BY u.full_name
                """, rs -> new Enrollment(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                rs.getString(5), rs.getObject(6, Long.class), rs.getString(7)), classId);
    }

    static List<Enrollment> studentsWithoutGroup(long classId) {
        return students(classId).stream().filter(e -> e.groupId() == null).toList();
    }

    /** The classes a student joined, each with their group in it (if they have one yet). */
    static List<StudentClass> studentClasses(long studentId) {
        return Db.query("""
                SELECT e.class_id, g.group_id, m.group_role
                FROM class_enrollments e
                LEFT JOIN group_members m ON m.enrollment_id = e.enrollment_id
                LEFT JOIN project_groups g ON g.group_id = m.group_id
                WHERE e.student_id = ?
                ORDER BY e.enrollment_id DESC
                """, rs -> new long[]{rs.getLong(1), rs.getLong(2), rs.getString(3) == null ? -1
                : GroupRole.valueOf(rs.getString(3)).ordinal()}, studentId)
                .stream().map(r -> {
                    ClassRoom room = get(r[0]);
                    ProjectGroup group = r[1] == 0 ? null : GroupService.get(r[1]);
                    GroupRole role = r[2] < 0 ? null : GroupRole.values()[(int) r[2]];
                    return new StudentClass(room, group, role);
                }).toList();
    }
}
