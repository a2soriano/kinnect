package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AccountRole;
import com.mycompany.mavenproject2.Models.ClassRoom;
import com.mycompany.mavenproject2.Models.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Welcome, log in, sign up, and join a class. All four share the split layout:
 * the form on the left and the pine brand panel on the right.
 *
 * @author Ariel Soriano
 */
public final class AuthScreens {

    private AuthScreens() {
    }

    // ---------- shared layout ----------

    private static Parent split(Node form, String headline, String text) {
        VBox formBox = new VBox(form);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setMaxWidth(400);
        VBox.setVgrow(formBox, Priority.ALWAYS);

        VBox left = new VBox(Ui.logo(22), formBox);
        left.setPadding(new Insets(44, 40, 44, 90));
        HBox.setHgrow(left, Priority.ALWAYS);

        StackPane panel = brandPanel(headline, text);
        panel.setMinWidth(420);
        HBox page = new HBox(left, panel);
        page.getStyleClass().add("page");
        panel.prefWidthProperty().bind(page.widthProperty().multiply(0.45));
        HBox.setMargin(panel, new Insets(16));
        return page;
    }

    private static StackPane brandPanel(String headline, String text) {
        Label title = Ui.wrap(headline, "hero-title");
        title.setMaxWidth(470);
        Label body = Ui.wrap(text, "hero-text");
        body.setMaxWidth(470);
        VBox words = new VBox(18, title, body);
        words.setPadding(new Insets(56, 56, 0, 56));
        words.setMaxHeight(Double.MAX_VALUE);
        words.setPickOnBounds(false);

        // The Kinnect logo, large and partly cut off at the corner. The pine ring is drawn
        // in white here so it shows on the pine panel.
        Pane art = new Pane();
        Runnable draw = () -> {
            double w = art.getWidth();
            double h = art.getHeight();
            double size = w * 0.52;
            double x = w * 0.14;
            double y = h * 0.6;
            art.getChildren().setAll(Ui.ring(x, y, size, Theme.ON_PINE),
                    Ui.ring(x + size * Ui.RING_OFFSET, y, size, Theme.AMBER));
        };
        art.widthProperty().addListener((o, a, b) -> draw.run());
        art.heightProperty().addListener((o, a, b) -> draw.run());

        StackPane panel = new StackPane(art, words);
        StackPane.setAlignment(words, Pos.TOP_LEFT);
        panel.setStyle("-fx-background-color: " + Theme.PINE + "; -fx-background-radius: 20;");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        clip.widthProperty().bind(panel.widthProperty());
        clip.heightProperty().bind(panel.heightProperty());
        panel.setClip(clip);
        return panel;
    }

    /** A password box with an eye button that shows or hides what was typed. */
    static final class PasswordBox extends StackPane {

        final PasswordField hidden = new PasswordField();
        final TextField shown = new TextField();

        PasswordBox(String prompt) {
            hidden.setPromptText(prompt);
            shown.setPromptText(prompt);
            shown.textProperty().bindBidirectional(hidden.textProperty());
            shown.setVisible(false);
            Button eye = new Button();
            eye.setGraphic(Ui.icon("eye", 18, Theme.MUTED));
            eye.setStyle("-fx-background-color: transparent; -fx-padding: 6 12;");
            eye.setFocusTraversable(false);
            eye.setOnAction(e -> {
                boolean show = !shown.isVisible();
                shown.setVisible(show);
                hidden.setVisible(!show);
            });
            hidden.setStyle("-fx-padding: 11 44 11 14;");
            shown.setStyle("-fx-padding: 11 44 11 14;");
            getChildren().addAll(hidden, shown, eye);
            StackPane.setAlignment(eye, Pos.CENTER_RIGHT);
        }

        String text() {
            return hidden.getText();
        }

        void invalid(boolean bad) {
            for (TextField f : new TextField[]{hidden, shown}) {
                f.getStyleClass().remove("invalid");
                if (bad) {
                    f.getStyleClass().add("invalid");
                }
            }
        }
    }

    private static void invalid(TextField f, boolean bad) {
        f.getStyleClass().remove("invalid");
        if (bad) {
            f.getStyleClass().add("invalid");
        }
    }

    private static HBox footer(String question, String action, Runnable go) {
        Button b = Ui.link(action);
        b.setOnAction(e -> go.run());
        HBox h = Ui.row(6, Ui.text(question, "muted"), b);
        h.setPadding(new Insets(4, 0, 0, 0));
        return h;
    }

    // ---------- welcome ----------

    static Parent welcome() {
        Button login = Ui.wide(Ui.primary("Log in"));
        login.setOnAction(e -> KinnectApp.show(login(null)));
        Button signup = Ui.wide(Ui.secondary("Create an account"));
        signup.setOnAction(e -> KinnectApp.show(signUp()));
        VBox form = new VBox(16,
                Ui.text("Welcome to Kinnect", "h1-auth"),
                Ui.wrap("A group project coordination and progress monitoring system for classes.", "lead"),
                new VBox(10, login, signup),
                Ui.wrap("Professors and students use the same system. Kinnect opens the right dashboard "
                        + "for each account.", "small", "muted"));
        VBox.setMargin(form.getChildren().get(2), new Insets(10, 0, 0, 0));
        return split(form, "Every group project, every task, in plain sight.",
                "Professors set up groups and pick leaders, leaders assign tasks, and progress updates "
                + "as soon as someone finishes one.");
    }

    // ---------- log in ----------

    static Parent login(String prefillEmail) {
        HBox error = Ui.errorBanner();
        TextField email = new TextField(prefillEmail == null ? "" : prefillEmail);
        email.setPromptText("you@school.edu");
        PasswordBox password = new PasswordBox("Your password");
        Button go = Ui.wide(Ui.primary("Log in"));
        go.setDefaultButton(true);
        go.setOnAction(e -> {
            try {
                AuthService.logIn(email.getText(), password.text());
                KinnectApp.home();
            } catch (RuntimeException ex) {
                Ui.showError(error, ex instanceof Models.AppException && ex.getMessage().startsWith("Incorrect")
                        ? "The email or password is incorrect. Check both and try again." : Ui.message(ex));
                invalid(email, true);
                password.invalid(true);
            }
        });
        VBox form = new VBox(14,
                Ui.text("Log in to Kinnect", "h1-auth"),
                Ui.wrap("Welcome back. Use the email and password you signed up with.", "lead"),
                error,
                Ui.field("Email", email),
                Ui.field("Password", password),
                go,
                Ui.wrap("Professors and students both log in here. Kinnect opens the dashboard that "
                        + "matches your account.", "small", "muted"),
                Ui.divider(),
                footer("New to Kinnect?", "Create an account", () -> KinnectApp.show(signUp())));
        VBox.setMargin(go, new Insets(8, 0, 0, 0));
        Platform.runLater(email::requestFocus);
        return split(form, "Pick up where your group left off.",
                "Your tasks, deadlines, files, and group chat are waiting in your workspace.");
    }

    // ---------- sign up ----------

    static Parent signUp() {
        HBox error = Ui.errorBanner();
        TextField name = new TextField();
        name.setPromptText("Your full name");
        TextField email = new TextField();
        email.setPromptText("you@school.edu");
        PasswordBox password = new PasswordBox("At least 8 characters");
        PasswordBox confirm = new PasswordBox("Type it again");
        AccountRole[] role = {AccountRole.STUDENT};

        VBox prof = roleCard("cap", "Professor", "Create classes and form groups");
        VBox student = roleCard("user", "Student", "Join a class with a code");
        Runnable paint = () -> {
            select(prof, role[0] == AccountRole.PROFESSOR);
            select(student, role[0] == AccountRole.STUDENT);
        };
        prof.setOnMouseClicked(e -> {
            role[0] = AccountRole.PROFESSOR;
            paint.run();
        });
        student.setOnMouseClicked(e -> {
            role[0] = AccountRole.STUDENT;
            paint.run();
        });
        paint.run();
        HBox roles = new HBox(10, prof, student);
        HBox.setHgrow(prof, Priority.ALWAYS);
        HBox.setHgrow(student, Priority.ALWAYS);
        prof.setMaxWidth(Double.MAX_VALUE);
        student.setMaxWidth(Double.MAX_VALUE);
        prof.setPrefWidth(10);
        student.setPrefWidth(10);

        Button go = Ui.wide(Ui.primary("Create account"));
        go.setDefaultButton(true);
        go.setOnAction(e -> {
            try {
                AuthService.signUp(name.getText(), email.getText(), password.text(), confirm.text(), role[0]);
                KinnectApp.home();
                KinnectApp.toast("Welcome to Kinnect, " + Session.user().fullName().split(" ")[0] + ".");
            } catch (RuntimeException ex) {
                Ui.showError(error, Ui.message(ex));
                String m = ex.getMessage() == null ? "" : ex.getMessage();
                invalid(name, m.contains("name"));
                invalid(email, m.contains("email"));
                password.invalid(m.startsWith("Password"));
                confirm.invalid(m.contains("match"));
            }
        });
        HBox passwords = new HBox(10, Ui.field("Password", password), Ui.field("Confirm password", confirm));
        passwords.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        VBox form = new VBox(12,
                Ui.text("Create your account", "h1-auth"),
                Ui.wrap("Kinnect sets up your workspace based on your role.", "lead"),
                error,
                Ui.field("Full name", name),
                Ui.field("Email", email),
                passwords,
                Ui.field("I'm signing up as", roles),
                go,
                Ui.divider(),
                footer("Already have an account?", "Log in", () -> KinnectApp.show(login(null))));
        VBox.setMargin(go, new Insets(6, 0, 0, 0));
        return split(form, "Start your next group project here.",
                "Professors create classes and groups. Students join with a class code and get straight to work.");
    }

    private static VBox roleCard(String icon, String title, String text) {
        Circle radio = new Circle(7, Color.WHITE);
        radio.setStroke(Color.web("#A7B9B0"));
        Circle dot = new Circle(3.5, Color.web(Theme.PINE));
        StackPane radioBox = new StackPane(radio, dot);
        HBox top = Ui.row(0, Ui.icon(icon, 18, Theme.PINE), Ui.grow(), radioBox);
        VBox card = new VBox(8, top, Ui.text(title, "strong"), Ui.wrap(text, "small", "muted"));
        card.getStyleClass().add("role-card");
        card.setUserData(new Node[]{radio, dot});
        return card;
    }

    private static void select(VBox card, boolean on) {
        card.getStyleClass().remove("selected");
        if (on) {
            card.getStyleClass().add("selected");
        }
        Node[] parts = (Node[]) card.getUserData();
        ((Circle) parts[0]).setStroke(Color.web(on ? Theme.PINE : "#A7B9B0"));
        parts[1].setVisible(on);
    }

    // ---------- join a class ----------

    static Parent joinClass(boolean hasClasses) {
        User me = Session.user();
        TextField code = new TextField();
        code.setPromptText("For example KQ7-4MX");
        Label error = Ui.wrap("", "error-text");
        error.setManaged(false);
        error.setVisible(false);
        Button go = Ui.wide(Ui.primary("Join class"));
        go.setDefaultButton(true);
        go.setOnAction(e -> {
            try {
                ClassRoom room = ClassService.joinClass(me.id(), code.getText());
                KinnectApp.show(StudentScreens.home());
                KinnectApp.toast("You joined " + room.name() + ".");
            } catch (RuntimeException ex) {
                String m = Ui.message(ex);
                error.setText(m.startsWith("That class code") ? "That code doesn't match any class. "
                        + "Check it with your professor and try again." : m);
                error.setManaged(true);
                error.setVisible(true);
                invalid(code, true);
            }
        });
        Button logout = Ui.link("Log out");
        logout.setOnAction(e -> KinnectApp.logOut());
        HBox notYou = Ui.row(6, Ui.text("Not you?", "muted"), logout);
        if (hasClasses) {
            Button back = Ui.link("Back to my classes");
            back.setOnAction(e -> KinnectApp.show(StudentScreens.home()));
            notYou.getChildren().addAll(Ui.text("  ", "muted"), back);
        }
        VBox form = new VBox(14,
                Ui.text(hasClasses ? "Join another class" : "Join your class", "h1-auth"),
                Ui.wrap("Enter the class code your professor shared. You only need to do this once per class.", "lead"),
                new VBox(7, Ui.text("Class code", "field-label"), code, error),
                go,
                Ui.text("Signed in as " + me.fullName() + ", " + me.email(), "small", "muted"),
                Ui.divider(),
                notYou);
        VBox.setMargin(go, new Insets(6, 0, 0, 0));
        Platform.runLater(code::requestFocus);
        return split(form, "One code and you're in.",
                "The code adds you to your professor's class. Your group and tasks show up as soon as you're assigned.");
    }
}
