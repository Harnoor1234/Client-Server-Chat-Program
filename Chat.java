/*
 This class implements the various functions given in the assignment.
 
 Author - Harnoor Singh Powar
 */
package server;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
public class Chat extends Thread {
private Socket socket;
private static int miliseconds = 1000;// miliseconds in second//
private int block_time = 60*miliseconds;// define block time//
private boolean running = true;
private HashMap<String, String> userMap;
private User currentUser;
private final static long TIME_OUT = miliseconds*60*30;
private static BufferedReader br;
private  ConcurrentHashMap<User,Boolean> pMap;//present User map//
private  ConcurrentHashMap<User,Boolean> bl; // blacklist//
Handy handy = new Handy();
Messi messi = new Messi();
Recognize recognize = new Recognize ();
Check check = new Check ();
public Chat (Socket socket, HashMap<String, String> userMap,ConcurrentHashMap<User,Boolean> pMap, ConcurrentHashMap<User,Boolean> bl){
this.socket = socket;
this.userMap = userMap;
this.pMap = pMap;
this.bl = bl;
}
public void run() {
        try {
        this.currentUser = new User();
        this.check = new Check();
        this.handy = new Handy(createBufferedReader());
        this.messi = new Messi(createPrintWriter());
        this.recognize = new Recognize();
        }
        catch (IOException e2) {
        e2.printStackTrace();}
        boolean userex = false;
        while (running){
        try {
    if (userex)
    {
        break;
    }
    else{   
            String username = handy.readInLine();
            String password = handy.readInLine();
            String prevusername = this.currentUser.getUsername();
            long loggedOnTime = System.currentTimeMillis();
            this.currentUser.setInformation(username, password, false,loggedOnTime, socket);
            if (isAlreadyOnline()) {
            messi.sendAlreadyOnlineMessage();
            socket.close();
            continue;
            }
            if (isOnBlackList() && !timeUp(block_time)) {
            messi.sendStillBlockedMessage();
            socket.close();
                    } else {
                        bl.remove(this.currentUser);
                    }
            if (check.emptyinput(username, password)) {
                        messi.askForIdentity();
                        continue;
                    }

                    if (!check.sameUserTryingToLogin(prevusername,currentUser)) {
                    recognize.setCount(0);
                    }
                    recognize.updateInformation(currentUser, userMap);
                    userex = recognize.authenticate();
                    if (check.failedLogin(userex,recognize)){
                    System.out.println("Failed Login. Please try again");
                    }
                     else if (check.failedTooManyTimes(userex, recognize)) {
                        currentUser.setBlocked(true);
                        currentUser.setAlive(false);
                        bl.put(currentUser, currentUser.isBlocked());
                        messi.sendBlockedMessage(block_time);
                        running = false;
                     }
    }
        }
                    
    catch (IOException e) {
    messi.sendServerDisconnect();
    try {
    closeIO();
    terminate();
    } 
    catch (IOException e1) {
    e1.printStackTrace();
                }
            }
        }
    if (userex) {
            currentUser.setAlive(true);
            pMap.put(currentUser, currentUser.isAlive());
            messi.sendWelcomeMessage();
            currentUser.setLastActiveTime(System.currentTimeMillis());
    }
    
    while (running){
            try {
                markStaleUsersFromPresentList();
                String command = handy.readInLine();
                currentUser.setLastActiveTime(System.currentTimeMillis());
                command_select(command);

                if (command.equals("logout")) {
                    break;
                }
            } catch (IOException e1) {

              e1.printStackTrace();
            } catch (NullPointerException e) {
            }

        }
}
private BufferedReader createBufferedReader() throws IOException {
return new BufferedReader(
new InputStreamReader(socket.getInputStream()));
    }

private PrintWriter createPrintWriter() throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }

    private boolean isAlreadyOnline() {
        boolean alreadyOnline = false;
        for (User user : pMap.keySet()) {
            if (user.getUsername().equals(currentUser.getUsername()) && user.isAlive()) {
                alreadyOnline = true;
            }
        }
        return alreadyOnline;
    }
    

    private boolean isOnBlackList() {
        boolean onList = false;
        for (User user : bl.keySet()) {
            if (checkIsCurrentUser(user) && checkIsSameIPAddress(user)) {
                onList = true;
            }
        }
        return onList;
    }

    private boolean timeUp(long timeInterval) {
        long lastLoggedIn = 0;
        for (User user : bl.keySet()) {
            if (checkIsCurrentUser(user) && checkIsSameIPAddress(user)) {
                lastLoggedIn = user.getLoggedInTime();
            }

        }
        long timeAgo = calculateTimeSpan(timeInterval);
        return lastLoggedIn < timeAgo;
    }
    public boolean checkIsSameIPAddress (User user){
        if (user.getIPAddress().equals(currentUser.getIPAddress()));
        return true;
        }

    private long calculateTimeSpan(long time_interval) {
        return System.currentTimeMillis() - time_interval;
    }

    private void markStaleUsersFromPresentList() throws IOException {
        for (User user : pMap.keySet()) {
            if (!actvieLessThanTime(user, TIME_OUT)
                    || user.getSocket().isClosed() || !checkIsUserAlive(user)) {
                user.setAlive(false);
                pMap.replace(user, user.isAlive());
            }
        }
    }
    private boolean checkIsCurrentUser (User user){
        return user.getUsername().equals(currentUser.getUsername());
    }

    private boolean actvieLessThanTime(User user, long time_interval) {
        long timeAgo = calculateTimeSpan(time_interval);
        return user.getLastActiveTime() > timeAgo;
    }

    private boolean checkIsUserAlive(User user) {
        return user.isAlive() == true ? true : false;
    }

    private void command_select(String command) throws IOException {
        if (check.whoelse_request(command)) {
            messi.sendResponse(whoelse_response());
        } else if (check.wholastthr_request(command)) {
            messi.sendResponse(wholasthr_response());
        } else if (check.broadcast_request(command)) {
            broadcast_response(command);
        } else if (check.message_request(command)) {
            private_message_response(command);
        } else if (check.logout_request(command)) {
            logout();
            terminate();
        } else {
            messi.sendUnrecognizedStatement();
        }

    }
    private String whoelse_response() throws IOException {
        String whoelse_response = "whoelse\n\n";
        markStaleUsersFromPresentList();
        for (User user : pMap.keySet()) {
            if (checkIsUserAlive(user) && !checkIsCurrentUser(user)) {
                whoelse_response += user.getUsername() + "\n";
            }
        }
        return whoelse_response;
    }
    private String wholasthr_response() {
        String wholasthr_response = "wholasthr\n\n";
        for (User user : pMap.keySet()) {
            long LAST_HOUR = miliseconds*60*60;
            if (actvieLessThanTime(user, LAST_HOUR)
                    && !checkIsCurrentUser(user) && wasActive(user)) {
                wholasthr_response += user.getUsername() + "\n";
            }
        }
        return wholasthr_response;
}

    private boolean wasActive(User user) {
        return user.getLastActiveTime() > 0;
    }

    private void broadcast_response(String command) throws IOException {
        String message = parseBroadcastMessage(command);
        for (User user : pMap.keySet()) {
        if (checkIsUserAlive(user) && !checkIsCurrentUser(user)) {
        messi.setOutput(new PrintWriter(user.getSocket().getOutputStream()));
        messi.sendResponse(message);
            }
        }
        messi.setOutput(createPrintWriter());
    }

    private String parseBroadcastMessage(String command) {
        int init = command.indexOf(" ");
        String message = command.substring(init).trim();
        return "broadcast\n" + "\n" + this.currentUser.getUsername() + ": "
                + message;
    }

    private void private_message_response(String command) throws IOException {
        String message = parsePrivateMessage(command);
        String recipient = parseRecipient(command);
        for (User user : pMap.keySet()) {
            if (checkIsUserAlive(user) && !checkIsCurrentUser(user)
                    && isRecipient(user, recipient)) {
                messi.setOutput(new PrintWriter(user.getSocket()
                        .getOutputStream()));
                messi.sendResponse(message);
            }
        }
        messi.setOutput(createPrintWriter());
    }

    private String parsePrivateMessage(String command) {
        String[] messageComponents = command.split(" ");
        String message = "";
        for (int i = 2; i < messageComponents.length; i++) {
            message += messageComponents[i] + " ";
        }
        return "message\n" + "\n" + this.currentUser.getUsername() + ": "
                + message;
    }

    private String parseRecipient(String command) {
        String[] messageComponents = command.split(" ");
        String recipientName = messageComponents[1];
        return recipientName;
    }

    private boolean isRecipient(User user, String username) {
        return user.getUsername().equals(username);
    }

    private void logout() throws IOException {
        currentUser.setAlive(false);
        currentUser.setLastActiveTime(System.currentTimeMillis());
        pMap.replace(currentUser, currentUser.isAlive());
        closeIO();
    }

    private void closeIO() throws IOException {
        handy.closeInput();
        messi.closeOutput();
        socket.close();
    }

    public void terminate() {
        running = false;
    }

/* Specialized class for reading inputs from socket.
    
    */
    
public class Handy {

    private BufferedReader input;

    public Handy() {
        this.input = null;
    }

    public Handy (BufferedReader input) {
        this.input = input;
    }

    public BufferedReader getInput() {
        return this.input;
    }

    public void setInput(BufferedReader input) {
        this.input = input;
    }

    public String readInLine() throws IOException {
        return input.readLine().trim();
    }

    public void closeInput() throws IOException {
        this.input.close();
    }

}
/* Specialized class for outputting messages.

*/ 
public class Messi {
  private static final String NEWLINE = "\n";
  private PrintWriter output;
  public Messi() {
  this.output = null;
}
public Messi(PrintWriter output) {
  this.output = output;
    }
public PrintWriter getOutput() {
   return this.output;
    }

    public void setOutput(PrintWriter output) {
    this.output = output;
    }
    public void askForIdentity() {
    writeAndClear("Please input a username and password");
    }
    public void sendFailedWarning() {
    writeAndClear("Failed login. Please try again");
    }
    public void sendBlockedMessage(long blockTime) {
    writeAndClear("Blocking IP address for " + blockTime + " milliseconds");
    }
    public void sendStillBlockedMessage() {
    writeAndClear("You are still blocked.");
    }
    public void sendResponse(String response) {
    writeAndClear(response);
    }
    public void sendLoggingOut() {
    writeAndClear("Logging out");
    }
    public void sendUnrecognizedStatement() {
    writeAndClear("Statement not recognized.");
    }
    public void sendWelcomeMessage() {
    writeAndClear("Welcome to this Chat Server");
    }
    public void sendServerDisconnect() {
    writeAndClear("It appears the server has disconnected.");
    }
    public void sendAlreadyOnlineMessage() {
    writeAndClear("This user is already online!");
    }
    public void closeOutput() {
    output.close();
    }
    private void writeAndClear(String response) {
    output.write(response);
    output.write(NEWLINE);
    output.flush();
    }
}
/* This class is used for checking or verifying the client's status.

*/
public class Check {
    public Check (){
    }
    public boolean emptyinput (String username, String password){
        return username.equals("")||password.equals("");
    }
    public boolean sameUserTryingToLogin(String previousUsername, User user) {
        return previousUsername.equals(user.getUsername());
    }

    public boolean failedLogin(boolean userExists, Recognize recognize) {
        return userExists == false && recognize.getCount() <= 2;
    }

    public boolean failedTooManyTimes(boolean userExists, Recognize recognize) {
        return userExists == false && recognize.getCount() > 2;
    }

    public boolean empty_request(String command) {
        return command.equals("");
    }


    public boolean whoelse_request(String command) {
        return command.equals("whoelse");
    }

    public boolean wholastthr_request(String command) {
        return command.equals("wholast");
    }

    public boolean broadcast_request(String command) {
        return command.contains("broadcast");
    }

    public boolean message_request(String command) {
        return command.contains("message");
    }

    public boolean logout_request(String command) {
        return command.equals("logout");
    }
    
}
/* This class is used to authenticate the client.

*/
public class Recognize {

    private User user;
    private HashMap<String, String> userPassCombos;
    private int count;

    public Recognize() {
        this.user = null;
        this.userPassCombos = null;
        this.count = 0;
    }

    public Recognize(User user, HashMap<String, String> userPassCombos) {
        this.user = user;
        this.userPassCombos = userPassCombos;
        this.count = 0;
    }

    public boolean authenticate() {
        if (hasUser() && isUser()) {
            return true;
        } else if (hasUser() && !isUser()) {
            this.count++;
            return false;
        } else {
            return false;
        }
    }

    public void updateInformation(User user, HashMap<String, String> userMap) {
        this.setUser(user);
        this.setUserPassCombos(userMap);
    }

    private boolean hasUser() {
        return userPassCombos.containsKey(user.getUsername());
    }

    private boolean isUser() {
        return userPassCombos.get(user.getUsername())
                .equals(user.getPassword());
    }

    public User getUser() {
        return this.user;
    }

    public HashMap<String, String> getUserPassCombos() {
        return this.userPassCombos;
    }

    public int getCount() {
        return this.count;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setUserPassCombos(HashMap<String, String> userPassCombos) {
        this.userPassCombos = userPassCombos;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
}


  

