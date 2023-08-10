import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    static class ConnectionHandler implements Runnable{
        
        private final Socket s;
        private final BufferedReader in;
        private final PrintWriter out;
        private final Console console = System.console();

        public ConnectionHandler(Socket s) throws IOException{
            this.s = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(new BufferedOutputStream(s.getOutputStream()), true);
        }
        
        public void run(){
            try(this.in; this.out){
                while(true){
                    String answer = this.in.readLine();
                    if(answer != null){
                        System.out.println(answer);
                        while(true){
                            String command = console.readLine();
                            if(command != null){
                                this.out.println(command);
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e){

            }
        }
    }


    public static void main(String args[]){
        ExecutorService executor = null;
        try{
            executor = Executors.newCachedThreadPool();
            while(true){
                try(Socket s = new Socket("localhost", 6050)){
                    executor.execute(new ConnectionHandler(s));
                    executor.awaitTermination(1, TimeUnit.DAYS);
                } catch(Exception e){
                    e.printStackTrace();
                    // TODO handle
                }
            }
        } finally{
            executor.shutdown();
            System.exit(0);
        }
    }
}
