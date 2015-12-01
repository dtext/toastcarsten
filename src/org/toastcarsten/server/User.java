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

    /**
     * Close the connection to this user and remove him from the user list.
     * @param u the user to remove
     * @return true if a user has been found and removed, false otherwise.
     */
    public static boolean remove(User u) {
        if (System.currentTimeMillis() < u.getTimeout()) {
            // user is not removed because of timeout, cancel the service as well
            u.stopTimeout();
        }
        try {
            u.getSelectionKey().cancel();
            u.getChannel().close();
            nameMap.remove(u.getName());
            return (selkeyMap.remove(u.getSelectionKey()) != null);
        } catch (IOException e) {}
        return false;
    }

    // --------------- non-static ---------------

    private String name = null;
    private SelectionKey key;
    private long timeout;
    private Thread timeoutservice;

    public User(SelectionKey key) {
        this.key = key;
        User.selkeyMap.put(key, this);
        // start timeout service
        this.resetTimeout();
        this.timeoutservice = TimeoutService.start(this);
    }

    public void setName(String name) throws NameAlreadyBoundException {
        // if this.name == null, the user is new: remap
        if (this.name != null)
            User.nameMap.remove(this.name);
        // if there's a user with that name, putIfAbsent() will return that user
        if (nameMap.putIfAbsent(name, this) != null)
            throw new NameAlreadyBoundException("The given username is taken!");
        this.name = name;
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
        timeout = System.currentTimeMillis() + 300000;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * Stops the timeout service for this user.
     */
    public void stopTimeout() {
        this.timeoutservice.interrupt();
    }
}
