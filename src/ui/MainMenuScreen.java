package ui;

import core.constants.Config;
import core.logic.Connect4AI;
import core.model.PlayerProfile;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.SoundManager;
import util.UiHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * MainMenuScreen — Ana menü ekranı.
 *
 * <p>Oyuncu adı girişi, zorluk/mod seçimi (2 kişi / Kolay / Orta / Zor / Uzman)
 * ve diğer ekranlara (ağ oyunu, ödüller, ayarlar, geçmiş) yönlendirmeyi içerir.
 * Tema değiştirme düğmesi ve dekoratif animasyonlar da burada bulunur.
 */
public class MainMenuScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;
    private final List<Label> diffButtons = new ArrayList<>(); // Zorluk butonları
    private int selectedDifficulty = Connect4AI.TWO_PLAYER;     // Seçili mod
    private TextField nameField;   // Oyuncu 1 adı
    private TextField nameField2;  // Oyuncu 2 adı (yalnız 2 kişilik modda)
    private VBox p2Box;
    private Label title;
    private Label levelBadge;
    private Label diffLbl;
    private Label footer;
    private Label settings;
    private Label themeToggle;
    private VBox menuBox;

    public MainMenuScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        menuBox = buildMenu();
        themeToggle = buildThemeToggle();
        StackPane.setAlignment(themeToggle, Pos.TOP_RIGHT); // Sağ üst köşe
        StackPane.setMargin(themeToggle, new Insets(18, 22, 0, 0));
        getChildren().addAll(bg, menuBox, themeToggle);
        bindResponsiveMenu(menuBox);
        updateBackgroundMode();
        animateIn(menuBox);
    }

    /** Tüm menü içeriğini oluşturur. */
    private VBox buildMenu() {
        PlayerProfile profile = app.getProfile();

        // Parıldayan başlık
        title = new Label("CONNECT 4");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 58));
        title.setTextFill(Config.COLOR_TEXT);
        DropShadow glow = new DropShadow();
        glow.setColor(Config.COLOR_GLOW_BLUE);
        glow.setRadius(24);
        title.setEffect(glow);
        UiHelper.pulseGlow(glow);

        HBox discs = buildAnimatedDiscs(); // Zıplayan dekoratif diskler

        // Seviye/XP rozeti
        levelBadge = new Label("Seviye " + profile.getLevel() + "  •  " + profile.getXp() + " XP");
        levelBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        levelBadge.setTextFill(Config.COLOR_GLOW_BLUE);

        // İsim giriş alanları
        nameField = new TextField(profile.getPlayerName());
        nameField.setPromptText("Adını gir...");
        nameField2 = new TextField("Oyuncu 2");
        nameField2.setPromptText("İkinci oyuncu adı...");

        VBox p1Box = buildPlayerFieldBox("OYUNCU ADI", nameField, Config.COLOR_PLAYER_1);
        p2Box = buildPlayerFieldBox("OYUNCU 2 ADI", nameField2, Config.COLOR_PLAYER_2);

        diffLbl = new Label("OYUN MODU");
        diffLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        diffLbl.setTextFill(Config.COLOR_TEXT_DIM);

        // Mod/zorluk seçim butonları
        HBox diffRow = new HBox(8,
            diffBtn("2 Kişi", Connect4AI.TWO_PLAYER, Color.web("#78909c")),
            diffBtn("Kolay", Connect4AI.EASY, Color.web("#00c853")),
            diffBtn("Orta", Connect4AI.MEDIUM, Color.web("#ffd600")),
            diffBtn("Zor", Connect4AI.HARD, Color.web("#ff5722")),
            diffBtn("Uzman", Connect4AI.EXPERT, Color.web("#d500f9"))
        );
        diffRow.setAlignment(Pos.CENTER);
        highlightDiff(Connect4AI.TWO_PLAYER);

        // Ana eylem butonları
        Label play = UiHelper.menuButton("▶  OYNA", Config.COLOR_GLOW_BLUE);
        play.prefWidthProperty().bind(widthProperty().multiply(0.38));
        play.setOnMouseClicked(e -> startGame());

        Label network = UiHelper.menuButton("🌐  Ağ Oyunu (2 Bilgisayar)", Color.web("#00bcd4"));
        network.setTextFill(Config.onAccentText());
        network.prefWidthProperty().bind(widthProperty().multiply(0.38));
        network.setOnMouseClicked(e -> transition(() -> app.showNetworkLobby()));

        Label rewards = UiHelper.menuButton("🏆  Ödüller & Kupalar", Config.COLOR_ACCENT);
        rewards.prefWidthProperty().bind(widthProperty().multiply(0.38));
        rewards.setOnMouseClicked(e -> transition(() -> app.showRewards()));

        settings = UiHelper.menuButton("⚙  Ayarlar",
            Config.lightMode ? Color.web("#cfd8dc") : Color.web("#37474f").deriveColor(0, 1, 1, 1));
        settings.setTextFill(Config.lightMode ? Config.COLOR_TEXT : Color.WHITE);
        settings.prefWidthProperty().bind(widthProperty().multiply(0.38));
        settings.setOnMouseClicked(e -> transition(() -> app.showSettings()));

        Label history = UiHelper.backLink("📋  Maç Geçmişi", () -> transition(() -> app.showHistory()));

        footer = new Label(Config.TEAM_CREDITS);
        footer.setFont(Font.font("Segoe UI", 11));
        footer.setTextFill(Config.COLOR_TEXT_DIM);

        VBox menu = new VBox(16, title, discs, levelBadge, p1Box, p2Box,
            diffLbl, diffRow, play, network, rewards, settings, history, footer);
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(36));
        menu.setOpacity(0); // Giriş animasyonu için
        return menu;
    }

    /** Tek bir zorluk/mod butonu üretir; tıklanınca seçimi ve 2. oyuncu alanını günceller. */
    private Label diffBtn(String text, int level, Color color) {
        Label btn = new Label(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        btn.setPrefWidth(78);
        btn.setPrefHeight(34);
        btn.setAlignment(Pos.CENTER);
        btn.setTextFill(color);
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setUserData(level); // Butona zorluk değerini iliştir
        btn.setBackground(new Background(new BackgroundFill(
            color.deriveColor(0, 1, 1, 0.12), new CornerRadii(8), Insets.EMPTY)));
        btn.setBorder(new Border(new BorderStroke(
            color.deriveColor(0, 1, 1, 0.4), BorderStrokeStyle.SOLID,
            new CornerRadii(8), new BorderWidths(1.2))));
        btn.setOnMouseClicked(e -> {
            selectedDifficulty = level;
            highlightDiff(level);
            // 2. oyuncu adı alanı yalnız "2 Kişi" modunda görünür
            p2Box.setVisible(level == Connect4AI.TWO_PLAYER);
            p2Box.setManaged(level == Connect4AI.TWO_PLAYER);
            SoundManager.play(SoundManager.Sound.CLICK);
        });
        diffButtons.add(btn);
        return btn;
    }

    /** Seçili zorluk butonunu vurgular (zeminini koyulaştırır). */
    private void highlightDiff(int level) {
        for (Label btn : diffButtons) {
            int lv = (int) btn.getUserData();
            Color color = diffColor(lv);
            boolean selected = lv == level;
            btn.setBackground(new Background(new BackgroundFill(
                selected ? color.deriveColor(0, 1, 1, 0.38) : color.deriveColor(0, 1, 1, 0.1),
                new CornerRadii(8), Insets.EMPTY)));
        }
    }

    /** Her zorluk seviyesinin temsil rengini döndürür. */
    private Color diffColor(int level) {
        return switch (level) {
            case Connect4AI.EASY -> Color.web("#00c853");
            case Connect4AI.MEDIUM -> Color.web("#ffd600");
            case Connect4AI.HARD -> Color.web("#ff5722");
            case Connect4AI.EXPERT -> Color.web("#d500f9");
            default -> Color.web("#78909c");
        };
    }

    /** "OYNA" butonuna basınca: isimleri ve modu hazırlayıp oyun ekranına geçer. */
    private void startGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Oyuncu";
        app.getProfile().setPlayerName(name);
        app.saveProfile();

        // 2. oyuncu adı: AI modlarında AI etiketi, 2 kişilikte girilen ad
        final String p2 = switch (selectedDifficulty) {
            case Connect4AI.EASY -> "AI (Kolay)";
            case Connect4AI.MEDIUM -> "AI (Orta)";
            case Connect4AI.HARD -> "AI (Zor)";
            case Connect4AI.EXPERT -> "AI (Uzman)";
            default -> {
                String n2 = nameField2.getText().trim();
                yield n2.isEmpty() ? "Oyuncu 2" : n2;
            }
        };

        final String p1 = name;
        final int diff = selectedDifficulty;
        transition(() -> app.showGame(p1, p2, diff));
    }

    /** Solma animasyonuyla başka bir ekrana geçiş yapar. */
    private void transition(Runnable next) {
        SoundManager.play(SoundManager.Sound.MENU);
        FadeTransition fade = new FadeTransition(Duration.millis(350), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            bg.stop();
            next.run();
        });
        fade.play();
    }

    /** Sağ üstteki açık/koyu tema değiştirme düğmesini üretir. */
    private Label buildThemeToggle() {
        Label btn = new Label(Config.lightMode ? "🌙  Koyu Tema" : "☀  Açık Tema");
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btn.setTextFill(Config.lightMode ? Config.COLOR_TEXT : Color.WHITE);
        btn.setPadding(new Insets(8, 16, 8, 16));
        btn.setCursor(javafx.scene.Cursor.HAND);
        btn.setBackground(new Background(new BackgroundFill(
            Config.lightMode ? Color.web("#ffffffcc") : Color.web("#ffffff18"),
            new CornerRadii(20), Insets.EMPTY)));
        btn.setBorder(new Border(new BorderStroke(
            Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, Config.lightMode ? 0.5 : 0.35),
            BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(1.2))));
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.04);
            btn.setScaleY(1.04);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        btn.setOnMouseClicked(e -> {
            SoundManager.play(SoundManager.Sound.CLICK);
            app.toggleDisplayMode();   // Modu değiştir + kaydet
            updateBackgroundMode();    // Arka plan paletini tazele
            applyDisplayMode();        // Menü renklerini güncelle
        });
        return btn;
    }

    /** Arka plan animasyonunu yeni tema/mod paletiyle yeniden tohumlar. */
    private void updateBackgroundMode() {
        bg.setVisible(true);
        bg.setManaged(true);
        bg.refreshPalette();
        bg.start();
    }

    /** Mod değişiminden sonra menüdeki tüm renkleri günceller. */
    private void applyDisplayMode() {
        title.setTextFill(Config.COLOR_TEXT);
        levelBadge.setTextFill(Config.COLOR_GLOW_BLUE);
        diffLbl.setTextFill(Config.COLOR_TEXT_DIM);
        footer.setTextFill(Config.COLOR_TEXT_DIM);
        nameField.setStyle(fieldStyle(Config.COLOR_PLAYER_1));
        nameField2.setStyle(fieldStyle(Config.COLOR_PLAYER_2));
        settings.setBackground(new Background(new BackgroundFill(
            Config.lightMode ? Color.web("#cfd8dc") : Color.web("#37474f").deriveColor(0, 1, 1, 1),
            new CornerRadii(10), Insets.EMPTY)));
        settings.setTextFill(Config.lightMode ? Config.COLOR_TEXT : Color.WHITE);
        themeToggle.setText(Config.lightMode ? "🌙  Koyu Tema" : "☀  Açık Tema");
        themeToggle.setTextFill(Config.lightMode ? Config.COLOR_TEXT : Color.WHITE);
        themeToggle.setBackground(new Background(new BackgroundFill(
            Config.lightMode ? Color.web("#ffffffcc") : Color.web("#ffffff18"),
            new CornerRadii(20), Insets.EMPTY)));
    }

    /** Başlığın altında zıplayan ve nabız atan 7 dekoratif disk üretir. */
    private HBox buildAnimatedDiscs() {
        HBox discs = new HBox(10);
        discs.setAlignment(Pos.CENTER);
        for (int i = 0; i < 7; i++) {
            Color c = i % 2 == 0 ? Config.COLOR_PLAYER_1 : Config.COLOR_PLAYER_2;
            Circle d = new Circle(12, c);
            DropShadow discGlow = new DropShadow();
            discGlow.setColor(c.deriveColor(0, 1, 1, 0.55));
            discGlow.setRadius(8);
            d.setEffect(discGlow);
            discs.getChildren().add(d);

            int delay = i * 110; // Her disk biraz gecikmeli (dalga etkisi)
            TranslateTransition bounce = new TranslateTransition(Duration.millis(520), d);
            bounce.setFromY(0);
            bounce.setToY(-16);
            bounce.setInterpolator(Interpolator.EASE_BOTH);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(TranslateTransition.INDEFINITE);
            bounce.setDelay(Duration.millis(delay));

            ScaleTransition pulse = new ScaleTransition(Duration.millis(520), d);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.12);
            pulse.setToY(1.12);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(ScaleTransition.INDEFINITE);
            pulse.setDelay(Duration.millis(delay));

            new ParallelTransition(bounce, pulse).play();
        }
        return discs;
    }

    /** Menüyü pencere boyutuna göre ölçekleyen duyarlı davranışı bağlar. */
    private void bindResponsiveMenu(VBox menu) {
        menu.maxWidthProperty().bind(widthProperty().multiply(0.92));
        widthProperty().addListener((obs, oldW, newW) -> {
            double scale = UiHelper.responsiveScale(newW.doubleValue(), getHeight());
            menu.setScaleX(scale);
            menu.setScaleY(scale);
        });
        heightProperty().addListener((obs, oldH, newH) -> {
            double scale = UiHelper.responsiveScale(getWidth(), newH.doubleValue());
            menu.setScaleX(scale);
            menu.setScaleY(scale);
        });
    }

    /** Etiket + giriş alanından oluşan bir oyuncu adı kutusu üretir. */
    private VBox buildPlayerFieldBox(String labelText, TextField field, Color accent) {
        Label lbl = new Label(labelText);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(accent);
        lbl.setMaxWidth(Double.MAX_VALUE);

        field.setMaxWidth(Double.MAX_VALUE);
        field.setPrefHeight(44);
        field.setStyle(fieldStyle(accent));

        VBox box = new VBox(6, lbl, field);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(340);
        box.setMinWidth(220);
        box.prefWidthProperty().bind(widthProperty().multiply(0.38));
        lbl.prefWidthProperty().bind(box.widthProperty());
        field.prefWidthProperty().bind(box.widthProperty());
        return box;
    }

    /** Giriş alanları için (vurgu rengine göre) CSS stil dizesi üretir. */
    private String fieldStyle(Color accent) {
        String border = String.format("#%02x%02x%02x88",
            (int) (accent.getRed() * 255),
            (int) (accent.getGreen() * 255),
            (int) (accent.getBlue() * 255));
        String bg = Config.lightMode ? "rgba(0,0,0,0.04)" : "rgba(255,255,255,0.07)";
        String text = Config.lightMode ? "#1e293b" : "white";
        return "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: " + border + ";" +
            "-fx-border-radius: 8;" +
            "-fx-text-fill: " + text + ";" +
            "-fx-font-size: 15px;" +
            "-fx-padding: 0 14;";
    }

    /** Menüyü aşağıdan yukarı kayarak ve belirerek sahneye sokar. */
    private void animateIn(VBox menu) {
        menu.setTranslateY(24);
        FadeTransition fade = new FadeTransition(Duration.millis(550), menu);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(550), menu);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }
}
