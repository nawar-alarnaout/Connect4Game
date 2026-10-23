package core.logic;

import core.constants.Config;

import java.util.List;
import java.util.Random;

/**
 * Connect4AI — Yapay zekânın hamle seçim mantığı.
 *
 * <p>Durumsuz (stateless) bir yardımcı sınıftır: verilen oyun durumuna ve
 * zorluk seviyesine göre en uygun sütunu döndürür. Zor ve Uzman seviyeler
 * minimax + alfa-beta budama kullanır.
 */
public final class Connect4AI {

    // Zorluk seviyesi sabitleri
    public static final int TWO_PLAYER = 0; // İki kişilik (AI yok)
    public static final int EASY = 1;       // Rastgele
    public static final int MEDIUM = 2;     // Tek hamlelik sezgi
    public static final int HARD = 3;       // 5 derinlik minimax
    public static final int EXPERT = 4;     // 7 derinlik minimax
    public static final int NETWORK = 5;    // Ağ oyunu (AI yok)

    private static final Random RANDOM = new Random();

    private Connect4AI() {} // Örneklenemez (yalnızca statik kullanım)

    /**
     * Zorluk seviyesine göre AI'nin oynayacağı sütunu seçer.
     * @return seçilen sütun; oynanabilir hamle yoksa -1
     */
    public static int chooseMove(GameLogic state, int difficulty) {
        List<Integer> cols = state.getPlayableColumns();
        if (cols.isEmpty()) {
            return -1;
        }
        return switch (difficulty) {
            case EASY -> randomMove(cols);
            case MEDIUM -> mediumMove(state, cols);
            case HARD -> bestMove(state, 5);   // 5 hamle ileriyi gör
            case EXPERT -> bestMove(state, 7); // 7 hamle ileriyi gör
            default -> randomMove(cols);
        };
    }

    /** Kolay: oynanabilir sütunlardan rastgele birini seçer. */
    private static int randomMove(List<Integer> cols) {
        return cols.get(RANDOM.nextInt(cols.size()));
    }

    /**
     * Orta: önce kendi kazanma hamlesini arar, yoksa rakibin kazanmasını
     * engeller, o da yoksa ortadaki güçlü sütunu (3) tercih eder.
     */
    private static int mediumMove(GameLogic state, List<Integer> cols) {
        int ai = state.getCurrentPlayer();
        int human = ai == Config.PLAYER_1 ? Config.PLAYER_2 : Config.PLAYER_1;

        // 1) Bu hamleyle kazanabiliyor muyum?
        for (int col : cols) {
            GameLogic copy = new GameLogic(state);
            copy.dropDisc(col);
            if (copy.isGameOver() && copy.getWinner() == ai) {
                return col;
            }
        }
        // 2) Rakip bir sonraki hamlede kazanacaksa o sütunu kapat.
        for (int col : cols) {
            GameLogic copy = new GameLogic(state);
            copy.currentPlayerForAi(human); // Rakip oynuyormuş gibi simüle et
            copy.dropDisc(col);
            if (copy.isGameOver() && copy.getWinner() == human) {
                return col;
            }
        }
        // 3) Aksi halde merkez sütun stratejik olarak en iyisidir.
        if (cols.contains(3)) {
            return 3;
        }
        return randomMove(cols);
    }

    /**
     * Zor/Uzman: her olası ilk hamleyi dener, minimax ile değerlendirir
     * ve en yüksek skoru veren sütunu seçer.
     */
    private static int bestMove(GameLogic state, int depth) {
        int ai = state.getCurrentPlayer();
        int bestCol = state.getPlayableColumns().get(0);
        int bestScore = Integer.MIN_VALUE;

        for (int col : state.getPlayableColumns()) {
            GameLogic copy = new GameLogic(state);
            copy.dropDisc(col);
            // Bir alt seviye: artık rakibin sırası (minimizing).
            int score = minimax(copy, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, ai);
            if (score > bestScore) {
                bestScore = score;
                bestCol = col;
            }
        }
        return bestCol;
    }

    /**
     * Minimax + alfa-beta budama. Oyun ağacını "depth" derinliğine kadar açar;
     * AI sırasında skoru büyütmeye, rakip sırasında küçültmeye çalışır.
     * Alfa-beta, sonucu değiştirmeyecek dalları erken keserek aramayı hızlandırır.
     */
    private static int minimax(GameLogic state, int depth, int alpha, int beta, boolean maximizing, int aiPlayer) {
        // Terminal durum: kazanma/kayıp/beraberlik puanlanır.
        if (state.isGameOver()) {
            int winner = state.getWinner();
            if (winner == aiPlayer) {
                return 1_000_000 + depth;   // En hızlı galibiyeti tercih et
            }
            if (winner != Config.EMPTY) {
                return -1_000_000 - depth;  // En geç yenilgiyi tercih et
            }
            return 0;                       // Beraberlik
        }
        if (depth == 0) {
            return evaluate(state, aiPlayer); // Derinlik bitti → sezgisel skor
        }

        List<Integer> cols = state.getPlayableColumns();
        if (maximizing) { // AI sırası: en yüksek skoru ara
            int max = Integer.MIN_VALUE;
            for (int col : cols) {
                GameLogic copy = new GameLogic(state);
                copy.dropDisc(col);
                int score = minimax(copy, depth - 1, alpha, beta, false, aiPlayer);
                max = Math.max(max, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break; // Beta budaması: kalan dallara bakmaya gerek yok
                }
            }
            return max;
        }

        // Rakip sırası: en düşük skoru ara
        int min = Integer.MAX_VALUE;
        for (int col : cols) {
            GameLogic copy = new GameLogic(state);
            copy.dropDisc(col);
            int score = minimax(copy, depth - 1, alpha, beta, true, aiPlayer);
            min = Math.min(min, score);
            beta = Math.min(beta, score);
            if (beta <= alpha) {
                break; // Alfa budaması
            }
        }
        return min;
    }

    /** Belirsiz konumları puanlar: AI'nin tehditleri eksi rakibin tehditleri. */
    private static int evaluate(GameLogic state, int aiPlayer) {
        int human = aiPlayer == Config.PLAYER_1 ? Config.PLAYER_2 : Config.PLAYER_1;
        return countThreats(state, aiPlayer) - countThreats(state, human);
    }

    /** Bir oyuncunun tahtadaki taş dizilerinden (komşuluklardan) gelen toplam puanı. */
    private static int countThreats(GameLogic state, int player) {
        int score = 0;
        int[][] board = state.getBoard();

        for (int r = 0; r < Config.ROWS; r++) {
            for (int c = 0; c < Config.COLS; c++) {
                if (board[r][c] != player) {
                    continue;
                }
                score += neighborCount(board, r, c, player) * 2; // Diziler ağırlıklı
            }
        }
        return score;
    }

    /** Bir taştan dört yönde uzanan ardışık aynı taş dizilerinin uzunluğunu toplar. */
    private static int neighborCount(int[][] board, int row, int col, int player) {
        int count = 0;
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int streak = 1;
            int r = row + d[0];
            int c = col + d[1];
            while (r >= 0 && r < Config.ROWS && c >= 0 && c < Config.COLS && board[r][c] == player) {
                streak++;
                r += d[0];
                c += d[1];
            }
            if (streak >= 2) { // En az 2'li bir dizi tehdittir
                count += streak;
            }
        }
        return count;
    }
}
