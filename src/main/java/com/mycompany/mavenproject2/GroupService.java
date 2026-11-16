package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.ClassRoom;
import com.mycompany.mavenproject2.Models.Enrollment;
import com.mycompany.mavenproject2.Models.GroupRole;
import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import com.mycompany.mavenproject2.Models.Progress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Groups: the professor creates a group and picks its leader; the leader adds members.
 *
 * @author Ariel Soriano
 */
public final class GroupService {

    private static final String GROUP_SELECT = """
            SELECT g.group_id, g.class_id, c.class_name, g.group_name, g.description,
              (SELECT MIN(u.full_name) FROM group_members m
                 JOIN class_enrollments e ON e.enrollment_id = m.enrollment_id
                 JOIN users u ON u.user_id = e.student_id
                 WHERE m.group_id = g.group_id AND m.group_role = 'LEADER') AS leader,
              (SELECT COUNT(*) FROM group_members m WHERE m.group_id = g.group_id) AS members
            FROM project_groups g JOIN classrooms c ON c.class_id = g.class_id
            """;

    static final String MEMBER_SELECT = """
            SELECT m.group_member_id, m.group_id, u.user_id, u.full_name, u.email, m.group_role
            FROM group_members m
            JOIN class_enrollments e ON e.enrollment_id = m.enrollment_id
            JOIN users u ON u.user_id = e.student_id
            """;

    private GroupService() {
    }

    private static ProjectGroup mapGroup(ResultSet rs) throws SQLException {
        return new ProjectGroup(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getInt(7));
    }

    static Member mapMember(ResultSet rs) throws SQLException {
        return new Member(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                GroupRole.valueOf(rs.getString(6)));
    }

    static ProjectGroup get(long groupId) {
        return Db.one(GROUP_SELECT + " WHERE g.group_id = ?", GroupService::mapGroup, groupId);
    }

    static List<ProjectGroup> groupsOf(long classId) {
        return Db.query(GROUP_SELECT + " WHERE g.class_id = ? ORDER BY g.group_name",
                GroupService::mapGroup, classId);
    }

    static ProjectGroup createGroup(long classId, long professorId, String groupName, String projectDetails,
            Enrollment leader) {
        ClassRoom room = ClassService.get(classId);
        if (room == null || room.professorId() != professorId) {
            throw new AppException("Only the professor of this class can create groups.");
        }
        String name = AuthService.clean(groupName);
        if (name.isEmpty()) {
            throw new AppException("Enter a group name.");
        }
        if (leader == null) {
            throw new AppException("Choose a group leader.");
        }
        if (Db.count("SELECT COUNT(*) FROM project_groups WHERE class_id = ? AND LOWER(group_name) = ?",
                classId, name.toLowerCase()) > 0) {
            throw new AppException(name + " already exists in this class.");
        }
        if (Db.count("SELECT COUNT(*) FROM group_members WHERE enrollment_id = ?", leader.id()) > 0) {
            throw new AppException(leader.studentName() + " is already in a group.");
        }
        long[] id = new long[1];
        Db.transaction(() -> {
            id[0] = Db.insert("INSERT INTO project_groups (class_id, group_name, description) VALUES (?, ?, ?)",
                    classId, name, AuthService.clean(projectDetails));
            Db.insert("INSERT INTO group_members (group_id, enrollment_id, group_role) VALUES (?, ?, 'LEADER')",
                    id[0], leader.id());
        });
        return get(id[0]);
    }

    static List<Member> members(long groupId) {
        return Db.query(MEMBER_SELECT + """
                 WHERE m.group_id = ?
                 ORDER BY CASE WHEN m.group_role = 'LEADER' THEN 0 ELSE 1 END, u.full_name
                """, GroupService::mapMember, groupId);
    }

    /** The user's membership in this group, or null if they are not in it. */
    static Member memberOf(long groupId, long userId) {
        return Db.one(MEMBER_SELECT + " WHERE m.group_id = ? AND u.user_id = ?",
                GroupService::mapMember, groupId, userId);
    }

    static boolean isProfessorOf(long groupId, long userId) {
        return Db.count("""
                SELECT COUNT(*) FROM project_groups g JOIN classrooms c ON c.class_id = g.class_id
                WHERE g.group_id = ? AND c.professor_id = ?""", groupId, userId) > 0;
    }

    static boolean isLeader(long groupId, long userId) {
        Member m = memberOf(groupId, userId);
        return m != null && m.isLeader();
    }

    static Member requireMember(long groupId, long userId) {
        Member m = memberOf(groupId, userId);
        if (m == null) {
            throw new AppException("You are not a member of this group.");
        }
        return m;
    }

    static Member requireLeader(long groupId, long userId) {
        Member m = requireMember(groupId, userId);
        if (!m.isLeader()) {
            throw new AppException("Only the group leader can do this.");
        }
        return m;
    }

    /** Students in the same class who are not in any group yet. */
    static List<Enrollment> candidates(long groupId) {
        ProjectGroup g = get(groupId);
        return ClassService.studentsWithoutGroup(g.classId());
    }

    /** The group leader or the professor of the class may manage members. */
    static void requireLeaderOrProfessor(long groupId, long userId) {
        if (!isProfessorOf(groupId, userId)) {
            requireLeader(groupId, userId);
        }
    }

    static void requireProfessor(long groupId, long userId) {
        if (!isProfessorOf(groupId, userId)) {
            throw new AppException("Only the professor of this class can do this.");
        }
    }

    /** Adds classmates to the group. Used by the group leader and by the professor. */
    static int addMembers(long groupId, long actingUserId, List<Enrollment> students) {
        requireLeaderOrProfessor(groupId, actingUserId);
        if (students == null || students.isEmpty()) {
            throw new AppException("Select at least one student to add.");
        }
        long classId = get(groupId).classId();
        Db.transaction(() -> {
            for (Enrollment e : students) {
                if (e.classId() != classId) {
                    throw new AppException(e.studentName() + " is not in this class.");
                }
                if (Db.count("SELECT COUNT(*) FROM group_members WHERE enrollment_id = ?", e.id()) > 0) {
                    throw new AppException(e.studentName() + " is already in a group.");
                }
                Db.insert("INSERT INTO group_members (group_id, enrollment_id, group_role) VALUES (?, ?, 'MEMBER')",
                        groupId, e.id());
            }
        });
        return students.size();
    }

    /** True when the professor set the group's name, so the leader can no longer change it. */
    static boolean nameLocked(long groupId) {
        return Db.count("SELECT COUNT(*) FROM group_name_locks WHERE group_id = ?", groupId) > 0;
    }

    /**
     * Changes the group's name and project title. The leader can do this until the professor
     * renames the group; a professor's change locks the name for the leader.
     */
    static ProjectGroup rename(long groupId, long userId, String groupName, String projectTitle) {
        boolean professor = isProfessorOf(groupId, userId);
        if (!professor) {
            requireLeader(groupId, userId);
            if (nameLocked(groupId)) {
                throw new AppException("Your professor set this group's name, so only they can change it.");
            }
        }
        ProjectGroup g = get(groupId);
        String name = AuthService.clean(groupName);
        if (name.isEmpty()) {
            throw new AppException("Enter a group name.");
        }
        if (name.length() > 80) {
            throw new AppException("Group name is too long.");
        }
        if (Db.count("SELECT COUNT(*) FROM project_groups WHERE class_id = ? AND LOWER(group_name) = ? AND group_id <> ?",
                g.classId(), name.toLowerCase(), groupId) > 0) {
            throw new AppException(name + " already exists in this class.");
        }
        String title = AuthService.clean(projectTitle);
        if (title.length() > 255) {
            throw new AppException("Project title is too long.");
        }
        Db.transaction(() -> {
            Db.update("UPDATE project_groups SET group_name = ?, description = ? WHERE group_id = ?", name, title, groupId);
            if (professor && !nameLocked(groupId)) {
                Db.insert("INSERT INTO group_name_locks (group_id, locked_by) VALUES (?, ?)", groupId, userId);
            }
        });
        return get(groupId);
    }

    /** Professor only: lets the leader rename the group again, or locks the name. */
    static void setNameLocked(long groupId, long professorId, boolean locked) {
        requireProfessor(groupId, professorId);
        if (locked == nameLocked(groupId)) {
            return;
        }
        if (locked) {
            Db.insert("INSERT INTO group_name_locks (group_id, locked_by) VALUES (?, ?)", groupId, professorId);
        } else {
            Db.update("DELETE FROM group_name_locks WHERE group_id = ?", groupId);
        }
    }

    /** Professor only: makes another member of the group its leader. The old leader becomes a member. */
    static void setLeader(long groupId, long professorId, Member newLeader) {
        requireProfessor(groupId, professorId);
        if (newLeader == null || newLeader.groupId() != groupId) {
            throw new AppException("Choose a member of this group.");
        }
        if (newLeader.isLeader()) {
            throw new AppException(newLeader.fullName() + " is already the leader.");
        }
        Db.transaction(() -> {
            Db.update("UPDATE group_members SET group_role = 'MEMBER' WHERE group_id = ? AND group_role = 'LEADER'",
                    groupId);
            Db.update("UPDATE group_members SET group_role = 'LEADER' WHERE group_member_id = ?", newLeader.id());
        });
    }

    /**
     * Professor only: takes a student out of the group so they can be placed in another one.
     * The leader can't be removed (choose a new leader first), and neither can a member who
     * already has work in the group, so no task, file, event, or link loses its owner.
     */
    static void removeMember(long groupId, long professorId, Member member) {
        requireProfessor(groupId, professorId);
        if (member == null || member.groupId() != groupId) {
            throw new AppException("Choose a member of this group.");
        }
        if (member.isLeader()) {
            throw new AppException("Make someone else the leader before removing " + member.fullName() + ".");
        }
        long work = Db.count("SELECT COUNT(*) FROM tasks WHERE assignee_id = ? OR creator_id = ?", member.id(), member.id())
                + Db.count("SELECT COUNT(*) FROM group_files WHERE uploader_id = ?", member.id())
                + Db.count("SELECT COUNT(*) FROM calendar_events WHERE creator_id = ?", member.id())
                + Db.count("SELECT COUNT(*) FROM shared_documents WHERE contributor_id = ?", member.id())
                + Db.count("SELECT COUNT(*) FROM expenses WHERE bought_by = ?", member.id())
                + Db.count("SELECT COUNT(*) FROM payments WHERE group_member_id = ?", member.id());
        if (work > 0) {
            throw new AppException(member.fullName() + " already has tasks, files, events, links, or ambagan payments "
                    + "in this group, so they can't be removed.");
        }
        Db.update("DELETE FROM group_members WHERE group_member_id = ?", member.id());
    }

    static Progress progress(long groupId) {
        return Db.one("""
                SELECT SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END),
                       SUM(CASE WHEN status = 'ONGOING' THEN 1 ELSE 0 END),
                       SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END)
                FROM tasks WHERE group_id = ?""",
                rs -> new Progress(rs.getInt(1), rs.getInt(2), rs.getInt(3)), groupId);
    }
}
