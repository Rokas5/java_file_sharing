import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    static class ConnectionHandler implements Runnable{
        
        private final Socket s;
        private final BufferedReader in;
        private final PrintWriter out;
        private final Console console = System.console();

        private BufferedReader uploadedFileReader = null;
        private PrintWriter downloadFileWritter = null;

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
                                closeUploadFileReader();
                                closeDownloadFileReader();
                                break;
                            }

                            if(answer.equalsIgnoreCase("<UPLOAD FILE>")){
                                String line;
                                while((line = uploadedFileReader.readLine()) != null){
                                    out.println("<CON>" + line);
                                }
                                out.println("<FIN>");
                                closeUploadFileReader();
                                continue;
                            }

                            if(answer.equalsIgnoreCase("<DOWNLOAD FILE>")){
                                try{
                                    while(true){
                                        String data = in.readLine();
                                        if(data == null || data.equals("<FIN>")){
                                            break;
                                        }
                                        if(data.substring(0, 5).equals("<CON>")){
                                            downloadFileWritter.write(data.substring(5)+'\n');
                                        }
                                    }
                                    downloadFileWritter.flush();
                                } finally {
                                    closeDownloadFileReader();
                                }
                                continue;
                            }

                            System.out.println(answer);
                        }
                    }
                    while(true){
                        String command = console.readLine();
                        if(command != null){
                            String[] commandParts = command.split(" "); 
                            if(commandParts[0].equalsIgnoreCase("upload")){
                                try{
                                    uploadedFileReader = Files.newBufferedReader(Paths.get(".").resolve(commandParts[1]));
                                } catch (Exception e){
                                    System.out.println("There is no " + commandParts[1] + " file");
                                    uploadedFileReader = null;
                                    continue;
                                }
                            }

                            if(commandParts[0].equalsIgnoreCase("download")){
                                try{
                                    downloadFileWritter = new PrintWriter(Files.newBufferedWriter(Paths.get(".").resolve(commandParts[1])));
                                } catch (Exception e){
                                    System.out.println("There is already a " + commandParts[1] + " file");
                                    e.printStackTrace();
                                    downloadFileWritter = null;
                                    continue;
                                }
                            }
    
                            this.out.println(command);
                            break;
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void closeUploadFileReader() throws IOException{
            if(uploadedFileReader != null){
                uploadedFileReader.close();
                uploadedFileReader = null;
            }
        }

        public void closeDownloadFileReader() throws IOException{
            if(downloadFileWritter != null){
                downloadFileWritter.close();
                downloadFileWritter = null;
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
