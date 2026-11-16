package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.BudgetSummary;
import com.mycompany.mavenproject2.Models.Contribution;
import com.mycompany.mavenproject2.Models.Expense;
import com.mycompany.mavenproject2.Models.ExpenseStatus;
import com.mycompany.mavenproject2.Models.Member;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Budget (ambagan). Only the group leader can open or change any of it.
 * By default the total of all expenses is split evenly. The leader can give a member a
 * custom share (for example, someone who agreed to pay more); everyone else then splits
 * what is left evenly. Shares are rounded up to the next whole peso so the group never
 * comes up short. Members can pay in parts; each payment is recorded.
 *
 * @author Ariel Soriano
 */
public final class BudgetService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private BudgetService() {
    }

    /** How the total is divided: the even share, who has a custom share, and any gap. */
    record Split(BigDecimal even, Map<Long, BigDecimal> custom, int evenCount, BigDecimal uncovered,
            BigDecimal overAssigned) {

        BigDecimal shareOf(long memberId) {
            return custom.getOrDefault(memberId, even);
        }
    }

    // ---------- expenses ----------

    static List<Expense> expenses(long groupId, long leaderUserId) {
        GroupService.requireLeader(groupId, leaderUserId);
        return Db.query("""
                SELECT x.expense_id, x.item, x.amount, x.status, u.full_name, x.bought_on
                FROM expenses x
                LEFT JOIN group_members m ON m.group_member_id = x.bought_by
                LEFT JOIN class_enrollments e ON e.enrollment_id = m.enrollment_id
                LEFT JOIN users u ON u.user_id = e.student_id
                WHERE x.group_id = ?
                ORDER BY CASE x.status WHEN 'BOUGHT' THEN 0 ELSE 1 END, x.expense_id""",
                rs -> new Expense(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3),
                        ExpenseStatus.valueOf(rs.getString(4)), rs.getString(5),
                        rs.getObject(6, LocalDate.class)), groupId);
    }

    static void addExpense(long groupId, long leaderUserId, String item, String amountText, boolean bought,
            Member boughtBy) {
        Member leader = GroupService.requireLeader(groupId, leaderUserId);
        String name = AuthService.clean(item);
        if (name.isEmpty()) {
            throw new AppException("Enter what the money is for.");
        }
        BigDecimal amount = parseAmount(amountText);
        Member buyer = boughtBy == null ? leader : boughtBy;
        Db.insert("INSERT INTO expenses (group_id, item, amount, status, bought_by, bought_on) VALUES (?, ?, ?, ?, ?, ?)",
                groupId, name, amount, bought ? "BOUGHT" : "PLANNED",
                bought ? buyer.id() : null, bought ? LocalDate.now() : null);
    }

    static BigDecimal parseAmount(String text) {
        String clean = text == null ? "" : text.replace("₱", "").replace(",", "").trim();
        try {
            BigDecimal amount = new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
            if (amount.signum() <= 0) {
                throw new AppException("The amount must be more than zero.");
            }
            if (amount.compareTo(new BigDecimal("1000000")) >= 0) {
                throw new AppException("That amount is too large.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new AppException("Enter the amount as a number, like 650 or 650.50.");
        }
    }

    static void markBought(long groupId, long leaderUserId, long expenseId) {
        Member leader = GroupService.requireLeader(groupId, leaderUserId);
        int n = Db.update("""
                UPDATE expenses SET status = 'BOUGHT', bought_by = ?, bought_on = ?
                WHERE expense_id = ? AND group_id = ? AND status = 'PLANNED'""",
                leader.id(), LocalDate.now(), expenseId, groupId);
        if (n == 0) {
            throw new AppException("That item is already marked as bought.");
        }
    }

    static void removeExpense(long groupId, long leaderUserId, long expenseId) {
        GroupService.requireLeader(groupId, leaderUserId);
        Db.update("DELETE FROM expenses WHERE expense_id = ? AND group_id = ?", expenseId, groupId);
    }

    static BigDecimal total(long groupId) {
        BigDecimal t = Db.one("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE group_id = ?",
                rs -> rs.getBigDecimal(1), groupId);
        return t == null ? ZERO : t.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- shares ----------

    /** Custom shares the leader set, by group member id. */
    static Map<Long, BigDecimal> customShares(long groupId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        Db.query("""
                SELECT s.group_member_id, s.amount FROM member_shares s
                JOIN group_members m ON m.group_member_id = s.group_member_id
                WHERE m.group_id = ?""", rs -> map.put(rs.getLong(1), rs.getBigDecimal(2).setScale(2)), groupId);
        return map;
    }

    /** Divides the total: custom shares first, then the rest evenly among everyone else. */
    static Split split(BigDecimal total, List<Member> members, Map<Long, BigDecimal> customShares) {
        Map<Long, BigDecimal> custom = new HashMap<>();
        BigDecimal customSum = ZERO;
        for (Member m : members) {
            BigDecimal c = customShares.get(m.id());
            if (c != null) {
                custom.put(m.id(), c);
                customSum = customSum.add(c);
            }
        }
        int evenCount = members.size() - custom.size();
        BigDecimal rest = total.subtract(customSum);
        BigDecimal even = ZERO;
        BigDecimal uncovered = ZERO;
        BigDecimal over = ZERO;
        if (rest.signum() < 0) {
            over = rest.negate();
        } else if (evenCount > 0) {
            even = rest.divide(BigDecimal.valueOf(evenCount), 0, RoundingMode.CEILING).setScale(2);
        } else {
            uncovered = rest;
        }
        return new Split(even, custom, evenCount, uncovered, over);
    }

    static Split split(long groupId) {
        return split(total(groupId), GroupService.members(groupId), customShares(groupId));
    }

    /** What the shares would be if this member's custom share were changed (null = split evenly). */
    static Split preview(long groupId, long leaderUserId, Member member, BigDecimal customAmount) {
        GroupService.requireLeader(groupId, leaderUserId);
        Map<Long, BigDecimal> custom = customShares(groupId);
        if (customAmount == null) {
            custom.remove(member.id());
        } else {
            custom.put(member.id(), customAmount);
        }
        return split(total(groupId), GroupService.members(groupId), custom);
    }

    /**
     * Gives a member a custom share, or (with a blank amount) puts them back on the even split.
     * The others always split whatever is left evenly.
     */
    static void setShare(long groupId, long leaderUserId, Member member, String amountText) {
        GroupService.requireLeader(groupId, leaderUserId);
        if (member == null || member.groupId() != groupId) {
            throw new AppException("Choose a member of this group.");
        }
        if (amountText == null || amountText.isBlank()) {
            Db.update("DELETE FROM member_shares WHERE group_member_id = ?", member.id());
            return;
        }
        BigDecimal amount = parseAmount(amountText);
        BigDecimal total = total(groupId);
        if (total.signum() == 0) {
            throw new AppException("Add an expense first, then set how much each member pays.");
        }
        Split s = preview(groupId, leaderUserId, member, amount);
        if (s.overAssigned().signum() > 0) {
            throw new AppException("Custom shares would add up to " + Ui.peso(total.add(s.overAssigned()))
                    + ", more than the " + Ui.peso(total) + " budget. Lower one of them.");
        }
        Db.transaction(() -> {
            Db.update("DELETE FROM member_shares WHERE group_member_id = ?", member.id());
            Db.insert("INSERT INTO member_shares (group_member_id, amount) VALUES (?, ?)", member.id(), amount);
        });
    }

    // ---------- payments ----------

    static List<Contribution> contributions(long groupId, long leaderUserId) {
        GroupService.requireLeader(groupId, leaderUserId);
        List<Member> members = GroupService.members(groupId);
        Split s = split(total(groupId), members, customShares(groupId));
        record Paid(BigDecimal amount, LocalDateTime last) {
        }
        Map<Long, Paid> paid = new HashMap<>();
        Db.query("""
                SELECT p.group_member_id, SUM(p.amount), MAX(p.paid_at) FROM payments p
                JOIN group_members m ON m.group_member_id = p.group_member_id
                WHERE m.group_id = ? GROUP BY p.group_member_id""",
                rs -> paid.put(rs.getLong(1), new Paid(rs.getBigDecimal(2).setScale(2),
                        rs.getObject(3, LocalDateTime.class))), groupId);
        return members.stream().map(m -> {
            Paid p = paid.get(m.id());
            return new Contribution(m, s.shareOf(m.id()), s.custom().containsKey(m.id()),
                    p == null ? ZERO : p.amount(), p == null ? null : p.last());
        }).toList();
    }

    private static Contribution contributionOf(long groupId, long leaderUserId, Member member) {
        if (member == null || member.groupId() != groupId) {
            throw new AppException("Choose a member of this group.");
        }
        return contributions(groupId, leaderUserId).stream()
                .filter(c -> c.member().id() == member.id()).findFirst()
                .orElseThrow(() -> new AppException("Choose a member of this group."));
    }

    /**
     * Records money a member handed over. A blank amount means they paid everything they still owe.
     * Partial payments are allowed; paying more than the balance is not.
     */
    static BigDecimal recordPayment(long groupId, long leaderUserId, Member member, String amountText) {
        Contribution c = contributionOf(groupId, leaderUserId, member);
        if (c.share().signum() == 0) {
            throw new AppException("Add an expense first so Kinnect knows how much each member owes.");
        }
        BigDecimal balance = c.balance();
        if (balance.signum() == 0) {
            throw new AppException(member.fullName() + " already paid their full share.");
        }
        BigDecimal amount = amountText == null || amountText.isBlank() ? balance : parseAmount(amountText);
        if (amount.compareTo(balance) > 0) {
            throw new AppException("That's more than " + member.fullName() + " still owes (" + Ui.peso(balance) + ").");
        }
        Db.insert("INSERT INTO payments (group_member_id, amount, paid_at) VALUES (?, ?, ?)",
                member.id(), amount, LocalDateTime.now());
        return amount;
    }

    /** Removes the member's most recent payment (for when one was recorded by mistake). */
    static void undoLastPayment(long groupId, long leaderUserId, Member member) {
        contributionOf(groupId, leaderUserId, member);
        Long last = Db.one("SELECT MAX(payment_id) FROM payments WHERE group_member_id = ?",
                rs -> rs.getObject(1, Long.class), member.id());
        if (last == null) {
            throw new AppException(member.fullName() + " has no payments to undo.");
        }
        Db.update("DELETE FROM payments WHERE payment_id = ?", last);
    }

    static void setDueDate(long groupId, long leaderUserId, LocalDate due) {
        GroupService.requireLeader(groupId, leaderUserId);
        if (Db.count("SELECT COUNT(*) FROM budget_settings WHERE group_id = ?", groupId) == 0) {
            Db.insert("INSERT INTO budget_settings (group_id, due_date) VALUES (?, ?)", groupId, due);
        } else {
            Db.update("UPDATE budget_settings SET due_date = ? WHERE group_id = ?", due, groupId);
        }
    }

    static BudgetSummary summary(long groupId, long leaderUserId) {
        List<Expense> expenses = expenses(groupId, leaderUserId);
        List<Contribution> people = contributions(groupId, leaderUserId);
        Split s = split(groupId);
        BigDecimal total = ZERO;
        BigDecimal spent = ZERO;
        for (Expense x : expenses) {
            total = total.add(x.amount());
            if (x.status() == ExpenseStatus.BOUGHT) {
                spent = spent.add(x.amount());
            }
        }
        BigDecimal due = ZERO;
        BigDecimal collected = ZERO;
        BigDecimal toCollect = ZERO;
        int paidCount = 0;
        for (Contribution c : people) {
            due = due.add(c.share());
            collected = collected.add(c.paid());
            toCollect = toCollect.add(c.balance());
            if (c.isPaid()) {
                paidCount++;
            }
        }
        LocalDate dueDate = Db.one("SELECT due_date FROM budget_settings WHERE group_id = ?",
                rs -> rs.getObject(1, LocalDate.class), groupId);
        return new BudgetSummary(total, s.even(), due, collected, toCollect, spent, collected.subtract(spent),
                total.subtract(spent), paidCount, people.size(), s.custom().size(), expenses.size(), dueDate,
                s.uncovered(), s.overAssigned());
    }
}
