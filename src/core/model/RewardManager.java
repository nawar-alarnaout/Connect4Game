package core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * RewardManager — Tüm başarımların merkezi kayıt defteri ve XP ödül tablosu.
 *
 * <p>Statik bir başlatma bloğunda 10 başarımı tanımlar ve maç sonucuna göre
 * verilecek XP miktarını hesaplayan yardımcı metotlar sunar.
 */
public final class RewardManager {

    // Oyundaki tüm başarımların sabit listesi.
    private static final List<Achievement> ACHIEVEMENTS = new ArrayList<>();

    // Sınıf ilk yüklendiğinde başarımları bir kez kaydet.
    static {
        ACHIEVEMENTS.add(new Achievement("first_win", "İlk Zafer", "İlk oyununu kazan", "🏅", 1, 50));
        ACHIEVEMENTS.add(new Achievement("ai_medium", "Orta Seviye Avcı", "Orta zorlukta AI'yi yen", "🥉", 2, 120));
        ACHIEVEMENTS.add(new Achievement("ai_hard", "Zorlu Rakip", "Zor modda AI'yi yen", "🥈", 4, 200));
        ACHIEVEMENTS.add(new Achievement("ai_expert", "Usta Stratejist", "Uzman modda AI'yi yen", "🥇", 6, 350));
        ACHIEVEMENTS.add(new Achievement("streak_3", "Seri Kazanan", "3 maç üst üste kazan", "🔥", 3, 100));
        ACHIEVEMENTS.add(new Achievement("games_10", "Deneyimli Oyuncu", "10 maç tamamla", "🎮", 2, 80));
        ACHIEVEMENTS.add(new Achievement("level_5", "Bronz Kupa", "5. seviyeye ulaş", "🏆", 5, 150));
        ACHIEVEMENTS.add(new Achievement("level_10", "Altın Kupa", "10. seviyeye ulaş", "👑", 10, 300));
        ACHIEVEMENTS.add(new Achievement("themes_all", "Renk Ustası", "Tüm temaları dene", "🎨", 3, 90));
        ACHIEVEMENTS.add(new Achievement("marathon", "Maratoncu", "25 maç oyna", "⚡", 5, 180));
    }

    private RewardManager() {} // Örneklenemez

    /** Tüm başarım tanımlarını döndürür. */
    public static List<Achievement> getAllAchievements() {
        return ACHIEVEMENTS;
    }

    /** Galibiyette zorluğa göre kazanılan XP (zorluk arttıkça artar). */
    public static int xpForWin(int difficulty) {
        return switch (difficulty) {
            case 0 -> 60;   // 2 kişilik
            case 1 -> 80;   // Kolay
            case 2 -> 120;  // Orta
            case 3 -> 180;  // Zor
            case 4 -> 250;  // Uzman
            default -> 50;
        };
    }

    /** Beraberlikte sabit XP. */
    public static int xpForDraw() {
        return 25;
    }

    /** Yenilgide teselli XP'si (AI'ye karşı biraz daha fazla). */
    public static int xpForLoss(int difficulty) {
        return difficulty > 0 ? 15 : 10;
    }
}
