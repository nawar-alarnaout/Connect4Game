package core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PlayerProfile — Oyuncunun kalıcı ilerleme verisi.
 *
 * <p>XP, seviye, galibiyet/kayıp istatistikleri, galibiyet serileri, açılan
 * başarımlar, kullanılan temalar ve maç geçmişini tutar. Tüm bu veriler
 * kullanıcının ev klasöründeki {@code .connect4gui_profile.txt} dosyasında
 * {@code anahtar=değer} biçiminde saklanır; böylece oyun kapatılsa da kalır.
 */
public class PlayerProfile {

    // Profil dosyasının yolu: ~/.connect4gui_profile.txt
    private static final Path SAVE_PATH = Path.of(System.getProperty("user.home"), ".connect4gui_profile.txt");

    private String playerName = "Oyuncu";
    private int xp;                  // Toplam deneyim puanı
    private int level = 1;           // Mevcut seviye (xp'den hesaplanır)
    private int totalGames;          // Oynanan toplam maç
    private int wins;                // Galibiyet sayısı
    private int losses;              // Kayıp sayısı
    private int winStreak;           // Anlık galibiyet serisi
    private int bestWinStreak;       // En iyi galibiyet serisi (rekor)
    private boolean beatAiMedium;    // Orta AI yenildi mi?
    private boolean beatAiHard;      // Zor AI yenildi mi?
    private boolean beatAiExpert;    // Uzman AI yenildi mi?
    private boolean lightMode;       // Tercih edilen görüntü modu
    private final Set<Integer> themesUsed = new HashSet<>();          // Denenen temalar
    private final Set<String> unlockedAchievements = new HashSet<>(); // Açılan başarım kimlikleri
    private final List<String> gameHistory = new ArrayList<>();       // Maç geçmişi (en yeni başta)

    /**
     * Profili diskten yükler. Dosya yoksa varsayılan (boş) profil döner.
     * Her satırı "anahtar=değer" olarak ayrıştırır.
     */
    public static PlayerProfile load() {
        PlayerProfile profile = new PlayerProfile();
        if (!Files.exists(SAVE_PATH)) {
            return profile; // İlk çalıştırma
        }
        try {
            List<String> lines = Files.readAllLines(SAVE_PATH);
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length < 2) {
                    continue; // Bozuk satırı atla
                }
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch (key) {
                    case "name" -> profile.playerName = value;
                    case "xp" -> profile.xp = Integer.parseInt(value);
                    case "level" -> profile.level = Integer.parseInt(value);
                    case "totalGames" -> profile.totalGames = Integer.parseInt(value);
                    case "wins" -> profile.wins = Integer.parseInt(value);
                    case "losses" -> profile.losses = Integer.parseInt(value);
                    case "winStreak" -> profile.winStreak = Integer.parseInt(value);
                    case "bestWinStreak" -> profile.bestWinStreak = Integer.parseInt(value);
                    case "beatAiMedium" -> profile.beatAiMedium = Boolean.parseBoolean(value);
                    case "beatAiHard" -> profile.beatAiHard = Boolean.parseBoolean(value);
                    case "beatAiExpert" -> profile.beatAiExpert = Boolean.parseBoolean(value);
                    case "lightMode" -> profile.lightMode = Boolean.parseBoolean(value);
                    case "themes" -> { // Virgülle ayrılmış tema indeksleri
                        if (!value.isEmpty()) {
                            for (String t : value.split(",")) {
                                profile.themesUsed.add(Integer.parseInt(t.trim()));
                            }
                        }
                    }
                    case "achievements" -> { // Virgülle ayrılmış başarım kimlikleri
                        if (!value.isEmpty()) {
                            for (String a : value.split(",")) {
                                profile.unlockedAchievements.add(a.trim());
                            }
                        }
                    }
                    case "history" -> { // '|' ile ayrılmış maç kayıtları
                        if (!value.isEmpty()) {
                            for (String h : value.split("\\|")) {
                                profile.gameHistory.add(h);
                            }
                        }
                    }
                    default -> { }
                }
            }
            profile.recalculateLevel(); // XP'den seviyeyi yeniden doğrula
        } catch (Exception e) {
            System.err.println("Profil yüklenemedi: " + e.getMessage());
        }
        return profile;
    }

    /** Profili "anahtar=değer" satırları olarak diske yazar. */
    public void save() {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("name=" + playerName);
            lines.add("xp=" + xp);
            lines.add("level=" + level);
            lines.add("totalGames=" + totalGames);
            lines.add("wins=" + wins);
            lines.add("losses=" + losses);
            lines.add("winStreak=" + winStreak);
            lines.add("bestWinStreak=" + bestWinStreak);
            lines.add("beatAiMedium=" + beatAiMedium);
            lines.add("beatAiHard=" + beatAiHard);
            lines.add("beatAiExpert=" + beatAiExpert);
            lines.add("lightMode=" + lightMode);
            lines.add("themes=" + String.join(",", themesUsed.stream().map(String::valueOf).toList()));
            lines.add("achievements=" + String.join(",", unlockedAchievements));
            lines.add("history=" + String.join("|", gameHistory));
            Files.write(SAVE_PATH, lines);
        } catch (IOException e) {
            System.err.println("Profil kaydedilemedi: " + e.getMessage());
        }
    }

    /** XP ekler ve seviyeyi günceller. */
    public void addXp(int amount) {
        xp += amount;
        recalculateLevel();
    }

    /**
     * Toplam XP'den mevcut seviyeyi hesaplar. Her seviye bir öncekinden
     * daha fazla XP gerektirir (eşik 200'den başlar, her seviyede +80 artar).
     */
    private void recalculateLevel() {
        int newLevel = 1;
        int threshold = 0;
        int step = 200;
        while (xp >= threshold + step) {
            threshold += step;
            newLevel++;
            step += 80;
        }
        level = newLevel;
    }

    /** Bir sonraki seviyeye ulaşmak için gereken kalan XP. */
    public int xpToNextLevel() {
        int threshold = 0;
        int step = 200;
        for (int i = 1; i < level; i++) {
            threshold += step;
            step += 80;
        }
        return threshold + step - xp;
    }

    /** Mevcut seviyedeki ilerlemenin yüzdesi (0-100). */
    public int levelProgressPercent() {
        int threshold = 0;
        int step = 200;
        for (int i = 1; i < level; i++) {
            threshold += step;
            step += 80;
        }
        int prev = threshold;          // Bu seviyenin başlangıç XP'si
        int next = threshold + step;   // Sonraki seviyenin XP'si
        if (next == prev) {
            return 100;
        }
        return (int) (((xp - prev) * 100.0) / (next - prev));
    }

    /** Kullanılan bir temayı kaydeder (başarım takibi için). */
    public void recordThemeUsed(int theme) {
        themesUsed.add(theme);
    }

    // Geçmiş kayıtları için tarih-saat biçimi
    private static final DateTimeFormatter HISTORY_TIME =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Maç geçmişine tarih damgalı bir kayıt ekler (en fazla 30 kayıt tutulur). */
    public void addHistory(String entry) {
        String stamp = LocalDateTime.now().format(HISTORY_TIME);
        gameHistory.add(0, "[" + stamp + "] " + entry); // En yeni başa
        while (gameHistory.size() > 30) {
            gameHistory.remove(gameHistory.size() - 1);  // En eskiyi at
        }
    }

    /**
     * Henüz açılmamış ama koşulu sağlanan başarımları açar, XP ödülünü ekler
     * ve yeni açılanların başlıklarını döndürür (ekranda kutlama için).
     */
    public List<String> newlyUnlocked(List<Achievement> all) {
        List<String> fresh = new ArrayList<>();
        for (Achievement a : all) {
            if (!unlockedAchievements.contains(a.getId()) && isAchievementUnlocked(a)) {
                unlockedAchievements.add(a.getId());
                addXp(a.getXpReward());
                fresh.add(a.getTitle());
            }
        }
        return fresh;
    }

    /** Bir başarımın açılma koşulunun sağlanıp sağlanmadığını kontrol eder. */
    public boolean isAchievementUnlocked(Achievement a) {
        if (unlockedAchievements.contains(a.getId())) {
            return true; // Zaten açık
        }
        return switch (a.getId()) {
            case "first_win" -> wins >= 1;
            case "ai_medium" -> beatAiMedium;
            case "ai_hard" -> beatAiHard;
            case "ai_expert" -> beatAiExpert;
            case "streak_3" -> bestWinStreak >= 3;
            case "games_10" -> totalGames >= 10;
            case "level_5" -> level >= 5;
            case "level_10" -> level >= 10;
            case "themes_all" -> themesUsed.size() >= 5;
            case "marathon" -> totalGames >= 25;
            default -> level >= a.getRequiredLevel();
        };
    }

    // ---- Erişimciler (getter/setter) ----
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getXp() { return xp; }
    public int getLevel() { return level; }
    public int getTotalGames() { return totalGames; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getWinStreak() { return winStreak; }
    public int getBestWinStreak() { return bestWinStreak; }
    public Set<String> getUnlockedAchievements() { return unlockedAchievements; }
    public List<String> getGameHistory() { return gameHistory; }

    public void setBeatAiMedium(boolean beatAiMedium) { this.beatAiMedium = beatAiMedium; }
    public void setBeatAiHard(boolean beatAiHard) { this.beatAiHard = beatAiHard; }
    public void setBeatAiExpert(boolean beatAiExpert) { this.beatAiExpert = beatAiExpert; }
    public boolean isLightMode() { return lightMode; }
    public void setLightMode(boolean lightMode) { this.lightMode = lightMode; }

    // ---- İstatistik güncelleyiciler ----
    public void incrementGames() { totalGames++; }

    /** Galibiyet kaydı: seri artar, rekor güncellenir. */
    public void recordWin() {
        wins++;
        winStreak++;
        bestWinStreak = Math.max(bestWinStreak, winStreak);
    }

    /** Kayıp kaydı: seri sıfırlanır. */
    public void recordLoss() {
        losses++;
        winStreak = 0;
    }

    /** Beraberlik: seri sıfırlanır. */
    public void recordDraw() {
        winStreak = 0;
    }
}
