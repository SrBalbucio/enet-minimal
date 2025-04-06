import balbucio.enet.EnetClient;
import balbucio.enet.EnetServer;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerTest {

    private EnetServer server;
    private EnetClient client;

    @BeforeAll
    public void beforeAll() {
    }

    @Test
    @DisplayName("Criar servidor")
    @Order(0)
    public void createServer() throws Exception {
        this.server = new EnetServer(5000) {
            @Override
            protected void onReceive(byte[] data) {
                System.out.println(new String(data));
            }
        };
    }

    @Test
    @DisplayName("Criar client")
    @Order(1)
    public void createClient() throws Exception {
        this.client = new EnetClient("localhost", 5000) {
            @Override
            protected void onReceive(byte[] data) {
                System.out.println("Servidor recebeu: " + new String(data));
            }
        };
    }

    @Test
    @DisplayName("Envio de mensagens ao servidor")
    @Order(2)
    public void clientSend() throws Exception {
        client.send(new String("enet server").getBytes());
    }

    @Test
    @DisplayName("Envio de mensagens ao client")
    @Order(3)
    public void serverSend() throws Exception {
        server.send(new String("enet client").getBytes());
    }
}
