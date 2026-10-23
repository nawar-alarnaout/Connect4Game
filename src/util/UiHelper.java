package util;

import core.constants.Config;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import ui.AnimatedBackground;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * UiHelper — Ekranlar arasında tekrar eden arayüz öğelerinin fabrikası.
 *
 * <p>Başlık etiketi, menü butonu, geri bağlantısı gibi bileşenleri tek yerde
 * üreterek tüm ekranlarda tutarlı bir görünüm sağlar. Ayrıca duyarlı ölçek
 * hesabı ve parıltı animasyonu gibi ortak yardımcılar içerir.
 */
public final class UiHelper {

    private UiHelper() {} // Örneklenemez

    /** Mavi parıltılı, büyük başlık etiketi üretir. */
    public static Label titleLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        lbl.setTextFill(Config.COLOR_TEXT);
        DropShadow glow = new DropShadow();
        glow.setColor(Config.COLOR_GLOW_BLUE);
        glow.setRadius(22);
        glow.setSpread(0.25);
        lbl.setEffect(glow);
        return lbl;
    }

    /** Animasyonlu arka planın boyutunu, kapsayıcı bölgenin boyutuna bağlar. */
    public static void bindBackground(AnimatedBackground bg, Region host) {
        bg.widthProperty().bind(host.widthProperty());
        bg.heightProperty().bind(host.heightProperty());
    }

    /** Bir bölgenin tüm boyut özelliklerini ebeveynine bağlar (tam kaplama). */
    public static void bindToParent(Region node, Region host) {
        makeFillParent(node);
        node.prefWidthProperty().bind(host.widthProperty());
        node.prefHeightProperty().bind(host.heightProperty());
        node.minWidthProperty().bind(host.widthProperty());
        node.minHeightProperty().bind(host.heightProperty());
        node.maxWidthProperty().bind(host.widthProperty());
        node.maxHeightProperty().bind(host.heightProperty());
    }

    /** Bir bölgenin sınırsız büyüyebilmesini sağlar (ebeveyni doldurur). */
    public static void makeFillParent(Region node) {
        node.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /** Bir değeri [min, max] aralığına sıkıştırır. */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Pencere boyutuna göre duyarlı (responsive) bir ölçek faktörü hesaplar.
     * İçeriğin taşmaması için 0.72 ile 1.0 arasında tutulur.
     */
    public static double responsiveScale(double width, double height) {
        double sx = width / Config.WINDOW_WIDTH;
        double sy = height / Config.WINDOW_HEIGHT;
        return clamp(Math.min(sx, sy), 0.72, 1.0);
    }

    /**
     * Standart bir menü butonu üretir. Parlak renklerde metin koyulaşır,
     * fare üzerine gelince hafifçe büyür ve tık sesi çalar.
     */
    public static Label menuButton(String text, Color color) {
        Label btn = new Label(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        btn.setTextFill(color.getBrightness() > 0.55 ? Config.onAccentText() : color);
        btn.setMinWidth(180);
        btn.setMaxWidth(320);
        btn.setPrefWidth(240);
        btn.setPrefHeight(46);
        btn.setAlignment(Pos.CENTER);
        btn.setCursor(Cursor.HAND);
        btn.setBackground(new Background(new BackgroundFill(
            color.getBrightness() > 0.55 ? color : color.deriveColor(0, 1, 1, 0.18),
            new CornerRadii(10), Insets.EMPTY
        )));
        btn.setOnMouseEntered(e -> { // Hover: büyüt + ses
            btn.setScaleX(1.04);
            btn.setScaleY(1.04);
            SoundManager.play(SoundManager.Sound.CLICK);
        });
        btn.setOnMouseExited(e -> {  // Çıkış: eski boyut
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
        return btn;
    }

    /**
     * "Geri" tarzı bir metin bağlantısı üretir. Üzerine gelince renklenir,
     * tıklanınca ses çalar ve verilen eylemi çalıştırır.
     */
    public static Label backLink(String text, Runnable action) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lbl.setTextFill(Config.COLOR_TEXT_DIM);
        lbl.setCursor(Cursor.HAND);
        lbl.setOnMouseEntered(e -> lbl.setTextFill(Config.COLOR_GLOW_BLUE));
        lbl.setOnMouseExited(e -> lbl.setTextFill(Config.COLOR_TEXT_DIM));
        lbl.setOnMouseClicked(e -> {
            SoundManager.play(SoundManager.Sound.CLICK);
            action.run();
        });
        return lbl;
    }

    /** Bir gölge efektine sonsuz "nefes alan" parıltı animasyonu uygular. */
    public static void pulseGlow(DropShadow glow) {
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(glow.radiusProperty(), 14),
                new KeyValue(glow.spreadProperty(), 0.12)),
            new KeyFrame(Duration.millis(1600),
                new KeyValue(glow.radiusProperty(), 34),
                new KeyValue(glow.spreadProperty(), 0.4))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true); // İleri-geri sürekli
        pulse.play();
    }
}
