package org.toastcarsten.client;

import org.toastcarsten.shared.IClient;

import java.util.Collection;

public class ChatClient implements IClient {

    public static void main(String[] args) {
        ChatClient c = new ChatClient();
        // TODO create Chat Client (GUI)
    }

    @Override
    public void print(String text) {
        // TODO implement printing of regular messages
    }

    @Override
    public void printHl(String text) {
        // TODO implement printing of highlighted messages
    }

    @Override
    public void recvUserlist(Collection<String> list) {
        // TODO handle the userlist answer
    }

    @Override
    public void handleNameError() {
        printHl("The chosen name is already taken. Please choose another name.");
        // TODO maybe do something here? If not, this could also be in the Protocol.
    }
}
