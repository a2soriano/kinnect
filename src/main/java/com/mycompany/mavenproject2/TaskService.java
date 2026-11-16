package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.Task;
import com.mycompany.mavenproject2.Models.TaskStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tasks: the leader creates and assigns them, the assigned member updates the status.
 *
 * @author Ariel Soriano
 */
public final class TaskService {

    private static final String TASK_SELECT = """
            SELECT t.task_id, t.group_id, t.title, t.description, t.deadline, t.status,
                   t.assignee_id, ua.full_name, t.creator_id, uc.full_name
            FROM tasks t
            JOIN group_members ma ON ma.group_member_id = t.assignee_id
            JOIN class_enrollments ea ON ea.enrollment_id = ma.enrollment_id
            JOIN users ua ON ua.user_id = ea.student_id
            JOIN group_members mc ON mc.group_member_id = t.creator_id
            JOIN class_enrollments ec ON ec.enrollment_id = mc.enrollment_id
            JOIN users uc ON uc.user_id = ec.student_id
            """;

    private static final String ORDER = """
             ORDER BY CASE t.status WHEN 'ONGOING' THEN 0 WHEN 'PENDING' THEN 1 ELSE 2 END, t.deadline, t.task_id
            """;

    private TaskService() {
    }

    private static Task map(ResultSet rs) throws SQLException {
        return new Task(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4),
                rs.getObject(5, LocalDate.class), TaskStatus.valueOf(rs.getString(6)),
                rs.getLong(7), rs.getString(8), rs.getLong(9), rs.getString(10));
    }

    static Task create(long groupId, long leaderUserId, String title, String description, LocalDate deadline,
            Member assignee) {
        Member leader = GroupService.requireLeader(groupId, leaderUserId);
        String name = AuthService.clean(title);
        if (name.isEmpty()) {
            throw new AppException("Enter a task title.");
        }
        if (name.length() > 150) {
            throw new AppException("Task title is too long.");
        }
        if (deadline == null) {
            throw new AppException("Pick a deadline.");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new AppException("The deadline cannot be in the past.");
        }
        if (assignee == null || assignee.groupId() != groupId) {
            throw new AppException("Assign the task to a member of this group.");
        }
        long id = Db.insert("""
                INSERT INTO tasks (group_id, title, description, deadline, status, assignee_id, creator_id)
                VALUES (?, ?, ?, ?, 'PENDING', ?, ?)""",
                groupId, name, description == null ? "" : description.trim(), deadline, assignee.id(), leader.id());
        return get(id);
    }

    static Task get(long taskId) {
        return Db.one(TASK_SELECT + " WHERE t.task_id = ?", TaskService::map, taskId);
    }

    static List<Task> tasks(long groupId) {
        return Db.query(TASK_SELECT + " WHERE t.group_id = ?" + ORDER, TaskService::map, groupId);
    }

    static List<Task> tasksOfMember(long memberId) {
        return Db.query(TASK_SELECT + " WHERE t.assignee_id = ?" + ORDER, TaskService::map, memberId);
    }

    /** Every task assigned to this student, across all of their groups. */
    static List<Task> tasksOfStudent(long userId) {
        return Db.query(TASK_SELECT + " WHERE ua.user_id = ? AND t.status <> 'COMPLETED'" + ORDER,
                TaskService::map, userId);
    }

    static Task updateStatus(long taskId, long userId, TaskStatus status) {
        Task task = get(taskId);
        if (task == null) {
            throw new AppException("That task no longer exists.");
        }
        Member me = GroupService.memberOf(task.groupId(), userId);
        if (me == null || me.id() != task.assigneeId()) {
            throw new AppException("Only " + task.assigneeName() + " can update this task.");
        }
        if (status == null) {
            throw new AppException("Choose a status.");
        }
        Db.update("UPDATE tasks SET status = ?, updated_at = ? WHERE task_id = ?",
                status.name(), LocalDateTime.now(), taskId);
        return get(taskId);
    }

    static boolean isOverdue(Task t) {
        return t.status() != TaskStatus.COMPLETED && t.deadline().isBefore(LocalDate.now());
    }
}
