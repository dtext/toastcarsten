package org.toastcarsten.server;

import org.toastcarsten.errors.ConnectionClosedException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ChannelIO {

    public static String read(SocketChannel client) throws IOException, ConnectionClosedException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        int numbytes = client.read(buf);
        switch (numbytes) {
            case (-1) :
                throw new ConnectionClosedException("Connection closed unexpectedly");
            case 0 :
                return"";
            default :
                if (buf.get(numbytes - 1) != (byte)'\n')
                    throw new IOException("Message Frame error");
                return new String(buf.array(),"utf-8").trim();
        }
    }

    public static void write(SocketChannel client, String s) throws IOException {
        if (!s.endsWith("\n"))
            s += "\n";
        client.write(ByteBuffer.wrap(s.getBytes("UTF-8")));
    }
}
