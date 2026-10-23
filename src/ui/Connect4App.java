package ui;

import core.constants.Config;
import core.model.PlayerProfile;
import core.network.NetworkPeer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import util.SoundManager;
import javafx.scene.image.Image;
import java.io.InputStream;

/**
 * Connect4App — Uygulamanın JavaFX giriş noktası ve merkezi yönlendiricisi.
 *
 * <p>Tek bir {@link Scene} tutar ve farklı ekranları bu sahnenin köküne
 * yerleştirerek ekranlar arası geçişi yönetir. Ayrıca oyuncu profilini ve
 * aktif ağ bağlantısının yaşam döngüsünü yönetir.
 */
public class Connect4App extends Application {

    private Stage primaryStage;             // Ana pencere
    private Scene scene;                    // Tüm ekranların paylaştığı tek sahne
    private PlayerProfile profile;          // Yüklenen oyuncu profili
    private NetworkPeer activeNetworkPeer;  // Aktif ağ bağlantısı (varsa)

    /** JavaFX uygulaması başlatıldığında çağrılır. */
    @Override
    public void start(Stage stage) {
    // Uygulama ikonunu ayarla
    try {
        java.io.InputStream iconStream = getClass().getResourceAsStream("/resources/icon.png");
        if (iconStream != null) {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(iconStream);
            stage.getIcons().add(icon);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
        SoundManager.init();                              // Ses efektlerini üret
        profile = PlayerProfile.load();                   // Profili diskten yükle
        Config.applyDisplayMode(profile.isLightMode());   // Kayıtlı tema modunu uygula

        primaryStage = stage;
        primaryStage.setTitle(Config.APP_TITLE);
        primaryStage.setMinWidth(Config.MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(Config.MIN_WINDOW_HEIGHT);
        primaryStage.setResizable(true);
        showSplash();                                     // Açılış ekranıyla başla
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /** Sahnenin arka plan rengini güncel temaya göre tazeler. */
    public void refreshSceneBackground() {
        if (scene != null) {
            scene.setFill(Config.COLOR_BG);
        }
    }

    /** Açık/koyu modu değiştirir, profili kaydeder ve arka planı tazeler. */
    public void toggleDisplayMode() {
        Config.toggleDisplayMode();
        profile.setLightMode(Config.lightMode);
        saveProfile();
        refreshSceneBackground();
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public void saveProfile() {
        profile.save();
    }

    // ---- Ekran geçişleri ----
    public void showSplash() {
        setRoot(new SplashScreen(this));
    }

    public void showMainMenu() {
        setRoot(new MainMenuScreen(this));
    }

    /** Yerel/AI oyununu başlatır (önce varsa ağ bağlantısını kapatır). */
    public void showGame(String p1, String p2, int difficulty) {
        disconnectNetwork();
        setRoot(new GameScreen(this, p1, p2, difficulty));
    }

    public void showNetworkLobby() {
        disconnectNetwork();
        setRoot(new NetworkLobbyScreen(this));
    }

    /** Ağ oyununu başlatır ve aktif bağlantıyı saklar. */
    public void startNetworkGame(String p1, String p2, NetworkPeer peer) {
        activeNetworkPeer = peer;
        setRoot(new GameScreen(this, p1, p2, peer));
    }

    /** Aktif ağ bağlantısını kapatır ve referansını temizler. */
    public void disconnectNetwork() {
        if (activeNetworkPeer != null) {
            activeNetworkPeer.disconnect();
            activeNetworkPeer = null;
        }
    }

    public void showSettings() {
        setRoot(new SettingsScreen(this));
    }

    public void showRewards() {
        setRoot(new RewardsScreen(this));
    }

    public void showHistory() {
        setRoot(new HistoryScreen(this));
    }

    /**
     * Verilen ekranı sahnenin köküne yerleştirir. Sahne ilk kez kuruluyorsa
     * oluşturur, aksi halde yalnızca kökü değiştirir (geçiş).
     */
    private void setRoot(javafx.scene.Parent root) {
        if (scene == null) {
            scene = new Scene(root, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
            primaryStage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        scene.setFill(Config.COLOR_BG);
        Platform.runLater(() -> { // Yerleşimi bir sonraki karede yenile
            if (root instanceof Region region) {
                region.requestLayout();
            }
        });
    }

    /** Programın başlangıç noktası — JavaFX uygulamasını başlatır. */
    public static void main(String[] args) {
        launch(args);
    }
}
