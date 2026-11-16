package com.mycompany.mavenproject2;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.stream.Stream;
import javafx.scene.text.Font;

/**
 * Colors, fonts, and the style sheet from the Kinnect Figma design.
 * Bricolage Grotesque (headings) and Instrument Sans (text) are used when installed
 * or placed in a "fonts" folder inside the project folder; otherwise Kinnect falls back to Segoe UI.
 *
 * @author Ariel Soriano
 */
public final class Theme {

    static final String BG = "#F2F5F1";
    static final String SURFACE = "#FFFFFF";
    static final String BORDER = "#D7E0DA";
    static final String SOFT = "#E1ECE5";
    static final String PINE = "#1E6B52";
    static final String PINE_DARK = "#17573F";
    static final String GREEN = "#2E8A66";
    static final String AMBER = "#E3A23B";
    static final String AMBER_SOFT = "#FCEBC9";
    static final String AMBER_TEXT = "#8A5A0B";
    static final String GREY = "#9AA6B2";
    static final String GREY_SOFT = "#E6EAEE";
    static final String INK = "#15302A";
    static final String MUTED = "#556A63";
    static final String ON_PINE = "#F4FAF6";
    static final String ERROR = "#B3261E";
    static final String ERROR_SOFT = "#FBE3DD";
    static final String[] AVATARS = {"#F7D9CB", "#E1DAF4", "#CFE0F5", "#CDE8DA", "#FCEBC9"};

    static String display = "System";
    static String body = "System";

    private Theme() {
    }

    static void loadFonts() {
        String[] files = {"BricolageGrotesque-Bold.ttf", "BricolageGrotesque-SemiBold.ttf",
            "InstrumentSans-Regular.ttf", "InstrumentSans-Medium.ttf", "InstrumentSans-SemiBold.ttf",
            "InstrumentSans-Bold.ttf"};
        for (String f : files) {
            try (InputStream in = Theme.class.getResourceAsStream("/fonts/" + f)) {
                if (in != null) {
                    Font.loadFont(in, 13);
                }
            } catch (Exception ignored) {
                // the font is optional
            }
        }
        Path folder = Paths.get("fonts");
        if (Files.isDirectory(folder)) {
            try (Stream<Path> found = Files.list(folder)) {
                found.filter(f -> f.toString().toLowerCase().matches(".*\\.(ttf|otf)$"))
                        .forEach(f -> Font.loadFont(f.toUri().toString(), 13));
            } catch (IOException ignored) {
                // the fonts are optional
            }
        }
        var families = Font.getFamilies();
        String fallback = families.contains("Segoe UI") ? "Segoe UI" : "System";
        display = families.contains("Bricolage Grotesque") ? "Bricolage Grotesque" : fallback;
        body = families.contains("Instrument Sans") ? "Instrument Sans" : fallback;
    }

    static String stylesheet() {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css().getBytes(StandardCharsets.UTF_8));
    }

    private static String css() {
        return """
            .root {
              -fx-font-family: "%BODY%";
              -fx-font-size: 13px;
              -fx-text-fill: %INK%;
              -fx-background-color: %BG%;
              -fx-focus-color: %PINE%;
              -fx-faint-focus-color: transparent;
              -fx-accent: %PINE%;
            }
            .label { -fx-text-fill: %INK%; }
            .page { -fx-background-color: %BG%; }
            .display { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; }
            .h1 { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 32px; }
            .h1-auth { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 30px; }
            .h2 { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 18px; }
            .h3 { -fx-font-size: 15px; -fx-font-weight: bold; }
            .stat { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 44px; }
            .stat-md { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 32px; }
            .hero-title { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 40px;
                          -fx-text-fill: %ON_PINE%; }
            .hero-text { -fx-font-size: 16px; -fx-text-fill: #CFE3D8; -fx-line-spacing: 4px; }
            .strong { -fx-font-weight: bold; }
            .body { -fx-font-size: 14px; }
            .lead { -fx-font-size: 15px; -fx-text-fill: %MUTED%; -fx-line-spacing: 3px; }
            .muted { -fx-text-fill: %MUTED%; }
            .small { -fx-font-size: 12px; }
            .eyebrow { -fx-font-size: 12px; -fx-text-fill: %MUTED%; }
            .error-text { -fx-text-fill: %ERROR%; -fx-font-size: 12px; }
            .amber-text { -fx-text-fill: %AMBER_TEXT%; }
            .green-text { -fx-text-fill: %PINE%; }

            .card {
              -fx-background-color: %SURFACE%;
              -fx-background-radius: 16;
              -fx-border-color: %BORDER%;
              -fx-border-radius: 16;
              -fx-padding: 22;
            }
            .soft-box { -fx-background-color: %BG%; -fx-background-radius: 12; -fx-padding: 14; }
            .divider { -fx-background-color: %BORDER%; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1; }
            .row-line { -fx-border-color: %BORDER% transparent transparent transparent; -fx-border-width: 1 0 0 0; }

            .button { -fx-cursor: hand; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10;
                      -fx-border-radius: 10; -fx-padding: 10 20; }
            .btn-primary { -fx-background-color: %PINE%; -fx-text-fill: %ON_PINE%; }
            .btn-primary:hover { -fx-background-color: %PINE_DARK%; }
            .btn-primary:disabled { -fx-opacity: 0.45; }
            .btn-secondary { -fx-background-color: %SURFACE%; -fx-text-fill: %INK%; -fx-border-color: #B8C7BE; }
            .btn-secondary:hover { -fx-background-color: %BG%; }
            .btn-lg { -fx-font-size: 15px; -fx-padding: 12 22; }
            .btn-icon { -fx-background-color: %PINE%; -fx-padding: 10 12; }
            .btn-icon:hover { -fx-background-color: %PINE_DARK%; }
            .link { -fx-background-color: transparent; -fx-text-fill: %PINE%; -fx-font-size: 13px;
                    -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand; }
            .link:hover { -fx-underline: true; }
            .link-muted { -fx-background-color: transparent; -fx-text-fill: %MUTED%; -fx-font-size: 12px;
                          -fx-padding: 0; -fx-cursor: hand; }
            .link-muted:hover { -fx-underline: true; }

            .text-field, .password-field, .text-area .content, .date-picker > .text-field {
              -fx-background-color: %SURFACE%;
              -fx-background-radius: 10;
              -fx-font-size: 14px;
              -fx-text-fill: %INK%;
              -fx-prompt-text-fill: #8A9A93;
            }
            .text-field, .password-field {
              -fx-border-color: #C3D0C8; -fx-border-radius: 10; -fx-padding: 11 14;
            }
            .text-field:focused, .password-field:focused { -fx-border-color: %PINE%; }
            .text-area { -fx-background-color: %SURFACE%; -fx-border-color: #C3D0C8; -fx-border-radius: 10;
                         -fx-background-radius: 10; -fx-padding: 4; }
            .text-area:focused { -fx-border-color: %PINE%; }
            .text-area .scroll-pane, .text-area .viewport { -fx-background-color: transparent; }
            .text-area .content { -fx-padding: 6 8; }
            .invalid, .invalid:focused { -fx-border-color: %ERROR%; }
            .field-label { -fx-font-size: 13px; -fx-font-weight: bold; }
            .chat-input { -fx-background-color: %BG%; -fx-border-color: %BORDER%; }

            .combo-box, .date-picker {
              -fx-background-color: %SURFACE%; -fx-border-color: #C3D0C8; -fx-border-radius: 10;
              -fx-background-radius: 10; -fx-font-size: 14px; -fx-padding: 4 4;
            }
            .date-picker > .text-field { -fx-border-color: transparent; -fx-padding: 7 10; }
            .combo-box .list-cell { -fx-padding: 6 8; -fx-text-fill: %INK%; }
            .combo-box .arrow-button, .date-picker .arrow-button { -fx-background-color: transparent; }
            .combo-box .arrow, .date-picker .arrow { -fx-background-color: %MUTED%; }
            .combo-box-popup .list-view, .context-menu {
              -fx-background-color: %SURFACE%; -fx-background-radius: 12; -fx-border-color: %BORDER%;
              -fx-border-radius: 12; -fx-padding: 6;
              -fx-effect: dropshadow(gaussian, rgba(21,48,42,0.16), 22, 0.1, 0, 6);
            }
            .combo-box-popup .list-cell { -fx-padding: 8 10; -fx-background-radius: 8; }
            .combo-box-popup .list-cell:filled:hover, .combo-box-popup .list-cell:filled:selected {
              -fx-background-color: %SOFT%; -fx-text-fill: %INK%;
            }
            .menu-item { -fx-background-radius: 8; -fx-padding: 6 10; }
            .menu-item:focused { -fx-background-color: %SOFT%; }

            .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; -fx-background-insets: 0; }
            .scroll-pane { -fx-padding: 0; }
            .scroll-bar:vertical, .scroll-bar:horizontal { -fx-background-color: transparent; }
            .scroll-bar .thumb { -fx-background-color: #C8D3CC; -fx-background-radius: 6; }
            .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; -fx-opacity: 0; }
            .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; -fx-shape: ""; }

            .topbar { -fx-background-color: %SURFACE%; -fx-border-color: transparent transparent %BORDER% transparent;
                      -fx-padding: 14 34; }
            .sidebar { -fx-background-color: %SURFACE%; -fx-border-color: transparent %BORDER% transparent transparent;
                       -fx-padding: 34 20 22 20; }
            .group-box { -fx-background-color: %BG%; -fx-background-radius: 12; -fx-padding: 14 14; }
            .nav-item { -fx-background-color: transparent; -fx-alignment: center-left; -fx-text-fill: %MUTED%;
                        -fx-font-size: 14px; -fx-font-weight: normal; -fx-padding: 9 14; -fx-graphic-text-gap: 12; }
            .nav-item:hover { -fx-background-color: %BG%; }
            .nav-item.active { -fx-background-color: %SOFT%; -fx-text-fill: %INK%; -fx-font-weight: bold; }

            .pill { -fx-background-radius: 999; -fx-padding: 3 10; -fx-font-size: 12px; -fx-font-weight: bold; }
            .pill-green { -fx-background-color: #DCEBE2; -fx-text-fill: %PINE%; }
            .pill-amber { -fx-background-color: %AMBER_SOFT%; -fx-text-fill: %AMBER_TEXT%; }
            .pill-grey { -fx-background-color: %GREY_SOFT%; -fx-text-fill: #4E5B66; }
            .chip { -fx-background-color: #E6ECE8; -fx-text-fill: %MUTED%; -fx-background-radius: 999;
                    -fx-padding: 3 10; -fx-font-size: 12px; }

            .banner-error { -fx-background-color: %ERROR_SOFT%; -fx-background-radius: 10; -fx-padding: 12 14; }
            .banner-error .label { -fx-text-fill: %ERROR%; }
            .banner-info { -fx-background-color: %SOFT%; -fx-background-radius: 10; -fx-padding: 11 16; }

            .role-card { -fx-background-color: %SURFACE%; -fx-border-color: #C3D0C8; -fx-border-radius: 12;
                         -fx-background-radius: 12; -fx-padding: 14; -fx-cursor: hand; }
            .role-card.selected { -fx-background-color: %SOFT%; -fx-border-color: %PINE%; -fx-border-width: 1.5; }
            .pick-row { -fx-padding: 12 16; -fx-cursor: hand; -fx-background-color: %SURFACE%; }
            .pick-row:hover { -fx-background-color: #F6F8F5; }
            .pick-row.selected { -fx-background-color: %SOFT%; }
            .pick-list { -fx-border-color: %BORDER%; -fx-border-radius: 12; -fx-background-radius: 12;
                         -fx-background-color: %SURFACE%; }

            .date-tile { -fx-background-color: %BG%; -fx-background-radius: 8; -fx-border-color: %BORDER%;
                         -fx-border-radius: 8; -fx-padding: 4 0; -fx-min-width: 44; -fx-pref-width: 44; }
            .file-tile { -fx-background-color: %SOFT%; -fx-background-radius: 8; -fx-min-width: 38;
                         -fx-min-height: 38; -fx-pref-width: 38; -fx-pref-height: 38; }
            .file-tile .label { -fx-text-fill: %PINE%; -fx-font-size: 12px; -fx-font-weight: bold; }

            .drop-zone { -fx-background-color: transparent; -fx-border-color: #A7B9B0; -fx-border-style: segments(6, 5);
                         -fx-border-width: 1.5; -fx-border-radius: 12; -fx-padding: 18 20; -fx-cursor: hand; }
            .drop-zone.drag-over { -fx-background-color: %SOFT%; -fx-border-color: %PINE%; }

            .day-cell { -fx-background-color: %BG%; -fx-background-radius: 8; -fx-padding: 7 8; -fx-cursor: hand; }
            .day-cell:hover { -fx-background-color: #E8EEE9; }
            .day-cell.other { -fx-background-color: transparent; }
            .day-cell.selected { -fx-border-color: %PINE%; -fx-border-radius: 8; -fx-border-width: 1.5;
                                 -fx-background-color: #FBFCFA; }
            .day-chip { -fx-background-radius: 4; -fx-padding: 1 6; -fx-font-size: 11px; }
            .day-chip.task { -fx-background-color: #DCEBE2; -fx-text-fill: %PINE%; }
            .day-chip.event { -fx-background-color: %AMBER_SOFT%; -fx-text-fill: %AMBER_TEXT%; }
            .today-dot { -fx-background-color: %PINE%; -fx-background-radius: 999; -fx-text-fill: %ON_PINE%;
                         -fx-font-weight: bold; -fx-min-width: 22; -fx-min-height: 22; -fx-alignment: center; }
            .event-card { -fx-background-color: %AMBER_SOFT%; -fx-background-radius: 10; -fx-padding: 14 12; }

            .tab-btn { -fx-background-color: transparent; -fx-text-fill: %MUTED%; -fx-font-weight: normal;
                       -fx-padding: 6 2; -fx-background-radius: 0; -fx-border-color: transparent;
                       -fx-border-width: 0 0 2 0; -fx-border-radius: 0; }
            .tab-btn.active { -fx-text-fill: %INK%; -fx-font-weight: bold; -fx-border-color: %PINE%; }

            .modal-scrim { -fx-background-color: rgba(54, 92, 78, 0.42); }
            .modal { -fx-background-color: %SURFACE%; -fx-background-radius: 18; -fx-padding: 26;
                     -fx-effect: dropshadow(gaussian, rgba(21,48,42,0.25), 40, 0.1, 0, 12); }
            .code-box { -fx-background-color: %SOFT%; -fx-background-radius: 12; -fx-padding: 18; }
            .code-text { -fx-font-family: "%DISPLAY%"; -fx-font-weight: bold; -fx-font-size: 40px;
                         -fx-text-fill: %INK%; }
            .icon-circle { -fx-background-color: %SOFT%; -fx-background-radius: 999; }

            .toast { -fx-background-color: %INK%; -fx-background-radius: 12; -fx-padding: 12 18;
                     -fx-effect: dropshadow(gaussian, rgba(21,48,42,0.3), 20, 0.1, 0, 6); }
            .toast .label { -fx-text-fill: %ON_PINE%; -fx-font-size: 14px; }
            .toast-error { -fx-background-color: %ERROR%; }

            .check-box { -fx-cursor: hand; }
            .check-box .box { -fx-background-color: %SURFACE%; -fx-border-color: #A7B9B0; -fx-border-radius: 5;
                              -fx-background-radius: 5; -fx-padding: 3; }
            .check-box:selected .box { -fx-background-color: %PINE%; -fx-border-color: %PINE%; }
            .check-box:selected .mark { -fx-background-color: %ON_PINE%; }
            .radio-button .radio { -fx-background-color: %SURFACE%; -fx-border-color: #A7B9B0; -fx-border-radius: 999;
                                   -fx-background-radius: 999; -fx-padding: 4; }
            .radio-button:selected .radio { -fx-border-color: %PINE%; -fx-border-width: 1.5; }
            .radio-button:selected .dot { -fx-background-color: %PINE%; -fx-background-radius: 999; }

            .tooltip { -fx-background-color: %INK%; -fx-text-fill: %ON_PINE%; -fx-font-size: 12px;
                       -fx-background-radius: 8; }
            """
                .replace("%BODY%", body).replace("%DISPLAY%", display)
                .replace("%BG%", BG).replace("%SURFACE%", SURFACE).replace("%BORDER%", BORDER)
                .replace("%SOFT%", SOFT).replace("%PINE_DARK%", PINE_DARK).replace("%PINE%", PINE)
                .replace("%AMBER_SOFT%", AMBER_SOFT).replace("%AMBER_TEXT%", AMBER_TEXT)
                .replace("%GREY_SOFT%", GREY_SOFT).replace("%INK%", INK).replace("%MUTED%", MUTED)
                .replace("%ON_PINE%", ON_PINE).replace("%ERROR_SOFT%", ERROR_SOFT).replace("%ERROR%", ERROR);
    }
}
