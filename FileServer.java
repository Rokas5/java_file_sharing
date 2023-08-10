import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;

class FileServer {
    static List<Object> serverCommands = new ArrayList<>();

    static class ConnectionHandler implements Runnable{

        private final Socket s;
        private final BufferedReader in;
        private final PrintWriter out;

        private String username;
        
        public ConnectionHandler(Socket s) throws IOException{
            this.s = s;
            this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            this.out = new PrintWriter(new BufferedOutputStream(s.getOutputStream()), true);
        }
        
        public void run(){
            try(this.in; this.out){
                out.println("Hello, please sign-in or sign-up. Type sign-in or sing-up:");

                while(true){
                    String command = readUserInput();
                    if(command != null){
                        if(command.equalsIgnoreCase("sign-in")){
                            this.username = login();
                            if(this.username != null){
                                break;
                            }
                        } else if(command.equalsIgnoreCase("sign-up")){
                            out.println("Create username: ");
                        } else {
                            out.println("Unknown command, please type in either sign-in or sign-up");
                        }
                    }
                }

                out.println("Hello " + this.username + ", below you can see a list of commands available to you:");
                serverCommands.forEach(out::println);

                while(true){
                    String command = readUserInput();
                    if(command != null){
                        if(command.equalsIgnoreCase("test")){

                        } else {
                            out.println("These is no such command");
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
                // TODO handle
            }
        }

        private String readUserInput() throws IOException{
            this.out.println("<END OF ANSWER>");
            return this.in.readLine();
        }

        private String login() throws IOException{
            while(true){
                out.println("Type in username: ");
                String username;
                while(true){
                    username = readUserInput();
                    if(username != null){
                        break;
                    }
                }
                // TODO retrieve password based on username
                String correntPassword = "pass"; // TODO fix to not use String to store passwords
                out.println("Type in password: ");
                String password;
                while(true){
                    password = readUserInput();
                    if(password != null){
                        if(password.equals(correntPassword)){
                            return username;
                        } else {
                            out.println("Information incorrect! ");
                            break;
                        }
                    }
                }
            }
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