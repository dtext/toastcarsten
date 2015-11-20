package org.toastcarsten.shared;

import java.io.IOException;

public interface IServer {

    /**
     * Send a message to a single user
     * @param user username of the user to send the message to
     * @param text message to send
     * @throws IOException
     */
    void send(String user, String text) throws IOException;

    /**
     * Sends a message from a sender to everyone else
     * @param sender username of the sender
     * @param text message to send
     * @throws IOException
     */
    void multicast(String sender, String text) throws IOException;

    /**
     * Sends a message from a sender to everyone else
     * @param text message to send
     * @throws IOException
     */
    void broadcast(String text) throws IOException;

}
