package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.User;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The Kinnect desktop window. Every screen is shown inside this one window;
 * dialogs open on top of the current screen.
 *
 * @author Ariel Soriano
 */
public class KinnectApp extends Application {

    private static Stage stage;
    private static StackPane root;
    private static final StackPane PAGE = new StackPane();
    private static final StackPane MODAL = new StackPane();
    private static final StackPane TOAST = new StackPane();
    private static Runnable onLeave;
    private static Runnable onModalClose;
    private static Application app;

    @Override
    public void start(Stage primaryStage) {
        app = this;
        stage = primaryStage;
        Theme.loadFonts();
        MODAL.setVisible(false);
        TOAST.setMouseTransparent(true);
        TOAST.setAlignment(Pos.BOTTOM_CENTER);
        TOAST.setPadding(new Insets(0, 0, 36, 0));
        root = new StackPane(PAGE, MODAL, TOAST);
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(Theme.stylesheet());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && MODAL.isVisible()) {
                closeModal();
            }
        });
        stage.setTitle("Kinnect");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.getIcons().addAll(appIcons());
        stage.setOnHidden(e -> leave());
        stage.show();
        connect();
    }

    /** Opens the database, then shows the welcome screen (or an error with Retry). */
    private static void connect() {
        try {
            Db.configure();
            Db.init();
            show(AuthScreens.welcome());
        } catch (RuntimeException e) {
            show(databaseError(e.getMessage()));
        }
    }

    private static Parent databaseError(String detail) {
        Button retry = Ui.primary("Try again");
        retry.setOnAction(e -> connect());
        Label hint = Ui.wrap("Make sure MySQL is running and that the settings in kinnect.properties "
                + "(db.url, db.user, db.password) are correct. Kinnect is trying to connect to:\n" + Db.url(), "muted");
        Label settings = Ui.wrap(settingsHint(), "muted");
        Label error = Ui.wrap(detail == null ? "" : detail, "error-text");
        VBox box = Ui.card(14, Ui.logo(24), Ui.text("Kinnect can't reach the database", "h2"), hint, settings, error, retry);
        box.setMaxWidth(560);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane page = new StackPane(box);
        page.getStyleClass().add("page");
        return page;
    }

    /** Says which settings file is in use, or where Kinnect looked for one. */
    private static String settingsHint() {
        Path used = Db.settingsUsed();
        if (used != null && !Db.urlFromFile()) {
            return "Settings file: " + used + "\nIt has no db.url line, so Kinnect is using XAMPP on this "
                    + "computer. Check that the line starts exactly with db.url= and save the file.";
        }
        if (used != null) {
            return "Settings file: " + used;
        }
        List<Path> folders = Db.settingsFolders();
        for (Path folder : folders) {
            if (Files.exists(folder.resolve("kinnect.properties.txt"))) {
                return "There is a kinnect.properties.txt in " + folder + ". Windows added .txt to the name. "
                        + "Rename it to kinnect.properties (turn on View, Show, File name extensions to see it), "
                        + "then click Try again.";
            }
        }
        StringBuilder text = new StringBuilder("No kinnect.properties file was found, so Kinnect is using "
                + "XAMPP on this computer. To use another database, put kinnect.properties in one of these "
                + "folders, then click Try again:");
        for (Path folder : folders) {
            text.append('\n').append(folder);
        }
        return text.toString();
    }

    /**
     * Shows a screen. A screen can put a Runnable under "kinnect.onLeave" in its properties
     * to stop background work (like the message refresher) when it is replaced.
     */
    static void show(Parent page) {
        leave();
        closeModal();
        PAGE.getChildren().setAll(page);
        onLeave = (Runnable) page.getProperties().get("kinnect.onLeave");
    }

    private static void leave() {
        if (onLeave != null) {
            onLeave.run();
            onLeave = null;
        }
    }

    /** Opens the right home screen for whoever is logged in. */
    static void home() {
        User u = Session.user();
        if (u == null) {
            show(AuthScreens.welcome());
        } else if (Session.isProfessor()) {
            show(ProfessorScreens.myClasses());
        } else if (ClassService.studentClasses(u.id()).isEmpty()) {
            show(AuthScreens.joinClass(false));
        } else {
            show(StudentScreens.home());
        }
    }

    static void logOut() {
        AuthService.logOut();
        show(AuthScreens.welcome());
    }

    /** Opens a dialog; onClose runs once when it closes (by a button, Escape, or leaving the screen). */
    static void openModal(Region card, Runnable onClose) {
        openModal(card);
        onModalClose = onClose;
    }

    static void openModal(Region card) {
        onModalClose = null;
        card.getStyleClass().add("modal");
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane scrim = new StackPane(card);
        scrim.getStyleClass().add("modal-scrim");
        scrim.setPadding(new Insets(30));
        MODAL.getChildren().setAll(scrim);
        MODAL.setVisible(true);
        Platform.runLater(card::requestFocus);
    }

    static void closeModal() {
        Runnable r = onModalClose;
        onModalClose = null;
        MODAL.getChildren().clear();
        MODAL.setVisible(false);
        if (r != null) {
            r.run();
        }
    }

    static void toast(String message) {
        toast(message, false);
    }

    static void toast(String message, boolean error) {
        Label l = new Label(message);
        HBox box = new HBox(10, Ui.icon(error ? "alert" : "check", 16, Theme.ON_PINE), l);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("toast");
        if (error) {
            box.getStyleClass().add("toast-error");
        }
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        TOAST.getChildren().setAll(box);
        FadeTransition in = new FadeTransition(Duration.millis(160), box);
        in.setFromValue(0);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(Duration.millis(300), box);
        out.setToValue(0);
        SequentialTransition t = new SequentialTransition(in, new PauseTransition(Duration.seconds(2.6)), out);
        t.setOnFinished(e -> TOAST.getChildren().remove(box));
        t.play();
    }

    static void openExternal(String uri) {
        app.getHostServices().showDocument(uri);
    }

    static Stage stage() {
        return stage;
    }

    /**
     * The Kinnect logo for the title bar and the Windows taskbar, in several sizes so
     * Windows can pick a sharp one. The files are in src/main/resources/icons.
     */
    private static List<Image> appIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            try (InputStream in = KinnectApp.class.getResourceAsStream("/icons/kinnect-" + size + ".png")) {
                if (in != null) {
                    icons.add(new Image(in));
                }
            } catch (IOException ignored) {
                // a missing size is fine, Windows uses the others
            }
        }
        return icons;
    }
}
