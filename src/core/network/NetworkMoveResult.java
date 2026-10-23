package core.network;

import core.constants.Config;

/**
 * NetworkMoveResult — Bir ağ hamlesinin sonucunu taşıyan değişmez veri nesnesi (DTO).
 *
 * <p>Sunucu, onayladığı her hamleyi bu nesneye paketleyip iki tarafa da yollar.
 * İçinde hamlenin konumu, hangi oyuncunun oynadığı, sırada kimin olduğu,
 * oyunun bitip bitmediği, kazanan ve kazandıran hücreler bulunur.
 */
public class NetworkMoveResult {

    private final int col;            // Oynanan sütun
    private final int row;            // Taşın düştüğü satır
    private final int player;         // Hamleyi yapan oyuncu
    private final int currentPlayer;  // Hamleden sonra sıradaki oyuncu
    private final boolean gameOver;   // Oyun bitti mi?
    private final int winner;         // Kazanan (yoksa EMPTY)
    private final int[][] winningCells; // Kazandıran hücreler (yoksa null)

    public NetworkMoveResult(int col, int row, int player, int currentPlayer,
                             boolean gameOver, int winner, int[][] winningCells) {
        this.col = col;
        this.row = row;
        this.player = player;
        this.currentPlayer = currentPlayer;
        this.gameOver = gameOver;
        this.winner = winner;
        this.winningCells = winningCells;
    }

    // ---- Salt-okunur erişimciler ----
    public int getCol() { return col; }
    public int getRow() { return row; }
    public int getPlayer() { return player; }
    public int getCurrentPlayer() { return currentPlayer; }
    public boolean isGameOver() { return gameOver; }
    public int getWinner() { return winner; }
    public int[][] getWinningCells() { return winningCells; }

    /** Bir hamle sonrası oyun durumundan sonuç nesnesi üretir (fabrika metodu). */
    public static NetworkMoveResult fromLogic(int col, int row, int player, core.logic.GameLogic logic) {
        return new NetworkMoveResult(
            col, row, player,
            logic.getCurrentPlayer(),
            logic.isGameOver(),
            logic.getWinner(),
            logic.getWinningCells()
        );
    }

}
