package core.constants;

import javafx.scene.paint.Color;

/**
 * Config — Projenin merkezi yapılandırma ve sabitler sınıfı.
 *
 * <p>Tahta boyutları, renkler, temalar, pencere ölçüleri ve oturum boyunca
 * değişebilen global durum (skor, ses ayarı, seçili tema) burada tutulur.
 * Tüm sınıflar buradaki tek "doğruluk kaynağı"na başvurur.
 *
 * <p>Private kurucu + {@code final} sınıf ile örneklenemez; yalnızca statik
 * bir yapılandırma havuzu olarak kullanılır.
 */
public final class Config {

    // Bu sınıftan nesne üretilmesini engeller (yalnızca statik kullanım).
    private Config() {}

    // ---- Tahta ve oyun kuralı sabitleri ----
    public static final int COLS = 7;        // Sütun sayısı
    public static final int ROWS = 6;        // Satır sayısı
    public static final int WIN_LENGTH = 4;  // Kazanmak için gereken ardışık taş sayısı
    public static final int EMPTY = 0;       // Boş hücre değeri
    public static final int PLAYER_1 = 1;    // 1. oyuncu taşı
    public static final int PLAYER_2 = 2;    // 2. oyuncu taşı

    // ---- Görsel ölçü sabitleri (piksel) ----
    public static final double TILE_SIZE = 92.0;        // Bir hücrenin kenar uzunluğu
    public static final double DISC_RADIUS = 36.0;      // Taş yarıçapı
    public static final double BOARD_PADDING = 10.0;    // Tahta iç boşluğu
    public static final double WINDOW_WIDTH = 900.0;    // Varsayılan pencere genişliği
    public static final double WINDOW_HEIGHT = 820.0;   // Varsayılan pencere yüksekliği
    public static final double MIN_WINDOW_WIDTH = 720.0;
    public static final double MIN_WINDOW_HEIGHT = 600.0;

    // ---- Aktif tema renkleri (tema/mod değişince güncellenir) ----
    public static Color COLOR_BG = Color.web("#070b18");        // Arka plan
    public static Color COLOR_EMPTY = Color.web("#050a20");     // Boş hücre rengi
    public static Color COLOR_BOARD = Color.web("#0c1a45");     // Tahta rengi
    public static Color COLOR_PLAYER_1 = Color.web("#ff3b5c");  // Oyuncu 1 diski
    public static Color COLOR_PLAYER_2 = Color.web("#ffd000");  // Oyuncu 2 diski
    public static Color COLOR_GLOW_BLUE = Color.web("#00d4ff"); // Neon mavi vurgu
    public static final Color COLOR_ACCENT = Color.web("#7c6cff"); // Mor vurgu (sabit)
    public static Color COLOR_TEXT = Color.web("#eceff1");      // Ana metin
    public static Color COLOR_TEXT_DIM = Color.web("#607d8b");  // Soluk metin

    // Açık tema (light mode) açık mı? false = koyu tema.
    public static boolean lightMode = false;

    // ---- Uygulama meta bilgileri ----
    public static final String APP_TITLE = "Connect 4 — GUI Projesi";
    public static final String STUDENT_NAME = "Ruslan";

    public static final int NETWORK_PORT = 5555; // Ağ oyununun TCP portu

    // Ekip rolleri ve alt bilgi yazısı
    public static final String TEAM_FRONTEND = "Mustafa";
    public static final String TEAM_BACKEND = "Nawar";
    public static final String TEAM_AI = "Ruslan";
    public static final String TEAM_CREDITS =
        "Frontend: " + TEAM_FRONTEND + "  •  Backend: " + TEAM_BACKEND + "  •  AI & Ağ: " + TEAM_AI;

    // ---- Oturum boyu değişebilen global durum ----
    public static int selectedTheme = 0;        // Seçili renk teması (0-4)
    public static boolean soundEnabled = true;  // Ses efektleri açık mı?
    public static boolean musicEnabled = true;  // Müzik açık mı? (ayrılmış)

    public static int scorePlayer1 = 0;         // Oturum skoru — Oyuncu 1
    public static int scorePlayer2 = 0;         // Oturum skoru — Oyuncu 2
    public static int draws = 0;                // Oturum beraberlik sayısı

    /** Panellerin yarı saydam dolgu rengini moda göre döndürür. */
    public static Color panelFill() {
        return lightMode ? Color.web("#0000000a") : Color.web("#ffffff08");
    }

    /** Kartların dolgu rengini; seçili (highlighted) ise daha belirgin. */
    public static Color cardFill(boolean highlighted) {
        if (lightMode) {
            return highlighted ? Color.web("#00000012") : Color.web("#00000006");
        }
        return highlighted ? Color.web("#ffffff22") : Color.web("#ffffff08");
    }

    /** Modal arka planı karartan/aydınlatan yarı saydam katman rengi. */
    public static Color overlayFill() {
        return lightMode ? Color.web("#ffffff99") : Color.web("#00000055");
    }

    /** Daha yoğun bir kaplama (örn. geri sayım ekranı). */
    public static Color overlayStrong() {
        return lightMode ? Color.web("#ffffffcc") : Color.web("#00000088");
    }

    /** Sonuç penceresi (modal) arka plan rengi. */
    public static Color modalBg() {
        return lightMode ? Color.web("#ffffffee") : Color.web("#080c1acc");
    }

    /** Vurgulu (parlak) buton zemini üzerindeki metin rengi. */
    public static Color onAccentText() {
        return lightMode ? Color.web("#1a237e") : Color.web("#070b18");
    }

    /** Açık/koyu mod arasında geçiş yapar. */
    public static void toggleDisplayMode() {
        applyDisplayMode(!lightMode);
    }

    /**
     * Açık veya koyu modu uygular: arka plan ve metin renklerini ayarlar,
     * ardından mevcut temayı yeniden yükler.
     */
    public static void applyDisplayMode(boolean light) {
        lightMode = light;
        if (light) {
            COLOR_BG = Color.web("#eef2f7");
            COLOR_EMPTY = Color.web("#c5d3e3");
            COLOR_TEXT = Color.web("#1e293b");
            COLOR_TEXT_DIM = Color.web("#64748b");
        } else {
            COLOR_BG = Color.web("#070b18");
            COLOR_EMPTY = Color.web("#050a20");
            COLOR_TEXT = Color.web("#eceff1");
            COLOR_TEXT_DIM = Color.web("#607d8b");
        }
        applyTheme(selectedTheme); // Tema renklerini güncel moda göre tazele
    }

    /**
     * 5 renk temasından birini yükler. Her tema oyuncu, tahta ve parıltı
     * renklerini değiştirir. Açık modda renkler ayrıca yumuşatılır.
     */
    public static void applyTheme(int theme) {
        selectedTheme = theme;
        switch (theme) {
            case 0 -> { // Klasik (kırmızı/sarı)
                COLOR_PLAYER_1 = Color.web("#ff3b5c");
                COLOR_PLAYER_2 = Color.web("#ffd000");
                COLOR_BOARD = Color.web("#0c1a45");
                COLOR_GLOW_BLUE = Color.web("#00d4ff");
            }
            case 1 -> { // Yeşil/Mor
                COLOR_PLAYER_1 = Color.web("#00e676");
                COLOR_PLAYER_2 = Color.web("#d500f9");
                COLOR_BOARD = Color.web("#0a1f12");
                COLOR_GLOW_BLUE = Color.web("#69f0ae");
            }
            case 2 -> { // Turuncu/Mavi
                COLOR_PLAYER_1 = Color.web("#ff6d00");
                COLOR_PLAYER_2 = Color.web("#2979ff");
                COLOR_BOARD = Color.web("#1a1200");
                COLOR_GLOW_BLUE = Color.web("#ff9100");
            }
            case 3 -> { // Pembe/Cyan
                COLOR_PLAYER_1 = Color.web("#f50057");
                COLOR_PLAYER_2 = Color.web("#00e5ff");
                COLOR_BOARD = Color.web("#180010");
                COLOR_GLOW_BLUE = Color.web("#18ffff");
            }
            case 4 -> { // Neon
                COLOR_PLAYER_1 = Color.web("#e040fb");
                COLOR_PLAYER_2 = Color.web("#76ff03");
                COLOR_BOARD = Color.web("#120018");
                COLOR_GLOW_BLUE = Color.web("#ea80fc");
            }
            default -> applyTheme(0); // Geçersiz değer → klasik temaya dön
        }
        // Açık modda tahta ve parıltı renklerini okunabilirlik için ayarla.
        if (lightMode) {
            COLOR_BOARD = Color.web("#b8cce4");
            COLOR_GLOW_BLUE = switch (theme) {
                case 1 -> Color.web("#2e7d32");
                case 2 -> Color.web("#e65100");
                case 3 -> Color.web("#c51162");
                case 4 -> Color.web("#7b1fa2");
                default -> Color.web("#1565c0");
            };
        }
    }
}
