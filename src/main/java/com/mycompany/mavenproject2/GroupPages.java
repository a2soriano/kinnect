package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.GroupScreen.Ctx;
import com.mycompany.mavenproject2.Models.BudgetSummary;
import com.mycompany.mavenproject2.Models.CalendarItem;
import com.mycompany.mavenproject2.Models.Contribution;
import com.mycompany.mavenproject2.Models.Expense;
import com.mycompany.mavenproject2.Models.ExpenseStatus;
import com.mycompany.mavenproject2.Models.GroupFile;
import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.Message;
import com.mycompany.mavenproject2.Models.Progress;
import com.mycompany.mavenproject2.Models.SharedDocument;
import com.mycompany.mavenproject2.Models.Task;
import com.mycompany.mavenproject2.Models.TaskStatus;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

/**
 * The pages inside a group: workspace, tasks, files, calendar, messages,
 * shared documents, progress, and the leader-only budget.
 *
 * @author Ariel Soriano
 */
public final class GroupPages {

    private GroupPages() {
    }

    static Node build(String page, Ctx ctx) {
        return switch (page) {
            case "mytasks" -> ctx.leader() ? allTasks(ctx, "all") : myTasks(ctx);
            case "tasks" -> allTasks(ctx, "all");
            case "files" -> files(ctx, null);
            case "calendar" -> calendar(ctx, YearMonth.now(), LocalDate.now());
            case "messages" -> messages(ctx);
            case "docs" -> documents(ctx);
            case "progress" -> progress(ctx);
            case "budget" -> budget(ctx);
            default -> workspace(ctx);
        };
    }

    private static Label eyebrow(Ctx ctx) {
        return Ui.text(ctx.group.name(), "eyebrow");
    }

    private static Node members(Ctx ctx, List<Member> members) {
        return Ui.row(10, Ui.avatarStack(members.stream().map(Member::fullName).toList(), 30),
                Ui.text(Ui.plural(members.size(), "member", "members"), "muted"));
    }

    // ===================== workspace =====================

    static Node workspace(Ctx ctx) {
        List<Member> members = GroupService.members(ctx.group.id());
        List<Task> tasks = TaskService.tasks(ctx.group.id());
        List<Node> actions = new ArrayList<>();
        actions.add(members(ctx, members));
        if (ctx.leader()) {
            Button add = Ui.secondary("Add member");
            add.setOnAction(e -> Dialogs.addMembers(ctx.group, ctx::reload));
            Button create = Ui.primary("Create task");
            create.setOnAction(e -> Dialogs.createTask(ctx.group, ctx::reload));
            actions.add(add);
            actions.add(create);
        } else if (ctx.professor) {
            Button manage = Ui.secondary("Manage group");
            manage.setOnAction(e -> Dialogs.manageGroup(GroupService.get(ctx.group.id()), ctx::reopen));
            actions.add(manage);
        }
        HBox crumb = Ui.breadcrumb(ctx.group.className(), ctx.group.name());
        HBox header = Ui.pageHeader(crumb, ctx.group.title(), null, actions.toArray(Node[]::new));

        GridPane top = Ui.columns(22, 63, 37);
        VBox progress = progressCard(ctx, members, tasks);
        VBox coming = comingUpCard(ctx);
        top.add(progress, 0, 0);
        top.add(coming, 1, 0);
        stretch(progress, coming);

        GridPane bottom = Ui.columns(22, 50, 50);
        VBox chat = chatCard(ctx);
        VBox files = recentFilesCard(ctx);
        bottom.add(chat, 0, 0);
        bottom.add(files, 1, 0);
        stretch(chat, files);
        return new VBox(24, header, top, bottom);
    }

    private static void stretch(Region... cards) {
        for (Region r : cards) {
            r.setMaxHeight(Double.MAX_VALUE);
            GridPane.setValignment(r, VPos.TOP);
        }
    }

    private static FlowPane bigPercent(Progress p) {
        Label pct = Ui.text(p.percent() + "%", "stat");
        pct.setMinWidth(Region.USE_PREF_SIZE);
        Label of = Ui.text(p.completed() + " of " + Ui.plural(p.total(), "task", "tasks") + " completed", "lead");
        of.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane f = new FlowPane(14, 0, pct, of);
        f.setAlignment(Pos.BASELINE_LEFT);
        f.setRowValignment(VPos.BASELINE);
        return f;
    }

    private static VBox progressCard(Ctx ctx, List<Member> members, List<Task> tasks) {
        Progress p = GroupService.progress(ctx.group.id());
        FlowPane people = new FlowPane(26, 12);
        for (Member m : members) {
            long mine = tasks.stream().filter(t -> t.assigneeId() == m.id()).count();
            long done = tasks.stream().filter(t -> t.assigneeId() == m.id() && t.status() == TaskStatus.COMPLETED).count();
            people.getChildren().add(Ui.person(Ui.shortName(m.fullName()), done + " of " + mine + " done", 28));
        }
        VBox card = Ui.card(14,
                Ui.cardHeader("Group progress", Ui.text("Updates whenever a task changes status", "small", "muted")),
                bigPercent(p), Ui.segmentBar(p, 8), Ui.legend(p), Ui.divider(),
                Ui.text("Tasks done by member", "small", "strong"), people);
        if (p.total() == 0) {
            card.getChildren().set(2, Ui.wrap(ctx.leader() ? "No tasks yet. Use Create task to give each member "
                    + "their part." : "No tasks yet. Your group leader will add them soon.", "muted"));
        }
        return card;
    }

    private static VBox dateTile(LocalDate d) {
        VBox tile = new VBox(-2, Ui.text(d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), "small", "muted"),
                Ui.text(String.valueOf(d.getDayOfMonth()), "h3"));
        tile.setAlignment(Pos.CENTER);
        tile.getStyleClass().add("date-tile");
        tile.setMinWidth(44);
        tile.setMaxHeight(Region.USE_PREF_SIZE);
        return tile;
    }

    private static HBox datedRow(CalendarItem item) {
        HBox r = Ui.row(14, dateTile(item.date()),
                new VBox(2, Ui.text(item.title(), "strong"), Ui.text(item.detail(), "small", "muted")));
        return r;
    }

    private static VBox comingUpCard(Ctx ctx) {
        LocalDate today = LocalDate.now();
        List<CalendarItem> items = CollabService.calendarItems(ctx.group.id(), today, today.plusDays(60)).stream()
                .filter(i -> i.isEvent() || !i.detail().startsWith("Completed")).limit(3).toList();
        Button add = Ui.link("Add event");
        add.setOnAction(e -> Dialogs.addEvent(ctx.group, null, ctx::reload));
        VBox card = Ui.card(16, Ui.cardHeader("Coming up", ctx.member() ? add : null));
        if (items.isEmpty()) {
            card.getChildren().add(Ui.wrap("No deadlines or events coming up.", "muted"));
        }
        for (CalendarItem i : items) {
            card.getChildren().add(datedRow(i));
        }
        return card;
    }

    private static Node messageRow(Message m) {
        HBox head = Ui.row(8, Ui.text(Ui.shortName(m.senderName()), "strong"), Ui.text(Ui.time(m.sentAt()), "small", "muted"));
        Label body = Ui.wrap(m.content(), "body");
        VBox v = new VBox(3, head, body);
        HBox.setHgrow(v, Priority.ALWAYS);
        HBox row = new HBox(12, Ui.avatar(m.senderName(), 30), v);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /** Message box with a send button. Calls after() once the message is saved. */
    private static VBox composer(Ctx ctx, Runnable after) {
        TextField input = new TextField();
        input.setPromptText("Message " + ctx.group.name());
        input.getStyleClass().add("chat-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        Button send = new Button();
        send.setGraphic(Ui.icon("send", 18, Theme.ON_PINE));
        send.getStyleClass().addAll("btn-primary", "btn-icon");
        Label error = Ui.text("", "error-text");
        error.setManaged(false);
        error.setVisible(false);
        Runnable go = () -> {
            try {
                CollabService.sendMessage(ctx.group.id(), ctx.user.id(), input.getText());
                input.clear();
                error.setVisible(false);
                error.setManaged(false);
                after.run();
            } catch (RuntimeException ex) {
                error.setText("Message not sent. " + Ui.message(ex));
                error.setVisible(true);
                error.setManaged(true);
            }
        };
        send.setOnAction(e -> go.run());
        input.setOnAction(e -> go.run());
        return new VBox(6, Ui.row(10, input, send), error);
    }

    private static VBox chatCard(Ctx ctx) {
        List<Message> all = CollabService.messages(ctx.group.id());
        Button open = Ui.link("Open messages");
        open.setOnAction(e -> ctx.go("messages"));
        VBox list = new VBox(16);
        all.stream().skip(Math.max(0, all.size() - 2)).forEach(m -> list.getChildren().add(messageRow(m)));
        if (all.isEmpty()) {
            list.getChildren().add(Ui.wrap("No messages yet. Say hi to your group.", "muted"));
        }
        VBox.setVgrow(list, Priority.ALWAYS);
        return Ui.card(16, Ui.cardHeader("Group chat", open), list, composer(ctx, ctx::reload));
    }

    private static Node fileTile(String type) {
        StackPane s = new StackPane(Ui.text(type.toUpperCase(), "small"));
        s.getStyleClass().add("file-tile");
        if (type.length() > 3) {
            s.setMinWidth(44);
        }
        return s;
    }

    private static VBox recentFilesCard(Ctx ctx) {
        List<GroupFile> files = CollabService.files(ctx.group.id());
        Button upload = Ui.link("Upload file");
        upload.setOnAction(e -> chooseAndUpload(ctx));
        VBox card = Ui.card(16, Ui.cardHeader("Recent files", ctx.member() ? upload : null));
        for (GroupFile f : files.stream().limit(3).toList()) {
            Button name = Ui.link(f.fileName());
            name.setStyle("-fx-text-fill: " + Theme.INK + "; -fx-font-size: 14px;");
            name.setOnAction(e -> openFile(f));
            card.getChildren().add(Ui.row(12, fileTile(f.fileType()), new VBox(2, name,
                    Ui.text(Ui.shortName(f.uploaderName()) + ", " + Ui.size(f.fileSize()) + ", " + Ui.day(f.uploadedAt()),
                            "small", "muted"))));
        }
        if (files.isEmpty()) {
            card.getChildren().add(Ui.wrap("No files yet." + (ctx.member() ? " Upload the first one." : ""), "muted"));
        }
        return card;
    }

    // ===================== tasks =====================

    static Node myTasks(Ctx ctx) {
        List<Task> mine = TaskService.tasksOfMember(ctx.me.id());
        VBox list = new VBox();
        for (Task t : mine) {
            String by = t.creatorId() == ctx.me.id() ? "you" : Ui.shortName(t.creatorName());
            String end = by.endsWith(".") ? "" : ".";
            Label due = Ui.text("Due " + Ui.day(t.deadline()) + ". Created by " + by + end, "small", "muted");
            if (TaskService.isOverdue(t)) {
                due.setText("Overdue since " + Ui.day(t.deadline()) + ". Created by " + by + end);
                due.getStyleClass().setAll("label", "error-text");
            }
            VBox text = new VBox(3, Ui.text(t.title(), "strong"), due);
            if (t.description() != null && !t.description().isBlank()) {
                text.getChildren().add(Ui.wrap(t.description(), "small", "muted"));
            }
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = Ui.row(16, text, statusPicker(ctx, t));
            row.setPadding(new Insets(14, 0, 14, 0));
            row.getStyleClass().add("row-line");
            list.getChildren().add(row);
        }
        VBox assigned = Ui.card(6, Ui.cardHeader("Assigned to you", Ui.text(Ui.plural(mine.size(), "task", "tasks"),
                "small", "muted")), list);
        if (mine.isEmpty()) {
            assigned.getChildren().set(1, Ui.emptyState("tasks", "Nothing assigned to you yet",
                    "When your group leader gives you a task, it shows up here with its deadline."));
        }
        Progress p = GroupService.progress(ctx.group.id());
        VBox summary = Ui.card(14, Ui.text("Group progress", "h2"), bigPercent(p), Ui.segmentBar(p, 8), Ui.legend(p),
                Ui.wrap("Progress updates on its own whenever anyone changes a task status.", "small", "muted"));
        GridPane grid = Ui.columns(22, 63, 37);
        grid.add(assigned, 0, 0);
        grid.add(summary, 1, 0);
        GridPane.setValignment(assigned, VPos.TOP);
        GridPane.setValignment(summary, VPos.TOP);
        return new VBox(24, Ui.pageHeader(eyebrow(ctx), "My tasks",
                "Tasks assigned to you. Change the status when you start or finish one."), grid);
    }

    /** The status button with a menu of Pending, Ongoing, and Completed. */
    private static Button statusPicker(Ctx ctx, Task t) {
        Button b = new Button();
        b.setGraphic(Ui.row(8, Ui.statusPill(t.status()), Ui.icon("down", 16, Theme.MUTED)));
        b.getStyleClass().setAll("button", "btn-secondary");
        b.setStyle("-fx-padding: 5 8 5 6;");
        ContextMenu menu = new ContextMenu();
        for (TaskStatus s : TaskStatus.values()) {
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox item = Ui.row(8, Ui.statusPill(s), gap);
            item.setPrefWidth(150);
            if (s == t.status()) {
                item.getChildren().add(Ui.icon("check", 16, Theme.PINE));
            }
            CustomMenuItem mi = new CustomMenuItem(item);
            mi.setOnAction(e -> {
                if (s == t.status()) {
                    return;
                }
                try {
                    TaskService.updateStatus(t.id(), ctx.user.id(), s);
                    ctx.reload();
                    KinnectApp.toast("Status saved. Group progress is now "
                            + GroupService.progress(ctx.group.id()).percent() + "%.");
                } catch (RuntimeException ex) {
                    KinnectApp.toast(Ui.message(ex), true);
                }
            });
            menu.getItems().add(mi);
        }
        b.setOnAction(e -> menu.show(b, javafx.geometry.Side.BOTTOM, -60, 6));
        return b;
    }

    /**
     * Every task in the group. The professor sees it read only. The leader sees it with
     * Create task, and can change the status of the tasks assigned to them.
     * Filter: "all", "mine", or a status name.
     */
    static Node allTasks(Ctx ctx, String filter) {
        List<Task> tasks = TaskService.tasks(ctx.group.id());
        List<Task> shown = tasks.stream().filter(t -> switch (filter) {
            case "all" -> true;
            case "mine" -> ctx.me != null && t.assigneeId() == ctx.me.id();
            default -> t.status().name().equals(filter);
        }).toList();
        HBox tabs = Ui.row(22);
        List<String[]> options = new ArrayList<>();
        options.add(new String[]{"all", "All"});
        if (ctx.member()) {
            options.add(new String[]{"mine", "Assigned to me"});
        }
        options.add(new String[]{"PENDING", "Pending"});
        options.add(new String[]{"ONGOING", "Ongoing"});
        options.add(new String[]{"COMPLETED", "Completed"});
        for (String[] o : options) {
            Button tab = new Button(o[1]);
            tab.getStyleClass().setAll("button", "tab-btn");
            if (o[0].equals(filter)) {
                tab.getStyleClass().add("active");
            }
            tab.setOnAction(e -> ctx.replace(allTasks(ctx, o[0])));
            tabs.getChildren().add(tab);
        }
        VBox list = new VBox();
        for (Task t : shown) {
            boolean mine = ctx.me != null && t.assigneeId() == ctx.me.id();
            String who = mine ? "You" : Ui.shortName(t.assigneeName());
            Node status = mine ? statusPicker(ctx, t) : Ui.statusPill(t.status());
            HBox row = Ui.row(12, Ui.avatar(t.assigneeName(), 30),
                    new VBox(2, Ui.text(t.title(), "strong"),
                            Ui.text(who + ", due " + Ui.day(t.deadline())
                                    + (TaskService.isOverdue(t) ? ", overdue" : ""), "small", "muted")),
                    Ui.grow(), status);
            row.setPadding(new Insets(12, 0, 12, 0));
            row.getStyleClass().add("row-line");
            list.getChildren().add(row);
        }
        String foot = switch (filter) {
            case "all" -> "Showing all " + Ui.plural(tasks.size(), "task", "tasks");
            case "mine" -> "Showing " + shown.size() + " assigned to you, out of " + tasks.size();
            default -> "Showing " + shown.size() + " " + filter.toLowerCase() + " of " + tasks.size();
        };
        VBox card = Ui.card(10, Ui.cardHeader("All tasks", Ui.text(Ui.plural(tasks.size(), "task", "tasks"), "small",
                "muted")), tabs, Ui.divider(), list, Ui.divider(), Ui.text(foot, "small", "muted"));
        if (shown.isEmpty()) {
            String text;
            if (!tasks.isEmpty()) {
                text = "mine".equals(filter) ? "None of the tasks are assigned to you."
                        : "No tasks have this status.";
            } else if (ctx.leader()) {
                text = "Use Create task to give each member their part. Tasks you assign to yourself "
                        + "show up here too.";
            } else {
                text = "The group leader hasn't created any tasks yet.";
            }
            card.getChildren().set(3, Ui.emptyState("tasks", tasks.isEmpty() ? "No tasks yet" : "No tasks here", text));
        }
        List<Node> actions = new ArrayList<>();
        actions.add(members(ctx, GroupService.members(ctx.group.id())));
        if (ctx.leader()) {
            Button create = Ui.primary("Create task");
            create.setOnAction(e -> Dialogs.createTask(ctx.group, ctx::reload));
            actions.add(create);
        }
        String subtitle = ctx.leader()
                ? "Every task in your group. Change the status of the ones assigned to you."
                : ctx.professor ? "Every task in this group and who it belongs to." : null;
        return new VBox(24, Ui.pageHeader(Ui.breadcrumb(ctx.group.className(), ctx.group.name()), "Tasks", subtitle,
                actions.toArray(Node[]::new)), card);
    }

    // ===================== files =====================

    private static final Map<String, String> TYPE_NAMES = Map.of("pdf", "PDF document", "docx", "Word document",
            "doc", "Word document", "pptx", "PowerPoint deck", "xlsx", "Excel sheet", "png", "PNG image",
            "jpg", "JPG image", "jpeg", "JPG image", "txt", "Text file", "zip", "ZIP archive");

    static Node files(Ctx ctx, String error) {
        List<GroupFile> files = CollabService.files(ctx.group.id());
        VBox page = new VBox(22);
        Button upload = Ui.primary("Upload file");
        upload.setOnAction(e -> chooseAndUpload(ctx));
        page.getChildren().add(Ui.pageHeader(eyebrow(ctx), "Files", ctx.professor
                ? "Project files shared by the group. You can open and download them."
                : "Project files shared by everyone in the group.", ctx.member() ? upload : new Region()));

        if (ctx.member()) {
            HBox banner = Ui.errorBanner();
            if (error != null) {
                Ui.showError(banner, error);
            }
            HBox drop = Ui.row(16, Ui.iconCircle("upload", 38),
                    new VBox(3, Ui.text("Drag a file here, or choose one to upload", "strong"),
                            Ui.text("PDF, DOCX, PPTX, XLSX, PNG, JPG, TXT, or ZIP, up to 25 MB", "small", "muted")));
            drop.getStyleClass().add("drop-zone");
            drop.setOnMouseClicked(e -> chooseAndUpload(ctx));
            drop.setOnDragOver(e -> {
                if (e.getDragboard().hasFiles()) {
                    e.acceptTransferModes(TransferMode.COPY);
                    if (!drop.getStyleClass().contains("drag-over")) {
                        drop.getStyleClass().add("drag-over");
                    }
                }
                e.consume();
            });
            drop.setOnDragExited(e -> drop.getStyleClass().remove("drag-over"));
            drop.setOnDragDropped(e -> {
                boolean ok = e.getDragboard().hasFiles();
                if (ok) {
                    File f = e.getDragboard().getFiles().get(0);
                    Platform.runLater(() -> upload(ctx, f.toPath()));
                }
                e.setDropCompleted(ok);
                e.consume();
            });
            page.getChildren().addAll(banner, drop);
        }

        double[] cols = {44, 16, 10, 10, 20};
        VBox table = new VBox();
        table.getChildren().add(tableRow(cols, 0, Ui.text("Name", "small", "muted"), Ui.text("Uploaded by", "small", "muted"),
                Ui.text("Size", "small", "muted"), Ui.text("Date", "small", "muted"), Ui.text("Actions", "small", "muted")));
        for (GroupFile f : files) {
            Button open = Ui.link("Open");
            open.setOnAction(e -> openFile(f));
            Button download = new Button("Download", Ui.icon("download", 15, Theme.PINE));
            download.getStyleClass().setAll("button", "link");
            download.setGraphicTextGap(6);
            download.setOnAction(e -> saveFile(f));
            Node name = Ui.row(12, fileTile(f.fileType()), new VBox(2, Ui.text(f.fileName(), "strong"),
                    Ui.text(TYPE_NAMES.getOrDefault(f.fileType(), f.fileType().toUpperCase() + " file"), "small", "muted")));
            GridPane r = tableRow(cols, 12, name, Ui.text(Ui.shortName(f.uploaderName()), "muted"),
                    Ui.text(Ui.size(f.fileSize()), "muted"), Ui.text(Ui.day(f.uploadedAt()), "muted"),
                    Ui.row(18, open, download));
            r.getStyleClass().add("row-line");
            table.getChildren().add(r);
        }
        VBox card = Ui.card(10, Ui.cardHeader("All files", Ui.text(Ui.plural(files.size(), "file", "files"), "small",
                "muted")), table);
        if (files.isEmpty()) {
            card.getChildren().set(1, Ui.emptyState("folder", "No files yet", ctx.member()
                    ? "Upload the proposal, diagrams, or anything the group needs. Everyone can open them here."
                    : "Files the group uploads will appear here."));
        }
        page.getChildren().add(card);
        return page;
    }

    private static GridPane tableRow(double[] percents, double vpad, Node... cells) {
        GridPane g = new GridPane();
        g.setHgap(12);
        for (double p : percents) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            g.getColumnConstraints().add(c);
        }
        for (int i = 0; i < cells.length; i++) {
            g.add(cells[i], i, 0);
            GridPane.setValignment(cells[i], VPos.CENTER);
        }
        g.setPadding(new Insets(vpad, 0, vpad, 0));
        return g;
    }

    private static void chooseAndUpload(Ctx ctx) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Upload a file to " + ctx.group.name());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Project files",
                CollabService.FILE_TYPES.stream().map(t -> "*." + t).toList()));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = fc.showOpenDialog(KinnectApp.stage());
        if (f != null) {
            upload(ctx, f.toPath());
        }
    }

    private static void upload(Ctx ctx, Path path) {
        try {
            GroupFile f = CollabService.uploadFile(ctx.group.id(), ctx.user.id(), path);
            ctx.go("files");
            KinnectApp.toast(f.fileName() + " uploaded.");
        } catch (RuntimeException ex) {
            String msg = Ui.message(ex);
            ctx.go("files");
            ctx.replace(files(ctx, path.getFileName() + " wasn't uploaded. " + msg));
            KinnectApp.toast("Upload failed. " + msg, true);
        }
    }

    private static void openFile(GroupFile f) {
        try {
            KinnectApp.openExternal(CollabService.openableCopy(f).toUri().toString());
        } catch (RuntimeException ex) {
            KinnectApp.toast(Ui.message(ex), true);
        }
    }

    private static void saveFile(GroupFile f) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save " + f.fileName());
        fc.setInitialFileName(f.fileName());
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) {
            fc.setInitialDirectory(downloads);
        }
        File target = fc.showSaveDialog(KinnectApp.stage());
        if (target != null) {
            try {
                CollabService.copyOut(f, target.toPath());
                KinnectApp.toast("Saved " + target.getName() + ".");
            } catch (RuntimeException ex) {
                KinnectApp.toast(Ui.message(ex), true);
            }
        }
    }

    // ===================== calendar =====================

    static Node calendar(Ctx ctx, YearMonth month, LocalDate selected) {
        VBox page = new VBox(22);
        Runnable[] redraw = new Runnable[1];
        YearMonth[] shownMonth = {month};
        LocalDate[] picked = {selected};
        redraw[0] = () -> page.getChildren().setAll(calendarContent(ctx, shownMonth, picked, redraw[0]));
        redraw[0].run();
        return page;
    }

    private static List<Node> calendarContent(Ctx ctx, YearMonth[] month, LocalDate[] picked, Runnable redraw) {
        Button add = Ui.primary("Add event");
        add.setOnAction(e -> Dialogs.addEvent(ctx.group, picked[0], redraw));
        HBox header = Ui.pageHeader(eyebrow(ctx), "Calendar and timeline",
                "Task deadlines appear on their own. Add meetings and other events for the group.",
                ctx.member() ? add : new Region());

        List<CalendarItem> items = CollabService.calendarMonth(ctx.group.id(), month[0]);
        LocalDate first = month[0].atDay(1);
        LocalDate start = first.minusDays(first.getDayOfWeek().getValue() - 1L);
        LocalDate today = LocalDate.now();

        Button prev = new Button(null, Ui.icon("back", 16, Theme.INK));
        Button next = new Button(null, Ui.icon("next", 16, Theme.INK));
        for (Button b : new Button[]{prev, next}) {
            b.getStyleClass().setAll("button", "btn-secondary");
            b.setStyle("-fx-padding: 5 7;");
        }
        prev.setOnAction(e -> {
            month[0] = month[0].minusMonths(1);
            redraw.run();
        });
        next.setOnAction(e -> {
            month[0] = month[0].plusMonths(1);
            redraw.run();
        });
        Button todayBtn = Ui.link("Today");
        todayBtn.setOnAction(e -> {
            month[0] = YearMonth.now();
            picked[0] = today;
            redraw.run();
        });
        HBox monthHead = Ui.row(10, Ui.text(Ui.MONTH.format(first), "h2"), prev, next, todayBtn, Ui.grow(),
                Ui.legendItem("Task deadline", Theme.PINE), Ui.legendItem("Event", Theme.AMBER));

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100 / 7.0);
            grid.getColumnConstraints().add(cc);
            grid.add(Ui.text(DayOfWeek.of(c + 1).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), "small", "strong"), c, 0);
        }
        int weeks = (int) Math.ceil((first.getDayOfWeek().getValue() - 1 + month[0].lengthOfMonth()) / 7.0);
        for (int i = 0; i < weeks * 7; i++) {
            LocalDate d = start.plusDays(i);
            boolean inMonth = YearMonth.from(d).equals(month[0]);
            VBox cell = new VBox(4);
            cell.getStyleClass().add("day-cell");
            cell.setMinHeight(66);
            cell.setMaxWidth(Double.MAX_VALUE);
            Label num = new Label(String.valueOf(d.getDayOfMonth()));
            if (d.equals(today)) {
                num.getStyleClass().add("today-dot");
            } else {
                num.getStyleClass().addAll(inMonth ? "strong" : "muted", "small");
            }
            cell.getChildren().add(num);
            if (!inMonth) {
                cell.getStyleClass().add("other");
            }
            if (d.equals(picked[0])) {
                cell.getStyleClass().add("selected");
            }
            List<CalendarItem> dayItems = items.stream().filter(it -> it.date().equals(d)).toList();
            for (int k = 0; k < Math.min(2, dayItems.size()); k++) {
                CalendarItem it = dayItems.get(k);
                Label chip = Ui.text(it.title(), "day-chip", it.isEvent() ? "event" : "task");
                chip.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(chip);
            }
            if (dayItems.size() > 2) {
                cell.getChildren().add(Ui.text("+" + (dayItems.size() - 2) + " more", "small", "muted"));
            }
            cell.setOnMouseClicked(e -> {
                picked[0] = d;
                if (!inMonth) {
                    month[0] = YearMonth.from(d);
                }
                redraw.run();
                if (e.getClickCount() == 2 && ctx.member()) {
                    Dialogs.addEvent(ctx.group, d, redraw);
                }
            });
            grid.add(cell, i % 7, 1 + i / 7);
        }
        VBox monthCard = Ui.card(14, monthHead, grid);

        // day panel
        LocalDate day = picked[0];
        VBox panel = Ui.card(14, Ui.text(Ui.LONG_DAY.format(day), "h2"));
        List<CalendarItem> around = CollabService.calendarItems(ctx.group.id(),
                day.minusDays(day.getDayOfWeek().getValue() - 1L), day.plusDays(7 - day.getDayOfWeek().getValue()));
        List<CalendarItem> onDay = around.stream().filter(i -> i.date().equals(day)).toList();
        for (CalendarItem it : onDay) {
            if (it.isEvent()) {
                var ev = it.event();
                VBox card = new VBox(6, Ui.text("Event", "small", "amber-text"), Ui.text(ev.title(), "h2"),
                        Ui.text(Ui.timeRange(ev.start(), ev.end()), "strong"));
                if (ev.description() != null && !ev.description().isBlank()) {
                    card.getChildren().add(Ui.wrap(ev.description(), "body"));
                }
                card.getChildren().add(Ui.text("Added by " + Ui.shortName(ev.creatorName()), "small", "amber-text"));
                card.getStyleClass().add("event-card");
                panel.getChildren().add(card);
            } else {
                panel.getChildren().add(datedRow(it));
            }
        }
        if (onDay.isEmpty()) {
            panel.getChildren().add(Ui.wrap("Nothing scheduled on this day." + (ctx.member()
                    ? " Double-click a day to add an event." : ""), "muted"));
        }
        List<CalendarItem> week = around.stream().filter(i -> !i.date().equals(day)).toList();
        if (!week.isEmpty()) {
            panel.getChildren().add(Ui.text("Also this week", "small", "strong"));
            week.forEach(i -> panel.getChildren().add(datedRow(i)));
        }

        GridPane layout = Ui.columns(22, 68, 32);
        layout.add(monthCard, 0, 0);
        layout.add(panel, 1, 0);
        GridPane.setValignment(monthCard, VPos.TOP);
        GridPane.setValignment(panel, VPos.TOP);
        return List.of(header, layout);
    }

    // ===================== messages =====================

    static Node messages(Ctx ctx) {
        VBox list = new VBox(18);
        list.setPadding(new Insets(4, 6, 4, 0));
        ScrollPane sp = Ui.scroll(list);
        sp.setPrefViewportHeight(440);
        sp.setMinHeight(300);
        long[] lastId = {0};
        LocalDate[] lastDay = {null};
        Runnable load = () -> {
            List<Message> fresh = CollabService.messagesAfter(ctx.group.id(), lastId[0]);
            for (Message m : fresh) {
                LocalDate d = m.sentAt().toLocalDate();
                if (!d.equals(lastDay[0])) {
                    lastDay[0] = d;
                    list.getChildren().add(daySeparator(d));
                }
                list.getChildren().add(messageRow(m));
                lastId[0] = m.id();
            }
            if (!fresh.isEmpty()) {
                Platform.runLater(() -> {
                    sp.layout();
                    sp.setVvalue(1.0);
                });
            }
        };
        load.run();
        if (lastId[0] == 0) {
            Label empty = Ui.text("No messages yet. Say hi to your group.", "muted");
            empty.setId("empty-chat");
            list.getChildren().add(empty);
        }
        Runnable refresh = () -> {
            list.getChildren().removeIf(n -> "empty-chat".equals(n.getId()));
            load.run();
        };
        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            try {
                refresh.run();
            } catch (RuntimeException ignored) {
                // try again on the next tick
            }
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
        ctx.onLeave(poll::stop);

        VBox chat = Ui.card(14, sp, composer(ctx, refresh));
        VBox.setVgrow(sp, Priority.ALWAYS);

        List<Member> members = GroupService.members(ctx.group.id());
        String prof = com.mycompany.mavenproject2.ClassService.get(ctx.group.classId()).professorName();
        VBox people = new VBox();
        for (Member m : members) {
            HBox r = Ui.person(m.fullName(), m.isLeader() ? "Group leader" : "Member", 28);
            r.setPadding(new Insets(10, 0, 10, 0));
            r.getStyleClass().add("row-line");
            people.getChildren().add(r);
        }
        HBox pr = Ui.person(prof, "Professor", 28);
        pr.setPadding(new Insets(10, 0, 10, 0));
        pr.getStyleClass().add("row-line");
        people.getChildren().add(pr);
        VBox side = Ui.card(6, Ui.cardHeader("In this chat", Ui.text(Ui.plural(members.size() + 1, "person", "people"),
                "small", "muted")), people);

        Button refreshBtn = Ui.secondary("Refresh");
        refreshBtn.setOnAction(e -> refresh.run());
        GridPane grid = Ui.columns(22, 68, 32);
        grid.add(chat, 0, 0);
        grid.add(side, 1, 0);
        GridPane.setValignment(chat, VPos.TOP);
        GridPane.setValignment(side, VPos.TOP);
        return new VBox(22, Ui.pageHeader(eyebrow(ctx), "Messages",
                "One conversation for the whole group. Your professor can read and reply.", refreshBtn), grid);
    }

    private static Node daySeparator(LocalDate d) {
        LocalDate today = LocalDate.now();
        String label = d.equals(today) ? "Today, " + Ui.day(d) : d.equals(today.minusDays(1)) ? "Yesterday, " + Ui.day(d)
                : d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ", " + Ui.day(d);
        Region a = Ui.divider();
        Region b = Ui.divider();
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        return Ui.row(12, a, Ui.text(label, "small", "muted"), b);
    }

    // ===================== shared documents =====================

    static Node documents(Ctx ctx) {
        List<SharedDocument> docs = CollabService.documents(ctx.group.id());
        Button add = Ui.primary("Add document link");
        add.setOnAction(e -> Dialogs.addLink(ctx.group, ctx::reload));
        double[] cols = {64, 22, 14};
        VBox table = new VBox(tableRow(cols, 0, Ui.text("Document", "small", "muted"),
                Ui.text("Added by", "small", "muted"), new Region()));
        for (SharedDocument d : docs) {
            StackPane tile = new StackPane(Ui.icon("link", 16, Theme.PINE));
            tile.getStyleClass().add("file-tile");
            Button open = Ui.link("Open link");
            open.setOnAction(e -> KinnectApp.openExternal(d.url()));
            GridPane r = tableRow(cols, 12, Ui.row(12, tile, new VBox(2, Ui.text(d.title(), "strong"),
                    Ui.text(where(d.url()), "small", "muted"))),
                    Ui.text(Ui.shortName(d.contributorName()) + ", " + Ui.day(d.addedAt()), "muted"), open);
            r.getStyleClass().add("row-line");
            table.getChildren().add(r);
        }
        VBox card = Ui.card(10, Ui.cardHeader("All links", Ui.text(Ui.plural(docs.size(), "link", "links"), "small",
                "muted")), table);
        if (docs.isEmpty()) {
            card.getChildren().set(1, Ui.emptyState("link", "No shared documents yet", ctx.member()
                    ? "Add links to Google Docs, Slides, Sheets, or any work the group keeps outside Kinnect."
                    : "Links the group adds will appear here."));
        }
        return new VBox(22, Ui.pageHeader(eyebrow(ctx), "Shared documents",
                "Links to work your group keeps outside Kinnect, like Google Docs and Slides.",
                ctx.member() ? add : new Region()), card);
    }

    private static String where(String url) {
        try {
            URI u = new URI(url);
            String host = u.getHost().replaceFirst("^www\\.", "");
            String[] parts = u.getPath() == null ? new String[0] : u.getPath().split("/");
            String first = parts.length > 1 ? parts[1] : "";
            return List.of("presentation", "spreadsheets", "forms").contains(first) ? host + "/" + first : host;
        } catch (Exception e) {
            return url;
        }
    }

    // ===================== progress =====================

    static Node progress(Ctx ctx) {
        Progress p = GroupService.progress(ctx.group.id());
        List<Task> tasks = TaskService.tasks(ctx.group.id());
        List<Member> members = GroupService.members(ctx.group.id());
        Button refresh = Ui.secondary("Refresh");
        refresh.setOnAction(e -> ctx.reload());
        HBox header = Ui.pageHeader(eyebrow(ctx), "Group progress", "Updated whenever a task changes status.", refresh);
        if (p.total() == 0) {
            return new VBox(22, header, Ui.card(0, Ui.emptyState("chart", "No tasks yet",
                    "Progress shows up once the group leader creates tasks and members start updating them.")));
        }
        VBox overall = Ui.card(14, bigPercent(p), Ui.segmentBar(p, 10), Ui.legend(p));

        GridPane cols = Ui.columns(18, 33.33, 33.33, 33.34);
        TaskStatus[] order = {TaskStatus.COMPLETED, TaskStatus.ONGOING, TaskStatus.PENDING};
        String[] colors = {Theme.GREEN, Theme.AMBER, Theme.GREY};
        for (int i = 0; i < 3; i++) {
            TaskStatus s = order[i];
            List<Task> in = tasks.stream().filter(t -> t.status() == s).toList();
            Label title = new Label(s.label(), new javafx.scene.shape.Circle(4, javafx.scene.paint.Color.web(colors[i])));
            title.getStyleClass().add("h2");
            title.setGraphicTextGap(8);
            VBox card = Ui.card(0, Ui.row(8, title, Ui.text(String.valueOf(in.size()), "muted")));
            VBox.setMargin(card.getChildren().get(0), new Insets(0, 0, 10, 0));
            for (Task t : in) {
                VBox r = new VBox(2, Ui.text(t.title(), "strong"), Ui.text(Ui.shortName(t.assigneeName()), "small", "muted"));
                r.setPadding(new Insets(10, 0, 10, 0));
                r.getStyleClass().add("row-line");
                card.getChildren().add(r);
            }
            if (in.isEmpty()) {
                card.getChildren().add(Ui.text("None", "muted"));
            }
            cols.add(card, i, 0);
            GridPane.setValignment(card, VPos.TOP);
        }

        VBox byMember = Ui.card(0, Ui.cardHeader("By member", Ui.text(Ui.plural(members.size(), "member", "members"),
                "small", "muted")));
        VBox.setMargin(byMember.getChildren().get(0), new Insets(0, 0, 10, 0));
        for (Member m : members) {
            long mine = tasks.stream().filter(t -> t.assigneeId() == m.id()).count();
            long done = tasks.stream().filter(t -> t.assigneeId() == m.id() && t.status() == TaskStatus.COMPLETED).count();
            StackPane track = Ui.track(mine == 0 ? 0 : done * 100.0 / mine, 5);
            track.setPrefWidth(180);
            track.setMinWidth(180);
            HBox r = Ui.row(12, Ui.avatar(m.fullName(), 28), Ui.text(m.fullName() + (m.isLeader() ? ", leader" : ""),
                    "strong"), Ui.grow(), Ui.text(done + " of " + mine + " done", "small", "muted"), track);
            r.setPadding(new Insets(11, 0, 11, 0));
            r.getStyleClass().add("row-line");
            byMember.getChildren().add(r);
        }
        return new VBox(22, header, overall, cols, byMember);
    }

    // ===================== budget (leader only) =====================

    static Node budget(Ctx ctx) {
        if (!ctx.leader()) {
            return Ui.card(0, Ui.emptyState("lock", "Only the group leader can see the budget",
                    "Members never see the budget or who has paid."));
        }
        long me = ctx.user.id();
        BudgetSummary s = BudgetService.summary(ctx.group.id(), me);
        List<Contribution> people = BudgetService.contributions(ctx.group.id(), me);
        List<Expense> expenses = BudgetService.expenses(ctx.group.id(), me);

        Button addExpense = Ui.secondary("Add expense");
        addExpense.setOnAction(e -> Dialogs.addExpense(ctx.group, ctx::reload));
        Button pay = Ui.primary("Record payment");
        pay.setOnAction(e -> Dialogs.recordPayment(ctx.group, ctx::reload));
        HBox header = Ui.pageHeader(eyebrow(ctx), "Budget and ambagan",
                "Track what the project costs and who has paid their share.", addExpense, pay);

        HBox notice = Ui.row(10, Ui.icon("lock", 16, Theme.PINE),
                Ui.text("Only you, the group leader, can see this page. Members never see the budget or who has paid."));
        notice.getStyleClass().add("banner-info");
        notice.setMaxWidth(Region.USE_PREF_SIZE);
        VBox top = new VBox(10, notice);
        if (s.overAssigned().signum() > 0) {
            HBox warn = Ui.errorBanner();
            Ui.showError(warn, "Custom shares add up to " + Ui.peso(s.overAssigned()) + " more than the budget. "
                    + "Lower a custom share or add the missing expense.");
            top.getChildren().add(warn);
        } else if (s.uncovered().signum() > 0) {
            HBox warn = Ui.errorBanner();
            Ui.showError(warn, "Everyone has a custom share, and " + Ui.peso(s.uncovered()) + " of the budget isn't "
                    + "covered by anyone. Raise a share or put someone back on the even split.");
            top.getChildren().add(warn);
        }

        String due = s.dueDate() == null ? "No due date" : "Due " + Ui.day(s.dueDate());
        int evenCount = s.memberCount() - s.customCount();
        String evenNote = s.customCount() == 0 ? Ui.plural(s.memberCount(), "member", "members")
                : evenCount + " split evenly, " + s.customCount() + " custom";
        GridPane stats = Ui.columns(0, 25, 25, 25, 25);
        stats.add(stat("Total budget", Ui.peso(s.total()), Ui.plural(s.expenseCount(), "expense", "expenses"), false), 0, 0);
        stats.add(stat(s.customCount() == 0 ? "Each member pays" : "Even share", Ui.peso(s.evenShare()), evenNote, true), 1, 0);
        stats.add(stat("Collected", Ui.peso(s.collected()), s.paidCount() + " of " + s.memberCount() + " fully paid", true), 2, 0);
        stats.add(stat("Still to collect", Ui.peso(s.toCollect()), due, true), 3, 0);
        HBox progHead = Ui.row(8, Ui.text("Collection progress", "small", "strong"), Ui.grow(),
                Ui.text(s.percentCollected() + "%", "small", "green-text", "strong"));
        VBox summary = Ui.card(14, stats, progHead, Ui.track(s.percentCollected(), 7));
        VBox.setMargin(progHead, new Insets(8, 0, 0, 0));

        // ambagan table
        double[] cols = {31, 15, 18, 14, 22};
        VBox table = new VBox(tableRow(cols, 0, Ui.text("Member", "small", "muted"), Ui.text("Share", "small", "muted"),
                Ui.text("Status", "small", "muted"), Ui.text("Paid", "small", "muted"), new Region()));
        List<Contribution> sorted = people.stream().sorted((a, b) -> Boolean.compare(a.isPaid(), b.isPaid())).toList();
        for (Contribution c : sorted) {
            String first = c.member().fullName().split(" ")[0];
            VBox share = new VBox(2, Ui.text(Ui.peso(c.share()), "body"));
            if (c.custom()) {
                Label chip = Ui.chip("Custom");
                chip.setMaxWidth(Region.USE_PREF_SIZE);
                share.getChildren().add(chip);
            }
            Label status;
            if (c.isPaid()) {
                status = Ui.pill("Paid", "green");
            } else if (c.share().signum() == 0) {
                status = Ui.pill("Nothing due", "grey");
            } else if (c.paid().signum() > 0) {
                status = Ui.pill("Owes " + Ui.peso(c.balance()), "amber");
            } else {
                status = Ui.pill("Not paid", "amber");
            }
            Node paid = c.paid().signum() > 0
                    ? new VBox(1, Ui.text(Ui.peso(c.paid()), "body"), Ui.text(Ui.day(c.lastPaidAt()), "small", "muted"))
                    : Ui.text("Not yet", "muted");

            Button setShare = Ui.linkMuted("Set share");
            setShare.setOnAction(e -> Dialogs.setShare(ctx.group, c, ctx::reload));
            HBox second = Ui.row(12, setShare);
            second.setAlignment(Pos.CENTER_RIGHT);
            if (c.paid().signum() > 0) {
                Button undo = Ui.linkMuted("Undo");
                undo.setOnAction(e -> {
                    try {
                        BudgetService.undoLastPayment(ctx.group.id(), me, c.member());
                        ctx.reload();
                        KinnectApp.toast(first + "'s last payment was removed.");
                    } catch (RuntimeException ex) {
                        KinnectApp.toast(Ui.message(ex), true);
                    }
                });
                second.getChildren().add(undo);
            }
            VBox actions = new VBox(4);
            actions.setAlignment(Pos.CENTER_RIGHT);
            if (c.balance().signum() > 0) {
                Button mark = Ui.link("Mark as paid");
                mark.setOnAction(e -> {
                    try {
                        BudgetService.recordPayment(ctx.group.id(), me, c.member(), null);
                        ctx.reload();
                        KinnectApp.toast(c.member().fullName() + " is fully paid.");
                    } catch (RuntimeException ex) {
                        KinnectApp.toast(Ui.message(ex), true);
                    }
                });
                actions.getChildren().add(mark);
            }
            actions.getChildren().add(second);
            GridPane r = tableRow(cols, 11,
                    Ui.person(c.member().fullName(), c.member().isLeader() ? "Group leader" : "Member", 30),
                    share, status, paid, actions);
            GridPane.setHalignment(actions, javafx.geometry.HPos.RIGHT);
            r.getStyleClass().add("row-line");
            table.getChildren().add(r);
        }
        Button change = Ui.linkMuted(s.dueDate() == null ? "Set due date" : "Change");
        change.setOnAction(e -> Dialogs.dueDate(ctx.group, s.dueDate(), ctx::reload));
        String each = s.customCount() == 0 ? " each, " : " even share, ";
        VBox ambagan = Ui.card(12, Ui.cardHeader("Ambagan", Ui.row(10, Ui.text(Ui.peso(s.evenShare()) + each
                + (s.dueDate() == null ? "no due date" : "due " + Ui.day(s.dueDate())), "small", "muted"), change)), table,
                Ui.wrap("Everyone pays the same by default. Use Set share when someone should pay a different amount; "
                        + "the rest is split evenly among everyone else.", "small", "muted"));

        // expenses
        VBox expenseRows = new VBox();
        for (Expense x : expenses) {
            VBox left = new VBox(3, Ui.text(x.item(), "strong"));
            if (x.status() == ExpenseStatus.BOUGHT) {
                left.getChildren().add(Ui.text("Bought " + Ui.day(x.boughtOn()) + " by " + Ui.shortName(x.boughtByName()),
                        "small", "muted"));
            } else {
                Button bought = Ui.linkMuted("Mark bought");
                bought.setOnAction(e -> {
                    try {
                        BudgetService.markBought(ctx.group.id(), me, x.id());
                        ctx.reload();
                        KinnectApp.toast(x.item() + " marked as bought.");
                    } catch (RuntimeException ex) {
                        KinnectApp.toast(Ui.message(ex), true);
                    }
                });
                left.getChildren().add(Ui.row(10, Ui.text("Planned", "small", "amber-text"), bought));
            }
            HBox r = Ui.row(10, left, Ui.grow(), Ui.text(Ui.peso(x.amount()), "strong"));
            r.setPadding(new Insets(12, 0, 12, 0));
            r.getStyleClass().add("row-line");
            expenseRows.getChildren().add(r);
        }
        if (expenses.isEmpty()) {
            expenseRows.getChildren().add(Ui.wrap("No expenses yet. Add what the project will cost and Kinnect "
                    + "splits it evenly.", "muted"));
        }
        HBox cash = Ui.row(8, Ui.text("Cash on hand", "muted"), Ui.grow(), Ui.text(Ui.peso(s.cashOnHand()), "strong"));
        HBox toSpend = Ui.row(8, Ui.text("Still to spend", "muted"), Ui.grow(), Ui.text(Ui.peso(s.stillToSpend()), "strong"));
        VBox totals = new VBox(10, cash, toSpend);
        totals.setPadding(new Insets(14, 0, 0, 0));
        totals.getStyleClass().add("row-line");
        VBox expensesCard = Ui.card(8, Ui.cardHeader("Expenses", Ui.text(Ui.peso(s.spent()) + " spent", "small", "muted")),
                expenseRows, totals);

        return new VBox(20, header, top, summary, Ui.sideBySide(ambagan, expensesCard, 62, 1150));
    }

    private static VBox stat(String label, String value, String note, boolean line) {
        VBox v = new VBox(4, Ui.text(label, "small", "muted"), Ui.text(value, "stat-md"), Ui.text(note, "small", "muted"));
        if (line) {
            v.setStyle("-fx-border-color: transparent transparent transparent " + Theme.BORDER + "; -fx-border-width: 0 0 0 1;"
                    + " -fx-padding: 0 0 0 22;");
        }
        return v;
    }
}
