package ui;

import core.constants.Config;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.SoundManager;
import util.UiHelper;

/**
 * SplashScreen — Uygulamanın açılış ekranı.
 *
 * <p>Parıldayan "CONNECT 4" logosu yumuşakça belirir. Kullanıcı tıklayınca veya
 * Enter'a basınca, ya da 4 saniye sonra otomatik olarak ana menüye geçilir.
 */
public class SplashScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;
    private boolean leaving; // Çift geçişi önlemek için bayrak

    public SplashScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);

        // Parıldayan büyük logo
        Label logo = new Label("CONNECT 4");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 72));
        logo.setTextFill(Config.COLOR_TEXT);
        DropShadow glow = new DropShadow();
        glow.setColor(Config.COLOR_GLOW_BLUE);
        glow.setRadius(28);
        logo.setEffect(glow);
        UiHelper.pulseGlow(glow); // Nabız gibi parıltı

        Label sub = new Label(Config.TEAM_CREDITS);
        sub.setFont(Font.font("Segoe UI", 14));
        sub.setTextFill(Config.COLOR_TEXT_DIM);

        Label hint = new Label("Devam etmek için tıkla veya Enter'a bas");
        hint.setFont(Font.font("Segoe UI", 13));
        hint.setTextFill(Config.COLOR_TEXT_DIM);

        VBox box = new VBox(16, logo, sub, hint);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setOpacity(0); // Başlangıçta görünmez (fade-in için)

        getChildren().addAll(bg, box);
        bg.start();

        // Giriş animasyonu: aynı anda belirme + büyüme
        FadeTransition fade = new FadeTransition(Duration.millis(800), box);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(800), box);
        scale.setFromX(0.85);
        scale.setFromY(0.85);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();

        // Tıklama / klavye ile menüye geç
        setOnMouseClicked(e -> goMenu());
        setOnKeyPressed(e -> goMenu());
        setFocusTraversable(true);
        requestFocus();

        // 4 saniye sonra otomatik geçiş
        PauseTransition auto = new PauseTransition(Duration.seconds(4));
        auto.setOnFinished(e -> goMenu());
        auto.play();
    }

    /** Solma animasyonuyla ana menüye geçer (yalnızca bir kez). */
    private void goMenu() {
        if (leaving) return;
        leaving = true;
        SoundManager.play(SoundManager.Sound.MENU);
        FadeTransition fade = new FadeTransition(Duration.millis(400), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            bg.stop();
            app.showMainMenu();
        });
        fade.play();
    }
}
