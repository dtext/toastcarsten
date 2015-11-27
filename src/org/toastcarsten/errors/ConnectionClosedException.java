package org.toastcarsten.errors;

public class ConnectionClosedException extends Exception {
    public ConnectionClosedException(String msg) {
        super(msg);
    }
}
