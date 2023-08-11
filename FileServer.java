import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.*;

class FileServer {
    static List<Command> serverCommands = new ArrayList<>();
    static Path uploadedFileDir = Paths.get(".").resolve("uploadedFiles");
    static {
        serverCommands.add(new ListUploadedFiles());
        serverCommands.add(new UploadFiles());
        serverCommands.add(new DownloadFiles());
        serverCommands.add(new DeleteFiles());
    }

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
                List<String> commandKeywords = serverCommands.stream().map(a->a.getCommandKeyword()).toList();

                while(true){
                    String command = readUserInput();
                    int commandIndex = -1;
                    if(command != null){
                        if((commandIndex = commandKeywords.indexOf(command.split(" ")[0].toLowerCase())) >= 0){
                            String answer = serverCommands.get(commandIndex).executeCommand(command, username, this.in, this.out);
                            out.println(answer);
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

    static interface Command {
        public String getCommandKeyword();
        public String executeCommand(String command, String username, BufferedReader in, PrintWriter out) throws Exception;
    }

    static class ListUploadedFiles implements Command {
        @Override
        public String toString() {
            return "* list - to list uploaded files";
        }

        @Override
        public String getCommandKeyword(){ return "list"; }

        @Override
        public String executeCommand(String command, String username, BufferedReader in, PrintWriter out) throws IOException{ 
            String publicFiles = Files.find(uploadedFileDir, 5, 
            (path, attr) -> attr.isRegularFile() && path.getFileName().toString().startsWith("pub-") && !path.getParent().getFileName().toString().equals(username))
            .map(path->path.getFileName().toString().substring(4))
            .collect(Collectors.joining("\n"));

            String userFiles = Files.find(uploadedFileDir.resolve(username), 5, 
            (path, attr) -> attr.isRegularFile())
            .map(path->{
                String filename = path.getFileName().toString();
                return filename.substring(4) + (filename.substring(0, 4).equals("pub-") ? " (public)" : " (private)");
            })
            .collect(Collectors.joining("\n"));
            

            String totalFiles = "Public:\n" + publicFiles + "\n\nPersonal:\n" + userFiles;
            return totalFiles.length() > 0 ? totalFiles : "You have access to 0 files";
        }
    }

    static class UploadFiles implements Command {
        @Override
        public String toString() {
            return "* upload <file path> <private/public for file access> - to upload a file";
        }

        @Override
        public String getCommandKeyword(){ return "upload"; }

        @Override
        public String executeCommand(String command, String username, BufferedReader in, PrintWriter out) throws IOException{
            String[] commandParts = command.split(" ");
            if(commandParts.length > 3){
                return "To many options (" + commandParts.length + ") provided in the comamnd. It should be: upload <file path> <private/public for file access>";
            }
            if(commandParts.length < 3){
                return "To few options (" + commandParts.length + ") provided in the comamnd. It should be: upload <file path> <private/public for file access>";
            }


            String filePrefix = commandParts[2].equalsIgnoreCase("private") ? "pri-" : "pub-";
            Path filePath = uploadedFileDir.resolve(username).resolve(filePrefix+commandParts[1]);

            if(Files.exists(filePath)){
                return commandParts[1] + " already exists. You must delete it before uploading a file with the same name";
            }

            try(var writter = Files.newBufferedWriter(filePath)){
                out.println("<UPLOAD FILE>");
                while(true){
                    String data = in.readLine();
                    if(data == null || data.equals("<FIN>")){
                        break;
                    }
                    if(data.substring(0, 5).equals("<CON>")){
                        writter.write(data.substring(5)+'\n');
                    }
                }
            } catch (Exception e){
                return "Failed to upload file";
            }

            return "File uploaded successfully";
        }
    }

    static class DownloadFiles implements Command {
        @Override
        public String toString() {
            return "* download <filename> - to download a file";
        }

        @Override
        public String getCommandKeyword(){ return "download"; }

        @Override
        public String executeCommand(String command, String username, BufferedReader in, PrintWriter out){
            return "<NOT IMPLEMENETED>";
        }
    }

    static class DeleteFiles implements Command {
        @Override
        public String toString() {
            return "* delete <filename> - to delete a file (you can only delete a file uploaded by you)";
        }

        @Override
        public String getCommandKeyword(){ return "delete"; }

        @Override
        public String executeCommand(String command, String username, BufferedReader in, PrintWriter out) throws IOException{
            String[] commandParts = command.split(" ");
            if(commandParts.length > 2){
                return "The command contains too many options. It should be delete <filename>";
            }
            if(commandParts.length < 2){
                return "The command doesn't specify file name. It should be delete <filename>";
            }
            String filename = commandParts[1];
            boolean isDeleted = Files.deleteIfExists(uploadedFileDir.resolve(username).resolve("pub-"+filename)) || 
                Files.deleteIfExists(uploadedFileDir.resolve(username).resolve("pri-"+filename));
            if(isDeleted){
                return "The file " + filename + " was deleted successfully";
            } else {
                return "There is no file " + filename;
            }
        }
    }

    public static void main(String args[]){
        ExecutorService executor = null;
        try{
            executor = Executors.newCachedThreadPool();
            Files.createDirectories(uploadedFileDir);
                try(ServerSocket ss = new ServerSocket(6050)) {
                    while(true){
                        Socket s = ss.accept();
                        executor.execute(new ConnectionHandler(s));
                    }
                } catch(Exception e){
                    e.printStackTrace();
                    // TODO handle
                }
        } catch(IOException e){
            e.printStackTrace();
        }finally {
            executor.shutdown();
            System.exit(0);
        }
    }
}