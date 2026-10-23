package core.network;

import core.constants.Config;
import core.logic.GameLogic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * NetworkClient — Ağ oyununun istemci ucu.
 *
 * <p>Sunucuya bağlanır, kendi hamlelerini gönderir ve sunucudan gelen onaylı
 * sonuçları yerel bir <b>ayna</b> tahtaya ({@code mirrorLogic}) uygulayarak
 * arayüzü günceller. İstemci yetkisizdir: hamleyi yalnızca önerir, kararı sunucu verir.
 * İstemci varsayılan olarak Oyuncu 2'dir ve yeni maç (reset) başlatamaz.
 */
public class NetworkClient implements NetworkPeer {

    private final GameLogic mirrorLogic = new GameLogic(); // Sunucunun durumunun yansıması

    // Gelen mesajları okumak için tek bir daemon thread.
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "connect4-client");
        t.setDaemon(true);
        return t;
    });

    private Socket socket;             // Sunucu bağlantısı
    private PrintWriter writer;         // Sunucuya yazma
    private BufferedReader reader;       // Sunucudan okuma
    private volatile boolean running;
    private int localPlayer = Config.PLAYER_2; // Karşılama gelene kadar varsayım

    // Olay dinleyicileri
    private Consumer<NetworkMoveResult> moveListener;
    private Runnable disconnectListener;
    private Consumer<String> errorListener;
    private Consumer<WelcomeInfo> welcomeListener;

    /** Sunucudan gelen karşılama bilgisini taşıyan kayıt (record). */
    public record WelcomeInfo(int player, String hostName, String clientName) { }

    /** Verilen IP'ye bağlanır, JOIN gönderir ve okuma döngüsünü başlatır. */
    public void connect(String hostIp, String playerName) throws IOException {
        socket = new Socket(hostIp, Config.NETWORK_PORT);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        writer.println(NetworkProtocol.encodeJoin(playerName)); // Kendini tanıt
        executor.submit(this::readLoop);
    }

    /** Sunucudan gelen satırları sürekli okur. */
    private void readLoop() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            if (running) {
                notifyError("Bağlantı koptu: " + e.getMessage());
                notifyDisconnect();
            }
        }
    }

    /** Gelen tek bir protokol satırını türüne göre işler. */
    private void handleLine(String line) {
        if (line.startsWith(NetworkProtocol.WELCOME + "|")) {
            // Karşılama: oyuncu numaramızı ve isimleri öğren.
            String[] p = line.split("\\|");
            localPlayer = Integer.parseInt(p[1]);
            if (welcomeListener != null) {
                welcomeListener.accept(new WelcomeInfo(localPlayer, p[2], p[3]));
            }
        } else if (line.startsWith(NetworkProtocol.RESULT + "|")) {
            // Onaylı hamle: ayna tahtaya aynı hamleyi uygula, arayüze bildir.
            NetworkMoveResult result = NetworkProtocol.parseResult(line);
            mirrorLogic.currentPlayerForAi(result.getPlayer());
            mirrorLogic.dropDisc(result.getCol());
            if (moveListener != null) {
                moveListener.accept(result);
            }
        } else if (line.startsWith(NetworkProtocol.RESET)) {
            // Yeni maç: aynayı sıfırla ve özel bir "sıfırlama" sonucu yolla.
            mirrorLogic.reset();
            if (moveListener != null) {
                moveListener.accept(new NetworkMoveResult(-1, -1, 0, Config.PLAYER_1, false, Config.EMPTY, null));
            }
        } else if (line.startsWith(NetworkProtocol.ERROR + "|")) {
            notifyError(line.split("\\|", 2)[1]);
        } else if (line.startsWith(NetworkProtocol.DISCONNECT)) {
            notifyDisconnect();
        }
    }

    /** Seçilen sütunu sunucuya MOVE olarak gönderir. */
    @Override
    public void sendMove(int col) {
        if (writer != null) {
            writer.println(NetworkProtocol.encodeMove(col));
        }
    }

    @Override
    public void sendReset() {
        // İstemci reset başlatamaz (yetki sunucudadır).
    }

    /** Karşılama mesajı geldiğinde tetiklenecek dinleyici. */
    public void setWelcomeListener(Consumer<WelcomeInfo> listener) {
        this.welcomeListener = listener;
    }

    @Override
    public void setMoveListener(Consumer<NetworkMoveResult> listener) {
        this.moveListener = listener;
    }

    @Override
    public void setDisconnectListener(Runnable listener) {
        this.disconnectListener = listener;
    }

    @Override
    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = listener;
    }

    /** Sunucuya ayrılma bildirir ve tüm kaynakları kapatır. */
    @Override
    public void disconnect() {
        running = false;
        try {
            if (writer != null) {
                writer.println(NetworkProtocol.DISCONNECT + "|");
            }
        } catch (Exception ignored) { }
        try { if (reader != null) reader.close(); } catch (IOException ignored) { }
        try { if (writer != null) writer.close(); } catch (Exception ignored) { }
        try { if (socket != null) socket.close(); } catch (IOException ignored) { }
        executor.shutdownNow();
    }

    private void notifyDisconnect() {
        if (disconnectListener != null) {
            disconnectListener.run();
        }
    }

    private void notifyError(String msg) {
        if (errorListener != null) {
            errorListener.accept(msg);
        }
    }

    @Override
    public boolean isHost() { return false; }

    @Override
    public int getLocalPlayer() { return localPlayer; }

    @Override
    public GameLogic getSharedLogic() { return mirrorLogic; }
}
