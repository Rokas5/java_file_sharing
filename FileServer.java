import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FileServer {

    static class ConnectionHandler implements Runnable{

        private final Socket s;
        private final BufferedReader in;
        private final PrintWriter out;
        
        public ConnectionHandler(Socket s) throws IOException{
            this.s = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(new BufferedOutputStream(s.getOutputStream()), true);
        }
        
        public void run(){
            try(this.in; this.out){
                out.println("Hello, please sign-in or sign-up. Type sign-in or sing-up:");

                while(true){
                    String command = in.readLine();
                    if(command != null){
                        if(command.equalsIgnoreCase("sign-in")){
                            out.println("Type in username: ");
                        } else if(command.equalsIgnoreCase("sign-up")){
                            out.println("Create username: ");
                        } else {
                            out.println("Unknown command, please type in either sign-in or sign-up");
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
                // TODO handle
            }
        }

        private boolean login(){
            return false;
        }

        private boolean signup(){
            return false;
        }
    }

    public static void main(String args[]){
        ExecutorService executor = null;
        try{
            executor = Executors.newCachedThreadPool();
                try(ServerSocket ss = new ServerSocket(6050)) {
                    while(true){
                        Socket s = ss.accept();
                        executor.execute(new ConnectionHandler(s));
                    }
                } catch(Exception e){
                    e.printStackTrace();
                    // TODO handle
                }
        } finally {
            executor.shutdown();
            System.exit(0);
        }
    }
}