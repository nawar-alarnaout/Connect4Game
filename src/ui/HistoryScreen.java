package ui;

import core.constants.Config;
import core.model.PlayerProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import util.UiHelper;

/**
 * HistoryScreen — Maç geçmişi ekranı.
 *
 * <p>Profilde saklanan son maçları (en yeni başta) numaralı bir liste hâlinde
 * gösterir. Hiç maç yoksa bilgilendirici bir mesaj görüntüler.
 */
public class HistoryScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;

    public HistoryScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        getChildren().addAll(bg, buildContent());
        bg.start();
    }

    /** Başlık ve maç listesi (veya boş mesajı) ile geri bağlantısını oluşturur. */
    private VBox buildContent() {
        Label title = UiHelper.titleLabel("MAÇ GEÇMİŞİ");
        PlayerProfile profile = app.getProfile();

        VBox list = new VBox(8);
        list.setAlignment(Pos.CENTER);

        if (profile.getGameHistory().isEmpty()) {
            // Hiç maç oynanmadıysa
            Label empty = new Label("Henüz maç oynanmadı.");
            empty.setTextFill(Config.COLOR_TEXT_DIM);
            empty.setFont(Font.font("Segoe UI", 15));
            list.getChildren().add(empty);
        } else {
            // Her geçmiş kaydını numaralı bir satır olarak ekle
            int i = 1;
            for (String entry : profile.getGameHistory()) {
                Label row = new Label(i++ + ". " + entry);
                row.setFont(Font.font("Segoe UI", 14));
                row.setTextFill(entry.startsWith("[") ? Config.COLOR_TEXT : Config.COLOR_TEXT_DIM);
                row.setMaxWidth(640);
                row.setWrapText(true);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setBackground(new Background(new BackgroundFill(
                    Config.panelFill(), new CornerRadii(8), Insets.EMPTY)));
                list.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(list); // Uzun liste için kaydırma
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(520);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Label back = UiHelper.backLink("← Ana Menü", () -> {
            bg.stop();
            app.showMainMenu();
        });

        VBox box = new VBox(20, title, scroll, back);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        return box;
    }
}
