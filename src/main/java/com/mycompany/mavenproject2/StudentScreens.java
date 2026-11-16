package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import com.mycompany.mavenproject2.Models.Progress;
import com.mycompany.mavenproject2.Models.StudentClass;
import com.mycompany.mavenproject2.Models.Task;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The student's home: each class they joined, their group in it, and their tasks.
 *
 * @author Ariel Soriano
 */
public final class StudentScreens {

    private StudentScreens() {
    }

    static Parent home() {
        long me = Session.user().id();
        List<StudentClass> classes = ClassService.studentClasses(me);
        VBox page = new VBox(40);
        for (int i = 0; i < classes.size(); i++) {
            page.getChildren().add(classSection(classes.get(i), me, i == 0, classes.size()));
        }
        return Ui.topLayout(page);
    }

    private static VBox classSection(StudentClass sc, long me, boolean first, int classCount) {
        Button join = Ui.secondary("Join another class");
        join.setOnAction(e -> KinnectApp.show(AuthScreens.joinClass(true)));
        HBox header = first
                ? Ui.pageHeader(Ui.text(classCount > 1 ? "My classes" : "My class", "eyebrow"), sc.classRoom().name(),
                        sc.classRoom().professorName() + ", class code " + sc.classRoom().code(), join)
                : Ui.pageHeader(null, sc.classRoom().name(),
                        sc.classRoom().professorName() + ", class code " + sc.classRoom().code());

        VBox groupCard;
        VBox tasksCard;
        ProjectGroup g = sc.group();
        if (g == null) {
            groupCard = Ui.card(0, Ui.emptyState("users", "You're not in a group yet",
                    "Your professor hasn't added you to a group. Your group, tasks, and deadlines will show up "
                    + "here once you're assigned."));
            tasksCard = Ui.card(10, Ui.cardHeader("Your tasks", Ui.text("None yet", "small", "muted")),
                    Ui.wrap("Tasks your group leader assigns to you will appear here.", "muted"));
        } else {
            Member mine = GroupService.memberOf(g.id(), me);
            List<Member> members = GroupService.members(g.id());
            Progress p = GroupService.progress(g.id());
            Button open = Ui.primary("Open my group");
            open.setOnAction(e -> KinnectApp.show(GroupScreen.open(g.id())));
            HBox top = Ui.row(8, Ui.text("Your group", "small", "muted"), Ui.grow(),
                    Ui.text(mine.isLeader() ? "Group leader" : "Member", "pill", "pill-green"));
            HBox people = Ui.row(12, Ui.avatarStack(members.stream().map(Member::fullName).toList(), 28),
                    Ui.text(Ui.plural(members.size(), "member", "members") + (g.leaderName() == null ? ""
                            : ", led by " + Ui.shortName(g.leaderName())), "muted"));
            HBox progHead = Ui.row(8, Ui.text("Group progress", "strong"), Ui.grow(),
                    Ui.text(p.completed() + " of " + Ui.plural(p.total(), "task", "tasks") + " done", "small", "muted"));
            groupCard = Ui.card(10, top, Ui.text(g.name(), "stat-md"), Ui.text(g.description(), "lead"), people,
                    progHead, Ui.segmentBar(p, 6), open);
            VBox.setMargin(people, new Insets(6, 0, 8, 0));
            VBox.setMargin(open, new Insets(10, 0, 0, 0));

            List<Task> tasks = TaskService.tasksOfMember(mine.id());
            VBox list = new VBox();
            for (Task t : tasks.stream().limit(5).toList()) {
                HBox r = Ui.row(10, Ui.avatar(t.assigneeName(), 28),
                        new VBox(2, Ui.text(t.title(), "strong"), Ui.text("Due " + Ui.day(t.deadline()), "small", "muted")),
                        Ui.grow(), Ui.statusPill(t.status()));
                r.setPadding(new Insets(12, 0, 12, 0));
                r.getStyleClass().add("row-line");
                list.getChildren().add(r);
            }
            if (tasks.isEmpty()) {
                list.getChildren().add(Ui.wrap("Nothing assigned to you yet. Tasks your group leader assigns to "
                        + "you will appear here.", "muted"));
            }
            tasksCard = Ui.card(8, Ui.cardHeader("Your tasks", Ui.text(tasks.size() + " assigned", "small", "muted")),
                    list);
        }
        GridPane grid = Ui.columns(20, 64, 36);
        grid.add(groupCard, 0, 0);
        grid.add(tasksCard, 1, 0);
        GridPane.setValignment(groupCard, VPos.TOP);
        GridPane.setValignment(tasksCard, VPos.TOP);
        groupCard.setMaxHeight(Double.MAX_VALUE);
        tasksCard.setMaxHeight(Double.MAX_VALUE);
        return new VBox(22, header, grid);
    }
}
