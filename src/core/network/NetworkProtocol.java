package core.network;

import core.constants.Config;
import core.logic.GameLogic;

/**
 * NetworkProtocol — Satır tabanlı metin protokolünün kodlayıcı/çözücüsü.
 *
 * <p>Sunucu ve istemci, birbirine düz metin satırları gönderir. Her mesaj bir
 * komutla başlar ve alanlar '|' karakteriyle ayrılır. Örnek: {@code MOVE|3}.
 * Bu sınıf bu satırları üretmek (encode) ve çözmek (parse) için metotlar sunar.
 */
public final class NetworkProtocol {

    // Protokol komutları
    public static final String JOIN = "JOIN";             // İstemci bağlanır
    public static final String WELCOME = "WELCOME";       // Sunucu karşılar
    public static final String MOVE = "MOVE";             // Hamle isteği
    public static final String RESULT = "RESULT";         // Onaylı hamle sonucu
    public static final String RESET = "RESET";           // Yeni maç
    public static final String DISCONNECT = "DISCONNECT"; // Bağlantı kesme
    public static final String ERROR = "ERROR";           // Hata mesajı

    private NetworkProtocol() {} // Örneklenemez

    /** "JOIN|isim" mesajı üretir. */
    public static String encodeJoin(String name) {
        return JOIN + "|" + sanitize(name);
    }

    /** "WELCOME|oyuncuNo|hostAdı|clientAdı" mesajı üretir. */
    public static String encodeWelcome(int player, String hostName, String clientName) {
        return WELCOME + "|" + player + "|" + sanitize(hostName) + "|" + sanitize(clientName);
    }

    /** "MOVE|sütun" mesajı üretir. */
    public static String encodeMove(int col) {
        return MOVE + "|" + col;
    }

    /**
     * Bir hamle sonucunu tek satırlık RESULT mesajına kodlar. Kazandıran
     * hücreler "satır,sütun" çiftleri olarak ';' ile ayrılır; yoksa "none".
     */
    public static String encodeResult(NetworkMoveResult r) {
        StringBuilder cells = new StringBuilder("none");
        if (r.getWinningCells() != null) {
            cells = new StringBuilder();
            for (int i = 0; i < r.getWinningCells().length; i++) {
                if (i > 0) cells.append(";");
                cells.append(r.getWinningCells()[i][0]).append(",").append(r.getWinningCells()[i][1]);
            }
        }
        return RESULT + "|" + r.getCol() + "|" + r.getRow() + "|" + r.getPlayer() + "|"
            + r.getCurrentPlayer() + "|" + (r.isGameOver() ? 1 : 0) + "|" + r.getWinner() + "|" + cells;
    }

    /** "RESET|" mesajı üretir. */
    public static String encodeReset() {
        return RESET + "|";
    }

    /** "ERROR|mesaj" mesajı üretir. */
    public static String encodeError(String message) {
        return ERROR + "|" + sanitize(message);
    }

    /** Gelen bir RESULT satırını çözüp {@link NetworkMoveResult} nesnesine dönüştürür. */
    public static NetworkMoveResult parseResult(String line) {
        String[] p = line.split("\\|");
        if (p.length < 8) {
            throw new IllegalArgumentException("Geçersiz RESULT: " + line);
        }
        int[][] cells = null;
        if (!"none".equals(p[7])) { // Kazandıran hücreleri çöz
            String[] parts = p[7].split(";");
            cells = new int[parts.length][2];
            for (int i = 0; i < parts.length; i++) {
                String[] rc = parts[i].split(",");
                cells[i][0] = Integer.parseInt(rc[0]);
                cells[i][1] = Integer.parseInt(rc[1]);
            }
        }
        return new NetworkMoveResult(
            Integer.parseInt(p[1]), // col
            Integer.parseInt(p[2]), // row
            Integer.parseInt(p[3]), // player
            Integer.parseInt(p[4]), // currentPlayer
            "1".equals(p[5]),       // gameOver
            Integer.parseInt(p[6]), // winner
            cells
        );
    }

    /** Oyun durumundan doğrudan bir RESULT sonucu üretir (kısayol). */
    public static NetworkMoveResult buildFromLogic(int col, int row, int player, GameLogic logic) {
        return NetworkMoveResult.fromLogic(col, row, player, logic);
    }

    /** Metni protokol için güvenli hale getirir: '|' karakterini siler, boşlukları kırpar. */
    private static String sanitize(String value) {
        return value == null ? "" : value.replace("|", "").trim();
    }
}
