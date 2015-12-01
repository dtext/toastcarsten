package org.toastcarsten.server;

import javax.naming.NameAlreadyBoundException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.HashMap;

public class User {

    // --------------- static ---------------

    private static HashMap<String, User> nameMap = new HashMap<>();
    private static HashMap<SelectionKey, User> selkeyMap = new HashMap<>();

    public static User get(String name) {
        return nameMap.get(name);
    }

    public static User get(SelectionKey k) {
        return selkeyMap.get(k);
    }

    public static Collection<User> getParticipants() {
        return nameMap.values();
    }

    public static Collection<User> getUsers() {
        return selkeyMap.values();
    }

    public static void remove(User u) {
        try {
            nameMap.remove(u.getName());
            selkeyMap.remove(u.getSelectionKey());
            u.getSelectionKey().cancel();
            u.getChannel().close();
        } catch (IOException e) {}
    }

    // --------------- non-static ---------------

    private String name = null;
    private SelectionKey key;
    private long timeout;

    public User(SelectionKey key) {
        this.key = key;
        User.selkeyMap.put(key, this);
    }

    public void setName(String name) throws NameAlreadyBoundException {
        // if this.name == null, the user is new: remap
        if (this.name != null)
            User.nameMap.remove(this.name);
        // if there's a user with that name, putIfAbsent() will return that user
        if (nameMap.putIfAbsent(name, this) != null)
            throw new NameAlreadyBoundException("The given username is taken!");
        this.name = name;
        //user logged in, initialize timeout

    }

    public String getName() {
        return name;
    }

    public SelectionKey getSelectionKey() {
        return key;
    }

    public SocketChannel getChannel() {
        return (SocketChannel)getSelectionKey().channel();
    }

    /**
     * Resets the "inactive-timer" on this user to 5 Minutes from now
     */
    public void resetTimeout() {
        timeout = System.currentTimeMillis() + 30000;
    }

    public long getTimeout() {
        return timeout;
    }
}
