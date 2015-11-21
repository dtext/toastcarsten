package org.toastcarsten.shared;

import org.toastcarsten.server.User;

import java.io.IOException;
import java.util.StringJoiner;

public class Protocol {

    private IServer server = null;
    private IClient client = null;

    public Protocol(IServer server) {
        this.server = server;
    }

    public Protocol(IClient client) {
        this.client = client;
    }

    // Command Base Class
    public abstract class Command {
        public String cmd = "";
        public String args = "";

        public String toString() {
            if (cmd.equals(""))
                return args;
            else
                return "/" + cmd + " " + args;
        }
    }

    // Client Command Base Class
    public abstract class ClientCommand extends Command {
        /**
         * This defines the action a receiver should execute when receiving the message.
         * Use the appropriate interface of Protocol (server or client).
         * @param user The user who sent the command
         */
        public abstract void action(String user);
    }

    // Server Command Base Class
    public abstract class ServerCommand extends Command {
        /**
         * This defines the action a receiver should execute when receiving the message.
         * Use the appropriate interface of Protocol (server or client).
         */
        public abstract void action();
    }

    // --------------- Client Commands ---------------
    public class Login extends ClientCommand {
        public Login(String username) {
            cmd = "/login";
            args = username;
        }

        @Override
        public void action(String user) {
            //Server needs to handle this
        }
    }

    public class Logout extends ClientCommand {
        public Logout() {
            cmd = "/logout";
        }

        @Override
        public void action(String user) {
            try {
                server.send(user, "Goodbye!");
                server.logout(user);
                server.broadcast(new UserLeft(user, Reason.logout).toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class UserlistRequest extends ClientCommand {
        public UserlistRequest() {
            cmd = "/userlist";
        }

        @Override
        public void action(String user) {
            try {
                server.send(user, new UserlistAnswer().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class Message extends ClientCommand {
        public Message(String text) {
            cmd = "";
            args = text;
        }

        @Override
        public void action(String user) {
            try {
                server.multicast(user, args);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --------------- Server Commands ---------------
    public static enum Error {
        NameAlreadyInUse, CommandNotAllowed, CommandNotFound
    }
    public class ErrorMessage extends ServerCommand {

        public ErrorMessage(Error err) {
            cmd = "/error";
            args = err.name();
        }

        @Override
        public void action() {
            Error e = Error.valueOf(args);
            if (e == Error.NameAlreadyInUse) {
                // TODO Name already in use
            }
            //TODO complete action
        }
    }

    public static enum Reason {
        timeout, logout
    }
    public class UserLeft extends ServerCommand {
        public UserLeft(String username, Reason r) {
            cmd = "/userleft";
            args = username + " " + r.name();
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class UserJoined extends ServerCommand {
        public UserJoined(String username) {
            cmd = "/userjoined";
            args = username;
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class UserTimeout extends ServerCommand {
        public UserTimeout() {
            cmd = "/usertimeout";
            args = "";
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class UserlistAnswer extends ServerCommand {
        public UserlistAnswer() {
            cmd = "/userlist";
            StringJoiner sj = new StringJoiner(",");
            for (User u : User.getUsers()) {
                sj.add(u.getName());
            }
            args = sj.toString();
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class RedirectedMessage extends ServerCommand {
        public RedirectedMessage(String user, String message) {
            args = user + ": " + message;
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class ServerMessage extends ServerCommand {
        public ServerMessage(String message) {
            cmd = "/server";
            args = message;
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    public class Welcome extends ServerCommand {
        public Welcome(String username) {
            cmd = "";
            args = "Hi, " + username + "! Welcome to the Toastcarsten Server.";
        }

        @Override
        public void action() {
            //TODO add action
        }
    }

    // --------------- Command Parsing ---------------

    /**
     * Parses a client command.
     * Should almost always be called by the server.
     * @param raw The raw command string
     * @return the corresponding Command object
     */
    public Command parseClient(String raw) {
        //TODO parse client commands here
        return new Welcome("tmp");
    }

    /**
     * Parses a server command.
     * Should almost always be called by the client.
     * @param raw The raw command string
     * @return the corresponding Command object
     */
    public Command parseServer(String raw) {
        //TODO parse server Commands here
        return new Welcome("tmp");
    }
}
