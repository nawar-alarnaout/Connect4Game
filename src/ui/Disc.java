package ui;

import core.constants.Config;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Disc — Tahtadaki tek bir taşı temsil eden görsel daire.
 *
 * <p>Sahibine (boş / Oyuncu 1 / Oyuncu 2) göre rengini ve parıltısını ayarlar.
 * Kazanan dizinin parçası olduğunda nabız gibi atan bir vurgu animasyonu oynatır.
 */
public class Disc extends Circle {

    private int owner;                              // 0=boş, 1=P1, 2=P2
    private final DropShadow glow = new DropShadow(); // Renkli parıltı efekti
    private Timeline winGlow;                        // Kazanma parıltı animasyonu
    private ScaleTransition winPulse;               // Kazanma büyüme/küçülme animasyonu

    public Disc(int owner) {
        super(Config.DISC_RADIUS);
        this.owner = owner;
        setEffect(glow);
        refreshColor();
    }

    /** Sahibine göre dolgu rengini ve parıltısını günceller. */
    public void refreshColor() {
        switch (owner) {
            case Config.PLAYER_1 -> {
                setFill(Config.COLOR_PLAYER_1);
                glow.setColor(Config.COLOR_PLAYER_1);
                glow.setRadius(14);
                glow.setSpread(0.12);
            }
            case Config.PLAYER_2 -> {
                setFill(Config.COLOR_PLAYER_2);
                glow.setColor(Config.COLOR_PLAYER_2);
                glow.setRadius(14);
                glow.setSpread(0.12);
            }
            default -> { // Boş hücre: parıltısız koyu renk
                setFill(Config.COLOR_EMPTY);
                glow.setColor(Color.TRANSPARENT);
                glow.setRadius(0);
            }
        }
    }

    /** Taşın sahibini değiştirir ve rengini tazeler. */
    public void setOwner(int owner) {
        this.owner = owner;
        refreshColor();
    }

    public int getOwner() {
        return owner;
    }

    /**
     * Kazanan taşı vurgular: beyaz parıltı sürekli büyüyüp küçülür ve taş
     * hafifçe nabız gibi atar (iki sonsuz, ileri-geri animasyon).
     */
    public void highlightWinner() {
        stopWinnerFx();
        glow.setColor(Color.WHITE);
        winGlow = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(glow.radiusProperty(), 12),
                new KeyValue(glow.spreadProperty(), 0.15)),
            new KeyFrame(Duration.millis(400),
                new KeyValue(glow.radiusProperty(), 30),
                new KeyValue(glow.spreadProperty(), 0.55))
        );
        winGlow.setAutoReverse(true);
        winGlow.setCycleCount(Timeline.INDEFINITE);
        winGlow.play();

        winPulse = new ScaleTransition(Duration.millis(400), this);
        winPulse.setFromX(1.0);
        winPulse.setFromY(1.0);
        winPulse.setToX(1.1);
        winPulse.setToY(1.1);
        winPulse.setAutoReverse(true);
        winPulse.setCycleCount(ScaleTransition.INDEFINITE);
        winPulse.play();
    }

    /** Kazanma animasyonlarını durdurur ve taşı normal görünümüne döndürür. */
    public void stopWinnerFx() {
        if (winGlow != null) {
            winGlow.stop();
            winGlow = null;
        }
        if (winPulse != null) {
            winPulse.stop();
            winPulse = null;
        }
        setScaleX(1.0);
        setScaleY(1.0);
        refreshColor();
    }
}
