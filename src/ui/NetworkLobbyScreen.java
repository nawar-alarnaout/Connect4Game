package ui;

import core.constants.Config;
import core.network.NetworkAddressUtil;
import core.network.NetworkClient;
import core.network.NetworkHost;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import util.SoundManager;
import util.UiHelper;

import java.util.List;

/**
 * NetworkLobbyScreen — Ağ oyunu giriş (lobi) ekranı.
 *
 * <p>İki seçenek sunar: <b>Oda Kur</b> (sunucu olur, IP'sini gösterir ve rakip
 * bekler) veya <b>Odaya Katıl</b> (sunucunun IP'sine bağlanır). Bağlantı
 * kurulduğunda otomatik olarak oyun ekranına geçilir.
 */
public class NetworkLobbyScreen extends StackPane {

    private final Connect4App app;
    private final AnimatedBackground bg;
    private NetworkHost activeHost;     // Bu makine sunucuysa
    private NetworkClient activeClient; // Bu makine istemciyse

    private Label statusLabel;  // Durum/hata mesajları
    private TextField nameField;
    private TextField ipField;
    private VBox hostPanel;
    private VBox clientPanel;

    public NetworkLobbyScreen(Connect4App app) {
        this.app = app;
        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        getChildren().addAll(bg, buildContent());
        bg.start();
    }

    /** Lobi içeriğini (isim, sunucu paneli, istemci paneli, durum) oluşturur. */
    private VBox buildContent() {
        Label title = UiHelper.titleLabel("AĞ OYUNU");
        Label sub = new Label("İki farklı bilgisayardan aynı ağ üzerinden oynayın");
        sub.setFont(Font.font("Segoe UI", 13));
        sub.setTextFill(Config.COLOR_TEXT_DIM);

        nameField = styledField(app.getProfile().getPlayerName());

        hostPanel = buildHostPanel();
        clientPanel = buildClientPanel();

        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        statusLabel.setTextFill(Config.COLOR_GLOW_BLUE);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(520);
        statusLabel.setAlignment(Pos.CENTER);

        Label back = UiHelper.backLink("← Ana Menü", this::returnToMenu);

        Label team = new Label(Config.TEAM_CREDITS);
        team.setFont(Font.font("Segoe UI", 10));
        team.setTextFill(Config.COLOR_TEXT_DIM);
        team.setWrapText(true);
        team.setMaxWidth(600);
        team.setAlignment(Pos.CENTER);

        VBox box = new VBox(16, title, sub, nameField, hostPanel, clientPanel, statusLabel, back, team);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));
        return box;
    }

    /** Sunucu paneli: yerel IP adres(ler)ini gösterir ve oda kurma butonu sunar. */
    private VBox buildHostPanel() {
        List<String> ips = NetworkAddressUtil.getAllLocalIps();
        String ipText = String.join("  •  ", ips);

        Label lbl = new Label("ODA KUR (Oyuncu 1 — Sunucu)");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(Config.COLOR_PLAYER_1);

        Label ipInfo = new Label("IP adresiniz: " + ipText + "  |  Port: " + Config.NETWORK_PORT);
        ipInfo.setFont(Font.font("Segoe UI", 11));
        ipInfo.setTextFill(Config.COLOR_TEXT_DIM);
        ipInfo.setWrapText(true);
        ipInfo.setMaxWidth(520);

        Label hostBtn = UiHelper.menuButton("🖥  Oda Kur & Bekle", Config.COLOR_PLAYER_1);
        hostBtn.setOnMouseClicked(e -> startHost());

        VBox panel = new VBox(8, lbl, ipInfo, hostBtn);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(14));
        panel.setMaxWidth(520);
        panel.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(
            Config.panelFill(), new javafx.scene.layout.CornerRadii(12), Insets.EMPTY)));
        return panel;
    }

    /** İstemci paneli: sunucu IP'si girişi ve katılma butonu sunar. */
    private VBox buildClientPanel() {
        Label lbl = new Label("KATIL (Oyuncu 2 — İstemci)");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lbl.setTextFill(Config.COLOR_PLAYER_2);

        ipField = styledField("192.168.1.10");
        ipField.setPromptText("Sunucu IP adresi...");

        Label joinBtn = UiHelper.menuButton("🔗  Odaya Katıl", Config.COLOR_PLAYER_2);
        joinBtn.setTextFill(Config.onAccentText());
        joinBtn.setOnMouseClicked(e -> joinGame());

        VBox panel = new VBox(8, lbl, ipField, joinBtn);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(14));
        panel.setMaxWidth(520);
        panel.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(
            Config.panelFill(), new javafx.scene.layout.CornerRadii(12), Insets.EMPTY)));
        return panel;
    }

    /** Tutarlı görünümlü bir metin giriş alanı üretir. */
    private TextField styledField(String value) {
        TextField field = new TextField(value);
        field.setPrefWidth(340);
        field.setPrefHeight(42);
        field.setStyle(
            "-fx-background-color: rgba(255,255,255,0.07);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: #00d4ff55;" +
            "-fx-border-radius: 8;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-padding: 0 14;"
        );
        return field;
    }

    /**
     * Sunucuyu başlatır: NetworkHost oluşturur, hata dinleyicisi bağlar ve
     * rakip katıldığında oyunu başlatacak geri çağrımı kurar.
     */
    private void startHost() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Oyuncu 1";

        statusLabel.setText("Oda açılıyor, rakip bekleniyor...");
        statusLabel.setTextFill(Config.COLOR_GLOW_BLUE);
        SoundManager.play(SoundManager.Sound.CLICK);

        activeHost = new NetworkHost(name);
        // Hata mesajlarını arayüz thread'inde (Platform.runLater) göster
        activeHost.setErrorListener(msg -> Platform.runLater(() -> {
            statusLabel.setText("Hata: " + msg);
            statusLabel.setTextFill(Color.web("#ff5252"));
        }));

        try {
            final String hostName = name;
            // Rakip bağlanınca oyun ekranına geç
            activeHost.start(clientName -> Platform.runLater(() -> {
                statusLabel.setText("Rakip bağlandı: " + clientName);
                app.startNetworkGame(hostName, clientName, activeHost);
            }));
            statusLabel.setText("Oda hazır! Rakibin şu IP ile katılması gerekiyor: "
                + NetworkAddressUtil.getBestLocalIp());
        } catch (Exception e) {
            statusLabel.setText("Sunucu başlatılamadı: " + e.getMessage());
            statusLabel.setTextFill(Color.web("#ff5252"));
        }
    }

    /**
     * İstemci olarak sunucuya bağlanır. Bağlantı engellememesi için ayrı bir
     * thread'de yapılır; karşılama gelince oyun ekranına geçilir.
     */
    private void joinGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Oyuncu 2";
        String ip = ipField.getText().trim();
        if (ip.isEmpty()) {
            statusLabel.setText("Lütfen sunucu IP adresini girin.");
            statusLabel.setTextFill(Color.web("#ff5252"));
            return;
        }

        statusLabel.setText("Bağlanılıyor: " + ip + "...");
        SoundManager.play(SoundManager.Sound.CLICK);

        activeClient = new NetworkClient();
        activeClient.setErrorListener(msg -> Platform.runLater(() -> {
            statusLabel.setText("Hata: " + msg);
            statusLabel.setTextFill(Color.web("#ff5252"));
        }));
        activeClient.setDisconnectListener(() -> Platform.runLater(() ->
            statusLabel.setText("Sunucu bağlantısı kesildi.")));

        final String clientName = name;
        // Karşılama mesajı gelince oyunu başlat
        activeClient.setWelcomeListener(info -> Platform.runLater(() ->
            app.startNetworkGame(info.hostName(), info.clientName(), activeClient)));

        // Bağlantıyı arka planda kur (arayüzü dondurmamak için)
        new Thread(() -> {
            try {
                activeClient.connect(ip, clientName);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Bağlantı başarısız: " + e.getMessage()
                        + "\nIP ve firewall ayarlarını kontrol edin.");
                    statusLabel.setTextFill(Color.web("#ff5252"));
                });
            }
        }, "connect4-join").start();
    }

    /** Lobiden çıkarken açık bağlantıları kapatır ve ana menüye döner. */
    private void returnToMenu() {
        if (activeHost != null) activeHost.disconnect();
        if (activeClient != null) activeClient.disconnect();
        bg.stop();
        app.showMainMenu();
    }
}
