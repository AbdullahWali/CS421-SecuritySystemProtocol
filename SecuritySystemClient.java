import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.nio.ByteBuffer;
import java.lang.Short;


public class SecuritySystemClient {

    static final int AUTHORIZE  = 0;
    static final int KEEPALIVE = 1;
    static final int OK = 2;
    static final int INVALID = 3;
    static final int EMERGENCY = 4;
    static final int ALARM = 5;
    static final int DISCARD = 6;
    static final int EXIT = 7;

    static final String[] ResponseNames = new String[]{"AUTHORIZE", "KEEPALIVE", "OK", "INVALID", "EMERGENCY",
        "ALARM", "DISCARD", "EXIT"};

    public static void main(String[] args) throws IOException {

        int emergencyNo = 0;
        int portNumber = 42;
        String username = args[0];
        String password = args[1];

        Socket clientSocket = new Socket("localhost", portNumber);
        System.out.println("Connection initiated");
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream inFromServer = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));


        //Authorise
        byte messageType = 0; //Auth type
        String data = username + ":" + password;
        byte[] dataBytes = data.getBytes(StandardCharsets.US_ASCII);
        short messageLen = (short) dataBytes.length;

        System.out.println("Sending Auth request..");
        outToServer.writeByte(messageType);
        outToServer.writeShort(messageLen);
        outToServer.write(dataBytes);

        //Read Response and start listening
        byte responseType;
        int responseLen;
        responseType = inFromServer.readByte();
        responseLen = inFromServer.readShort() & 0xffff;

        if (responseType == OK)
            System.out.println("Response from server: OK");
        else if (responseType == INVALID){ 
            System.out.println("Response from server: Invalid\nAbort mission..");
            return;
        }


        while (true){
            System.out.println("\nReceiving message from server:");
            responseType = inFromServer.readByte();
            responseLen = inFromServer.readShort() & 0xffff;
           
            System.out.println("Message type: " + ResponseNames[responseType]);
            if (responseLen > 0)
            	System.out.println("Data length: " + responseLen);
		
			switch (responseType){
                case KEEPALIVE: 
                    System.out.println("Sending KEEPALIVE request to server");
                    outToServer.writeByte(KEEPALIVE);
                    outToServer.writeShort(0);
                    break;
                case OK:
                    break;
                case INVALID:
                    System.out.println("Aborting");
                    clientSocket.close();
                    return;
                case EMERGENCY:
                	emergencyNo++;
                	String fileName = "snapshot_" + emergencyNo + ".txt"; 
                	FileWriter fileWriter = new FileWriter(fileName);
                	PrintWriter printWriter = new PrintWriter(fileWriter);
                	byte[] buffer = new byte[responseLen];
                	inFromServer.readFully(buffer);
                	String res = new String(buffer,"ASCII");
                	printWriter.print(res);
            		System.out.println("Emergency message received. Enter 1 to ring the alarm, enter 2 to discard:");
            		Scanner reader = new Scanner(System.in);
            		int n = reader.nextInt();
            		if(n == 1)
            		{
            			System.out.println("Sending ALARM message to server");
                    	outToServer.writeByte(ALARM);
                    	outToServer.writeShort(0);
                    	System.out.println("ALARM message sent");
            		}
            		else
            		{
            			System.out.println("Sending DISCARD message to server");
                    	outToServer.writeByte(DISCARD);
                    	outToServer.writeShort(0);
                    	System.out.println("DISCARD message sent");
            		} 
            		break;

                case EXIT:
                    System.out.println("Aborting");
                    clientSocket.close();
                    return;
                default:
                    break;
            }


        }

    }
}
