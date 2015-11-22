package org.toastcarsten.shared;

import org.toastcarsten.server.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
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

        Error error;

        public ErrorMessage(Error err) {
            cmd = "/error";
            args = err.name();
            error = err;
        }

        @Override
        public void action() {
            if (error == Error.NameAlreadyInUse) {
                client.handleNameError();
            } else if (error == Error.CommandNotAllowed) {
                client.printHl("The given command is not allowed in this context.");
            } else if (error == Error.CommandNotFound) {
                client.printHl("You have entered an invalid command.");
            }
        }
    }

    public static enum Reason {
        timeout, logout
    }
    public class UserLeft extends ServerCommand {

        Reason reason;
        String name;

        public UserLeft(String username, Reason r) {
            cmd = "/userleft";
            name = username;
            args = username + " " + r.name();
            reason = r;
        }

        @Override
        public void action() {
            if (reason == Reason.logout) {
                client.printHl(name + " left the channel.");
            } else if (reason == Reason.timeout) {
                client.printHl(name + " timed out.");
            }
        }
    }

    public class UserJoined extends ServerCommand {
        public UserJoined(String username) {
            cmd = "/userjoined";
            args = username;
        }

        @Override
        public void action() {
            client.printHl(args + " just joined.");
        }
    }

    public class UserTimeout extends ServerCommand {
        public UserTimeout() {
            cmd = "/usertimeout";
            args = "";
        }

        @Override
        public void action() {
            // TODO: according to the protocol, this shouldn't be here. Look this up.
        }
    }

    public class UserlistAnswer extends ServerCommand {

        private static final String sep = ", ";

        public UserlistAnswer() {
            cmd = "/userlist";
            StringJoiner sj = new StringJoiner(sep);
            for (User u : User.getUsers()) {
                sj.add(u.getName());
            }
            args = sj.toString();
        }

        @Override
        public void action() {
            List<String> ul = Arrays.asList(args.split(sep));
            client.recvUserlist(ul);
        }
    }

    public class RedirectedMessage extends ServerCommand {
        public RedirectedMessage(String user, String message) {
            args = user + ": " + message;
        }

        @Override
        public void action() {
            client.print(this.toString());
        }
    }

    public class ServerMessage extends ServerCommand {
        public ServerMessage(String message) {
            cmd = "/server";
            args = message;
        }

        @Override
        public void action() {
            client.printHl("[Server message] " + args);
        }
    }

    public class Welcome extends ServerCommand {
        public Welcome(String username) {
            cmd = "/welcome";
            args = "Hi, " + username + "! Welcome to the Toastcarsten Server.";
        }

        @Override
        public void action() {
            client.printHl(args);
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
