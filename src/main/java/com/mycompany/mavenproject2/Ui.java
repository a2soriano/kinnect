package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AppException;
import com.mycompany.mavenproject2.Models.Progress;
import com.mycompany.mavenproject2.Models.TaskStatus;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.StringConverter;

/**
 * Small building blocks shared by every screen: text, buttons, cards, icons, avatars,
 * status pills, progress bars, and date and money formats.
 *
 * @author Ariel Soriano
 */
public final class Ui {

    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    static final DateTimeFormatter LONG_DAY = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH);
    static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("grid", "M4 4h6v6H4z M14 4h6v6h-6z M4 14h6v6H4z M14 14h6v6h-6z"),
            Map.entry("tasks", "M10 6h10 M10 12h10 M10 18h10 M4 6h2 M4 12h2 M4 18h2"),
            Map.entry("folder", "M3 7a2 2 0 0 1 2 -2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2z"),
            Map.entry("calendar", "M5 5h14a1 1 0 0 1 1 1v13a1 1 0 0 1 -1 1H5a1 1 0 0 1 -1 -1V6a1 1 0 0 1 1 -1z M4 10h16 M8 3v4 M16 3v4"),
            Map.entry("message", "M5 5h14a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1H10l-4 3v-3H5a1 1 0 0 1 -1 -1V6a1 1 0 0 1 1 -1z"),
            Map.entry("doc", "M7 3h7l5 5v12a1 1 0 0 1 -1 1H7a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1z M14 3v5h5 M9 13h6 M9 17h6"),
            Map.entry("chart", "M4 20h16 M7 16v-4 M12 16V8 M17 16v-6"),
            Map.entry("wallet", "M4 8a2 2 0 0 1 2 -2h12a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2H6a2 2 0 0 1 -2 -2z M4 10h16 M15 15h2"),
            Map.entry("back", "M15 6l-6 6l6 6"),
            Map.entry("next", "M9 6l6 6l-6 6"),
            Map.entry("down", "M6 9l6 6l6 -6"),
            Map.entry("logout", "M10 5H6a1 1 0 0 0 -1 1v12a1 1 0 0 0 1 1h4 M14 8l4 4l-4 4 M18 12H9"),
            Map.entry("send", "M21 3L10 14 M21 3l-7 18l-4 -7l-7 -4z"),
            Map.entry("upload", "M12 15V4 M7 9l5 -5l5 5 M5 15v3a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-3"),
            Map.entry("download", "M12 4v11 M7 10l5 5l5 -5 M5 19h14"),
            Map.entry("link", "M10 14a4 4 0 0 0 5.66 0l3 -3a4 4 0 0 0 -5.66 -5.66l-1 1 M14 10a4 4 0 0 0 -5.66 0l-3 3a4 4 0 0 0 5.66 5.66l1 -1"),
            Map.entry("lock", "M6 11h12v9H6z M8 11V8a4 4 0 0 1 8 0v3"),
            Map.entry("eye", "M2 12c2 -4 5.5 -7 10 -7c4.5 0 8 3 10 7c-2 4 -5.5 7 -10 7c-4.5 0 -8 -3 -10 -7z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0 -6z"),
            Map.entry("check", "M5 12l5 5l9 -10"),
            Map.entry("users", "M9 11a3 3 0 1 0 0 -6a3 3 0 1 0 0 6z M3 20v-1a5 5 0 0 1 5 -5h2a5 5 0 0 1 5 5v1 M16 5a3 3 0 0 1 0 6 M18 14a5 5 0 0 1 3 5v1"),
            Map.entry("alert", "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0 -18z M12 8v5 M12 16v0.5"),
            Map.entry("cap", "M2 9l10 -5l10 5l-10 5z M6 11v5c3 2 9 2 12 0v-5"),
            Map.entry("user", "M12 11a4 4 0 1 0 0 -8a4 4 0 1 0 0 8z M4 21a8 8 0 0 1 16 0"),
            Map.entry("plus", "M12 5v14 M5 12h14"),
            Map.entry("edit", "M4 20h4l11 -11l-4 -4l-11 11z M13.5 6.5l4 4"),
            Map.entry("x", "M6 6l12 12 M18 6L6 18"));

    private Ui() {
    }

    // ---------- text ----------

    static Label text(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        return l;
    }

    static Label wrap(String text, String... classes) {
        Label l = text(text, classes);
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    // ---------- buttons ----------

    static Button primary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    static Button secondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-secondary");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    static Button link(String text) {
        Button b = new Button(text);
        b.getStyleClass().setAll("button", "link");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    static Button linkMuted(String text) {
        Button b = new Button(text);
        b.getStyleClass().setAll("button", "link-muted");
        return b;
    }

    static Button wide(Button b) {
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    // ---------- layout ----------

    static VBox card(double spacing, Node... children) {
        VBox v = new VBox(spacing, children);
        v.getStyleClass().add("card");
        return v;
    }

    static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    static Region divider() {
        Region r = new Region();
        r.getStyleClass().add("divider");
        return r;
    }

    static HBox row(double spacing, Node... children) {
        HBox h = new HBox(spacing, children);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    /** Card header: title on the left, a small note or link on the right. */
    static HBox cardHeader(String title, Node right) {
        HBox h = row(8, text(title, "h2"), grow());
        if (right != null) {
            h.getChildren().add(right);
        }
        return h;
    }

    static ScrollPane scroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    /** Columns that share the width by percentage. */
    static GridPane columns(double gap, double... percents) {
        GridPane g = new GridPane();
        g.setHgap(gap);
        g.setVgap(gap);
        for (double p : percents) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(p);
            c.setHgrow(Priority.ALWAYS);
            c.setFillWidth(true);
            g.getColumnConstraints().add(c);
        }
        return g;
    }

    /**
     * Two cards side by side when there is room (wider than breakpoint), otherwise one above the other.
     * Keeps wide tables readable on a smaller window.
     */
    static StackPane sideBySide(Region left, Region right, double leftPercent, double breakpoint) {
        StackPane holder = new StackPane();
        boolean[] wide = {false};
        Runnable layout = () -> {
            boolean w = holder.getWidth() >= breakpoint;
            if (w == wide[0] && !holder.getChildren().isEmpty()) {
                return;
            }
            wide[0] = w;
            if (w) {
                GridPane g = columns(22, leftPercent, 100 - leftPercent);
                g.add(left, 0, 0);
                g.add(right, 1, 0);
                GridPane.setValignment(left, javafx.geometry.VPos.TOP);
                GridPane.setValignment(right, javafx.geometry.VPos.TOP);
                holder.getChildren().setAll(g);
            } else {
                VBox v = new VBox(22, left, right);
                holder.getChildren().setAll(v);
            }
        };
        holder.widthProperty().addListener((o, a, b) -> javafx.application.Platform.runLater(layout));
        layout.run();
        holder.setMinWidth(0);
        return holder;
    }

    /** Page heading: eyebrow (breadcrumb), big title, subtitle, and buttons on the right. */
    static HBox pageHeader(Node eyebrow, String title, String subtitle, Node... actions) {
        VBox left = new VBox(4);
        if (eyebrow != null) {
            left.getChildren().add(eyebrow);
        }
        left.getChildren().add(wrap(title, "h1"));
        if (subtitle != null) {
            left.getChildren().add(wrap(subtitle, "lead"));
        }
        HBox right = row(12, actions);
        right.setAlignment(Pos.BOTTOM_RIGHT);
        right.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        HBox h = new HBox(24, left, right);
        h.setAlignment(Pos.BOTTOM_LEFT);
        return h;
    }

    /** "Software Design, Section B / Group 3", where earlier parts can be clicked. */
    static HBox breadcrumb(Object... parts) {
        HBox h = row(6);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                h.getChildren().add(text("/", "eyebrow"));
            }
            if (parts[i] instanceof Button b) {
                b.getStyleClass().setAll("button", "link-muted");
                h.getChildren().add(b);
            } else {
                h.getChildren().add(text(String.valueOf(parts[i]), "eyebrow"));
            }
        }
        return h;
    }

    /** Screens without a sidebar (class lists): white top bar with the user and Log out. */
    static javafx.scene.layout.BorderPane topLayout(Node content) {
        var u = Session.user();
        Button logout = new Button("Log out", icon("logout", 16, Theme.MUTED));
        logout.getStyleClass().setAll("button", "link-muted");
        logout.setStyle("-fx-font-size: 13px; -fx-graphic-text-gap: 8;");
        logout.setOnAction(e -> KinnectApp.logOut());
        Region sep = new Region();
        sep.setStyle("-fx-background-color: " + Theme.BORDER + ";");
        sep.setPrefSize(1, 26);
        HBox bar = row(14, logo(20), grow(),
                person(u.fullName(), Session.isProfessor() ? "Professor" : "Student", 32), sep, logout);
        bar.getStyleClass().add("topbar");
        VBox inner = new VBox(content);
        inner.setPadding(new Insets(34, 0, 40, 0));
        inner.setMaxWidth(1200);
        StackPane center = new StackPane(inner);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(0, 56, 0, 56));
        javafx.scene.layout.BorderPane page = new javafx.scene.layout.BorderPane(scroll(center));
        page.setTop(bar);
        page.getStyleClass().add("page");
        return page;
    }

    // ---------- icons, logo, avatars ----------

    static Node icon(String name, double size, String color) {
        SVGPath p = new SVGPath();
        p.setContent(ICONS.getOrDefault(name, ICONS.get("x")));
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(color));
        double scale = size / 24.0;
        p.setStrokeWidth(1.75 / scale);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.setScaleX(scale);
        p.setScaleY(scale);
        StackPane box = new StackPane(new Group(p));
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        box.setMouseTransparent(true);
        return box;
    }

    // The Kinnect logo: two equal rounded rings, pine on the left and amber in front on the right.
    // Proportions measured from kinnect.ico, as fractions of the logo's height.
    static final double RING_WIDTH = 1.162;
    static final double RING_OFFSET = 0.603;
    static final double RING_STROKE = 0.169;
    static final double LOGO_WIDTH = RING_OFFSET + RING_WIDTH;

    /** One rounded ring of the logo, h tall, with its left edge at x and top at y. */
    static Rectangle ring(double x, double y, double h, String color) {
        double sw = h * RING_STROKE;
        Rectangle r = new Rectangle(x + sw / 2, y + sw / 2, h * RING_WIDTH - sw, h - sw);
        r.setArcWidth(h - sw);
        r.setArcHeight(h - sw);
        r.setFill(Color.TRANSPARENT);
        r.setStroke(Color.web(color));
        r.setStrokeWidth(sw);
        return r;
    }

    /** The Kinnect logo mark, h pixels tall. */
    static Pane mark(double h) {
        double w = h * LOGO_WIDTH;
        Pane p = new Pane(ring(0, 0, h, Theme.PINE), ring(h * RING_OFFSET, 0, h, Theme.AMBER));
        p.setMinSize(w, h);
        p.setPrefSize(w, h);
        p.setMaxSize(w, h);
        return p;
    }

    static HBox logo(double size) {
        Label name = text("Kinnect", "display");
        name.setStyle("-fx-font-size: " + size + "px;");
        HBox h = row(size * 0.35, mark(size * 0.78), name);
        return h;
    }

    static String initials(String name) {
        String clean = name.replace("Prof.", "").trim();
        String[] parts = clean.split("\\s+");
        String a = parts[0].isEmpty() ? "?" : parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    /** "Mika Reyes" becomes "Mika R." */
    static String shortName(String name) {
        if (name == null) {
            return "";
        }
        if (name.startsWith("Prof.")) {
            return name;
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length < 2 ? name : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    static StackPane avatar(String name, double size) {
        Circle c = new Circle(size / 2, Color.web(Theme.AVATARS[Math.floorMod(name.hashCode(), Theme.AVATARS.length)]));
        Label l = new Label(initials(name));
        l.setStyle("-fx-font-size: " + Math.round(size * 0.38) + "px; -fx-text-fill: " + Theme.INK + ";");
        StackPane s = new StackPane(c, l);
        s.setMinSize(size, size);
        s.setMaxSize(size, size);
        return s;
    }

    static HBox avatarStack(List<String> names, double size) {
        HBox h = new HBox(-size * 0.3);
        h.setAlignment(Pos.CENTER_LEFT);
        for (String n : names) {
            StackPane a = avatar(n, size);
            Circle ring = new Circle(size / 2 + 1.5, Color.WHITE);
            StackPane s = new StackPane(ring, a);
            h.getChildren().add(s);
        }
        return h;
    }

    static HBox person(String name, String detail, double size) {
        VBox v = new VBox(1, text(name, "strong"));
        if (detail != null) {
            v.getChildren().add(text(detail, "small", "muted"));
        }
        return row(10, avatar(name, size), v);
    }

    static StackPane iconCircle(String icon, double size) {
        StackPane s = new StackPane(icon(icon, size * 0.42, Theme.PINE));
        s.getStyleClass().add("icon-circle");
        s.setMinSize(size, size);
        s.setMaxSize(size, size);
        return s;
    }

    // ---------- pills and progress ----------

    static Label pill(String text, String kind) {
        Circle dot = new Circle(2.5, Color.web(switch (kind) {
            case "green" -> Theme.GREEN;
            case "amber" -> Theme.AMBER;
            default -> Theme.GREY;
        }));
        Label l = new Label(text, dot);
        l.setGraphicTextGap(6);
        l.getStyleClass().addAll("pill", "pill-" + kind);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    static Label statusPill(TaskStatus s) {
        return pill(s.label(), kind(s));
    }

    static String kind(TaskStatus s) {
        return switch (s) {
            case COMPLETED -> "green";
            case ONGOING -> "amber";
            case PENDING -> "grey";
        };
    }

    static Label chip(String text) {
        Label l = text(text, "chip");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** The three-color bar: completed, ongoing, pending. */
    static Node segmentBar(Progress p, double height) {
        if (p.total() == 0) {
            return track(0, height);
        }
        GridPane g = new GridPane();
        g.setHgap(4);
        int[] counts = {p.completed(), p.ongoing(), p.pending()};
        String[] colors = {Theme.GREEN, Theme.AMBER, Theme.GREY};
        int col = 0;
        for (int i = 0; i < 3; i++) {
            if (counts[i] == 0) {
                continue;
            }
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(counts[i] * 100.0 / p.total());
            g.getColumnConstraints().add(cc);
            Region r = new Region();
            r.setMinHeight(height);
            r.setMaxWidth(Double.MAX_VALUE);
            r.setStyle("-fx-background-color: " + colors[i] + "; -fx-background-radius: 999;");
            g.add(r, col++, 0);
        }
        return g;
    }

    static HBox legend(Progress p) {
        return row(18, legendItem(p.completed() + " completed", Theme.GREEN),
                legendItem(p.ongoing() + " ongoing", Theme.AMBER),
                legendItem(p.pending() + " pending", Theme.GREY));
    }

    static Label legendItem(String text, String color) {
        Label l = new Label(text, new Circle(3.5, Color.web(color)));
        l.setGraphicTextGap(7);
        l.getStyleClass().add("small");
        l.setStyle("-fx-text-fill: " + Theme.MUTED + ";");
        return l;
    }

    /** A thin bar filled to the given percent. */
    static StackPane track(double percent, double height) {
        Region bg = new Region();
        bg.setStyle("-fx-background-color: " + Theme.SOFT + "; -fx-background-radius: 999;");
        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + Theme.PINE + "; -fx-background-radius: 999;");
        StackPane s = new StackPane(bg, fill);
        s.setAlignment(Pos.CENTER_LEFT);
        s.setMinHeight(height);
        s.setPrefHeight(height);
        s.setMaxHeight(height);
        double pct = Math.max(0, Math.min(100, percent)) / 100.0;
        fill.maxWidthProperty().bind(Bindings.multiply(s.widthProperty(), pct));
        fill.setVisible(pct > 0);
        return s;
    }

    // ---------- forms ----------

    static VBox field(String label, Node control) {
        VBox v = new VBox(7, text(label, "field-label"), control);
        if (control instanceof Region r) {
            r.setMaxWidth(Double.MAX_VALUE);
        }
        return v;
    }

    static HBox errorBanner() {
        Label l = wrap("", "small");
        HBox h = row(10, icon("alert", 16, Theme.ERROR), l);
        h.getStyleClass().add("banner-error");
        h.setAlignment(Pos.TOP_LEFT);
        h.setVisible(false);
        h.setManaged(false);
        HBox.setHgrow(l, Priority.ALWAYS);
        return h;
    }

    static void showError(HBox banner, String message) {
        ((Label) banner.getChildren().get(1)).setText(message);
        banner.setVisible(message != null);
        banner.setManaged(message != null);
    }

    static String message(RuntimeException e) {
        if (e instanceof AppException) {
            return e.getMessage();
        }
        return "Something went wrong with the database. " + e.getMessage();
    }

    static <T> StringConverter<T> converter(Function<T, String> f) {
        return new StringConverter<>() {
            @Override
            public String toString(T t) {
                return t == null ? "" : f.apply(t);
            }

            @Override
            public T fromString(String s) {
                return null;
            }
        };
    }

    // ---------- empty states ----------

    static VBox emptyState(String icon, String title, String text) {
        Label t = text(title, "h3");
        Label d = wrap(text, "muted");
        d.setMaxWidth(420);
        d.setStyle("-fx-text-alignment: center;");
        VBox v = new VBox(10, iconCircle(icon, 46), t, d);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(34, 20, 34, 20));
        return v;
    }

    // ---------- formats ----------

    static String day(LocalDate d) {
        return d == null ? "" : DAY.format(d);
    }

    static String day(LocalDateTime d) {
        return d == null ? "" : DAY.format(d);
    }

    static String time(LocalDateTime t) {
        return TIME.format(t);
    }

    /** "3:00 to 4:00 PM" or "11:30 AM to 1:00 PM". */
    static String timeRange(LocalDateTime start, LocalDateTime end) {
        String a = TIME.format(start);
        String b = TIME.format(end);
        if (start.getHour() < 12 == end.getHour() < 12 && start.toLocalDate().equals(end.toLocalDate())) {
            a = a.substring(0, a.length() - 3);
        }
        return a + " to " + b;
    }

    static String peso(BigDecimal amount) {
        DecimalFormat f = amount.stripTrailingZeros().scale() <= 0 ? new DecimalFormat("#,##0")
                : new DecimalFormat("#,##0.00");
        return "₱" + f.format(amount);
    }

    static String size(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return Math.round(bytes / 1024.0) + " KB";
        }
        return new DecimalFormat("0.0").format(bytes / 1024.0 / 1024.0) + " MB";
    }

    static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
