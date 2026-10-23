package ui;

import core.constants.Config;
import core.model.Achievement;
import core.model.PlayerProfile;
import core.model.RewardManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import util.UiHelper;

import java.util.List;

/**
 * RewardsScreen — Ödüller ve kupalar ekranı.
 *
 * <p>Oyuncunun seviye/XP durumunu, bir ilerleme çubuğunu ve tüm başarımların
 * (açık/kilitli) kart listesini gösterir.
 */
public class RewardsScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;

    public RewardsScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        getChildren().addAll(bg, buildContent());
        bg.start();
    }

    /** Başlık, ilerleme çubuğu, başarım listesi ve geri bağlantısını oluşturur. */
    private VBox buildContent() {
        PlayerProfile profile = app.getProfile();
        Label title = UiHelper.titleLabel("ÖDÜLLER & KUPALAR");

        // Seviye / XP özeti
        Label progress = new Label(String.format(
            "Seviye %d  •  %d XP  •  Sonraki seviye: %d XP",
            profile.getLevel(), profile.getXp(), profile.xpToNextLevel()
        ));
        progress.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        progress.setTextFill(Config.COLOR_GLOW_BLUE);

        // İlerleme çubuğu: arka plan + doluluk
        Region barBg = new Region();
        barBg.setPrefSize(500, 10);
        barBg.setBackground(new Background(new BackgroundFill(
            Color.web("#ffffff15"), new CornerRadii(5), Insets.EMPTY)));

        Region barFill = new Region();
        barFill.setPrefSize(500 * profile.levelProgressPercent() / 100.0, 10); // Yüzdeye göre genişlik
        barFill.setBackground(new Background(new BackgroundFill(
            Config.COLOR_GLOW_BLUE, new CornerRadii(5), Insets.EMPTY)));

        StackPane bar = new StackPane(barBg, barFill);
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

        // Başarım kartları
        VBox grid = new VBox(10);
        grid.setAlignment(Pos.CENTER);
        List<Achievement> all = RewardManager.getAllAchievements();

        for (Achievement a : all) {
            boolean unlocked = profile.isAchievementUnlocked(a);
            grid.getChildren().add(achievementCard(a, unlocked));
        }

        ScrollPane scroll = new ScrollPane(grid); // Liste uzunsa kaydırılabilir
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(480);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label back = UiHelper.backLink("← Ana Menü", () -> {
            bg.stop();
            app.showMainMenu();
        });

        VBox box = new VBox(18, title, progress, bar, scroll, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(36));
        return box;
    }

    /** Tek bir başarımı ikon, başlık, açıklama ve durum (AÇIK/KİLİTLİ) ile gösterir. */
    private HBox achievementCard(Achievement a, boolean unlocked) {
        Label icon = new Label(a.getIcon());
        icon.setFont(Font.font(32));
        icon.setOpacity(unlocked ? 1.0 : 0.25); // Kilitli ise soluk

        Label name = new Label(a.getTitle());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        name.setTextFill(unlocked ? Config.COLOR_TEXT : Config.COLOR_TEXT_DIM);

        Label desc = new Label(a.getDescription());
        desc.setFont(Font.font("Segoe UI", 12));
        desc.setTextFill(Config.COLOR_TEXT_DIM);

        Label status = new Label(unlocked ? "AÇIK" : "KİLİTLİ");
        status.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        status.setTextFill(unlocked ? Color.web("#00e676") : Color.web("#ff5252"));

        VBox text = new VBox(4, name, desc);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Durumu sağa it

        HBox card = new HBox(16, icon, text, spacer, status);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setMaxWidth(620);
        card.setBackground(new Background(new BackgroundFill(
            unlocked ? Color.web("#ffffff12") : Color.web("#ffffff05"),
            new CornerRadii(12), Insets.EMPTY)));
        card.setBorder(new Border(new BorderStroke(
            unlocked ? Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.5) : Color.web("#ffffff10"),
            BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.2))));
        return card;
    }
}
