package org.toastcarsten.shared;

import java.util.Collection;

public interface IClient {

    /**
     * Prints text to the screen.
     * @param text text to print
     */
    void print(String text);

    /**
     * Prints highlighted text to the screen
     * @param text text to print
     */
    void printHl(String text);

    /**
     * Called to handle the userlist answer sent by the server.
     * @param list collection of usernames
     */
    void recvUserlist(Collection<String> list);

    /**
     * Called to handle the event that the chosen name is already taken.
     */
    void handleNameError();
}
