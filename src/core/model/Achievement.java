package core.model;

/**
 * Achievement — Tek bir başarımın (kupanın) değişmez (immutable) tanımı.
 *
 * <p>Yalnızca veri taşır: kimlik, başlık, açıklama, ikon (emoji), açılması için
 * gereken seviye ve verdiği XP ödülü. Tüm alanlar {@code final}'dır.
 */
public class Achievement {

    private final String id;          // Benzersiz kimlik (örn. "first_win")
    private final String title;       // Görünen başlık
    private final String description; // Kısa açıklama / koşul
    private final String icon;        // Emoji ikon
    private final int requiredLevel;  // Varsayılan açılma koşulu için gereken seviye
    private final int xpReward;       // Açıldığında verilen XP

    public Achievement(String id, String title, String description, String icon, int requiredLevel, int xpReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.requiredLevel = requiredLevel;
        this.xpReward = xpReward;
    }

    // ---- Salt-okunur erişimciler ----
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getXpReward() {
        return xpReward;
    }
}
