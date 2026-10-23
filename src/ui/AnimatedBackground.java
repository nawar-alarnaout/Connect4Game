package ui;

import core.constants.Config;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AnimatedBackground — Menü ve ekranların canlı arka planı (premium görünüm).
 *
 * <p>Katmanlar (arkadan öne): nefes alan gradyan, yumuşak bokeh küreler, düşen
 * Connect-4 temalı diskler, birbirine bağlanan parçacık takımyıldızı ve bir vinyet.
 * Hem koyu hem açık modda çalışır. Tamamen Canvas çizimidir; video/görsel dosya gerektirmez.
 */
public class AnimatedBackground extends Canvas {

    // Sahnedeki öğe sayıları ve parçacık bağlantı mesafesi
    private static final int PARTICLE_COUNT = 70;
    private static final int ORB_COUNT = 6;
    private static final int DISC_COUNT = 9;
    private static final double LINK_DIST = 150;                       // Bağlantı eşik mesafesi
    private static final double LINK_DIST_SQ = LINK_DIST * LINK_DIST;  // Karesi (karekök maliyetinden kaçınmak için)

    /** Parıldayan küçük parçacık. */
    private static class Particle {
        double x, y, vx, vy, radius, alpha, alphaDir;
        Color color;
    }

    /** Yumuşak, büyük ışık kümesi (bokeh). */
    private static class Orb {
        double x, y, radius, dx, dy, alpha;
        Color color;
    }

    /** Yavaşça düşen, sallanan Connect-4 diski. */
    private static class FallingDisc {
        double x, y, radius, speed, drift, wobblePhase, wobbleAmp, alpha;
        Color color;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final List<Orb> orbs = new ArrayList<>();
    private final List<FallingDisc> discs = new ArrayList<>();
    private final Random rng = new Random();
    private AnimationTimer timer; // Kare-kare animasyon döngüsü
    private long lastFrame;
    private double time;          // Geçen toplam zaman (gradyan dalgalanması için)
    private double seededFor = -1;// Hangi genişlik için tohumlandığı

    public AnimatedBackground(double w, double h) {
        super(w, h);
        setMouseTransparent(true); // Fare olaylarını alttaki öğelere geçir
        seed();
    }

    /** Animasyon döngüsünü başlatır (zaten çalışıyorsa tekrar başlatmaz). */
    public void start() {
        lastFrame = 0;
        if (timer != null) {
            return;
        }
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Kareler arası geçen süreyi (dt) hesapla; ilk karede 1.0 say.
                double dt = lastFrame == 0 ? 1.0 : (now - lastFrame) / 16_666_666.0;
                lastFrame = now;
                dt = Math.min(dt, 3.0); // Takılma sonrası sıçramayı sınırla
                time += dt;
                ensureSeeded();
                update(dt);
                render();
            }
        };
        timer.start();
    }

    /** Animasyon döngüsünü durdurur (kaynak tasarrufu için ekrandan çıkışta). */
    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        lastFrame = 0;
    }

    /** Her öğeyi güncel tema/mod paletiyle yeniden üretir. */
    public void refreshPalette() {
        seed();
    }

    /** Çalışma genişliği (henüz boyut yoksa varsayılan). */
    private double areaW() {
        double w = getWidth();
        return w > 0 ? w : Config.WINDOW_WIDTH;
    }

    /** Çalışma yüksekliği (henüz boyut yoksa varsayılan). */
    private double areaH() {
        double h = getHeight();
        return h > 0 ? h : Config.WINDOW_HEIGHT;
    }

    /** Canvas gerçek boyutuna kavuşunca öğeleri bir kez yeniden tohumlar. */
    private void ensureSeeded() {
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0 && Math.abs(w - seededFor) > 1) {
            seed();
        }
    }

    /** Tüm öğe listelerini temizleyip yeniden oluşturur. */
    private void seed() {
        seededFor = getWidth();
        particles.clear();
        orbs.clear();
        discs.clear();
        spawnParticles();
        spawnOrbs();
        spawnDiscs();
    }

    /** Moda göre parçacık renk paleti. */
    private Color[] particlePalette() {
        return Config.lightMode
            ? new Color[] {
                Config.COLOR_GLOW_BLUE, Config.COLOR_ACCENT,
                Color.web("#64748b"), Color.web("#94a3b8")
            }
            : new Color[] {
                Config.COLOR_GLOW_BLUE, Color.WHITE, Config.COLOR_ACCENT,
                Config.COLOR_PLAYER_1, Config.COLOR_PLAYER_2
            };
    }

    /** Rastgele konum, hız ve saydamlıkla parçacıkları oluşturur. */
    private void spawnParticles() {
        double w = areaW();
        double h = areaH();
        Color[] palette = particlePalette();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            Particle p = new Particle();
            p.x = rng.nextDouble() * w;
            p.y = rng.nextDouble() * h;
            double ang = rng.nextDouble() * Math.PI * 2;      // Rastgele yön
            double sp = rng.nextDouble() * 0.22 + 0.04;       // Rastgele hız
            p.vx = Math.cos(ang) * sp;
            p.vy = Math.sin(ang) * sp - 0.12;                 // Hafif yukarı sürüklenme
            p.radius = rng.nextDouble() * 1.8 + 0.7;
            p.alpha = rng.nextDouble() * 0.5 + 0.15;
            p.alphaDir = (0.004 + rng.nextDouble() * 0.008) * (rng.nextBoolean() ? 1 : -1); // Parıldama yönü
            p.color = palette[rng.nextInt(palette.length)];
            particles.add(p);
        }
    }

    /** Büyük, yavaş hareket eden bokeh kürelerini oluşturur. */
    private void spawnOrbs() {
        double w = areaW();
        double h = areaH();
        Color[] colors = Config.lightMode
            ? new Color[] {
                Config.COLOR_GLOW_BLUE, Config.COLOR_ACCENT,
                Config.COLOR_PLAYER_1, Color.web("#9ec5ff")
            }
            : new Color[] {
                Config.COLOR_GLOW_BLUE, Config.COLOR_ACCENT,
                Config.COLOR_PLAYER_1, Config.COLOR_PLAYER_2,
                Color.web("#1a237e")
            };
        for (int i = 0; i < ORB_COUNT; i++) {
            Orb o = new Orb();
            o.x = rng.nextDouble() * w;
            o.y = rng.nextDouble() * h;
            o.radius = rng.nextDouble() * 180 + 120;
            o.dx = (rng.nextDouble() - 0.5) * 0.25; // Çok yavaş kayma
            o.dy = (rng.nextDouble() - 0.5) * 0.25;
            o.color = colors[i % colors.length];
            o.alpha = Config.lightMode
                ? rng.nextDouble() * 0.05 + 0.04
                : rng.nextDouble() * 0.07 + 0.05;
            orbs.add(o);
        }
    }

    /** Düşen Connect-4 disklerini oluşturur. */
    private void spawnDiscs() {
        double w = areaW();
        double h = areaH();
        Color[] colors = { Config.COLOR_PLAYER_1, Config.COLOR_PLAYER_2, Config.COLOR_GLOW_BLUE };
        for (int i = 0; i < DISC_COUNT; i++) {
            FallingDisc d = new FallingDisc();
            d.x = rng.nextDouble() * w;
            d.y = rng.nextDouble() * h;
            d.radius = rng.nextDouble() * 16 + 12;
            d.speed = rng.nextDouble() * 0.5 + 0.25;          // Düşme hızı
            d.drift = (rng.nextDouble() - 0.5) * 0.25;        // Yana kayma
            d.wobblePhase = rng.nextDouble() * Math.PI * 2;   // Sallanma fazı
            d.wobbleAmp = rng.nextDouble() * 14 + 6;          // Sallanma genliği
            d.color = colors[rng.nextInt(colors.length)];
            d.alpha = Config.lightMode
                ? rng.nextDouble() * 0.07 + 0.05
                : rng.nextDouble() * 0.1 + 0.07;
            discs.add(d);
        }
    }

    /** Her karede tüm öğelerin konumunu ve durumunu günceller. */
    private void update(double dt) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        // Küreler: yavaşça kayar, ekrandan çıkınca karşı taraftan girer (sarmalama).
        for (Orb o : orbs) {
            o.x += o.dx * dt;
            o.y += o.dy * dt;
            if (o.x < -o.radius) o.x = w + o.radius;
            if (o.x > w + o.radius) o.x = -o.radius;
            if (o.y < -o.radius) o.y = h + o.radius;
            if (o.y > h + o.radius) o.y = -o.radius;
        }

        // Diskler: aşağı düşer ve sallanır; ekranın altına inince yukarıdan yeniden başlar.
        for (FallingDisc d : discs) {
            d.y += d.speed * dt;
            d.x += d.drift * dt;
            d.wobblePhase += 0.02 * dt;
            if (d.y - d.radius > h) {
                d.y = -d.radius - rng.nextDouble() * h * 0.3;
                d.x = rng.nextDouble() * w;
            }
            if (d.x < -d.radius) d.x = w + d.radius;
            if (d.x > w + d.radius) d.x = -d.radius;
        }

        // Parçacıklar: hareket eder ve saydamlıkları ileri-geri salınarak parıldar.
        for (Particle p : particles) {
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.alpha += p.alphaDir * dt;
            if (p.alpha > 0.85) { p.alpha = 0.85; p.alphaDir = -p.alphaDir; }
            if (p.alpha < 0.08) { p.alpha = 0.08; p.alphaDir = -p.alphaDir; }
            if (p.x < -6) p.x = w + 6;
            if (p.x > w + 6) p.x = -6;
            if (p.y < -6) p.y = h + 6;
            if (p.y > h + 6) p.y = -6;
        }
    }

    /** Tüm katmanları doğru sırayla (arkadan öne) çizer. */
    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        drawGradient(gc, w, h); // 1) Arka plan gradyanı + ışık huzmesi

        // 2) Bokeh küreler (radyal gradyanlı yumuşak ışık)
        for (Orb o : orbs) {
            RadialGradient rg = new RadialGradient(
                0, 0, o.x, o.y, o.radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, o.color.deriveColor(0, 1, 1, o.alpha * 3.2)),
                new Stop(0.55, o.color.deriveColor(0, 1, 1, o.alpha)),
                new Stop(1, o.color.deriveColor(0, 1, 1, 0))
            );
            gc.setFill(rg);
            gc.fillOval(o.x - o.radius, o.y - o.radius, o.radius * 2, o.radius * 2);
        }

        // 3) Düşen diskler (sallanma x ekseninde sinüs ile uygulanır)
        for (FallingDisc d : discs) {
            double dx = d.x + Math.sin(d.wobblePhase) * d.wobbleAmp;
            drawDisc(gc, dx, d.y, d.radius, d.color, d.alpha);
        }

        drawLinks(gc); // 4) Yakın parçacıkları çizgilerle bağla

        // 5) Parçacıklar (hafif halesiyle birlikte)
        for (Particle p : particles) {
            double r = p.radius;
            gc.setFill(p.color.deriveColor(0, 1, 1, p.alpha * 0.25));
            gc.fillOval(p.x - r * 2.4, p.y - r * 2.4, r * 4.8, r * 4.8); // Hale
            gc.setFill(p.color.deriveColor(0, 1, 1, p.alpha));
            gc.fillOval(p.x - r, p.y - r, r * 2, r * 2);                 // Çekirdek
        }

        drawVignette(gc, w, h); // 6) Kenarları karartan vinyet
    }

    /** Dikey gradyan + zamanla yavaşça hareket eden ışık huzmesi çizer. */
    private void drawGradient(GraphicsContext gc, double w, double h) {
        Color base = Config.COLOR_BG;
        double breathe = (Math.sin(time * 0.01) + 1) * 0.5; // 0..1 nefes alma katsayısı
        Color tint = Config.lightMode ? Config.COLOR_GLOW_BLUE : Config.COLOR_ACCENT;
        Color top = mix(base, tint, 0.05 + 0.05 * breathe);
        Color mid = base;
        Color bottom = Config.lightMode
            ? mix(base, Color.web("#c7d6ea"), 0.55)
            : mix(base, Color.BLACK, 0.45);

        LinearGradient lg = new LinearGradient(
            0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, top),
            new Stop(0.5, mid),
            new Stop(1, bottom)
        );
        gc.setFill(lg);
        gc.fillRect(0, 0, w, h);

        // Üstten gelen, yumuşak bir spot ışığı gibi yavaşça salınan parıltı.
        double cx = w * (0.5 + 0.25 * Math.sin(time * 0.006));
        double glowR = Math.max(w, h) * 0.7;
        RadialGradient wash = new RadialGradient(
            0, 0, cx, -h * 0.1, glowR, false, CycleMethod.NO_CYCLE,
            new Stop(0, Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, Config.lightMode ? 0.10 : 0.08)),
            new Stop(1, Config.COLOR_GLOW_BLUE.deriveColor(0, 1, 1, 0))
        );
        gc.setFill(wash);
        gc.fillRect(0, 0, w, h);
    }

    /** Tek bir arka plan diskini hale, gövde ve ortadaki delikle çizer. */
    private void drawDisc(GraphicsContext gc, double x, double y, double r, Color color, double alpha) {
        // Dış hale
        RadialGradient glow = new RadialGradient(
            0, 0, x, y, r * 1.6, false, CycleMethod.NO_CYCLE,
            new Stop(0, color.deriveColor(0, 1, 1, alpha * 1.4)),
            new Stop(1, color.deriveColor(0, 1, 1, 0))
        );
        gc.setFill(glow);
        gc.fillOval(x - r * 1.6, y - r * 1.6, r * 3.2, r * 3.2);

        // Disk gövdesi
        gc.setFill(color.deriveColor(0, 1, 1, alpha));
        gc.fillOval(x - r, y - r, r * 2, r * 2);

        // Ortadaki delik, klasik Connect-4 pulunun silüetini verir.
        gc.setFill(Config.COLOR_BG.deriveColor(0, 1, 1, alpha * 4));
        gc.fillOval(x - r * 0.42, y - r * 0.42, r * 0.84, r * 0.84);
    }

    /** Birbirine yakın parçacıkları, mesafeye göre soluklaşan çizgilerle bağlar. */
    private void drawLinks(GraphicsContext gc) {
        Color link = Config.lightMode ? Config.COLOR_GLOW_BLUE : Color.WHITE;
        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);
                double ddx = a.x - b.x;
                double ddy = a.y - b.y;
                double dsq = ddx * ddx + ddy * ddy;
                if (dsq < LINK_DIST_SQ) {                 // Yeterince yakınsa bağla
                    double strength = 1 - dsq / LINK_DIST_SQ; // Yakınlık = çizgi gücü
                    double aLine = strength * (Config.lightMode ? 0.10 : 0.13);
                    gc.setStroke(link.deriveColor(0, 1, 1, aLine));
                    gc.setLineWidth(strength * 1.1);
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
    }

    /** Kenarları karartarak (veya açık modda yumuşatarak) odak hissi veren vinyet. */
    private void drawVignette(GraphicsContext gc, double w, double h) {
        double r = Math.hypot(w, h) * 0.62;
        Color edge = Config.lightMode ? Color.web("#9aa9bd") : Color.BLACK;
        RadialGradient vg = new RadialGradient(
            0, 0, w * 0.5, h * 0.5, r, false, CycleMethod.NO_CYCLE,
            new Stop(0, edge.deriveColor(0, 1, 1, 0)),
            new Stop(0.72, edge.deriveColor(0, 1, 1, 0)),
            new Stop(1, edge.deriveColor(0, 1, 1, Config.lightMode ? 0.22 : 0.45))
        );
        gc.setFill(vg);
        gc.fillRect(0, 0, w, h);
    }

    /** İki rengi f oranında (0..1) doğrusal karıştırır. */
    private static Color mix(Color a, Color b, double f) {
        f = Math.max(0, Math.min(1, f));
        return new Color(
            a.getRed() + (b.getRed() - a.getRed()) * f,
            a.getGreen() + (b.getGreen() - a.getGreen()) * f,
            a.getBlue() + (b.getBlue() - a.getBlue()) * f,
            1.0
        );
    }
}
