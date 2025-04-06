package balbucio.enet;

import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public abstract class ReliableUDP {
    protected DatagramSocket socket;
    protected InetAddress peerAddress;
    protected int peerPort;

    protected int sequenceId = 0;
    protected int expectedSequenceId = 0;
    protected Map<Integer, byte[]> pendingPackets = new ConcurrentHashMap<>();
    protected Map<Integer, Long> packetTimers = new ConcurrentHashMap<>();
    protected final int TIMEOUT_MS = 500;

    public ReliableUDP(int port, ScheduledExecutorService executor) throws Exception {
        this.socket = new DatagramSocket(port);
        executor.submit(this::receiveLoop);
        executor.scheduleAtFixedRate(this::resendLoop, 500, 200, TimeUnit.MILLISECONDS);
    }

    public ReliableUDP(int port) throws Exception {
        this(port, Executors.newScheduledThreadPool(1));
    }

    protected abstract void onReceive(byte[] data);

    public void send(byte[] data) throws Exception {
        if (peerAddress == null) return;
        byte[] packet = new byte[data.length + 4];
        packet[0] = (byte) (sequenceId >> 24);
        packet[1] = (byte) (sequenceId >> 16);
        packet[2] = (byte) (sequenceId >> 8);
        packet[3] = (byte) (sequenceId);
        System.arraycopy(data, 0, packet, 4, data.length);

        DatagramPacket datagram = new DatagramPacket(packet, packet.length, peerAddress, peerPort);
        socket.send(datagram);

        pendingPackets.put(sequenceId, packet);
        packetTimers.put(sequenceId, System.currentTimeMillis());
        sequenceId++;
    }

    public void sendMessage(MsgPackCreator creator) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(out);
        creator.createMessage(packer);
        packer.flush();
        out.flush();
        send(out.toByteArray());
        packer.close();
        out.close();
    }

    private void receiveLoop() {
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                peerAddress = packet.getAddress();
                peerPort = packet.getPort();

                int seqId = ((buffer[0] & 0xFF) << 24) | ((buffer[1] & 0xFF) << 16)
                        | ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);

                byte[] payload = Arrays.copyOfRange(buffer, 4, packet.getLength());

                if (seqId == expectedSequenceId) {
                    onReceive(payload);
                    expectedSequenceId++;
                }

                // Envia ACK
                byte[] ack = new byte[]{'A', 'C', 'K', buffer[3]};
                socket.send(new DatagramPacket(ack, ack.length, peerAddress, peerPort));

                // Remove da lista se for ACK
                if (payload.length == 3 && payload[0] == 'A' && payload[1] == 'C' && payload[2] == 'K') {
                    int ackId = buffer[3];
                    pendingPackets.remove(ackId);
                    packetTimers.remove(ackId);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resendLoop() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : packetTimers.entrySet()) {
            if (now - entry.getValue() > TIMEOUT_MS) {
                int seqId = entry.getKey();
                byte[] packet = pendingPackets.get(seqId);
                if (packet != null) {
                    try {
                        socket.send(new DatagramPacket(packet, packet.length, peerAddress, peerPort));
                        packetTimers.put(seqId, now);
                        System.out.println("Retransmitindo pacote " + seqId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
