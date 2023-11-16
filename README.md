# Java File Sharing App
### This is a simple application that allows to upload and download files to/from server.

## To run it:
* Launch the server by calling: java FileServer.java
* Launch clients by calling: java Client.java

## Functionality
### Server:
Once launched will log all actions taken by users.
### Client:

Once the server is launched, the user can run Client.java to connect to it.
Once connection in complete, they will be asked to login or create an account.
```
Hello, please sign-in or sign-up. Type sign-in or sing-up:
```
After logging in the user can upload a file, see all uploaded files, download or delete a file.
```
* list - to list uploaded files
* upload <file path> <private/public for file access> - to upload a file
* download <filename> - to download a file
* delete <filename> - to delete a file (you can only delete a file uploaded by you)
```
When uploading a file a user can specify it they want it to be public (available to all users) or private (seen only to them).
The user can only delete their own files.
The user can only download their own files or other users public files.



The FileServer.java file will create folders called "uploadedFiles" and "userInfo" where it will store uploaded files and user information.