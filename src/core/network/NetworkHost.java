package core.network;

import core.constants.Config;
import core.logic.GameLogic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * NetworkHost — Ağ oyununun sunucu (otoriter) ucu.
 *
 * <p>Port 5555'te bir {@link ServerSocket} açar, istemci bağlantısını bekler ve
 * oyunun tek <b>doğru</b> kopyasını ({@link GameLogic}) tutar. Tüm hamleler önce
 * burada doğrulanır, sonra her iki tarafa yayınlanır — bu, tutarsızlığı ve
 * hileyi önler. Sunucu her zaman Oyuncu 1'dir.
 */
public class NetworkHost implements NetworkPeer {

    private final GameLogic gameLogic = new GameLogic(); // Otoriter oyun durumu
    private final String hostName;

    // Ağ işlemlerini arayüzü bloklamadan yürütmek için daemon thread havuzu.
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "connect4-host");
        t.setDaemon(true);
        return t;
    });

    private ServerSocket serverSocket;     // Bağlantıları dinleyen soket
    private Socket clientSocket;            // Bağlanan istemci soketi
    private PrintWriter clientWriter;       // İstemciye yazma
    private BufferedReader clientReader;    // İstemciden okuma
    private String clientName = "Oyuncu 2";

    // Olay dinleyicileri (arayüz tarafından ayarlanır)
    private Consumer<NetworkMoveResult> moveListener;
    private Runnable disconnectListener;
    private Consumer<String> errorListener;
    private Consumer<String> clientJoinedListener;
    private volatile boolean running;       // Sunucu çalışıyor mu? (thread'ler arası görünür)

    public NetworkHost(String hostName) {
        this.hostName = hostName;
    }

    /** Sunucuyu başlatır ve istemci kabul döngüsünü ayrı thread'de çalıştırır. */
    public void start(Consumer<String> onClientJoined) throws IOException {
        this.clientJoinedListener = onClientJoined;
        serverSocket = new ServerSocket(Config.NETWORK_PORT);
        running = true;
        executor.submit(this::acceptLoop);
    }

    /** Tek bir istemci bağlantısını kabul eder, karşılar ve okuma döngüsüne geçer. */
    private void acceptLoop() {
        try {
            clientSocket = serverSocket.accept(); // İstemci bağlanana kadar bekler
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            // İlk satır istemcinin JOIN mesajıdır → ismini al.
            String line = clientReader.readLine();
            if (line != null && line.startsWith(NetworkProtocol.JOIN + "|")) {
                clientName = line.split("\\|", 2)[1];
            }
            clientWriter.println(NetworkProtocol.encodeWelcome(2, hostName, clientName)); // Karşıla
            if (clientJoinedListener != null) {
                clientJoinedListener.accept(clientName); // Arayüze "rakip geldi" bildir
            }
            readClientLoop();
        } catch (IOException e) {
            if (running) {
                notifyError("Bağlantı hatası: " + e.getMessage());
                notifyDisconnect();
            }
        }
    }

    /** İstemciden gelen MOVE/DISCONNECT mesajlarını sürekli okur. */
    private void readClientLoop() throws IOException {
        String line;
        while (running && (line = clientReader.readLine()) != null) {
            if (line.startsWith(NetworkProtocol.MOVE + "|")) {
                int col = Integer.parseInt(line.split("\\|")[1]);
                processMove(col, Config.PLAYER_2); // İstemci = Oyuncu 2
            } else if (line.startsWith(NetworkProtocol.DISCONNECT)) {
                notifyDisconnect();
                break;
            }
        }
    }

    /** Yerel oyuncunun (sunucu = Oyuncu 1) hamlesini işler. */
    @Override
    public void sendMove(int col) {
        executor.submit(() -> processMove(col, Config.PLAYER_1));
    }

    /**
     * Bir hamleyi otoriter biçimde işler: sıra ve geçerlilik kontrolü yapar,
     * taşı bırakır ve sonucu hem istemciye yollar hem de yerel arayüze bildirir.
     * synchronized: iki thread'in (yerel + uzak) aynı anda oynamasını engeller.
     */
    private synchronized void processMove(int col, int expectedPlayer) {
        if (!running || gameLogic.isGameOver()) {
            return;
        }
        if (gameLogic.getCurrentPlayer() != expectedPlayer) {
            sendToClient(NetworkProtocol.encodeError("Sıra sende değil"));
            return;
        }
        if (!gameLogic.isColumnPlayable(col)) {
            sendToClient(NetworkProtocol.encodeError("Geçersiz sütun"));
            return;
        }

        int row = gameLogic.dropDisc(col);
        if (row < 0) {
            return;
        }

        // Sonucu paketleyip iki tarafa da dağıt.
        NetworkMoveResult result = NetworkProtocol.buildFromLogic(col, row, expectedPlayer, gameLogic);
        sendToClient(NetworkProtocol.encodeResult(result));
        if (moveListener != null) {
            moveListener.accept(result);
        }
    }

    /** Yeni maç: tahtayı sıfırlar ve istemciye RESET yollar. */
    @Override
    public void sendReset() {
        gameLogic.reset();
        sendToClient(NetworkProtocol.encodeReset());
    }

    /** İstemciye bir protokol mesajı gönderir. */
    private void sendToClient(String message) {
        if (clientWriter != null) {
            clientWriter.println(message);
        }
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

    /** Bağlantıyı kapatır: istemciye haber verir, soketleri kapatır, thread'leri durdurur. */
    @Override
    public void disconnect() {
        running = false;
        try {
            sendToClient(NetworkProtocol.DISCONNECT + "|");
        } catch (Exception ignored) { }
        closeQuietly();
        executor.shutdownNow();
    }

    /** Tüm ağ kaynaklarını hata fırlatmadan kapatır. */
    private void closeQuietly() {
        try { if (clientReader != null) clientReader.close(); } catch (IOException ignored) { }
        try { if (clientWriter != null) clientWriter.close(); } catch (Exception ignored) { }
        try { if (clientSocket != null) clientSocket.close(); } catch (IOException ignored) { }
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) { }
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
    public boolean isHost() { return true; }

    @Override
    public int getLocalPlayer() { return Config.PLAYER_1; } // Sunucu daima Oyuncu 1

    @Override
    public GameLogic getSharedLogic() { return gameLogic; }

    public String getClientName() { return clientName; }

    /** İstemci hâlâ bağlı mı? */
    public boolean isClientConnected() {
        return clientSocket != null && clientSocket.isConnected() && !clientSocket.isClosed();
    }
}
