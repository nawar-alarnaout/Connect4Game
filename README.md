# Connect 4 GUI — Java Dersi Projesi

JavaFX tabanlı Connect 4 oyunu. Tek bilgisayarda, AI'ye karşı veya **iki farklı bilgisayar** üzerinden ağ oyunu destekler.

## Ekip

| Rol | Geliştirici |
|-----|-------------|
| **Frontend** | Mustafa |
| **Backend** | Nawar |
| **AI & Ağ Entegrasyonu** | Ruslan |

## Özellikler

- JavaFX GUI (animasyonlar, ses, temalar, ödül sistemi)
- Yerel 2 kişilik ve AI modları (Kolay / Orta / Zor / Uzman)
- **Ağ oyunu**: İki bilgisayar, aynı Wi‑Fi / LAN üzerinden
- Minimax AI + TCP soket sunucusu (port **5555**)

---

## İki Bilgisayardan Nasıl Oynanır?

Her iki bilgisayarda da **aynı Connect4GUI projesi** çalıştırılmalıdır.

### Adım 1 — Aynı ağa bağlanın
İki PC de aynı Wi‑Fi veya ethernet ağında olmalı (ör. ev / okul ağı).

### Adım 2 — Bilgisayar A (Sunucu / Oyuncu 1)
1. Oyunu başlatın → Ana menü → **🌐 Ağ Oyunu (2 Bilgisayar)**
2. Adınızı girin → **🖥 Oda Kur & Bekle**
3. Ekranda görünen **IP adresini** not alın (ör. `192.168.1.15`)
4. Rakip bağlanana kadar bekleyin

### Adım 3 — Bilgisayar B (İstemci / Oyuncu 2)
1. Oyunu başlatın → Ana menü → **🌐 Ağ Oyunu (2 Bilgisayar)**
2. Adınızı girin
3. **Sunucu IP** alanına Bilgisayar A'nın IP'sini yazın (ör. `192.168.1.15`)
4. **🔗 Odaya Katıl** → Oyun otomatik başlar

### Adım 4 — Oyna
- **Oyuncu 1 (Sunucu)**: Kırmızı/mor disk — ilk hamle
- **Oyuncu 2 (İstemci)**: Sarı/yeşil disk
- Sıra sizdeyken sütuna tıklayın; hamle anında karşı tarafa iletilir
- **Tekrar Oyna** yalnızca sunucu (Oyuncu 1) tarafından başlatılabilir

### Bağlantı sorunları
- Windows Güvenlik Duvarı'nda **port 5555** için Java'ya izin verin
- IP adresini `ipconfig` (Windows) veya `ip a` (Linux) ile doğrulayın
- `127.0.0.1` yalnızca aynı bilgisayarda test içindir; iki PC için gerçek LAN IP kullanın
- Antivirüs / okul ağı bazen soket bağlantısını engelleyebilir

---

## Gereksinimler

- Java 17+
- JavaFX SDK (`javafx-sdk-26.0.1` önerilir)

## Eclipse

1. **Import** → `Connect4GUI`
2. User Library `JavaFX` → `javafx-sdk-26.0.1/lib` içindeki JAR'lar
3. Run: `ui.Connect4App`
4. VM args:
   ```
   --module-path "C:/Users/USER/Dev/javafx-sdk-26.0.1/lib" --add-modules javafx.controls,javafx.media,javafx.graphics --enable-native-access=javafx.graphics,javafx.media
   ```

## VS Code

1. Klasörü açın → **F5**

## Komut satırı

```powershell
$JFX = "C:\Users\USER\Dev\javafx-sdk-26.0.1\lib"
javac --module-path $JFX --add-modules javafx.controls,javafx.media,javafx.graphics -d bin -sourcepath src src/module-info.java src/core/constants/Config.java src/core/logic/*.java src/core/model/*.java src/core/network/*.java src/util/*.java src/ui/*.java
java --module-path "$JFX;bin" --add-modules javafx.controls,javafx.media,javafx.graphics --module Connect4GUI/ui.Connect4App
```

## Proje yapısı

```
src/
  core/logic/     → Tahta + AI
  core/network/   → TCP sunucu/istemci (Backend — Nawar / Ruslan)
  core/model/     → Profil, ödüller
  ui/             → Ekranlar (Frontend — Mustafa)
  util/           → Ses, UI yardımcıları
```

## Ağ protokolü (özet)

| Mesaj | Açıklama |
|-------|----------|
| `JOIN\|isim` | İstemci bağlanır |
| `WELCOME\|2\|host\|client` | Hoş geldin |
| `MOVE\|sütun` | Hamle gönder |
| `RESULT\|...` | Hamle sonucu |
| `RESET\|` | Yeni maç (sunucu) |

Port: **5555** (`Config.NETWORK_PORT`)
