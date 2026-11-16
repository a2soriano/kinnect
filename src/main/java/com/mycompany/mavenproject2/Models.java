package com.mycompany.mavenproject2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data classes shared by the database layer and the screens.
 * They follow the Kinnect class diagram: User, Classroom, ClassEnrollment,
 * ProjectGroup, GroupMember, Task, GroupFile, CalendarEvent, Message,
 * SharedDocument, plus the leader-only budget (Expense and Contribution).
 *
 * @author Ariel Soriano
 */
public final class Models {

    private Models() {
    }

    enum AccountRole { PROFESSOR, STUDENT }

    enum GroupRole { LEADER, MEMBER }

    enum TaskStatus {
        PENDING, ONGOING, COMPLETED;

        String label() {
            return name().charAt(0) + name().substring(1).toLowerCase();
        }
    }

    enum ExpenseStatus { PLANNED, BOUGHT }

    record User(long id, String fullName, String email, AccountRole role) {
    }

    record ClassRoom(long id, long professorId, String professorName, String name,
            String description, String code, int studentCount, int groupCount) {
    }

    record Enrollment(long id, long classId, long studentId, String studentName,
            String email, Long groupId, String groupName) {
    }

    record ProjectGroup(long id, long classId, String className, String name,
            String description, String leaderName, int memberCount) {

        String title() {
            return description == null || description.isBlank() ? name : description;
        }
    }

    record Member(long id, long groupId, long userId, String fullName, String email, GroupRole role) {

        boolean isLeader() {
            return role == GroupRole.LEADER;
        }
    }

    record Task(long id, long groupId, String title, String description, LocalDate deadline,
            TaskStatus status, long assigneeId, String assigneeName, long creatorId, String creatorName) {
    }

    record GroupFile(long id, String fileName, String storagePath, String fileType,
            long fileSize, String uploaderName, LocalDateTime uploadedAt) {
    }

    record CalendarEvent(long id, String title, String description, LocalDateTime start,
            LocalDateTime end, String creatorName) {
    }

    record Message(long id, long senderId, String senderName, String content, LocalDateTime sentAt) {
    }

    record SharedDocument(long id, String title, String url, String contributorName, LocalDateTime addedAt) {
    }

    record Expense(long id, String item, BigDecimal amount, ExpenseStatus status,
            String boughtByName, LocalDate boughtOn) {
    }

    /**
     * One member's ambagan: what they should pay (their share), whether the leader set it by hand
     * (custom), how much they have handed over so far, and when they last paid.
     */
    record Contribution(Member member, BigDecimal share, boolean custom, BigDecimal paid, LocalDateTime lastPaidAt) {

        BigDecimal balance() {
            BigDecimal b = share.subtract(paid);
            return b.signum() > 0 ? b : BigDecimal.ZERO.setScale(2);
        }

        boolean isPaid() {
            return share.signum() > 0 && paid.compareTo(share) >= 0;
        }
    }

    record Progress(int completed, int ongoing, int pending) {

        int total() {
            return completed + ongoing + pending;
        }

        int percent() {
            return total() == 0 ? 0 : Math.round(completed * 100f / total());
        }
    }

    /**
     * The numbers at the top of the Budget page. evenShare is what each member without a custom
     * share pays. uncovered is money no one is asked to pay (everyone has a custom share and they
     * add up to less than the total); overAssigned is custom shares adding up to more than the total.
     */
    record BudgetSummary(BigDecimal total, BigDecimal evenShare, BigDecimal totalDue, BigDecimal collected,
            BigDecimal toCollect, BigDecimal spent, BigDecimal cashOnHand, BigDecimal stillToSpend, int paidCount,
            int memberCount, int customCount, int expenseCount, LocalDate dueDate, BigDecimal uncovered,
            BigDecimal overAssigned) {

        int percentCollected() {
            if (totalDue.signum() == 0) {
                return 0;
            }
            int p = collected.multiply(BigDecimal.valueOf(100)).divide(totalDue, 0, java.math.RoundingMode.DOWN).intValue();
            return Math.min(100, p);
        }
    }

    /** One item on the calendar: either a task deadline or a group event. */
    record CalendarItem(LocalDate date, String title, String detail, boolean isEvent, CalendarEvent event) {
    }

    /** A class the student joined, with their group in it (or null if not grouped yet). */
    record StudentClass(ClassRoom classRoom, ProjectGroup group, GroupRole role) {
    }

    /** A rule from the flowchart was broken; the message is shown to the user as is. */
    static final class AppException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        AppException(String message) {
            super(message);
        }
    }
}
