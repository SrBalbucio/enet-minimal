package balbucio.enet;

public class EnetServer extends ReliableUDP {
    public EnetServer(int port) throws Exception {
        super(port);
        System.out.println("Servidor iniciado na porta " + port);
    }

    @Override
    protected void onReceive(byte[] data) {
        System.out.println("Servidor recebeu: " + new String(data));
    }
}
