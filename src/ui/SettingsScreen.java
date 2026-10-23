package ui;

import core.constants.Config;
import core.model.PlayerProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import util.SoundManager;
import util.UiHelper;

/**
 * SettingsScreen — Ayarlar ekranı.
 *
 * <p>Ses efektlerini açıp kapatmaya ve 5 renk temasından birini seçmeye olanak
 * tanır. Ayrıca oyuncunun temel istatistiklerini gösterir.
 */
public class SettingsScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;

    // Tema kartlarında gösterilen isimler
    private static final String[] THEME_NAMES = {
        "Klasik", "Yeşil/Mor", "Turuncu/Mavi", "Pembe/Cyan", "Neon"
    };

    public SettingsScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        getChildren().addAll(bg, buildContent());
        bg.start();
    }

    /** Ayarlar ekranının içeriğini (ses, temalar, istatistik, geri) oluşturur. */
    private VBox buildContent() {
        Label title = UiHelper.titleLabel("AYARLAR");

        // Ses efekti açma/kapama anahtarı
        HBox soundRow = toggleRow("Ses Efektleri", Config.soundEnabled, v -> Config.soundEnabled = v);

        Label themeTitle = new Label("RENK TEMASI");
        themeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        themeTitle.setTextFill(Config.COLOR_TEXT_DIM);

        // Tema seçim kartları
        HBox themes = new HBox(12);
        themes.setAlignment(Pos.CENTER);
        Color[] preview = { // Her temanın önizleme rengi
            Color.web("#ff3b5c"), Color.web("#00e676"),
            Color.web("#ff6d00"), Color.web("#f50057"), Color.web("#e040fb")
        };
        for (int i = 0; i < THEME_NAMES.length; i++) {
            final int idx = i;
            VBox card = themeCard(THEME_NAMES[i], preview[i], Config.selectedTheme == i);
            card.setOnMouseClicked(e -> {
                Config.applyTheme(idx);                 // Temayı uygula
                app.getProfile().recordThemeUsed(idx);  // Başarım için kaydet
                app.saveProfile();
                SoundManager.play(SoundManager.Sound.CLICK);
                // Kartları yeniden çiz (seçili çerçeveyi güncellemek için)
                themes.getChildren().setAll(
                    themeCard(THEME_NAMES[0], preview[0], idx == 0),
                    themeCard(THEME_NAMES[1], preview[1], idx == 1),
                    themeCard(THEME_NAMES[2], preview[2], idx == 2),
                    themeCard(THEME_NAMES[3], preview[3], idx == 3),
                    themeCard(THEME_NAMES[4], preview[4], idx == 4)
                );
            });
            themes.getChildren().add(card);
        }

        // Özet istatistik satırı
        PlayerProfile p = app.getProfile();
        Label stats = new Label(String.format(
            "İstatistik: %d maç | %d galibiyet | Seviye %d",
            p.getTotalGames(), p.getWins(), p.getLevel()
        ));
        stats.setFont(Font.font("Segoe UI", 13));
        stats.setTextFill(Config.COLOR_TEXT_DIM);

        Label back = UiHelper.backLink("← Ana Menü", () -> {
            bg.stop();
            app.saveProfile();
            app.showMainMenu();
        });

        VBox box = new VBox(22, title, soundRow, themeTitle, themes, stats, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }

    /** Bir renk önizlemesi + isim içeren, seçiliyse çerçeveli tema kartı üretir. */
    private VBox themeCard(String name, Color color, boolean selected) {
        Circle dot = new Circle(18, color);
        Label lbl = new Label(name);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Config.COLOR_TEXT);
        VBox card = new VBox(8, dot, lbl);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setMinWidth(90);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setBackground(new Background(new BackgroundFill(
            Config.cardFill(selected),
            new CornerRadii(10), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            selected ? color : Color.TRANSPARENT, // Seçiliyse renkli çerçeve
            BorderStrokeStyle.SOLID, new CornerRadii(10), new BorderWidths(2))));
        return card;
    }

    /**
     * Etiket + kaydırmalı (toggle) anahtar içeren bir satır üretir. Anahtar
     * tıklanınca durumu değişir, görseli güncellenir ve {@code onChange} çağrılır.
     */
    private HBox toggleRow(String label, boolean on, java.util.function.Consumer<Boolean> onChange) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 15));
        lbl.setTextFill(Config.COLOR_TEXT);
        lbl.setMinWidth(200);

        // Anahtarın gövdesi ve yuvarlak topuzu
        StackPane toggle = new StackPane();
        toggle.setPrefSize(52, 28);
        toggle.setBackground(new Background(new BackgroundFill(
            on ? Config.COLOR_GLOW_BLUE : (Config.lightMode ? Color.web("#b0bec5") : Color.web("#37474f")),
            new CornerRadii(14), Insets.EMPTY)));
        Circle knob = new Circle(11, Color.WHITE);
        knob.setTranslateX(on ? 12 : -12); // Açıksa sağda, kapalıysa solda
        toggle.getChildren().add(knob);
        toggle.setCursor(javafx.scene.Cursor.HAND);

        final boolean[] state = {on}; // Lambda içinde değiştirilebilir durum
        toggle.setOnMouseClicked(e -> {
            state[0] = !state[0];
            onChange.accept(state[0]);
            toggle.setBackground(new Background(new BackgroundFill(
                state[0] ? Config.COLOR_GLOW_BLUE : (Config.lightMode ? Color.web("#b0bec5") : Color.web("#37474f")),
                new CornerRadii(14), Insets.EMPTY)));
            knob.setTranslateX(state[0] ? 12 : -12);
            SoundManager.play(SoundManager.Sound.CLICK);
        });

        HBox row = new HBox(20, lbl, toggle);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(14, 24, 14, 24));
        row.setBackground(new Background(new BackgroundFill(
            Config.panelFill(), new CornerRadii(10), Insets.EMPTY)));
        return row;
    }
}
