// ===================================================================
//  module-info.java — JPMS (Java Platform Module System) modül tanımı
//  Projeyi "Connect4GUI" adlı tek bir modül olarak paketler ve
//  ihtiyaç duyduğu JavaFX modüllerini açıkça bildirir.
// ===================================================================
module Connect4GUI {
    requires javafx.controls;   // Label, TextField, ScrollPane gibi UI bileşenleri
    requires javafx.graphics;   // Sahne grafiği, Canvas, animasyonlar
    requires javafx.media;      // Ses çalma (AudioClip)
    requires javafx.base;       // Temel JavaFX altyapısı (property, collections)

    // JavaFX, yansıma (reflection) ile bu paketlere erişebilsin diye "opens".
    opens ui to javafx.graphics;
    opens core.logic to javafx.graphics;
    opens core.constants to javafx.graphics;
    opens core.model to javafx.graphics;
    opens util to javafx.graphics;
    opens core.network to javafx.graphics;
}
