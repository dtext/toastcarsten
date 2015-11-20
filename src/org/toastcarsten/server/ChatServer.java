package org.dt.toastcarsten.server;

import org.dt.toastcarsten.shared.CmdParser;
import org.dt.toastcarsten.shared.IServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class ChatServer implements IServer {

    private Selector events;
    private ServerSocketChannel listener;
    private CmdParser cmdParser;

    public static void main(String[] args) {
        try {
            ChatServer srv = new ChatServer(47711);
            srv.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChatServer(int port) throws IOException {
        events = Selector.open();
        listener = ServerSocketChannel.open();
        listener.configureBlocking(false);
        listener.socket().bind(new InetSocketAddress(port));
        listener.register(events, SelectionKey.OP_ACCEPT);
        cmdParser = new CmdParser(this);
    }

    public void run() {
        while (true) {
            try {
                //wait for events
                events.select();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error occured when waiting for events.");
            }
            //process all events that occured
            Iterator<SelectionKey> itr = events.selectedKeys().iterator();
            while (itr.hasNext()) {
                SelectionKey event = itr.next();
                itr.remove();
                if (event.isReadable()) {
                    //someone sent a message
                    try {
                        //event.attachment()
                        this.processRead(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error receiving a message.");
                    }
                } else if (event.isAcceptable()) {
                    //someone tries to connect
                    try {
                        this.processAccept();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error registering a client.");
                    }
                }
            }
        }
    }

    private void processRead(SelectionKey client) throws IOException {
        String sendername = ((User)client.attachment()).name;
        SocketChannel senderchannel = (SocketChannel)client.channel();
        String[] messages = ChannelIO.read((SocketChannel)client.channel()).split("\n");
        for (String message : messages) {
            CmdParser.Command cmd = cmdParser.parseClient(message);
            cmd.action();
        }
    }

    private void processAccept() throws IOException {
        SocketChannel talkChannel = listener.accept();
        talkChannel.configureBlocking(false);
        talkChannel.register(events, SelectionKey.OP_READ);
    }

    @Override
    public void send(String username, String message) throws IOException {
        for (SelectionKey key : events.keys()) {
            SelectableChannel user = key.channel();
            if (user instanceof ServerSocketChannel |! username.equals(((User) key.attachment()).name))
                continue;
            ChannelIO.write((SocketChannel)user, message);
        }
    }

    @Override
    public void multicast(String sender, String text) throws IOException {
        for (SelectionKey key : events.keys()) {
            SelectableChannel user = key.channel();
            if (user instanceof ServerSocketChannel)
                continue;
            else if (sender.equals(((User) key.attachment()).name))
                continue;
            ChannelIO.write((SocketChannel) key.channel(), text);
        }
    }

    @Override
    public void broadcast(String text) throws IOException {
        multicast("", text);
    }
}
