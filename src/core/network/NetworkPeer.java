package core.network;

import core.logic.GameLogic;

import java.util.function.Consumer;

/**
 * NetworkPeer — Ağ oyununun iki ucu (Host ve Client) için ortak arayüz.
 *
 * <p>Bu soyutlama sayesinde {@code GameScreen}, karşısındakinin sunucu mu
 * istemci mi olduğunu bilmeden tek bir kod yoluyla ağ oyununu yürütür
 * (polimorfizm). Hem {@link NetworkHost} hem {@link NetworkClient} bu arayüzü uygular.
 */
public interface NetworkPeer {

    /** Yerel oyuncunun seçtiği sütunu karşı tarafa gönderir. */
    void sendMove(int col);

    /** Yeni maç başlatır (yalnızca sunucu için anlamlıdır). */
    void sendReset();

    /** Karşıdan bir hamle sonucu geldiğinde çağrılacak dinleyiciyi ayarlar. */
    void setMoveListener(Consumer<NetworkMoveResult> listener);

    /** Bağlantı koptuğunda çağrılacak dinleyiciyi ayarlar. */
    void setDisconnectListener(Runnable listener);

    /** Hata oluştuğunda çağrılacak dinleyiciyi ayarlar. */
    void setErrorListener(Consumer<String> listener);

    /** Bağlantıyı kapatır ve kaynakları serbest bırakır. */
    void disconnect();

    /** Bu uç sunucu mu? (Host → true, Client → false) */
    boolean isHost();

    /** Bu makinedeki yerel oyuncunun numarası (1 veya 2). */
    int getLocalPlayer();

    /** Arayüzün çizeceği paylaşılan oyun durumunu döndürür. */
    GameLogic getSharedLogic();
}
