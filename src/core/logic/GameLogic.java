package core.logic;

import core.constants.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * GameLogic — Oyunun çekirdek kural motoru.
 *
 * <p>Tahtanın durumunu (6×7'lik bir tamsayı matrisi), sırayı, oyun bitti mi
 * bilgisini ve kazanan hücreleri tutar. Arayüzden (JavaFX) tamamen bağımsızdır;
 * bu sayede hem yerel oyunda hem ağ oyununda hem de yapay zekânın simülasyonunda
 * yeniden kullanılır.
 */
public class GameLogic {

    private final int[][] board;   // Tahta: board[satır][sütun], 0=boş, 1=P1, 2=P2
    private int currentPlayer;     // Sırası gelen oyuncu
    private boolean gameOver;      // Oyun bitti mi?
    private int moveCount;         // Toplam yapılan hamle sayısı
    private int[][] winningCells;  // Kazandıran 4 hücrenin [satır,sütun] koordinatları

    /** Yeni, boş bir oyun başlatır (sıra Oyuncu 1'de). */
    public GameLogic() {
        board = new int[Config.ROWS][Config.COLS];
        currentPlayer = Config.PLAYER_1;
        gameOver = false;
        moveCount = 0;
    }

    /**
     * Kopya kurucu — verilen oyunun birebir kopyasını oluşturur.
     * Yapay zekâ, gerçek tahtayı bozmadan olası hamleleri denemek için kullanır.
     */
    public GameLogic(GameLogic other) {
        board = new int[Config.ROWS][Config.COLS];
        for (int r = 0; r < Config.ROWS; r++) {
            System.arraycopy(other.board[r], 0, board[r], 0, Config.COLS); // Satırı kopyala
        }
        currentPlayer = other.currentPlayer;
        gameOver = other.gameOver;
        moveCount = other.moveCount;
        if (other.winningCells != null) {
            winningCells = new int[4][2];
            for (int i = 0; i < 4; i++) {
                winningCells[i][0] = other.winningCells[i][0];
                winningCells[i][1] = other.winningCells[i][1];
            }
        }
    }

    /**
     * Belirtilen sütuna sıradaki oyuncunun taşını bırakır (yerçekimi etkisiyle
     * en alttaki boş hücreye). Hamleden sonra kazanma/beraberlik kontrol eder.
     *
     * @return taşın düştüğü satır; hamle geçersizse -1
     */
    public int dropDisc(int col) {
        if (gameOver || !isColumnPlayable(col)) {
            return -1; // Oyun bitti ya da sütun dolu/geçersiz
        }

        // Sütunda en alttaki boş satırı bul (yerçekimi).
        int row = -1;
        for (int r = Config.ROWS - 1; r >= 0; r--) {
            if (board[r][col] == Config.EMPTY) {
                row = r;
                break;
            }
        }
        if (row == -1) {
            return -1;
        }

        board[row][col] = currentPlayer; // Taşı yerleştir
        moveCount++;

        if (checkWin(row, col)) {
            gameOver = true;             // 4'lü dizi oluştu → kazanan var
        } else if (isBoardFull()) {
            gameOver = true;             // Tahta doldu → beraberlik
        } else {
            // Sırayı diğer oyuncuya geçir.
            currentPlayer = currentPlayer == Config.PLAYER_1 ? Config.PLAYER_2 : Config.PLAYER_1;
        }
        return row;
    }

    /** Sütuna taş atılabilir mi? (sınır içinde ve en üst hücresi boşsa) */
    public boolean isColumnPlayable(int col) {
        if (col < 0 || col >= Config.COLS) {
            return false;
        }
        return board[0][col] == Config.EMPTY;
    }

    /** Oynanabilir tüm sütunların listesini döndürür (AI tarafından kullanılır). */
    public List<Integer> getPlayableColumns() {
        List<Integer> cols = new ArrayList<>();
        for (int c = 0; c < Config.COLS; c++) {
            if (isColumnPlayable(c)) {
                cols.add(c);
            }
        }
        return cols;
    }

    /** Bir hücrenin değerini güvenli şekilde döndürür (sınır dışı = boş). */
    public int getCell(int row, int col) {
        if (row < 0 || row >= Config.ROWS || col < 0 || col >= Config.COLS) {
            return Config.EMPTY;
        }
        return board[row][col];
    }

    /** Ham tahta dizisine erişim (AI sezgisel değerlendirmede kullanır). */
    public int[][] getBoard() {
        return board;
    }

    /**
     * Son atılan taştan (row,col) başlayarak 4 yönde (yatay, dikey, iki çapraz)
     * 4 ardışık aynı taş olup olmadığını kontrol eder. Yalnız son taşın
     * etrafına baktığı için çok hızlıdır.
     */
    private boolean checkWin(int row, int col) {
        int player = board[row][col];
        // Yönler: yatay, dikey, "/" çapraz, "\" çapraz
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};

        for (int[] dir : directions) {
            int[][] cells = new int[4][2];
            int count = 1;
            cells[0][0] = row;
            cells[0][1] = col;

            // Bir yönde ilerleyerek aynı taşları say.
            int r = row + dir[0];
            int c = col + dir[1];
            while (count < 4 && inBounds(r, c) && board[r][c] == player) {
                cells[count][0] = r;
                cells[count][1] = c;
                count++;
                r += dir[0];
                c += dir[1];
            }

            // Ters yöne ilerleyerek aynı taşları say (diziyi başa ekleyerek).
            r = row - dir[0];
            c = col - dir[1];
            while (count < 4 && inBounds(r, c) && board[r][c] == player) {
                for (int i = count; i > 0; i--) { // Diziyi bir kaydır
                    cells[i][0] = cells[i - 1][0];
                    cells[i][1] = cells[i - 1][1];
                }
                cells[0][0] = r;
                cells[0][1] = c;
                count++;
                r -= dir[0];
                c -= dir[1];
            }

            if (count >= 4) {       // 4 ardışık taş bulundu
                winningCells = cells;
                return true;
            }
        }
        return false;
    }

    /** Koordinat tahtanın sınırları içinde mi? */
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < Config.ROWS && c >= 0 && c < Config.COLS;
    }

    /** Üst satırın tamamı doluysa tahta dolmuştur. */
    private boolean isBoardFull() {
        for (int c = 0; c < Config.COLS; c++) {
            if (board[0][c] == Config.EMPTY) {
                return false;
            }
        }
        return true;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getCurrentPlayer() {
        return currentPlayer;
    }

    /** AI simülasyonu için — sadece test kopyalarında sırayı ayarlamak amacıyla kullanılır. */
    public void currentPlayerForAi(int player) {
        this.currentPlayer = player;
    }

    /** Kazandıran hücrelerin koordinatları (kazanan yoksa null). */
    public int[][] getWinningCells() {
        return winningCells;
    }

    /** Oyun bitti ama kazanan yoksa beraberliktir. */
    public boolean isDraw() {
        return gameOver && winningCells == null;
    }

    /** Kazanan oyuncuyu döndürür (yoksa EMPTY). */
    public int getWinner() {
        if (!gameOver || winningCells == null) {
            return Config.EMPTY;
        }
        return board[winningCells[0][0]][winningCells[0][1]];
    }

    public int getMoveCount() {
        return moveCount;
    }

    /** Tahtayı tamamen sıfırlar ve yeni maça hazırlar. */
    public void reset() {
        for (int r = 0; r < Config.ROWS; r++) {
            for (int c = 0; c < Config.COLS; c++) {
                board[r][c] = Config.EMPTY;
            }
        }
        currentPlayer = Config.PLAYER_1;
        gameOver = false;
        winningCells = null;
        moveCount = 0;
    }
}
