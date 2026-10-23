package ui;

import core.constants.Config;
import core.logic.Connect4AI;
import core.logic.GameLogic;
import core.model.Achievement;
import core.model.PlayerProfile;
import core.model.RewardManager;
import core.network.NetworkMoveResult;
import core.network.NetworkPeer;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.SoundManager;
import util.UiHelper;

import java.util.List;

/**
 * GameScreen — Asıl oyun ekranı (projenin en kapsamlı arayüzü).
 *
 * <p>Üç oyun modunu da tek bir ekranda yönetir: yerel 2 kişilik, AI'ye karşı ve
 * ağ oyunu. Sorumlulukları: tahtanın çizimi, taş düşme animasyonları, üst bilgi
 * çubuğu, istatistik yan paneli, geri sayım, AI/ağ hamle akışı, kazanma vurgusu,
 * konfeti ve maç sonu ekranı.
 */
public class GameScreen extends StackPane {

    private final Connect4App app;
    private final GameLogic logic;                // Oyun durumu (yerelde yeni, ağda paylaşılan)
    private final NetworkPeer networkPeer;        // Ağ modunda bağlantı; değilse null
    private final int localPlayer;                // Ağda bu makinenin oyuncusu
    private final AnimatedBackground bg;
    private final Disc[][] discGrid = new Disc[Config.ROWS][Config.COLS]; // Görsel taş ızgarası
    private final Circle[] colIndicators = new Circle[Config.COLS];       // Sütun üstü hedef göstergeleri

    private final String player1Name;
    private final String player2Name;
    private final int difficulty;                 // Zorluk/mod (Connect4AI sabitleri)

    // Üst çubuk ve yan panel etiketleri
    private Label statusLabel;
    private Label p1Label;
    private Label p2Label;
    private Label timerLabel;
    private Label moveCountLabel;
    private Label tipLabel;
    private Label aiStatusLabel;
    private Label xpLabel;
    private final Rectangle[] columnBars = new Rectangle[Config.COLS]; // Sütun doluluk çubukları
    private HBox gameCenter;
    private BorderPane rootLayout;
    private Timeline gameTimer;   // Süre sayacı
    private Timeline tipTimer;    // İpucu döndürme zamanlayıcısı
    private int seconds;
    private boolean animating;    // Taş düşerken yeni tıklamaları engeller
    private String player2InputName = "Oyuncu 2";

    // Yan panelde dönüşümlü gösterilen strateji ipuçları
    private static final String[] TIPS = {
        "İpucu: Orta sütun (4) en güçlü başlangıç hamlesidir.",
        "İpucu: Rakibinin 3'lü dizisini mutlaka engelle!",
        "İpucu: Dikey ve çapraz kombinasyonlara dikkat et.",
        "İpucu: AI'ye karşı tuzak kurmak için çift tehdit oluştur.",
        "İpucu: Köşe sütunları bazen sürpriz zafer getirir.",
        "İpucu: Her hamleden önce rakibinin kazanma ihtimalini kontrol et."
    };

    /** Yerel/AI oyunu için kurucu. */
    public GameScreen(Connect4App app, String player1Name, String player2Name, int difficulty) {
        this(app, player1Name, player2Name, difficulty, null);
    }

    /** Ağ oyunu için kurucu (zorluk NETWORK olarak ayarlanır). */
    public GameScreen(Connect4App app, String player1Name, String player2Name, NetworkPeer peer) {
        this(app, player1Name, player2Name, Connect4AI.NETWORK, peer);
    }

    /** Ortak kurucu: tüm modları kurar. */
    public GameScreen(Connect4App app, String player1Name, String player2Name, int difficulty, NetworkPeer peer) {
        this.app = app;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.difficulty = difficulty;
        this.networkPeer = peer;
        // Ağda yerel oyuncu peer'den gelir; yerelde 0.
        this.localPlayer = peer != null ? peer.getLocalPlayer() : 0;
        // Ağda paylaşılan tahta, aksi halde yeni bir tahta.
        this.logic = peer != null ? peer.getSharedLogic() : new GameLogic();
        if (difficulty == Connect4AI.TWO_PLAYER) {
            this.player2InputName = player2Name;
        } else if (isNetwork()) {
            this.player2InputName = player2Name;
        }

        if (isNetwork()) {
            setupNetworkListeners(); // Ağ olaylarını bağla
        }

        UiHelper.makeFillParent(this);
        bg = new AnimatedBackground(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        UiHelper.bindBackground(bg, this);
        rootLayout = buildLayout();
        getChildren().addAll(bg, rootLayout);
        // Pencere boyutu değişince tahtayı yeniden ölçekle
        widthProperty().addListener((obs, o, n) -> updateLayoutScale());
        heightProperty().addListener((obs, o, n) -> updateLayoutScale());
        bg.start();
        animateIn(rootLayout);
        javafx.application.Platform.runLater(() -> {
            updateLayoutScale();
            showCountdown(); // 3-2-1-BAŞLA geri sayımı
        });
    }

    private boolean isNetwork() {
        return networkPeer != null;
    }

    /** Ağ olaylarını (hamle, bağlantı kopması, hata) arayüz thread'ine bağlar. */
    private void setupNetworkListeners() {
        networkPeer.setMoveListener(result -> Platform.runLater(() -> handleNetworkMove(result)));
        networkPeer.setDisconnectListener(() -> Platform.runLater(() -> {
            aiStatusLabel.setText("⚠ Bağlantı kesildi");
            aiStatusLabel.setTextFill(Color.web("#ff5252"));
        }));
        networkPeer.setErrorListener(msg -> Platform.runLater(() -> {
            animating = false;
            aiStatusLabel.setText("⚠ " + msg);
            aiStatusLabel.setTextFill(Color.web("#ff5252"));
        }));
    }

    /** Ağdan gelen bir hamle sonucunu işler (kendi veya rakip hamlesi). */
    private void handleNetworkMove(NetworkMoveResult result) {
        if (result.getCol() < 0) { // Özel "reset" sinyali
            resetBoardVisual();
            return;
        }
        animating = true;
        hoverColumn(result.getCol(), false);
        animateDrop(result.getRow(), result.getCol(), result.getPlayer(), () -> {
            animating = false;
            updateSidePanel();
            refreshTurnUi();
            aiStatusLabel.setText("");
            if (result.isGameOver()) {
                finishGame();
            } else if (isMyTurn()) {
                aiStatusLabel.setText("Sıra sende!");
                aiStatusLabel.setTextFill(Config.COLOR_GLOW_BLUE);
            } else {
                aiStatusLabel.setText("🌐 Rakip hamle yapıyor...");
                aiStatusLabel.setTextFill(Config.COLOR_PLAYER_2);
            }
        });
    }

    /** Ağ modunda sıra bu makinede mi? */
    private boolean isMyTurn() {
        return isNetwork() && logic.getCurrentPlayer() == localPlayer;
    }

    /** Ekran iskeletini kurar: üst çubuk, ortada tahta + yan panel. */
    private BorderPane buildLayout() {
        BorderPane bp = new BorderPane();
        bp.setPadding(new Insets(18, 22, 22, 22));
        bp.setBackground(Background.EMPTY);
        bp.setTop(buildTopBar());
        VBox boardArea = buildBoardArea();
        VBox sidePanel = buildSidePanel();
        gameCenter = new HBox(20, boardArea, sidePanel);
        gameCenter.setAlignment(Pos.CENTER);
        StackPane centerHost = new StackPane(gameCenter);
        centerHost.setAlignment(Pos.CENTER);
        bp.setCenter(centerHost);
        return bp;
    }

    /** Tahta+panelin pencereye sığması için tek bir ölçek faktörü hesaplar. */
    private void updateLayoutScale() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0 || gameCenter == null) return;

        double horizontalPadding = 44;
        double verticalPadding = 120;
        double gap = 20;
        double sideNatural = 240;

        // Tahtanın ve yan panelin doğal (ölçeksiz) boyutları
        double boardNaturalW = Config.COLS * Config.TILE_SIZE + (Config.COLS - 1) * 6
            + 2 * Config.BOARD_PADDING + 32;
        double boardNaturalH = Config.ROWS * Config.TILE_SIZE + (Config.ROWS - 1) * 6
            + 2 * Config.BOARD_PADDING + 50 + 32;
        double centerNaturalW = boardNaturalW + gap + sideNatural;
        double centerNaturalH = boardNaturalH;

        double availW = w - horizontalPadding;
        double availH = h - verticalPadding;
        // Hem yatay hem dikey sığacak en küçük ölçeği seç (1.0'ı aşma).
        double scale = Math.min(1.0, Math.min(availW / centerNaturalW, availH / centerNaturalH));

        gameCenter.setScaleX(scale);
        gameCenter.setScaleY(scale);
    }

    /** Üst bilgi çubuğu: geri linki, oyuncu rozetleri, sıra durumu ve süre. */
    private HBox buildTopBar() {
        Label back = UiHelper.backLink("← Menü", this::returnToMenu);

        p1Label = playerChip(player1Name, Config.COLOR_PLAYER_1, true);
        statusLabel = new Label("Sıra: " + player1Name);
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Config.COLOR_GLOW_BLUE);
        p2Label = playerChip(player2Name, Config.COLOR_PLAYER_2, false);

        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        timerLabel.setTextFill(Config.COLOR_GLOW_BLUE);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS); // Süreyi sağa it

        HBox bar = new HBox(12, back, p1Label, statusLabel, p2Label, sp, timerLabel);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 14, 0));

        // Her saniye süreyi artıran sayaç (oyun bitince durur)
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!logic.isGameOver()) {
                seconds++;
                timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
            }
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();
        return bar;
    }

    /** Bir oyuncunun adını gösteren, aktifken parlayan yuvarlak rozet. */
    private Label playerChip(String name, Color color, boolean active) {
        Label lbl = new Label(name);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lbl.setTextFill(active ? color : color.deriveColor(0, 1, 1, 0.35));
        lbl.setPadding(new Insets(6, 14, 6, 14));
        lbl.setMinWidth(110);
        lbl.setMaxWidth(110);
        lbl.setAlignment(Pos.CENTER);
        lbl.setBackground(new Background(new BackgroundFill(
            color.deriveColor(0, 1, 1, active ? 0.2 : 0.06),
            new CornerRadii(20), Insets.EMPTY)));
        return lbl;
    }

    /** Sağdaki istatistik panelini (hamle, doluluk, istatistik, kupalar, ipucu) kurar. */
    private VBox buildSidePanel() {
        PlayerProfile profile = app.getProfile();

        Label panelTitle = new Label(isNetwork() ? "AĞ OYUNU" : "MAÇ PANELİ");
        panelTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        panelTitle.setTextFill(Config.COLOR_GLOW_BLUE);

        moveCountLabel = statRow("Hamle", "0");

        // Sütun doluluk göstergeleri (her sütun için küçük dikey çubuk)
        Label colTitle = sectionLabel("SÜTUN DOLULUĞU");
        HBox colBars = new HBox(5);
        colBars.setAlignment(Pos.BOTTOM_CENTER);
        for (int c = 0; c < Config.COLS; c++) {
            VBox colStack = new VBox(2);
            colStack.setAlignment(Pos.BOTTOM_CENTER);
            Rectangle bar = new Rectangle(14, 4);
            bar.setFill(Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.3));
            bar.setArcWidth(4);
            bar.setArcHeight(4);
            columnBars[c] = bar;
            Label num = new Label(String.valueOf(c + 1));
            num.setFont(Font.font("Segoe UI", 9));
            num.setTextFill(Config.COLOR_TEXT_DIM);
            colStack.getChildren().addAll(bar, num);
            colBars.getChildren().add(colStack);
        }

        // İstatistik satırları
        Label statsTitle = sectionLabel("İSTATİSTİKLER");
        xpLabel = statRow("Seviye / XP", profile.getLevel() + "  •  " + profile.getXp());
        Label winsLbl = statRow("Toplam Galibiyet", String.valueOf(profile.getWins()));
        Label streakLbl = statRow("Seri Rekor", profile.getBestWinStreak() + " maç");

        // Kupalar (kilitliler soluk gösterilir, ipucu balonlu)
        Label trophyTitle = sectionLabel("KUPALAR");
        FlowPane trophies = new FlowPane(6, 6);
        trophies.setPrefWrapLength(200);
        trophies.setAlignment(Pos.CENTER);
        for (Achievement a : RewardManager.getAllAchievements()) {
            boolean unlocked = profile.isAchievementUnlocked(a);
            Label icon = new Label(a.getIcon());
            icon.setFont(Font.font(22));
            icon.setOpacity(unlocked ? 1.0 : 0.2);
            icon.setTooltip(new javafx.scene.control.Tooltip(
                unlocked ? a.getTitle() : "Kilitli: " + a.getDescription()));
            trophies.getChildren().add(icon);
        }

        // Dönüşümlü ipucu
        Label tipTitle = sectionLabel("İPUCU");
        tipLabel = new Label(TIPS[0]);
        tipLabel.setFont(Font.font("Segoe UI", 12));
        tipLabel.setTextFill(Config.COLOR_TEXT);
        tipLabel.setWrapText(true);
        tipLabel.setMaxWidth(220);
        tipLabel.setMinHeight(60);
        tipLabel.setAlignment(Pos.TOP_LEFT);

        // AI/ağ durum mesajı (örn. "AI düşünüyor...")
        aiStatusLabel = new Label("");
        aiStatusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        aiStatusLabel.setTextFill(Config.COLOR_PLAYER_2);

        VBox panel = new VBox(12);
        panel.getChildren().add(panelTitle);
        if (isNetwork()) {
            // Ağ modunda hangi rolde olduğumuzu gösteren rozet
            Label netBadge = new Label(localPlayer == Config.PLAYER_1
                ? "🖥 Oyuncu 1 — Sunucu" : "🔗 Oyuncu 2 — İstemci");
            netBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            netBadge.setTextFill(Config.COLOR_GLOW_BLUE);
            panel.getChildren().add(netBadge);
        }
        panel.getChildren().addAll(
            moveCountLabel,
            colTitle, colBars,
            statsTitle, xpLabel, winsLbl, streakLbl,
            trophyTitle, trophies,
            tipTitle, tipLabel,
            aiStatusLabel
        );
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(20, 18, 20, 18));
        panel.setMinWidth(240);
        panel.setMaxWidth(240);
        panel.setBackground(new Background(new BackgroundFill(
            Config.panelFill(), new CornerRadii(16), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
            Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.25),
            BorderStrokeStyle.SOLID, new CornerRadii(16), new BorderWidths(1))));

        DropShadow panelGlow = new DropShadow();
        panelGlow.setColor(Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.2));
        panelGlow.setRadius(18);
        panel.setEffect(panelGlow);

        startTipRotation(); // İpuçlarını döndürmeye başla
        return panel;
    }

    /** Bölüm başlığı etiketi (küçük, soluk). */
    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lbl.setTextFill(Config.COLOR_TEXT_DIM);
        return lbl;
    }

    /** "anahtar: değer" biçiminde bir istatistik satırı. */
    private Label statRow(String key, String value) {
        Label lbl = new Label(key + ":  " + value);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lbl.setTextFill(Config.COLOR_TEXT);
        return lbl;
    }

    /** İpucu etiketini 6 saniyede bir solma efektiyle değiştirir. */
    private void startTipRotation() {
        final int[] idx = {0};
        tipTimer = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
            idx[0] = (idx[0] + 1) % TIPS.length;
            FadeTransition out = new FadeTransition(Duration.millis(250), tipLabel);
            out.setToValue(0);
            out.setOnFinished(ev -> { // Önce sön, metni değiştir, sonra belir
                tipLabel.setText(TIPS[idx[0]]);
                FadeTransition in = new FadeTransition(Duration.millis(250), tipLabel);
                in.setFromValue(0);
                in.setToValue(1);
                in.play();
            });
            out.play();
        }));
        tipTimer.setCycleCount(Timeline.INDEFINITE);
        tipTimer.play();
    }

    /** Yan paneldeki hamle sayısını ve sütun doluluk çubuklarını günceller. */
    private void updateSidePanel() {
        moveCountLabel.setText("Hamle:  " + logic.getMoveCount());
        for (int c = 0; c < Config.COLS; c++) {
            int height = 0;
            for (int r = 0; r < Config.ROWS; r++) {
                if (logic.getCell(r, c) != Config.EMPTY) {
                    height++; // Sütundaki dolu hücre sayısı
                }
            }
            double barH = 8 + height * 14;
            columnBars[c].setHeight(barH);
            // Boş = soluk mavi, dolu = kırmızı, arası = doluluğa göre maviden
            Color fill = height == 0 ? Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.2)
                : height >= Config.ROWS ? Color.web("#ff5252")
                : Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.35 + height * 0.1);
            columnBars[c].setFill(fill);
        }
    }

    /** Tahta alanını kurar: üstte hedef göstergeleri, altta hücre ızgarası. */
    private VBox buildBoardArea() {
        // Her sütunun üstündeki, fareyle gelince renklenen hedef noktası
        HBox indicators = new HBox(6);
        indicators.setAlignment(Pos.CENTER);
        for (int c = 0; c < Config.COLS; c++) {
            Circle ind = new Circle(Config.DISC_RADIUS * 0.4);
            ind.setFill(Color.TRANSPARENT);
            ind.setEffect(new DropShadow());
            colIndicators[c] = ind;
            StackPane cell = new StackPane(ind);
            cell.setMinSize(Config.TILE_SIZE, Config.TILE_SIZE * 0.5);
            indicators.getChildren().add(cell);
        }

        // Tahtanın hücre ızgarası
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setPadding(new Insets(Config.BOARD_PADDING));
        grid.setBackground(new Background(new BackgroundFill(
            Config.COLOR_BOARD, new CornerRadii(16), Insets.EMPTY)));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0.35));
        shadow.setRadius(28);
        grid.setEffect(shadow);

        for (int r = 0; r < Config.ROWS; r++) {
            for (int c = 0; c < Config.COLS; c++) {
                grid.add(buildCell(r, c), c, r);
            }
        }

        StackPane gridHolder = new StackPane(grid);
        gridHolder.setPadding(new Insets(16));
        gridHolder.setPickOnBounds(false);

        VBox area = new VBox(6, indicators, gridHolder);
        area.setAlignment(Pos.CENTER);
        return area;
    }

    /** Tek bir tahta hücresini (delik + taş) oluşturur ve fare olaylarını bağlar. */
    private StackPane buildCell(int row, int col) {
        Rectangle tile = new Rectangle(Config.TILE_SIZE, Config.TILE_SIZE);
        tile.setFill(Config.COLOR_EMPTY);
        tile.setArcWidth(10);
        tile.setArcHeight(10);

        Disc disc = new Disc(Config.EMPTY);
        discGrid[row][col] = disc; // Görsel ızgarada sakla

        StackPane cell = new StackPane(tile, disc);
        cell.setPrefSize(Config.TILE_SIZE, Config.TILE_SIZE);
        cell.setCursor(Cursor.HAND);

        int finalCol = col;
        cell.setOnMouseEntered(e -> hoverColumn(finalCol, true));
        cell.setOnMouseExited(e -> hoverColumn(finalCol, false));
        cell.setOnMouseClicked(e -> onColumnClick(finalCol));
        return cell;
    }

    /** Fare bir sütunun üzerine gelince hedef göstergesini renklendirir/söndürür. */
    private void hoverColumn(int col, boolean enter) {
        if (logic.isGameOver() || animating) return;
        if (isNetwork() && !isMyTurn()) return; // Sıra bizde değilse gösterme
        Circle ind = colIndicators[col];
        DropShadow glow = (DropShadow) ind.getEffect();
        Color color = logic.getCurrentPlayer() == Config.PLAYER_1
            ? Config.COLOR_PLAYER_1 : Config.COLOR_PLAYER_2;
        if (enter && logic.isColumnPlayable(col)) {
            ind.setFill(color.deriveColor(0, 1, 1, 0.5));
            glow.setColor(color);
            glow.setRadius(12);
        } else {
            ind.setFill(Color.TRANSPARENT);
            glow.setRadius(0);
        }
    }

    /**
     * Bir sütuna tıklanınca hamleyi işler. Ağ modunda hamleyi sunucuya yollar;
     * yerel modda taşı bırakır, animasyonu oynatır ve gerekiyorsa AI'yi tetikler.
     */
    private void onColumnClick(int col) {
        if (logic.isGameOver() || animating || !logic.isColumnPlayable(col)) return;

        if (isNetwork()) {
            if (!isMyTurn()) return;        // Sıra bizde değilse yok say
            animating = true;
            hoverColumn(col, false);
            aiStatusLabel.setText("Hamle gönderiliyor...");
            networkPeer.sendMove(col);      // Kararı sunucu verecek
            return;
        }

        int player = logic.getCurrentPlayer();
        int row = logic.dropDisc(col);
        if (row < 0) return;

        animating = true;
        hoverColumn(col, false);
        animateDrop(row, col, player, () -> {
            animating = false;
            updateSidePanel();
            refreshTurnUi();
            if (logic.isGameOver()) {
                aiStatusLabel.setText("");
                finishGame();
            } else if (difficulty > 0 && difficulty != Connect4AI.NETWORK
                    && logic.getCurrentPlayer() == Config.PLAYER_2) {
                // AI modunda ve sıra AI'de → kısa bir gecikmeyle AI oynar
                aiStatusLabel.setText("🤖 AI düşünüyor...");
                PauseTransition pause = new PauseTransition(Duration.millis(450));
                pause.setOnFinished(e -> {
                    int aiCol = Connect4AI.chooseMove(logic, difficulty);
                    aiStatusLabel.setText("");
                    if (aiCol >= 0) onColumnClick(aiCol); // AI'nin hamlesini aynı yoldan oyna
                });
                pause.play();
            } else {
                aiStatusLabel.setText("");
            }
        });
    }

    /**
     * Taşın yukarıdan düşüşünü animasyonlar: spline ile hızlanarak iner, sonra
     * yere çarpınca "squish" (ezilme) sıçraması yapar. Bitince {@code done} çalışır.
     */
    private void animateDrop(int row, int col, int player, Runnable done) {
        Disc disc = discGrid[row][col];
        SoundManager.play(SoundManager.Sound.DROP);
        disc.setOwner(player);

        double startY = -(row + 2) * (Config.TILE_SIZE + 6); // Ekranın üstünden başlat
        disc.setTranslateY(startY);

        TranslateTransition drop = new TranslateTransition(Duration.millis(90 + row * 45), disc);
        drop.setFromY(startY);
        drop.setToY(0);
        drop.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.2, 1.0)); // Yerçekimi hissi
        drop.setOnFinished(e -> {
            // Çarpma anında ezilme efekti: önce yassılaş, sonra normale dön.
            ScaleTransition squish = new ScaleTransition(Duration.millis(80), disc);
            squish.setToX(1.1);
            squish.setToY(0.9);
            ScaleTransition back = new ScaleTransition(Duration.millis(100), disc);
            back.setToX(1.0);
            back.setToY(1.0);
            SequentialTransition bounce = new SequentialTransition(squish, back);
            bounce.setOnFinished(ev -> done.run());
            bounce.play();
        });
        drop.play();
    }

    /** Sıra değişince üst çubuktaki durum metnini ve rozet renklerini günceller. */
    private void refreshTurnUi() {
        if (logic.isGameOver()) return;
        boolean p1Turn = logic.getCurrentPlayer() == Config.PLAYER_1;
        String name = p1Turn ? player1Name : player2Name;
        statusLabel.setText("Sıra: " + name);
        p1Label.setTextFill(p1Turn ? Config.COLOR_PLAYER_1 : Config.COLOR_PLAYER_1.deriveColor(0, 1, 1, 0.35));
        p2Label.setTextFill(!p1Turn ? Config.COLOR_PLAYER_2 : Config.COLOR_PLAYER_2.deriveColor(0, 1, 1, 0.35));
    }

    /**
     * Maç bittiğinde çağrılır: kazanan taşları vurgular, profili/istatistikleri
     * günceller, XP/başarımları işler, geçmişe kaydeder ve sonuç ekranını açar.
     */
    private void finishGame() {
        int[][] win = logic.getWinningCells();
        if (win != null) {
            for (int[] cell : win) {
                discGrid[cell[0]][cell[1]].highlightWinner(); // Kazanan diziyi parıldat
            }
        }

        PlayerProfile profile = app.getProfile();
        profile.incrementGames();
        profile.recordThemeUsed(Config.selectedTheme);
        String time = timerLabel.getText();
        int moves = logic.getMoveCount();

        if (logic.isDraw()) {
            // Beraberlik
            profile.recordDraw();
            profile.addXp(RewardManager.xpForDraw());
            String prefix = isNetwork() ? "Ağ — Beraberlik" : "Beraberlik";
            profile.addHistory(prefix + " — " + moves + " hamle — " + time);
            SoundManager.play(SoundManager.Sound.DRAW);
        } else {
            int winner = logic.getWinner();
            boolean vsAi = difficulty > 0 && difficulty != Connect4AI.NETWORK;
            // "Yerel oyuncu kazandı mı?" — ağda localPlayer'a, yerelde P1'e bakılır
            boolean localWon = isNetwork() ? winner == localPlayer : winner == Config.PLAYER_1;

            if (localWon) {
                profile.recordWin();
                profile.addXp(RewardManager.xpForWin(isNetwork() ? Connect4AI.TWO_PLAYER : difficulty));
                String who = isNetwork()
                    ? (localPlayer == Config.PLAYER_1 ? player1Name : player2Name)
                    : player1Name;
                profile.addHistory((isNetwork() ? "Ağ — " : "") + who + " kazandı — " + moves + " hamle — " + time);
                SoundManager.play(SoundManager.Sound.WIN);
                if (vsAi) { // AI'yi yendiyse ilgili başarım bayrağını aç
                    if (difficulty == Connect4AI.MEDIUM) profile.setBeatAiMedium(true);
                    if (difficulty == Connect4AI.HARD) profile.setBeatAiHard(true);
                    if (difficulty == Connect4AI.EXPERT) profile.setBeatAiExpert(true);
                }
            } else {
                // Yerel oyuncu kaybetti
                profile.recordLoss();
                profile.addXp(RewardManager.xpForLoss(isNetwork() ? Connect4AI.TWO_PLAYER : difficulty));
                String winnerName = isNetwork()
                    ? (winner == Config.PLAYER_1 ? player1Name : player2Name)
                    : (vsAi ? "AI" : player2InputName);
                profile.addHistory((isNetwork() ? "Ağ — " : "") + winnerName + " kazandı — " + moves + " hamle — " + time);
                SoundManager.play(vsAi || (isNetwork() && !localWon) ? SoundManager.Sound.LOSE : SoundManager.Sound.WIN);
            }
        }

        List<String> unlocked = profile.newlyUnlocked(RewardManager.getAllAchievements()); // Yeni açılan başarımlar
        app.saveProfile();
        spawnConfetti();
        showOverlay(unlocked);
    }

    /** Maç sonu modal ekranını gösterir (sonuç, XP, yeni ödüller, butonlar). */
    private void showOverlay(List<String> unlocked) {
        boolean draw = logic.isDraw();
        boolean localWon = !draw && (isNetwork()
            ? logic.getWinner() == localPlayer
            : logic.getWinner() == Config.PLAYER_1);
        boolean vsAiLost = !draw && difficulty > 0 && difficulty != Connect4AI.NETWORK
            && logic.getWinner() == Config.PLAYER_2;

        // Başlık ve vurgu rengini sonuca göre belirle
        String headline;
        Color accent;
        if (draw) {
            headline = "BERABERLİK";
            accent = Config.COLOR_GLOW_BLUE;
        } else if (localWon) {
            headline = isNetwork() ? "KAZANDIN!" : player1Name.toUpperCase() + " KAZANDI!";
            accent = localPlayer == Config.PLAYER_1 ? Config.COLOR_PLAYER_1 : Config.COLOR_PLAYER_2;
        } else if (vsAiLost || (isNetwork() && !draw)) {
            headline = isNetwork() ? "KAYBETTİN!" : "KAYBETTİN!";
            accent = Color.web("#ff1744");
        } else {
            headline = player2InputName.toUpperCase() + " KAZANDI!";
            accent = Config.COLOR_PLAYER_2;
        }

        VBox panel = new VBox(14);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(36, 48, 36, 48));
        panel.setMaxWidth(420);
        panel.setBackground(new Background(new BackgroundFill(
            Config.modalBg(), new CornerRadii(18), Insets.EMPTY)));
        panel.setBorder(new Border(new BorderStroke(
            accent.deriveColor(0, 1, 1, 0.6), BorderStrokeStyle.SOLID,
            new CornerRadii(18), new BorderWidths(1.5))));

        // Sonuca uygun emoji
        Label emoji = new Label(draw ? "🤝" : (localWon ? "🏆" : (vsAiLost || isNetwork() ? "💀" : "🎉")));
        emoji.setFont(Font.font(44));
        Label title = new Label(headline);
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(accent);

        PlayerProfile p = app.getProfile();
        if (xpLabel != null) {
            xpLabel.setText("Seviye / XP:  " + p.getLevel() + "  •  " + p.getXp());
        }
        // Kazanılan XP bilgisi
        int xpMode = isNetwork() ? Connect4AI.TWO_PLAYER : difficulty;
        Label xpLbl = new Label("+" + (draw ? RewardManager.xpForDraw() :
            (localWon ? RewardManager.xpForWin(xpMode) : RewardManager.xpForLoss(xpMode))) + " XP  |  Seviye " + p.getLevel());
        xpLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        xpLbl.setTextFill(Config.COLOR_GLOW_BLUE);

        panel.getChildren().addAll(emoji, title, xpLbl);

        // Yeni açılan başarımları satır satır göster
        if (!unlocked.isEmpty()) {
            for (String ach : unlocked) {
                Label achLbl = new Label("🔓 Yeni ödül: " + ach);
                achLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
                achLbl.setTextFill(Color.web("#00e676"));
                panel.getChildren().add(achLbl);
            }
            SoundManager.play(SoundManager.Sound.UNLOCK);
        }

        // Tekrar Oyna (ağda yalnız sunucu başlatabilir) ve Ana Menü butonları
        Label again = UiHelper.menuButton("↻  Tekrar Oyna", Config.COLOR_GLOW_BLUE);
        again.setVisible(!isNetwork() || networkPeer.isHost());
        again.setManaged(!isNetwork() || networkPeer.isHost());
        again.setOnMouseClicked(e -> resetMatch());
        Label menu = UiHelper.menuButton("⌂  Ana Menü", Color.web("#455a64"));
        menu.setTextFill(Config.COLOR_TEXT);
        menu.setOnMouseClicked(e -> returnToMenu());

        HBox buttons = new HBox(12, again, menu);
        buttons.setAlignment(Pos.CENTER);
        panel.getChildren().add(buttons);

        // Modalı yarı saydam bir kaplama üzerinde göster
        StackPane overlay = new StackPane(panel);
        overlay.setBackground(new Background(new BackgroundFill(
            Config.overlayFill(), CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().add(overlay);

        // Belirme + büyüme animasyonu
        panel.setOpacity(0);
        panel.setScaleX(0.8);
        panel.setScaleY(0.8);
        FadeTransition fade = new FadeTransition(Duration.millis(300), panel);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), panel);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    /** "Tekrar Oyna": modalı kaldırır, (ağda) reset yollar, tahtayı sıfırlar. */
    private void resetMatch() {
        getChildren().removeIf(n -> n instanceof StackPane && n != bg && !(n instanceof BorderPane));
        if (isNetwork() && networkPeer.isHost()) {
            networkPeer.sendReset();
        }
        resetBoardVisual();
        showCountdown();
    }

    /** Görsel tahtayı ve sayaç/sıra durumunu yeni maça hazırlar. */
    private void resetBoardVisual() {
        for (int r = 0; r < Config.ROWS; r++) {
            for (int c = 0; c < Config.COLS; c++) {
                discGrid[r][c].stopWinnerFx();
                discGrid[r][c].setTranslateY(0);
                discGrid[r][c].setOwner(Config.EMPTY);
            }
        }
        // Ağda tahtayı yalnız sunucu sıfırlar (istemci aynayı RESET ile sıfırlar)
        if (!isNetwork() || networkPeer.isHost()) {
            logic.reset();
        }
        seconds = 0;
        timerLabel.setText("00:00");
        gameTimer.playFromStart();
        updateSidePanel();
        refreshTurnUi();
        animating = false;
    }

    /** Maç başında "3 - 2 - 1 - BAŞLA!" geri sayımını gösterir. */
    private void showCountdown() {
        Label count = new Label("3");
        count.setFont(Font.font("Segoe UI", FontWeight.BOLD, 100));
        count.setTextFill(Config.COLOR_GLOW_BLUE);
        StackPane overlay = new StackPane(count);
        overlay.setBackground(new Background(new BackgroundFill(
            Config.overlayStrong(), CornerRadii.EMPTY, Insets.EMPTY)));
        getChildren().add(overlay);

        String[] texts = {"3", "2", "1", "BAŞLA!"};
        SequentialTransition seq = new SequentialTransition();
        for (String t : texts) {
            Timeline show = new Timeline(new KeyFrame(Duration.millis(1), e -> {
                count.setText(t);
                if (!t.equals("BAŞLA!")) {
                    SoundManager.play(SoundManager.Sound.COUNTDOWN);
                }
            }));
            seq.getChildren().addAll(show, new PauseTransition(Duration.millis(650)));
        }
        seq.setOnFinished(e -> getChildren().remove(overlay)); // Bitince kaldır
        seq.play();
    }

    /** Zafer anında ekrana 80 parça renkli konfeti döker (düşme + dönme). */
    private void spawnConfetti() {
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 80; i++) {
            Rectangle piece = new Rectangle(5 + rand.nextInt(6), 5 + rand.nextInt(6));
            Color[] colors = {Config.COLOR_PLAYER_1, Config.COLOR_PLAYER_2, Config.COLOR_GLOW_BLUE, Color.WHITE};
            piece.setFill(colors[rand.nextInt(colors.length)]);
            double sx = rand.nextDouble() * getWidth() - getWidth() / 2;
            double sy = -getHeight() / 2 - rand.nextInt(100); // Ekranın üstünden başlat
            piece.setTranslateX(sx);
            piece.setTranslateY(sy);
            getChildren().add(piece);

            TranslateTransition fall = new TranslateTransition(Duration.millis(1800 + rand.nextInt(1200)), piece);
            fall.setToY(getHeight() / 2 + 40);
            fall.setDelay(Duration.millis(rand.nextInt(800)));
            RotateTransition rot = new RotateTransition(Duration.millis(1000), piece);
            rot.setByAngle(rand.nextBoolean() ? 360 : -360);
            rot.setCycleCount(Timeline.INDEFINITE);
            ParallelTransition pt = new ParallelTransition(fall, rot);
            pt.setOnFinished(e -> getChildren().remove(piece)); // Yere inince temizle
            pt.play();
        }
    }

    /** Ekranı aşağıdan yukarı kayarak ve belirerek sahneye sokar. */
    private void animateIn(javafx.scene.Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(500), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(500), node);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    /** Ana menüye döner: zamanlayıcıları durdurur, ağ bağlantısını kapatır. */
    private void returnToMenu() {
        gameTimer.stop();
        if (tipTimer != null) {
            tipTimer.stop();
        }
        FadeTransition fade = new FadeTransition(Duration.millis(350), this);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            bg.stop();
            app.disconnectNetwork();
            app.showMainMenu();
        });
        fade.play();
    }
}
