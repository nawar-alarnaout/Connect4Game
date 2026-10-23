package util;

import core.constants.Config;
import javafx.scene.media.AudioClip;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * SoundManager — Ses efektlerini yöneten yardımcı sınıf.
 *
 * <p>İlginç yanı: harici WAV dosyası kullanmaz. Her efekti, belirli bir
 * frekans/süredeki sinüs dalgasından matematiksel olarak üretip geçici bir
 * WAV dosyasına yazar ve {@link AudioClip} ile çalar. Böylece proje hiçbir
 * ses dosyasına bağımlı değildir.
 */
public final class SoundManager {

    /** Oyundaki ses efekti türleri. */
    public enum Sound {
        DROP, WIN, LOSE, DRAW, CLICK, COUNTDOWN, UNLOCK, MENU
    }

    private static final Map<Sound, AudioClip> CLIPS = new HashMap<>();
    private static boolean initialized;

    private SoundManager() {} // Örneklenemez

    /**
     * Tüm ses efektlerini bir kez üretir. Her ses için (frekans, süre, ses düzeyi)
     * değerleri o efektin karakterini belirler (örn. WIN tiz ve uzun, LOSE pes ve uzun).
     */
    public static void init() {
        if (initialized) {
            return;
        }
        try {
            load(Sound.DROP, 520, 90, 0.35);       // Taş düşme
            load(Sound.WIN, 880, 220, 0.4);        // Zafer
            load(Sound.LOSE, 180, 350, 0.35);      // Yenilgi
            load(Sound.DRAW, 330, 180, 0.3);       // Beraberlik
            load(Sound.CLICK, 760, 50, 0.25);      // Tıklama
            load(Sound.COUNTDOWN, 640, 70, 0.3);   // Geri sayım
            load(Sound.UNLOCK, 1040, 300, 0.4);    // Ödül açılması
            load(Sound.MENU, 420, 120, 0.2);       // Menü geçişi
            initialized = true;
        } catch (IOException e) {
            System.err.println("Ses efektleri yüklenemedi: " + e.getMessage());
        }
    }

    /** Tek bir efekti üretir: geçici WAV dosyası yazar ve AudioClip olarak yükler. */
    private static void load(Sound sound, double freq, int durationMs, double volume) throws IOException {
        File temp = Files.createTempFile("c4_" + sound.name().toLowerCase(), ".wav").toFile();
        temp.deleteOnExit(); // Uygulama kapanınca geçici dosyayı sil
        writeToneWav(temp, freq, durationMs);
        AudioClip clip = new AudioClip(temp.toURI().toString());
        clip.setVolume(volume);
        CLIPS.put(sound, clip);
    }

    /** Bir efekti çalar (ses kapalıysa veya yüklenmemişse hiçbir şey yapmaz). */
    public static void play(Sound sound) {
        if (!Config.soundEnabled || !initialized) {
            return;
        }
        AudioClip clip = CLIPS.get(sound);
        if (clip != null) {
            clip.play();
        }
    }

    /**
     * Belirtilen frekans ve sürede bir sinüs tonu üretip WAV dosyasına yazar.
     * Sonda sönen bir "envelope" ile ton yumuşakça kısılır (klik sesini önler).
     */
    private static void writeToneWav(File file, double frequency, int durationMs) throws IOException {
        int sampleRate = 22050;
        int samples = sampleRate * durationMs / 1000;
        byte[] data = new byte[samples * 2]; // 16-bit örnekler
        for (int i = 0; i < samples; i++) {
            double t = (double) i / sampleRate;
            double envelope = 1.0 - ((double) i / samples);  // Zamanla sönüş
            short value = (short) (Math.sin(2 * Math.PI * frequency * t) * 12000 * envelope);
            data[i * 2] = (byte) (value & 0xff);             // Düşük bayt
            data[i * 2 + 1] = (byte) ((value >> 8) & 0xff);  // Yüksek bayt
        }
        byte[] wav = buildWav(data, sampleRate);
        Files.write(file.toPath(), wav);
    }

    /** Ham PCM verisinin başına 44 baytlık standart WAV başlığı ekler. */
    private static byte[] buildWav(byte[] pcm, int sampleRate) throws IOException {
        int dataLen = pcm.length;
        int total = 36 + dataLen;
        byte[] header = new byte[44];
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F'; // "RIFF"
        writeInt(header, 4, total);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E'; // "WAVE"
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' '; // "fmt "
        writeInt(header, 16, 16);              // fmt blok uzunluğu
        writeShort(header, 20, (short) 1);     // PCM formatı
        writeShort(header, 22, (short) 1);     // Kanal sayısı (mono)
        writeInt(header, 24, sampleRate);      // Örnekleme hızı
        writeInt(header, 28, sampleRate * 2);  // Byte/sn
        writeShort(header, 32, (short) 2);     // Blok hizalama
        writeShort(header, 34, (short) 16);    // Bit derinliği
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a'; // "data"
        writeInt(header, 40, dataLen);
        byte[] out = new byte[header.length + pcm.length];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(pcm, 0, out, header.length, pcm.length);
        return out;
    }

    /** Little-endian 32-bit tamsayıyı tampona yazar. */
    private static void writeInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
        buf[offset + 2] = (byte) ((value >> 16) & 0xff);
        buf[offset + 3] = (byte) ((value >> 24) & 0xff);
    }

    /** Little-endian 16-bit tamsayıyı tampona yazar. */
    private static void writeShort(byte[] buf, int offset, short value) {
        buf[offset] = (byte) (value & 0xff);
        buf[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
}
