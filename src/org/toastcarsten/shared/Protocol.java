package org.toastcarsten.shared;

import org.toastcarsten.errors.CommandNotFoundException;
import org.toastcarsten.server.User;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                Collection<String> names = new ArrayList<>();
                for (User u : User.getUsers())
                    names.add(u.getName());
                server.send(user, new UserlistAnswer(names).toString());
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
            client.printHl("You have been inactive for 5 Minutes, you got disconnected.");
        }
    }

    public class UserlistAnswer extends ServerCommand {

        private static final String sep = ", ";

        public UserlistAnswer(Collection<String> users) {
            cmd = "/userlist";
            StringJoiner sj = new StringJoiner(sep);
            users.forEach(sj::add);
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
    private final static Pattern
            rLogin =             Pattern.compile("\\/login ([a-zA-Z0-9]+)"),
            rLogout =            Pattern.compile("\\/logout (.+)"),
            rUserlistRequest =   Pattern.compile("\\/userlist"),
            rMessage =           Pattern.compile("[^\\/].+"),
            rError =             Pattern.compile("\\/error (NameAlreadyInUse|CommandNotAllowed|CommandNotFound)"),
            rUserleft =          Pattern.compile("\\/userleft ([a-zA-Z0-9]+) (timeout|logout)"),
            rUserjoined =        Pattern.compile("\\/userjoined ([a-zA-Z0-9]+)"),
            rUsertimeout =       Pattern.compile("\\/usertimeout ([a-zA-Z0-9]+)"),
            rUserlistAnswer =    Pattern.compile("\\/userlist ((?:(?:[a-zA-Z0-9]+)(?:, )?)+)"),
            rRedirectedMessage = Pattern.compile("([a-zA-Z0-9]+)\\: (.+)"),
            rServerMessage =     Pattern.compile("\\/server (.+)"),
            rWelcome =           Pattern.compile("\\/welcome ([a-zA-Z0-9]+)");

    /**
     * Get arguments of a command
     * @param raw the raw command text
     * @param regex the regex corresponding to that command type
     * @param n the amount of arguments to return
     * @return String array of arguments
     */
    private String[] getArgs(String raw, Pattern regex, int n) {
        String[] result = new String[n];
        Matcher m = regex.matcher(raw);
        for (int i = 0; i < n; ++i) {
            result[i] = m.group(i);
        }
        return result;
    }

    /**
     * Parses a client command.
     * Should almost always be called by the server.
     * @param raw The raw command string
     * @return the corresponding Command object, or null if the command was not found.
     */
    public Command parseClient(String raw) throws CommandNotFoundException {
        if (rLogin.matcher(raw).matches()) {
            return new Login(getArgs(raw, rLogin, 1)[0]);
        }
        else if (rLogout.matcher(raw).matches()) {
            return new Logout();
        }
        else if (rUserlistRequest.matcher(raw).matches()) {
            return new UserlistRequest();
        }
        else if (rMessage.matcher(raw).matches()) {
            return new Message(raw);
        }
        throw new CommandNotFoundException("The given command could not be evaluated: " + raw);
    }

    /**
     * Parses a server command.
     * Should almost always be called by the client.
     * @param raw The raw command string
     * @return the corresponding Command object, or null if the command was not found.
     */
    public Command parseServer(String raw) throws CommandNotFoundException{
        if (rError.matcher(raw).matches()) {
            String err = getArgs(raw, rError, 1)[0];
            return new ErrorMessage(Error.valueOf(err));
        }
        else if (rUserleft.matcher(raw).matches()) {
            String[] args = getArgs(raw, rUserleft, 2);
            return new UserLeft(args[0], Reason.valueOf(args[1]));
        }
        else if (rUserjoined.matcher(raw).matches()) {
            return new UserJoined(getArgs(raw, rUserjoined, 1)[0]);
        }
        else if (rUsertimeout.matcher(raw).matches()) {
            return new UserTimeout();
        }
        else if (rUserlistAnswer.matcher(raw).matches()) {
            String users = getArgs(raw, rUserlistAnswer, 1)[0];
            return new UserlistAnswer(Arrays.asList(users.split(", ")));
        }
        else if (rServerMessage.matcher(raw).matches()) {
            return new ServerMessage(getArgs(raw, rServerMessage, 1)[0]);
        }
        else if (rWelcome.matcher(raw).matches()) {
            return new Welcome(getArgs(raw, rServerMessage, 1)[0]);
        }
        else if (rRedirectedMessage.matcher(raw).matches()) {
            String[] args = getArgs(raw, rRedirectedMessage, 2);
            return new RedirectedMessage(args[1], args[2]);
        }
        throw new CommandNotFoundException("The given command could not be evaluated: " + raw);
    }
}
