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
    static Path userInfoDir = Paths.get(".").resolve("userInfo");
    static Path userInfoFile = userInfoDir.resolve("info.txt");
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
                            this.username = signup();
                            if(this.username != null){
                                break;
                            }
                        } else {
                            out.println("Unknown command, please type in either sign-in or sign-up");
                        }
                    }
                }
                System.out.println(username + " has connected");
                out.println("Hello " + this.username + ", below you can see a list of commands available to you:");
                serverCommands.forEach(out::println);
                List<String> commandKeywords = serverCommands.stream().map(a->a.getCommandKeyword()).toList();
                Files.createDirectories(uploadedFileDir.resolve(username));

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
                if(username != null){
                    System.out.println(username + " has disconnected");
                } else {
                    System.out.println("Unregistered user disconnected");
                }
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
                out.println("Type in password: ");
                String password;
                while(true){
                    password = readUserInput();
                    if(password != null){
                        if(checkPassword(username, password)){
                            return username;
                        } else {
                            out.println("Information incorrect! ");
                            break;
                        }
                    }
                }
            }
        }

        private String signup() throws IOException{
            while(true){
                out.println("Type in username: ");
                String username;
                while(true){
                    username = readUserInput();
                    if(username != null){
                        if(usernameExists(username)){
                            out.println("Such username already exists");
                        } else {
                            break;
                        }
                    } else {
                        out.println("You must provide a user name");
                    }
                }
                out.println("Type in password: ");
                String password;
                while(true){
                    password = readUserInput();
                    if(password != null){
                        if(!password.contains(" ")){
                            addUserInfo(username, password);
                            out.println("Account successfully created!");
                            return username;
                        } else {
                            out.println("Password cannot contain spaces");
                        }
                    }
                }
            }
        }

        private boolean usernameExists(String username) throws IOException{
            List<String> data = Files.readAllLines(userInfoFile);
            for(String line : data){
                String existingUser = line.split(" ")[0];
                if(username.equals(existingUser)){
                    return true;
                }
            }
            return false;
        }

        private void addUserInfo(String user, String pass) throws IOException{
            try(var writter = Files.newBufferedWriter(userInfoFile, StandardOpenOption.APPEND)){
                writter.write(user + " " + pass + '\n');
            }
        }

        private boolean checkPassword(String username, String password) throws IOException{
            List<String> data = Files.readAllLines(userInfoFile);
            for(String line : data){
                String[] userData = line.split(" ");
                if(username.equals(userData[0]) && password.equals(userData[1])){
                    return true;
                }
            }
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

            boolean isPrivate = commandParts[2].equalsIgnoreCase("private");
            String filePrefix = isPrivate ? "pri-" : "pub-";
            Path filePath = uploadedFileDir.resolve(username).resolve(filePrefix+commandParts[1]);

            if(Files.exists(filePath)){
                return commandParts[1] + " already exists. You must delete it before uploading a file with the same name";
            }

            if(!isPrivate && checkIfPublicFileExists(commandParts[1], uploadedFileDir)){
                return "There is already a public file with name " + commandParts[1] + ". Rename your file or upload it as private.";
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
                writter.flush();
            } catch (Exception e){
                return "Failed to upload file";
            }

            return "File uploaded successfully";
        }

        private boolean checkIfPublicFileExists(String filename, Path uploadedFileDir) throws IOException {
            return Files.find(uploadedFileDir, 5, (path, attr) -> attr.isRegularFile() && path.getFileName().toString().startsWith("pub-"))
            .map(path->path.getFileName().toString().substring(4)).anyMatch(path -> path.equals(filename));
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
            String[] commandParts = command.split(" ");
            if(commandParts.length > 2){
                return "The command contains too many options. It should be delete <filename>";
            }
            if(commandParts.length < 2){
                return "The command doesn't specify file name. It should be delete <filename>";
            }

            Path userFileDir = uploadedFileDir.resolve(username);
            Path filepath;
            if(Files.exists(userFileDir.resolve("pri-"+commandParts[1]))){
                filepath = userFileDir.resolve("pri-"+commandParts[1]);
            } else if(Files.exists(userFileDir.resolve("pub-"+commandParts[1]))) {
                filepath = userFileDir.resolve("pri-"+commandParts[1]);
            } else {
                return "Such file doesn't exists";
            }
            out.println("<DOWNLOAD FILE>");
            try(var reader = Files.newBufferedReader(filepath)) {
                String line;
                while((line = reader.readLine()) != null){
                    out.println("<CON>" + line);
                }
                out.println("<FIN>");
            } catch(IOException e){
                System.out.println("Failure to download a file");
                e.printStackTrace();
                return "Failed to download a file";
            }
            return "File downloaded successfully";
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
            Files.createDirectories(userInfoDir);
            if(!Files.exists(userInfoFile)){
                Files.createFile(userInfoFile);
            }
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