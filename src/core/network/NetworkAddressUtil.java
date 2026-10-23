package core.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NetworkAddressUtil — Yerel ağ (LAN) IP adreslerini keşfeden yardımcı.
 *
 * <p>Sunucu kuran oyuncunun, rakibine vereceği IP adresini bulmasını sağlar.
 * Loopback (127.0.0.1) ve sanal arayüzler elenir.
 */
public final class NetworkAddressUtil {

    private NetworkAddressUtil() {} // Örneklenemez

    /** Bulunan ilk uygun yerel IP'yi döndürür (yoksa loopback). */
    public static String getBestLocalIp() {
        List<String> ips = getAllLocalIps();
        return ips.isEmpty() ? "127.0.0.1" : ips.get(0);
    }

    /**
     * Tüm aktif ağ arayüzlerini tarayıp gerçek IPv4 LAN adreslerini toplar.
     * Kapalı, loopback ve sanal arayüzler atlanır.
     */
    public static List<String> getAllLocalIps() {
        List<String> result = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue; // Kullanılamaz arayüzü atla
                }
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    // Yalnızca gerçek IPv4 adreslerini al.
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        result.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (result.isEmpty()) {
            result.add("127.0.0.1"); // Hiçbir şey bulunamazsa yerel test adresi
        }
        return result;
    }
}
