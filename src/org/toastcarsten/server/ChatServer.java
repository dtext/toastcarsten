package org.toastcarsten.server;

import org.toastcarsten.errors.CommandNotFoundException;
import org.toastcarsten.errors.ConnectionClosedException;
import org.toastcarsten.shared.Protocol;
import org.toastcarsten.shared.IServer;

import javax.naming.NameAlreadyBoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

public class ChatServer implements IServer {

    private Selector events;
    private ServerSocketChannel listener;
    private Protocol protocol;

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
        protocol = new Protocol(this);
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
                        this.processRead(event);
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Error receiving a message.");
                    } catch (ConnectionClosedException e) {
                        User.remove(User.get(event));
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

    private void processRead(SelectionKey client) throws IOException, ConnectionClosedException {
        User u = User.get(client);
        SocketChannel clientChannel = u.getChannel();
        String[] messages = ChannelIO.read((SocketChannel)client.channel()).split("\n");
        for (String message : messages) {
            Protocol.ClientCommand cmd = null;
            // try to parse command, if invalid, send error
            try {
                cmd = (Protocol.ClientCommand) protocol.parseClient(message);
            } catch (CommandNotFoundException e) {
                Protocol.Error err = Protocol.Error.CommandNotFound;
                ChannelIO.write(clientChannel, protocol.new ErrorMessage(err).toString());
                continue;
            }
            // Login handled by the server because the SelectionKey needs to be present
            if (cmd instanceof Protocol.Login) {
                String name = cmd.args;
                try {
                    u.setName(name);
                } catch (NameAlreadyBoundException e) {
                    Protocol.Error err = Protocol.Error.NameAlreadyInUse;
                    ChannelIO.write(clientChannel, protocol.new ErrorMessage(err).toString());
                }
            }
            // for any other message, do what is defined in the Protocol
            else {
                String username = u.getName();
                if (username == null) {
                    // user still in lobby, should only be able to login
                    Protocol.Error err = Protocol.Error.CommandNotAllowed;
                    ChannelIO.write(clientChannel, protocol.new ErrorMessage(err).toString());
                    return;
                }
                cmd.action(username);
            }
        }
    }

    private void processAccept() throws IOException {
        SocketChannel talkChannel = listener.accept();
        talkChannel.configureBlocking(false);
        SelectionKey userkey = talkChannel.register(events, SelectionKey.OP_READ);
        new User(userkey);
    }

    @Override
    public void send(String username, String message) throws IOException {
        SocketChannel sc = User.get(username).getChannel();
        ChannelIO.write(sc, message);
    }

    @Override
    public void multicast(String sender, String text) throws IOException {
        for (User user : User.getParticipants()) {
            if (!user.getName().equals(sender))
                ChannelIO.write(user.getChannel(), text);
        }
    }

    @Override
    public void broadcast(String text) throws IOException {
        multicast("", text);
    }

    @Override
    public void logout(String name){
        User u = User.get(name);
        User.remove(u);
    }
}
