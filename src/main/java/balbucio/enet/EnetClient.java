package balbucio.enet;

import java.net.InetAddress;

public abstract class EnetClient extends ReliableUDP {
    public EnetClient(String host, int port) throws Exception {
        super(0); // porta aleatória
        this.peerAddress = InetAddress.getByName(host);
        this.peerPort = port;
    }
}