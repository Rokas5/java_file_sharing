import java.io.*;
import java.net.*;
import java.util.Arrays;
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
                    while(true){
                        String answer = this.in.readLine();
                        if(answer != null){
                            if(answer.equalsIgnoreCase("<END OF ANSWER>")) {
                                break;
                            }
                            System.out.println(answer);
                        }
                    }
                    String command = console.readLine();
                    if(command != null){
                        this.out.println(command);
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public String readLine() throws IOException{
            final int bufferSize = 100;
            char[] buffer = new char[bufferSize];
            StringBuilder outputLine = new StringBuilder();
            int readLength = bufferSize;
            while(readLength != 0){
                System.out.println();
                readLength = console.reader().read(buffer, 0, bufferSize);
                if(readLength < bufferSize){
                    outputLine.append(Arrays.copyOfRange(buffer, 0, readLength));
                }
            }

            if(outputLine.length() == 0){
                return null;
            } else {
                return outputLine.toString();
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
