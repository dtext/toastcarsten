package org.dt.toastcarsten.shared;

import java.util.Collection;

public class CmdParser {

    private IServer server = null;
    private IClient client = null;

    public CmdParser(IServer server) {
        this.server = server;
    }

    public CmdParser(IClient client) {
        this.client = client;
    }

    public abstract class Command {
        public String cmd = "";
        public String args = "";

        public String toString() {
            if (cmd.equals(""))
                return args;
            else
                return "/" + cmd + " " + args;
        }

        /**
         * This defines the action a receiver should execute when receiving the message.
         * Use the appropriate interface of CmdParser.
         */
        public abstract void action();
    }

    // --------------- Client Commands ---------------
    public class Login extends Command {
        public Login(String username) {
            cmd = "/login";
            args = username;
        }

        @Override
        public void action() {

        }
    }

    public class Logout extends Command {
        public Logout() {
            cmd = "/logout";
        }

        @Override
        public void action() {

        }
    }

    public class UserlistRequest extends Command {
        public UserlistRequest() {
            cmd = "/userlist";
        }

        @Override
        public void action() {

        }
    }

    public class Message extends Command {
        public Message(String text) {
            cmd = "";
            args = text;
        }

        @Override
        public void action() {

        }
    }

    // --------------- Server Commands ---------------
    enum Error {
        NameAlreadyInUse, CommandNotAllowed, CommandNotFound
    }
    public class ErrorMessage extends Command {

        public ErrorMessage(Error err) {
            cmd = "/error";
            args = err.name();
        }

        @Override
        public void action() {

        }
    }

    enum Reason {
        timeout, login
    }
    public class UserLeft extends Command {
        public UserLeft(String username, Reason r) {
            cmd = "/userleft";
            args = username + " " + r.name();
        }

        @Override
        public void action() {

        }
    }

    public class UserJoined extends Command {
        public UserJoined(String username) {
            cmd = "/userjoined";
            args = username;
        }

        @Override
        public void action() {

        }
    }

    public class UserTimeout extends Command {
        public UserTimeout() {
            cmd = "/usertimeout";
            args = "";
        }

        @Override
        public void action() {

        }
    }

    public class UserlistAnswer extends Command {
        public UserlistAnswer(Collection<String> users) {
            cmd = "/userlist";
            for (String user : users) {
                args += user + " ";
            }
            args = args.substring(0, args.length() - 1);
        }

        @Override
        public void action() {

        }
    }

    public class RedirectedMessage extends Command {
        public RedirectedMessage(String user, String message) {
            args = user + ": " + message;
        }

        @Override
        public void action() {

        }
    }

    public class ServerMessage extends Command {
        public ServerMessage(String message) {
            cmd = "/server";
            args = message;
        }

        @Override
        public void action() {

        }
    }

    public class Welcome extends Command {
        public Welcome() {
            cmd = "";
            args = "Welcome to the Toastcarsten Server.";
        }

        @Override
        public void action() {

        }
    }

    // --------------- Command Parsing ---------------
    public Command parseClient(String raw) {
        //TODO parse client commands here
        return new Welcome();
    }

    public Command parseServer(String raw) {
        //TODO parse server Commands here
        return new Welcome();
    }

    public static String asText(Command cmd) {
        return cmd.toString();
    }
}
