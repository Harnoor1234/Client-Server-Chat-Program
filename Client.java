/*
 This basic client class implements an interface for the user to input and see commands which are coming from the server
 */
package client;
import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Harnoor Singh Powar
 */
public class Client {
    private int pN;
    private boolean running;
    private String serverAddress;
    private Socket socket;
    private BufferedReader socketInput;
    private PrintWriter output;
    private BufferedReader In;
    private final static String NEWLINE = "\n";
    private final static long MILLISECONDS_IN_SECOND = 1000;
    private final static long TIME_OUT = MILLISECONDS_IN_SECOND* 60 * 30;
    public Client() {
        this.pN = 0;
        this.serverAddress = null;
        this.socket = null;
        this.socketInput = null;
        this.output = null;
    }
    public static void main(String[] args) {
     if (args.length != 2) {
        System.out.println("Please enter the arguments correctly.");
        System.exit(1);
    }
         else {
            Client client = new Client();
            try {
                client.run(args);
            } catch (IOException e) {
                System.out.println("error. Perhaps"
                        + " the server disconnected or is not running.");
            } catch (NullPointerException e) {
                System.out.println("Disconnected from the server.");
            }
        }
    }
        
 
        private void run(String[] args) throws IOException {
        while (true){
        serverAddress = args[0];
        pN = Integer.parseInt(args[1]);
        socket = new Socket(serverAddress, pN);
        socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        In = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
        System.out.print("Username: ");
        String username = In.readLine();
        output.write(username);
        output.write(NEWLINE);
        output.flush();
        System.out.print("Password: ");
        String password = In.readLine();
        output.write(password);
        output.write(NEWLINE);
        output.flush();
        String line = socketInput.readLine();
        if (line.equals("Welcome to this Chat Server")) {
        System.out.println(line);
        break;
            } else if (line.contains("Blocking")) {
                System.out.println(line);
                this.socket.close();
                System.exit(1);
            } else if (line.contains("blocked")) {
                System.out.println(line);
                this.socket.close();
                System.exit(1);
            } else if (line.contains("online")) {
                System.out.println(line);
                this.socket.close();
                System.exit(1);
            } else {
                System.out.println(line);
            }
        }
        new CL(socketInput).start();
        new CP(output, In, TIME_OUT, socket).start();
    }

                
             
            
   public class CP extends Thread {

    private BufferedReader stdIn;
    private PrintWriter output;
    private long timeInterval;
    private Socket socket;
    private Timer logoutTimer;
    private volatile boolean running = true;
    private static final String NEWLINE = "\n";

    public CP(PrintWriter output, BufferedReader stdIn, long timeInterval, Socket socket) {
        this.output = output;
        this.stdIn = stdIn;
        this.timeInterval = timeInterval;
        this.socket = socket;
    }

    public void run() {
        String command = null;
        try {
            logoutTimer = new Timer();
            while (running) {
                LogoutTask logoutSequence = new LogoutTask(this.socket,timeInterval);
                logoutTimer.schedule(logoutSequence, timeInterval);
                System.out.print("Command: ");
                command = stdIn.readLine();
                if (command.equals("")) {
                    continue;
                } else if (command.equals("logout")) {
                    System.out.println("Logging out...");
                    writeAndClear(command);
                    this.socket.getInputStream().close();
                    this.socket.close();
                    terminate();
                    System.exit(0);
                }
                writeAndClear(command);
                logoutSequence.cancel();
                logoutTimer.purge();
                sleep(50);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeAndClear(String command) {
        output.write(command);
        output.write(NEWLINE);
        output.flush();
    }

    private void terminate() {
        running = false;
    }

    class LogoutTask extends TimerTask {
        private Socket socket;

        public LogoutTask(Socket socket, long timeInterval) {
            this.socket = socket;
        }

        public void run() {

            System.out.println("\nUser inactive for extended period of time. "
                    + "Exiting server.");
            output.write("logout");
            output.write("\n");
            output.flush();
            try {
                socket.shutdownOutput();
                socket.shutdownInput();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }

}
   /* This class helps the client deal with the commands
   
   */
     public class CL extends Thread {

    private BufferedReader socketInput;

    public CL (BufferedReader socketInput) {
        this.socketInput = socketInput;
    }

    public void run() {
        while (true) {
            String response = null;
            try {
                response = socketInput.readLine();
                receiveResponseAndAct(response);
            } catch (IOException e) {
                try {
                    socketInput.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (NullPointerException e) {
            }

        }
    }

    
    
     
    private void receiveResponseAndAct(String response) throws IOException {
        if (checkUnrecognizedCommand(response)) {
            printResponse(response);} 
        else if (checkWhoelseCommand(response)|| checkWholasthrCommand(response)) {
          readInUserMessagesAndPrint(response);
        } else if (checkBroadcastCommand(response)) {
          readInMessageAndPrint(response);
        } else if (checkPrivateMessageCommand(response)) {
          readInHelpMessageAndPrint(response);
        }

    }

    private boolean checkHelpCommand(String response) {
        return checkCommand(response, "-");
    }

    private boolean checkUnrecognizedCommand(String response) {
        return checkCommand(response, "recognized");
    }

    private boolean checkWhoelseCommand(String response) {
        return checkCommand(response, "whoelse");
    }

    private boolean checkWholasthrCommand(String response) {
        return checkCommand(response, "wholasthr");
    }

    private boolean checkBroadcastCommand(String response) {
        return checkCommand(response, "broadcast");
    }

    private boolean checkPrivateMessageCommand(String response) {
        return checkCommand(response, "message");
    }

    private boolean checkCommand(String response, String match) {
        return response.contains(match);
    }

    private void printResponse(String response) {
        System.out.println(response);
    }

    private void readInHelpMessageAndPrint(String response) throws IOException {
        while ((response = socketInput.readLine()) != null) {
            printResponse(response);
        }
    }

    private void readInUserMessagesAndPrint(String response) throws IOException {
        while ((response = socketInput.readLine()) != null) {
            if (checkWhoelseCommand(response)
                    || checkWholasthrCommand(response)) {
            } else {
                printResponse(response);
            }
        }
    }

    private void readInMessageAndPrint(String response) throws IOException {
        while ((response = socketInput.readLine()) != null) {
            if (checkBroadcastCommand(response)) {
            } else {
                printResponse(response);
            }
        }
    }  
    }
    
}
