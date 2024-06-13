import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);

            pool = Executors.newCachedThreadPool();

            while (!done) {

                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);

            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {

        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            pool.shutdown();
            if (!server.isClosed()) {
                server.close();
            }

            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }

        } catch (IOException e) {
            // ignore exception as we can not handle it again and again
        }

    }

    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // To-DO : Check Edge Cases (for now here it is assumed that the client will
                // enter a valid username)
                // 1.possibly check if the string is empty or too long
                // 1.possibly check if the string does not contain any malicious or bad words
                out.println("Please Enter a Username: ");

                username = in.readLine();

                // log connected
                System.out.println(username + " Connected");

                broadcast(username + " joined the chat");

                String message;

                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/user ")) {
                        String[] messageSplit = message.split(" ", 2);

                        if (messageSplit.length == 2) {
                            broadcast(username + " renamed themselves to " + messageSplit[1]);
                            System.out.println(username + " renamed themselves to " + messageSplit[1]);

                            username = messageSplit[1];

                            out.println("succssfully changed the username to : " + username);

                        } else {
                            out.println("No Username provided!");
                        }
                    }

                    else if (message.startsWith(("/quit"))) {
                        broadcast(username + " left the chat");
                        shutdown();
                    }

                    else {
                        broadcast(username + ": " + message);
                    }
                }

            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore this execption
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
