package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import com.mycompany.mavenproject2.Models.User;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * The group window: sidebar on the left, the chosen page on the right.
 * Leaders, members, and the professor each get their own set of pages.
 *
 * @author Ariel Soriano
 */
public final class GroupScreen {

    /** Everything a group page needs to know about who is looking at it. */
    static final class Ctx {

        final ProjectGroup group;
        final User user;
        final Member me;
        final boolean professor;
        private final GroupScreen screen;

        Ctx(ProjectGroup group, User user, Member me, boolean professor, GroupScreen screen) {
            this.group = group;
            this.user = user;
            this.me = me;
            this.professor = professor;
            this.screen = screen;
        }

        boolean leader() {
            return me != null && me.isLeader();
        }

        boolean member() {
            return me != null;
        }

        void go(String page) {
            screen.show(page);
        }

        void reload() {
            screen.show(screen.current);
        }

        void onLeave(Runnable r) {
            screen.stopper = r;
        }

        /** Rebuilds the whole group window, sidebar included (after a rename or a leader change). */
        void reopen() {
            KinnectApp.show(GroupScreen.open(group.id(), screen.current));
        }

        /** Shows a different version of the current page (like a filtered list). */
        void replace(Node content) {
            screen.setContent(content);
        }
    }

    private final Ctx ctx;
    private final BorderPane root = new BorderPane();
    private final Map<String, Button> nav = new LinkedHashMap<>();
    private String current = "workspace";
    private Runnable stopper;

    private GroupScreen(long groupId) {
        User u = Session.user();
        ProjectGroup g = GroupService.get(groupId);
        Member me = GroupService.memberOf(groupId, u.id());
        boolean prof = me == null && GroupService.isProfessorOf(groupId, u.id());
        if (g == null || (me == null && !prof)) {
            throw new Models.AppException("You don't have access to this group.");
        }
        ctx = new Ctx(g, u, me, prof, this);
        root.setLeft(sidebar());
        root.getStyleClass().add("page");
        root.getProperties().put("kinnect.onLeave", (Runnable) this::stop);
    }

    static Parent open(long groupId) {
        return open(groupId, "workspace");
    }

    static Parent open(long groupId, String page) {
        GroupScreen s = new GroupScreen(groupId);
        s.show(page);
        return s.root;
    }

    private void stop() {
        if (stopper != null) {
            stopper.run();
            stopper = null;
        }
    }

    void show(String page) {
        stop();
        current = page;
        nav.forEach((k, b) -> {
            b.getStyleClass().remove("active");
            if (k.equals(page)) {
                b.getStyleClass().add("active");
            }
        });
        Node content;
        try {
            content = GroupPages.build(page, ctx);
        } catch (RuntimeException e) {
            content = Ui.card(0, Ui.emptyState("alert", "This page couldn't load", Ui.message(e)));
        }
        setContent(content);
    }

    private void setContent(Node content) {
        VBox holder = new VBox(content);
        holder.setPadding(new Insets(30, 40, 40, 40));
        ScrollPane sp = Ui.scroll(holder);
        root.setCenter(sp);
    }

    private VBox sidebar() {
        ProjectGroup g = ctx.group;
        String role = ctx.professor ? "Professor view" : ctx.leader() ? "Group leader" : "Member";
        VBox groupBox = new VBox(4, Ui.text(g.className(), "small", "muted"), Ui.text(g.name(), "h3"),
                Ui.text(role, "pill", "pill-green"));
        groupBox.getStyleClass().add("group-box");
        VBox.setMargin(groupBox.getChildren().get(2), new Insets(4, 0, 0, 0));
        ((javafx.scene.control.Label) groupBox.getChildren().get(2)).setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        if (ctx.leader()) {
            if (GroupService.nameLocked(g.id())) {
                javafx.scene.control.Label locked = new javafx.scene.control.Label("Name set by your professor",
                        Ui.icon("lock", 12, Theme.MUTED));
                locked.getStyleClass().addAll("small", "muted");
                locked.setGraphicTextGap(5);
                locked.setWrapText(true);
                groupBox.getChildren().add(locked);
                VBox.setMargin(locked, new Insets(4, 0, 0, 0));
            } else {
                Button rename = new Button("Rename group", Ui.icon("edit", 12, Theme.PINE));
                rename.getStyleClass().setAll("button", "link");
                rename.setStyle("-fx-font-size: 12px;");
                rename.setGraphicTextGap(5);
                rename.setOnAction(e -> Dialogs.renameGroup(GroupService.get(g.id()), ctx::reopen));
                groupBox.getChildren().add(rename);
                VBox.setMargin(rename, new Insets(4, 0, 0, 0));
            }
        }

        VBox items = new VBox(4);
        if (ctx.professor) {
            add(items, "workspace", "Workspace", "grid");
            add(items, "tasks", "Tasks", "tasks");
            add(items, "progress", "Progress", "chart");
            add(items, "files", "Files", "folder");
            add(items, "calendar", "Calendar", "calendar");
            add(items, "docs", "Shared documents", "doc");
            add(items, "messages", "Messages", "message");
        } else {
            add(items, "workspace", "Workspace", "grid");
            if (ctx.leader()) {
                add(items, "tasks", "Tasks", "tasks");
            } else {
                add(items, "mytasks", "My tasks", "tasks");
            }
            add(items, "files", "Files", "folder");
            add(items, "calendar", "Calendar", "calendar");
            add(items, "messages", "Messages", "message");
            add(items, "docs", "Shared documents", "doc");
            add(items, "progress", "Progress", "chart");
            if (ctx.leader()) {
                add(items, "budget", "Budget", "wallet");
            }
        }

        Button back = navButton("Back to class", "back");
        back.setOnAction(e -> {
            if (ctx.professor) {
                KinnectApp.show(ProfessorScreens.classWorkspace(g.classId()));
            } else {
                KinnectApp.show(StudentScreens.home());
            }
        });
        Button logout = navButton("Log out", "logout");
        logout.setOnAction(e -> KinnectApp.logOut());
        String userRole = ctx.professor ? "Professor" : ctx.leader() ? "Group leader" : "Member";
        javafx.scene.layout.HBox user = Ui.person(ctx.user.fullName(), userRole, 32);
        user.setPadding(new Insets(6, 10, 6, 10));

        VBox side = new VBox(8, Ui.logo(22), groupBox, items, Ui.grow(), back, Ui.divider(), user, logout);
        VBox.setMargin(groupBox, new Insets(18, 0, 14, 0));
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(250);
        side.setMinWidth(230);
        javafx.scene.layout.HBox logo = (javafx.scene.layout.HBox) side.getChildren().get(0);
        logo.setPadding(new Insets(0, 0, 0, 10));
        return side;
    }

    private void add(VBox items, String key, String label, String icon) {
        Button b = navButton(label, icon);
        b.setOnAction(e -> show(key));
        nav.put(key, b);
        items.getChildren().add(b);
    }

    private static Button navButton(String label, String icon) {
        Button b = new Button(label, Ui.icon(icon, 18, Theme.MUTED));
        b.getStyleClass().setAll("button", "nav-item");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }
}
