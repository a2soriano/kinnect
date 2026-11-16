package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.ClassRoom;
import com.mycompany.mavenproject2.Models.Contribution;
import com.mycompany.mavenproject2.Models.Enrollment;
import com.mycompany.mavenproject2.Models.Member;
import com.mycompany.mavenproject2.Models.ProjectGroup;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Every pop-up form in Kinnect. A form stays open and shows the problem
 * if a rule is broken, and closes only when the save works.
 *
 * @author Ariel Soriano
 */
public final class Dialogs {

    interface Action {
        void run();
    }

    private Dialogs() {
    }

    /** Title, subtitle, fields, then Cancel and the main button. */
    static Button form(String title, String subtitle, double width, Node content, String primaryText, Action action) {
        HBox error = Ui.errorBanner();
        Button cancel = Ui.secondary("Cancel");
        cancel.setOnAction(e -> KinnectApp.closeModal());
        Button ok = Ui.primary(primaryText);
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            try {
                action.run();
                KinnectApp.closeModal();
            } catch (RuntimeException ex) {
                Ui.showError(error, Ui.message(ex));
            }
        });
        HBox buttons = Ui.row(10, Ui.grow(), cancel, ok);
        VBox box = new VBox(16);
        box.getChildren().add(new VBox(4, Ui.text(title, "h2"), Ui.wrap(subtitle, "muted")));
        box.getChildren().addAll(error, content, buttons);
        VBox.setMargin(buttons, new Insets(4, 0, 0, 0));
        box.setPrefWidth(width);
        KinnectApp.openModal(box);
        return ok;
    }

    static TextField input(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        return t;
    }

    static TextArea area(String prompt) {
        TextArea a = new TextArea();
        a.setPromptText(prompt);
        a.setWrapText(true);
        a.setPrefRowCount(3);
        return a;
    }

    static DatePicker datePicker(LocalDate value, boolean noPast) {
        DatePicker d = new DatePicker(value);
        d.setConverter(Ui.converter(Ui.FULL_DAY::format));
        d.setEditable(false);
        d.setMaxWidth(Double.MAX_VALUE);
        if (noPast) {
            d.setDayCellFactory(p -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(empty || item.isBefore(LocalDate.now()));
                }
            });
        }
        return d;
    }

    /** Times every 30 minutes, 7:00 AM to 10:00 PM. */
    static ComboBox<LocalTime> timePicker(LocalTime value) {
        ComboBox<LocalTime> c = new ComboBox<>();
        for (LocalTime t = LocalTime.of(7, 0); !t.isAfter(LocalTime.of(22, 0)); t = t.plusMinutes(30)) {
            c.getItems().add(t);
        }
        c.setConverter(Ui.converter(t -> t.format(Ui.TIME)));
        c.setValue(value);
        c.setVisibleRowCount(8);
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    static HBox twoUp(Node a, Node b) {
        HBox h = new HBox(14, a, b);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        ((javafx.scene.layout.Region) a).setPrefWidth(10);
        ((javafx.scene.layout.Region) b).setPrefWidth(10);
        return h;
    }

    /** A bordered list of people, one row each, with a radio or a check box. */
    static <T> VBox pickList(List<T> items, java.util.function.Function<T, String> name,
            java.util.function.Function<T, String> detail, boolean many, List<T> chosen, Runnable changed) {
        VBox list = new VBox();
        list.getStyleClass().add("pick-list");
        ToggleGroup group = new ToggleGroup();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            javafx.scene.control.ButtonBase box;
            if (many) {
                box = new CheckBox();
            } else {
                RadioButton r = new RadioButton();
                r.setToggleGroup(group);
                box = r;
            }
            box.setMouseTransparent(true);
            HBox row = Ui.row(14, box, Ui.person(name.apply(item), detail.apply(item), 30));
            row.getStyleClass().add("pick-row");
            if (i > 0) {
                row.setStyle("-fx-border-color: " + Theme.BORDER + " transparent transparent transparent;");
            }
            Runnable paint = () -> {
                boolean on = chosen.contains(item);
                row.getStyleClass().remove("selected");
                if (on) {
                    row.getStyleClass().add("selected");
                }
                if (box instanceof CheckBox cb) {
                    cb.setSelected(on);
                } else {
                    ((RadioButton) box).setSelected(on);
                }
            };
            row.setOnMouseClicked(e -> {
                if (many) {
                    if (!chosen.remove(item)) {
                        chosen.add(item);
                    }
                } else {
                    chosen.clear();
                    chosen.add(item);
                }
                list.getChildren().forEach(n -> ((Runnable) n.getUserData()).run());
                if (changed != null) {
                    changed.run();
                }
            });
            row.setUserData(paint);
            paint.run();
            list.getChildren().add(row);
        }
        return list;
    }

    /** Long lists scroll inside the dialog; short ones show in full. */
    static Node scrollIfLong(Node list, int count, int maxRows) {
        if (count <= maxRows) {
            return list;
        }
        javafx.scene.control.ScrollPane sp = Ui.scroll(list);
        sp.setPrefViewportHeight(maxRows * 58);
        return sp;
    }

    // ---------- professor ----------

    static void createClass(java.util.function.Consumer<ClassRoom> created) {
        TextField name = input("For example Software Design, Section B");
        TextField details = input("Schedule or room, for example MWF 1:00 to 2:30 PM");
        VBox content = new VBox(14, Ui.field("Class name", name), Ui.field("Details (optional)", details));
        ClassRoom[] made = new ClassRoom[1];
        form("Create a class", "Kinnect makes a class code your students use to join.", 460, content,
                "Create class", () -> {
                    made[0] = ClassService.createClass(Session.user().id(), name.getText(), details.getText());
                    Platform.runLater(() -> created.accept(made[0]));
                });
        Platform.runLater(name::requestFocus);
    }

    static void classCode(ClassRoom room) {
        Label code = Ui.text(room.code(), "code-text");
        VBox codeBox = new VBox(2, code, Ui.text("Class code", "small", "muted"));
        codeBox.setAlignment(Pos.CENTER);
        codeBox.getStyleClass().add("code-box");
        Button copy = Ui.secondary("Copy code");
        copy.setOnAction(e -> {
            ClipboardContent c = new ClipboardContent();
            c.putString(room.code());
            Clipboard.getSystemClipboard().setContent(c);
            KinnectApp.toast("Class code copied.");
        });
        Button open = Ui.primary("Open class");
        open.setOnAction(e -> KinnectApp.show(ProfessorScreens.classWorkspace(room.id())));
        Node check = Ui.iconCircle("check", 46);
        VBox box = new VBox(14, check,
                new VBox(4, Ui.text("Your class is ready", "h2"),
                        Ui.wrap("Share this code with your students so they can join " + room.name() + ".", "muted")),
                codeBox, Ui.row(10, Ui.grow(), copy, open));
        box.setPrefWidth(440);
        KinnectApp.openModal(box);
    }

    static void createGroup(ClassRoom room, Runnable done) {
        List<Enrollment> free = ClassService.studentsWithoutGroup(room.id());
        TextField name = input("Group name");
        name.setText("Group " + (GroupService.groupsOf(room.id()).size() + 1));
        TextField details = input("Project title or details");
        List<Enrollment> chosen = new ArrayList<>();
        Node leaders;
        if (free.isEmpty()) {
            leaders = Ui.wrap("Every student in this class is already in a group. Ask new students to join "
                    + "with code " + room.code() + " first.", "muted");
        } else {
            leaders = pickList(free, Enrollment::studentName, Enrollment::email, false, chosen, null);
        }
        VBox content = new VBox(14, Ui.field("Group name", name), Ui.field("Project or details", details),
                Ui.field("Group leader", scrollIfLong(leaders, free.size(), 4)),
                Ui.text("Only enrolled students without a group are listed.", "small", "muted"));
        Button ok = form("Create a group", "Choose a leader from students who aren't in a group yet. "
                + "Next you can pick the members, or leave that to the leader.", 480, content, "Create group", () -> {
                    if (chosen.isEmpty()) {
                        throw new Models.AppException("Choose a group leader.");
                    }
                    ProjectGroup g = GroupService.createGroup(room.id(), Session.user().id(), name.getText(),
                            details.getText(), chosen.get(0));
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast(g.name() + " created. " + chosen.get(0).studentName() + " is the leader.");
                        manageGroup(GroupService.get(g.id()), done);
                    });
                });
        ok.setDisable(free.isEmpty());
    }

    /**
     * Professor only: choose the leader, add classmates, and remove members.
     * Changes save right away; the screen behind refreshes when the dialog closes.
     */
    static void manageGroup(ProjectGroup group, Runnable changed) {
        long me = Session.user().id();
        boolean[] dirty = {false};
        VBox box = new VBox(16);
        box.setPrefWidth(540);
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            HBox error = Ui.errorBanner();
            ProjectGroup current = GroupService.get(group.id());
            List<Member> members = GroupService.members(group.id());

            TextField nameField = input("Group name");
            nameField.setText(current.name());
            TextField titleField = input("Project title");
            titleField.setText(current.description() == null ? "" : current.description());
            CheckBox leaderCan = new CheckBox("Leader can rename the group");
            leaderCan.setSelected(!GroupService.nameLocked(group.id()));
            leaderCan.setOnAction(e -> {
                try {
                    GroupService.setNameLocked(group.id(), me, !leaderCan.isSelected());
                    KinnectApp.toast(leaderCan.isSelected() ? "The leader can rename the group again."
                            : "Only you can rename this group now.");
                } catch (RuntimeException ex) {
                    Ui.showError(error, Ui.message(ex));
                }
            });
            Button saveName = Ui.secondary("Save name");
            saveName.setOnAction(e -> {
                try {
                    GroupService.rename(group.id(), me, nameField.getText(), titleField.getText());
                    dirty[0] = true;
                    render[0].run();
                    KinnectApp.toast("Name saved. The leader can't change it unless you allow it.");
                } catch (RuntimeException ex) {
                    Ui.showError(error, Ui.message(ex));
                }
            });
            javafx.scene.layout.GridPane names = Ui.columns(14, 40, 60);
            names.add(Ui.field("Group name", nameField), 0, 0);
            names.add(Ui.field("Project title", titleField), 1, 0);
            VBox nameSection = new VBox(10, names, Ui.row(10, leaderCan, Ui.grow(), saveName));
            VBox memberList = new VBox();
            memberList.getStyleClass().add("pick-list");
            for (int i = 0; i < members.size(); i++) {
                Member m = members.get(i);
                HBox actions = Ui.row(14);
                if (m.isLeader()) {
                    actions.getChildren().add(Ui.text("Leader", "pill", "pill-green"));
                } else {
                    Button lead = Ui.link("Make leader");
                    lead.setOnAction(e -> {
                        try {
                            GroupService.setLeader(group.id(), me, m);
                            dirty[0] = true;
                            render[0].run();
                            KinnectApp.toast(m.fullName() + " is now the leader of " + current.name() + ".");
                        } catch (RuntimeException ex) {
                            Ui.showError(error, Ui.message(ex));
                        }
                    });
                    Button remove = Ui.linkMuted("Remove");
                    remove.setOnAction(e -> {
                        try {
                            GroupService.removeMember(group.id(), me, m);
                            dirty[0] = true;
                            render[0].run();
                            KinnectApp.toast(m.fullName() + " was removed from " + current.name() + ".");
                        } catch (RuntimeException ex) {
                            Ui.showError(error, Ui.message(ex));
                        }
                    });
                    actions.getChildren().addAll(lead, remove);
                }
                HBox row = Ui.row(14, Ui.person(m.fullName(), m.email(), 30), Ui.grow(), actions);
                row.setStyle("-fx-padding: 10 16;" + (i > 0
                        ? " -fx-border-color: " + Theme.BORDER + " transparent transparent transparent;" : ""));
                memberList.getChildren().add(row);
            }
            if (members.isEmpty()) {
                memberList.getChildren().add(Ui.wrap("This group has no members yet.", "muted"));
            }

            List<Enrollment> free = ClassService.studentsWithoutGroup(group.classId());
            List<Enrollment> chosen = new ArrayList<>();
            Button add = Ui.primary("Add to group");
            add.setDisable(true);
            Node addList;
            if (free.isEmpty()) {
                addList = Ui.wrap("Every student in " + group.className() + " is already in a group.", "muted");
            } else {
                addList = scrollIfLong(pickList(free, Enrollment::studentName, Enrollment::email, true, chosen, () -> {
                    add.setDisable(chosen.isEmpty());
                    add.setText(chosen.isEmpty() ? "Add to group" : "Add " + Ui.plural(chosen.size(), "student", "students"));
                }), free.size(), 3);
            }
            add.setOnAction(e -> {
                try {
                    int n = GroupService.addMembers(group.id(), me, new ArrayList<>(chosen));
                    dirty[0] = true;
                    render[0].run();
                    KinnectApp.toast(Ui.plural(n, "student", "students") + " added to " + current.name() + ".");
                } catch (RuntimeException ex) {
                    Ui.showError(error, Ui.message(ex));
                }
            });

            Button done = Ui.primary("Done");
            done.setDefaultButton(true);
            done.setOnAction(e -> KinnectApp.closeModal());
            HBox addRow = Ui.row(10, Ui.text("Students without a group", "field-label"), Ui.grow());
            if (!free.isEmpty()) {
                addRow.getChildren().add(add);
                add.getStyleClass().setAll("button", "btn-secondary");
            }
            box.getChildren().setAll(
                    new VBox(4, Ui.text("Manage " + current.name(), "h2"),
                            Ui.wrap("Rename the group, choose the leader, and pick the members. The leader can "
                                    + "still add classmates from their own workspace.", "muted")),
                    error,
                    nameSection,
                    Ui.divider(),
                    Ui.field("Members", scrollIfLong(memberList, members.size(), 4)),
                    new VBox(7, addRow, addList),
                    Ui.text("A member with tasks, files, events, links, or payments in the group can't be removed.",
                            "small", "muted"),
                    Ui.row(10, Ui.grow(), done));
        };
        render[0].run();
        KinnectApp.openModal(box, () -> {
            if (dirty[0]) {
                changed.run();
            }
        });
    }

    // ---------- group leader ----------

    static void addMembers(ProjectGroup group, Runnable done) {
        List<Enrollment> free = GroupService.candidates(group.id());
        List<Enrollment> chosen = new ArrayList<>();
        Button[] ok = new Button[1];
        Node list = free.isEmpty()
                ? Ui.emptyState("users", "No classmates to add", "Everyone in " + group.className()
                        + " is already in a group.")
                : pickList(free, Enrollment::studentName, Enrollment::email, true, chosen, () -> {
                    ok[0].setText(chosen.isEmpty() ? "Add members" : "Add " + Ui.plural(chosen.size(), "member", "members"));
                });
        VBox content = new VBox(12, scrollIfLong(list, free.size(), 5),
                Ui.text("Only students from the same class who are not in a group are listed.", "small", "muted"));
        ok[0] = form("Add members", "Classmates from " + group.className() + " who aren't in a group yet.", 480,
                content, "Add members", () -> {
                    if (chosen.isEmpty()) {
                        throw new Models.AppException("Select at least one classmate to add.");
                    }
                    int n = GroupService.addMembers(group.id(), Session.user().id(), chosen);
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast(Ui.plural(n, "member", "members") + " added to " + group.name() + ".");
                    });
                });
        ok[0].setDisable(free.isEmpty());
    }

    static void createTask(ProjectGroup group, Runnable done) {
        TextField title = input("What needs to be done");
        TextArea desc = area("Details the member needs to know");
        DatePicker deadline = datePicker(LocalDate.now().plusDays(7), true);
        ComboBox<Member> assignee = new ComboBox<>();
        assignee.getItems().addAll(GroupService.members(group.id()));
        assignee.setPromptText("Choose a member");
        assignee.setConverter(Ui.converter(Member::fullName));
        assignee.setCellFactory(l -> memberCell());
        assignee.setButtonCell(memberCell());
        assignee.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(14, Ui.field("Task title", title), Ui.field("Description", desc),
                twoUp(Ui.field("Deadline", deadline), Ui.field("Assign to", assignee)));
        form("Create a task", "New tasks start as Pending. The member you assign sees it in My tasks.", 500,
                content, "Create task", () -> {
                    var t = TaskService.create(group.id(), Session.user().id(), title.getText(), desc.getText(),
                            deadline.getValue(), assignee.getValue());
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast("Task created and assigned to " + t.assigneeName() + ".");
                    });
                });
        Platform.runLater(title::requestFocus);
    }

    private static ListCell<Member> memberCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Member m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setText(null);
                    setGraphic(Ui.row(8, Ui.avatar(m.fullName(), 24), new Label(m.fullName())));
                }
            }
        };
    }

    // ---------- group tools ----------

    static void addEvent(ProjectGroup group, LocalDate day, Runnable done) {
        TextField title = input("For example Group meeting");
        TextArea desc = area("What the event is for");
        DatePicker date = datePicker(day == null || day.isBefore(LocalDate.now()) ? LocalDate.now() : day, false);
        ComboBox<LocalTime> start = timePicker(LocalTime.of(15, 0));
        ComboBox<LocalTime> end = timePicker(LocalTime.of(16, 0));
        javafx.scene.layout.GridPane times = Ui.columns(14, 42, 29, 29);
        times.add(Ui.field("Date", date), 0, 0);
        times.add(Ui.field("Starts", start), 1, 0);
        times.add(Ui.field("Ends", end), 2, 0);
        VBox content = new VBox(14, Ui.field("Event title", title), Ui.field("Description", desc), times);
        form("Add an event", "Everyone in " + group.name() + " will see it on the calendar.", 540, content,
                "Add event", () -> {
                    LocalDateTime s = date.getValue() == null || start.getValue() == null ? null
                            : date.getValue().atTime(start.getValue());
                    LocalDateTime e = date.getValue() == null || end.getValue() == null ? null
                            : date.getValue().atTime(end.getValue());
                    var ev = CollabService.addEvent(group.id(), Session.user().id(), title.getText(),
                            desc.getText(), s, e);
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast(ev.title() + " added on " + Ui.day(ev.start()) + ".");
                    });
                });
        Platform.runLater(title::requestFocus);
    }

    static void addLink(ProjectGroup group, Runnable done) {
        TextField title = input("For example Final paper draft");
        TextField url = input("https://docs.google.com/...");
        VBox content = new VBox(14, Ui.field("Title", title), Ui.field("Link", url),
                Ui.wrap("Paste the share link from Google Docs, Sheets, Slides, Canva, Trello, or any site.",
                        "small", "muted"));
        form("Add a document link", "Everyone in " + group.name() + " can open it from Shared documents.", 480,
                content, "Add link", () -> {
                    CollabService.addDocument(group.id(), Session.user().id(), title.getText(), url.getText());
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast("Link added.");
                    });
                });
        Platform.runLater(title::requestFocus);
    }

    // ---------- budget (leader only) ----------

    static void addExpense(ProjectGroup group, Runnable done) {
        TextField item = input("For example Printing and binding");
        TextField amount = input("650");
        CheckBox bought = new CheckBox("Already bought");
        ComboBox<Member> buyer = new ComboBox<>();
        buyer.getItems().addAll(GroupService.members(group.id()));
        buyer.setConverter(Ui.converter(Member::fullName));
        buyer.getSelectionModel().selectFirst();
        buyer.setMaxWidth(Double.MAX_VALUE);
        VBox buyerField = Ui.field("Bought by", buyer);
        buyerField.disableProperty().bind(bought.selectedProperty().not());
        VBox content = new VBox(14, Ui.field("What it's for", item), twoUp(Ui.field("Amount in pesos", amount),
                buyerField), bought,
                Ui.wrap("Each member's share is the total of all expenses divided by the number of members.",
                        "small", "muted"));
        form("Add an expense", "Planned costs count toward the ambagan too.", 480, content, "Add expense", () -> {
            BudgetService.addExpense(group.id(), Session.user().id(), item.getText(), amount.getText(),
                    bought.isSelected(), buyer.getValue());
            Platform.runLater(() -> {
                done.run();
                KinnectApp.toast("Expense added. Each member's share was updated.");
            });
        });
        Platform.runLater(item::requestFocus);
    }

    /** Records money a member handed over: the full balance or a partial payment. */
    static void recordPayment(ProjectGroup group, Runnable done) {
        long me = Session.user().id();
        List<Contribution> owing = BudgetService.contributions(group.id(), me).stream()
                .filter(c -> c.balance().signum() > 0).toList();
        List<Contribution> chosen = new ArrayList<>();
        TextField amount = input("Amount in pesos");
        Node content;
        if (owing.isEmpty()) {
            content = Ui.emptyState("check", "Everyone has paid", "No one in " + group.name() + " owes anything right now.");
        } else {
            Node list = pickList(owing, c -> c.member().fullName(),
                    c -> "Owes " + Ui.peso(c.balance()) + (c.paid().signum() > 0 ? ", paid " + Ui.peso(c.paid()) + " so far" : ""),
                    false, chosen, () -> amount.setText(plain(chosen.get(0).balance())));
            content = new VBox(14, scrollIfLong(list, owing.size(), 4), Ui.field("Amount received", amount),
                    Ui.wrap("The full balance marks them as paid. Type a smaller amount for a partial payment.",
                            "small", "muted"));
        }
        Button ok = form("Record a payment", "Choose who handed you money for the ambagan.", 460, content,
                "Record payment", () -> {
                    if (chosen.isEmpty()) {
                        throw new Models.AppException("Choose who paid.");
                    }
                    Member m = chosen.get(0).member();
                    java.math.BigDecimal paid = BudgetService.recordPayment(group.id(), me, m, amount.getText());
                    java.math.BigDecimal left = chosen.get(0).balance().subtract(paid);
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast(left.signum() > 0
                                ? Ui.peso(paid) + " recorded. " + m.fullName() + " still owes " + Ui.peso(left) + "."
                                : m.fullName() + " is fully paid.");
                    });
                });
        ok.setDisable(owing.isEmpty());
    }

    static String plain(java.math.BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    /**
     * Leader only: give one member a custom share, or put them back on the even split.
     * The preview shows what everyone else will pay before saving.
     */
    static void setShare(ProjectGroup group, Contribution c, Runnable done) {
        long me = Session.user().id();
        Member m = c.member();
        String first = m.fullName().split(" ")[0];
        ToggleGroup mode = new ToggleGroup();
        RadioButton even = new RadioButton("Split evenly with the others");
        RadioButton custom = new RadioButton("Custom amount");
        even.setToggleGroup(mode);
        custom.setToggleGroup(mode);
        TextField amount = input("For example 700");
        if (c.custom()) {
            custom.setSelected(true);
            amount.setText(plain(c.share()));
        } else {
            even.setSelected(true);
        }
        amount.disableProperty().bind(custom.selectedProperty().not());
        Label preview = Ui.wrap("", "small");
        preview.setStyle("-fx-text-fill: " + Theme.INK + ";");
        VBox previewBox = new VBox(preview);
        previewBox.getStyleClass().add("banner-info");
        Runnable update = () -> {
            try {
                java.math.BigDecimal a = null;
                if (custom.isSelected()) {
                    if (amount.getText().isBlank()) {
                        preview.setText("Type how much " + first + " should pay.");
                        return;
                    }
                    a = BudgetService.parseAmount(amount.getText());
                }
                BudgetService.Split s = BudgetService.preview(group.id(), me, m, a);
                StringBuilder sb = new StringBuilder(first + " pays " + Ui.peso(s.shareOf(m.id())) + ".");
                int others = s.evenCount() - (a == null ? 1 : 0);
                if (s.overAssigned().signum() > 0) {
                    sb.append(" Custom shares would be ").append(Ui.peso(s.overAssigned()))
                            .append(" more than the budget, so this can't be saved.");
                } else if (others > 0) {
                    sb.append(" The other ").append(Ui.plural(others, "member", "members"))
                            .append(" without a custom share pay ").append(Ui.peso(s.even())).append(" each.");
                } else if (s.uncovered().signum() > 0) {
                    sb.append(" Everyone now has a custom share, and ").append(Ui.peso(s.uncovered()))
                            .append(" of the budget isn't covered by anyone.");
                }
                preview.setText(sb.toString());
            } catch (RuntimeException ex) {
                preview.setText(Ui.message(ex));
            }
        };
        mode.selectedToggleProperty().addListener((o, a, b) -> update.run());
        amount.textProperty().addListener((o, a, b) -> update.run());
        update.run();
        VBox content = new VBox(12, even, custom, amount, previewBox);
        form("Set " + first + "'s share", "Everyone pays the same by default. Giving " + first
                + " a custom share changes what the others pay.", 460, content, "Save share", () -> {
                    BudgetService.setShare(group.id(), me, m, custom.isSelected() ? amount.getText() : null);
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast(custom.isSelected() ? first + "'s share is saved. The others were recalculated."
                                : first + " is back on the even split.");
                    });
                });
    }

    /** Leader: change the group's name and project title (until the professor locks it). */
    static void renameGroup(ProjectGroup group, Runnable done) {
        TextField name = input("Group name");
        name.setText(group.name());
        TextField title = input("Project title");
        title.setText(group.description() == null ? "" : group.description());
        VBox content = new VBox(14, Ui.field("Group name", name), Ui.field("Project title", title));
        form("Edit group name", "Your group and your professor will see the new name. If your professor changes it, "
                + "only they can change it after that.", 460, content, "Save", () -> {
                    ProjectGroup g = GroupService.rename(group.id(), Session.user().id(), name.getText(), title.getText());
                    Platform.runLater(() -> {
                        done.run();
                        KinnectApp.toast("Group renamed to " + g.name() + ".");
                    });
                });
        Platform.runLater(name::requestFocus);
    }

    static void dueDate(ProjectGroup group, LocalDate current, Runnable done) {
        DatePicker d = datePicker(current == null ? LocalDate.now().plusDays(7) : current, false);
        form("Ambagan due date", "Members should pay their share on or before this day.", 400,
                Ui.field("Due date", d), "Save", () -> {
                    if (d.getValue() == null) {
                        throw new Models.AppException("Pick a date.");
                    }
                    BudgetService.setDueDate(group.id(), Session.user().id(), d.getValue());
                    Platform.runLater(done);
                });
    }
}
