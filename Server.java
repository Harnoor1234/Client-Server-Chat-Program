/*
 This server file establishes the server. It opens a socket at a port and uses hashmaps to store the username and password
combinations to authenticate clients through the Chat class. 

Author - Harnoor Singh Powar
 */
package server;

import java.io.*;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static int portnumber;
    private static Socket clientSocket;
    private static ConcurrentHashMap<User,Boolean> pMap;//present User map//
    private static ConcurrentHashMap<User,Boolean> bl; // blacklist//
    private static ServerSocket serversocket;
    private static HashMap<String,String> um;// User Map//
    
    public static void main(String[] args) throws IOException{
 
        try{
           System.out.println("The server has started");
           pMap = new ConcurrentHashMap<User, Boolean> ();
           bl = new ConcurrentHashMap<User, Boolean>();
           um = readUsersInFromFile();
           int portNumber = Integer.parseInt(args[0]);// Setting up the client server connection//
           ServerSocket serverSocket = new ServerSocket(portNumber);  
           while (true){
                clientSocket = serverSocket.accept();
                new Chat(clientSocket, um, pMap,bl).start();
                InetAddress address = clientSocket.getInetAddress();
                String Ip = address.getHostAddress();
                System.out.println(" "+Ip+"is trying to connect. Waiting to get its username and password.");
                }
             
        }
                    catch (IOException e){
                    e.printStackTrace();
                    }
        
            finally{
            try {
                    System.out.println("Session has ended");
                    serversocket.close();
                    }
            catch (NullPointerException e){
            }
        }
    }

        private static HashMap<String, String> readUsersInFromFile()// Method to read the user-password combiantions in a HashMap//
        throws IOException {
        List<String> uplist = LinesFromFile("user_pass.txt");
        HashMap<String, String> PMap = toMapFromList(uplist);
        return PMap;
   }
        private static ArrayList<String> LinesFromFile(String filename) throws IOException //Read the user password combination in an array list//
        {
        File readFile = new File(filename);
        BufferedReader input = new BufferedReader(new FileReader(readFile));
        ArrayList<String> tread = new ArrayList<String>();
        String start = null;
        while ((start = input.readLine()) != null) {
            tread.add(start);
        }
        input.close();
        return tread;
    }

    private static HashMap<String, String> toMapFromList(
      List<String> userPasswordList) {
        HashMap<String, String> mapping = new HashMap<String, String>();
        for (String string : userPasswordList) {
            int indexOfSpace = string.indexOf(" ");
            String username = string.substring(0, indexOfSpace).trim();
            String password = string.substring(indexOfSpace + 1).trim();
            mapping.put(username, password);
        }
        return mapping;
    }
}
