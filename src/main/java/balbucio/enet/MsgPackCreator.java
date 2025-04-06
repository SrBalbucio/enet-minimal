package balbucio.enet;

import org.msgpack.core.MessagePacker;

import java.io.IOException;

public interface MsgPackCreator {

    public void createMessage(MessagePacker packer) throws IOException;
}
