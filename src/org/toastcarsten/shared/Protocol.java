package org.toastcarsten.shared;

import org.toastcarsten.errors.CommandArgumentParsingException;
import org.toastcarsten.errors.CommandNotFoundException;
import org.toastcarsten.server.User;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                return cmd + " " + args;
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
            rLogout =            Pattern.compile("\\/logout(?: (.+))?"),
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
     * @param m A matcher object created from the regex and the matching string.
     * @param n the amount of arguments to return
     * @return String array of arguments
     * @throws CommandArgumentParsingException
     */
    private String[] getArgs(Matcher m, int n) throws CommandArgumentParsingException{
        String[] result = new String[n];
        try {
            for (int i = 0; i < n; ++i) {
                // exclude "0th" capturing group, that would be the whole match
                result[i] = m.group(i + 1);
            }
            return result;
        } catch (Exception e) {
            throw new CommandArgumentParsingException("An error occured while extracting command arguments from command");
        }
    }

    /**
     * Parses a client command.
     * Should almost always be called by the server.
     * @param raw The raw command string
     * @return the corresponding Command object, or null if the command was not found.
     */
    public Command parseClient(String raw) throws CommandNotFoundException {
        Matcher m;
        try {
            if ( (m = rLogin.matcher(raw)).matches()) {
                return new Login(getArgs(m, 1)[0]);
            }
            else if ((m = rLogout.matcher(raw)).matches()) {
                return new Logout();
            }
            else if ((m = rUserlistRequest.matcher(raw)).matches()) {
                return new UserlistRequest();
            }
            else if ((m = rMessage.matcher(raw)).matches()){
                return new Message(raw);
            }
        } catch (CommandArgumentParsingException e) {
            throw new CommandNotFoundException("The given command could not be evaluated: " + raw);
        }
        // the given text does not match any of the patterns
        throw new CommandNotFoundException("The given command could not be evaluated: " + raw);
    }

    /**
     * Parses a server command.
     * Should almost always be called by the client.
     * @param raw The raw command string
     * @return the corresponding Command object, or null if the command was not found.
     */
    public Command parseServer(String raw) throws CommandNotFoundException{
        try {
            Matcher m;
            if ((m = rError.matcher(raw)).matches()) {
                String err = getArgs(m, 1)[0];
                return new ErrorMessage(Error.valueOf(err));
            }
            else if ((m = rUserleft.matcher(raw)).matches()) {
                String[] args = getArgs(m, 2);
                return new UserLeft(args[0], Reason.valueOf(args[1]));
            }
            else if ((m = rUserjoined.matcher(raw)).matches()) {
                return new UserJoined(getArgs(m, 1)[0]);
            }
            else if ((m = rUsertimeout.matcher(raw)).matches()) {
                return new UserTimeout();
            }
            else if ((m = rUserlistAnswer.matcher(raw)).matches()) {
                String users = getArgs(m, 1)[0];
                return new UserlistAnswer(Arrays.asList(users.split(", ")));
            }
            else if ((m = rServerMessage.matcher(raw)).matches()) {
                return new ServerMessage(getArgs(m, 1)[0]);
            }
            else if ((m = rWelcome.matcher(raw)).matches()) {
                return new Welcome(getArgs(m, 1)[0]);
            }
            else if ((m = rRedirectedMessage.matcher(raw)).matches()) {
                String[] args = getArgs(m, 2);
                return new RedirectedMessage(args[1], args[2]);
            }
        } catch (CommandArgumentParsingException e) {
            throw new CommandNotFoundException("Error parsing arguments for the given command: " + raw);
        }
        // the given text does not match any of the patterns
        throw new CommandNotFoundException("The given command could not be evaluated: " + raw);
    }
}
