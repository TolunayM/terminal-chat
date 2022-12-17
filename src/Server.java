import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
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
    public Server(){
        connections = new ArrayList<>();
        done = false;
    }

    private final int PORT = 3000;
    @Override
    public void run() {
        try{
            server = new ServerSocket(PORT);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        }catch (IOException e){
            shutDown();
        }

    }
    public void broadCast(String message){
        for(ConnectionHandler ch: connections){
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }
    public void shutDown(){
        try {
            done = true;
            if(!server.isClosed()){
                server.close();
            }

        }catch (IOException e){
            //ignre
        }

    }

    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;
        public ConnectionHandler(Socket client){
            this.client = client;
        }
        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter nickname: ");
                nickname = in.readLine();

                //TODO:CHECK NICKNAME
                if(!nickname.isEmpty()){
                    System.out.println(nickname + " connected");
                    broadCast(nickname + " joined the chat!");
                    String message;
                    while((message = in.readLine()) != null){
                        if(message.startsWith("/nick ")){
                            // change nickname
                            String[] messageSplit = message.split(" ", 2);
                            if(messageSplit.length == 2){
                                broadCast(nickname + " renamed as" + messageSplit[1]);
                                System.out.println(nickname + " renamed as " + messageSplit[1]);
                                nickname = messageSplit[1];
                                out.println("Nickname changed to " + nickname);
                            }else{
                                out.println("No nickname provided");
                            }
                        }else if(message.startsWith("/quit")){
                            broadCast(nickname + " left the chat");
                            shutDown();
                        }else{
                            broadCast(nickname + ": " + message);
                        }
                    }
                }else{
                    out.println("Please enter nickname: ");
                    nickname = in.readLine();
                }
            }catch (IOException e){
                shutDown();
            }
        }
        public void sendMessage(String message){
            out.println(message);
        }
        public void shutDown(){

            try{
                done = true;
                pool.shutdown();
                out.close();
                in.close();
                if(!client.isClosed()){
                    client.close();
                }
            }catch (IOException e){
                //ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();

    }

}
