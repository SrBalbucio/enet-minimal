package balbucio.enet;

public abstract class EnetServer extends ReliableUDP {
    public EnetServer(int port) throws Exception {
        super(port);
        System.out.println("Servidor iniciado na porta " + port);
    }
}
