package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.ClassRoom;
import com.mycompany.mavenproject2.Models.Enrollment;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Professor screens: My classes and the class workspace (groups and students).
 *
 * @author Ariel Soriano
 */
public final class ProfessorScreens {

    private ProfessorScreens() {
    }

    static Parent myClasses() {
        List<ClassRoom> classes = ClassService.classesOf(Session.user().id());
        Button create = Ui.primary("Create class");
        create.setOnAction(e -> Dialogs.createClass(room -> {
            KinnectApp.show(myClasses());
            Dialogs.classCode(room);
        }));
        VBox page = new VBox(22);
        page.getChildren().add(Ui.pageHeader(null, "My classes",
                "Create a class to get a code your students can join with.", create));
        if (classes.isEmpty()) {
            Button first = Ui.primary("Create your first class");
            first.setOnAction(create.getOnAction());
            VBox empty = Ui.emptyState("cap", "No classes yet",
                    "Create a class and Kinnect gives you a class code. Students join with it, then you "
                    + "form groups and pick their leaders.");
            empty.getChildren().add(first);
            page.getChildren().add(Ui.card(0, empty));
        } else {
            GridPane grid = Ui.columns(18, 33.33, 33.33, 33.34);
            for (int i = 0; i < classes.size(); i++) {
                grid.add(classCard(classes.get(i)), i % 3, i / 3);
            }
            page.getChildren().add(grid);
        }
        return Ui.topLayout(page);
    }

    private static VBox classCard(ClassRoom c) {
        Label code = Ui.text("Code " + c.code(), "pill", "pill-green");
        HBox counts = Ui.row(18,
                new Label(Ui.plural(c.studentCount(), "student", "students"), Ui.icon("users", 16, Theme.MUTED)),
                new Label(Ui.plural(c.groupCount(), "group", "groups"), Ui.icon("grid", 15, Theme.MUTED)));
        counts.getChildren().forEach(n -> ((Label) n).setStyle("-fx-text-fill: " + Theme.MUTED + "; -fx-graphic-text-gap: 7;"));
        String note;
        if (c.groupCount() == 0) {
            note = c.studentCount() == 0 ? "No students yet. Share the code to get started."
                    : "No groups yet. " + Ui.plural(c.studentCount(), "student is", "students are") + " waiting.";
        } else {
            int sum = 0;
            for (ProjectGroup g : GroupService.groupsOf(c.id())) {
                sum += GroupService.progress(g.id()).percent();
            }
            note = "Average group progress " + Math.round(sum / (double) c.groupCount()) + "%";
        }
        Button open = Ui.wide(Ui.secondary("Open class"));
        open.setOnAction(e -> KinnectApp.show(classWorkspace(c.id())));
        Label name = Ui.wrap(c.name(), "h3");
        name.setStyle("-fx-font-size: 16px;");
        VBox card = Ui.card(12, name, code, counts, Ui.text(note, "small", "muted"), open);
        code.setMaxWidth(Label.USE_PREF_SIZE);
        VBox.setMargin(open, new Insets(4, 0, 0, 0));
        return card;
    }

    static Parent classWorkspace(long classId) {
        ClassRoom c = ClassService.get(classId);
        List<ProjectGroup> groups = GroupService.groupsOf(classId);
        List<Enrollment> students = ClassService.students(classId);
        long ungrouped = students.stream().filter(s -> s.groupId() == null).count();

        Button myClasses = new Button("My classes");
        myClasses.setOnAction(e -> KinnectApp.show(myClasses()));
        Button back = Ui.secondary("Back to my classes");
        back.setOnAction(e -> KinnectApp.show(myClasses()));
        Button create = Ui.primary("Create group");
        create.setOnAction(e -> Dialogs.createGroup(c, () -> KinnectApp.show(classWorkspace(classId))));
        Button copy = Ui.link("Copy code");
        copy.setOnAction(e -> Dialogs.classCode(c));

        HBox header = Ui.pageHeader(Ui.breadcrumb(myClasses, c.name()), c.name(),
                "Class code " + c.code() + ", " + Ui.plural(c.studentCount(), "student", "students") + ", "
                + Ui.plural(c.groupCount(), "group", "groups")
                + (c.description() == null || c.description().isBlank() ? "" : ", " + c.description()),
                back, create);

        // groups card
        VBox groupRows = new VBox();
        if (groups.isEmpty()) {
            groupRows.getChildren().add(Ui.emptyState("users", "No groups yet",
                    students.isEmpty() ? "Students join with code " + c.code() + ". Then create groups and pick their leaders."
                    : "Create a group and choose its leader. The leader adds the other members."));
        }
        for (ProjectGroup g : groups) {
            int pct = GroupService.progress(g.id()).percent();
            VBox name = new VBox(2, Ui.text(g.name(), "strong"), Ui.text(g.description(), "small", "muted"));
            name.setPrefWidth(220);
            HBox leader = g.leaderName() == null ? Ui.row(0, Ui.text("No leader", "small", "muted"))
                    : Ui.row(8, Ui.avatar(g.leaderName(), 26),
                            new VBox(1, Ui.text("Led by " + Ui.shortName(g.leaderName()), "small", "strong"),
                                    Ui.text(Ui.plural(g.memberCount(), "member", "members"), "small", "muted")));
            leader.setPrefWidth(170);
            VBox prog = new VBox(6, Ui.text(pct + "% complete", "small", "muted"), Ui.track(pct, 5));
            prog.setPrefWidth(150);
            Button manage = Ui.linkMuted("Manage");
            manage.setStyle("-fx-font-size: 13px;");
            manage.setOnAction(e -> Dialogs.manageGroup(g, () -> KinnectApp.show(classWorkspace(classId))));
            Button view = Ui.link("View group");
            view.setOnAction(e -> KinnectApp.show(GroupScreen.open(g.id())));
            HBox row = Ui.row(16, name, Ui.grow(), leader, prog, manage, view);
            row.setPadding(new Insets(16, 0, 16, 0));
            if (!groupRows.getChildren().isEmpty()) {
                row.getStyleClass().add("row-line");
            }
            groupRows.getChildren().add(row);
        }
        VBox groupsCard = Ui.card(0, Ui.cardHeader("Groups", Ui.text(Ui.plural(groups.size(), "group", "groups"),
                "small", "muted")), spacer(), Ui.divider(), groupRows);

        // students card
        VBox studentRows = new VBox();
        int[] shown = {6};
        Button more = Ui.link("Show all " + Ui.plural(students.size(), "student", "students"));
        Runnable fill = () -> {
            studentRows.getChildren().clear();
            List<Enrollment> sorted = students.stream()
                    .sorted((a, b) -> a.groupId() == null && b.groupId() != null ? -1
                    : a.groupId() != null && b.groupId() == null ? 1 : a.studentName().compareTo(b.studentName()))
                    .toList();
            for (int i = 0; i < Math.min(shown[0], sorted.size()); i++) {
                Enrollment s = sorted.get(i);
                HBox r = Ui.row(10, Ui.avatar(s.studentName(), 28), Ui.text(s.studentName()), Ui.grow(),
                        Ui.chip(s.groupName() == null ? "No group" : s.groupName()));
                r.setPadding(new Insets(10, 0, 10, 0));
                r.getStyleClass().add("row-line");
                studentRows.getChildren().add(r);
            }
            more.setVisible(sorted.size() > shown[0]);
            more.setManaged(sorted.size() > shown[0]);
        };
        more.setOnAction(e -> {
            shown[0] = Integer.MAX_VALUE;
            fill.run();
        });
        fill.run();
        Node studentsBody;
        if (students.isEmpty()) {
            studentsBody = Ui.wrap("No students yet. Share the class code " + c.code() + " so they can join.", "muted");
        } else {
            VBox b = new VBox(studentRows, more);
            VBox.setMargin(more, new Insets(12, 0, 0, 0));
            studentsBody = b;
        }
        VBox studentsCard = Ui.card(4, Ui.cardHeader("Students", Ui.text(students.size() + " enrolled", "small", "muted")),
                Ui.text(ungrouped == 0 ? "Every student is in a group." : ungrouped + (ungrouped == 1
                        ? " student isn't" : " students aren't") + " in a group yet.", "small", "muted"),
                spacer(), studentsBody, copy);
        VBox.setMargin(copy, new Insets(8, 0, 0, 0));

        GridPane grid = Ui.columns(20, 64, 36);
        grid.add(groupsCard, 0, 0);
        grid.add(studentsCard, 1, 0);
        groupsCard.setMaxHeight(Double.MAX_VALUE);
        GridPane.setValignment(studentsCard, javafx.geometry.VPos.TOP);
        GridPane.setValignment(groupsCard, javafx.geometry.VPos.TOP);
        VBox page = new VBox(24, header, grid);
        return Ui.topLayout(page);
    }

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        r.setMinHeight(10);
        return r;
    }
}
