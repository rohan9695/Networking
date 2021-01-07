/*
 * A UDP Client
 * Implementing the Stop-n-Wait-ARQ algorithm
 * Uses: 
 * 		class Packet  payload, sequence numbers, etc
 * 		class NoisyDatagramSocket - extends DatagramSocket - add drops & delays
 * 
 * Last modified:  Malcolm 20171005
 *
 */

import java.io.*;
import java.net.*;
import java.nio.*;

class StopAndWaitClient{
    private static final int BUFFER_SIZE = 2048;
	private static final int size = 1;
    private static final int PORT = 6789;
    private static final int SEQUENCE_NUMBER = 0;
    private static final int TimeOutValue = 5000;
    private static final String SERVER = "localhost";
	private static int N = 10;                        // number of times to loop 
	
    public static void main(String args[]) throws Exception{
		// Create a socket  //DatagramSocket socket = new DatagramSocket();
		NoisyDatagramSocket socket = new NoisyDatagramSocket();
		socket.setSoTimeout( TimeOutValue );

		// The message we're going to send converted to bytes
		Integer sequenceNumber = SEQUENCE_NUMBER;

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //String sentence = " 0123456789";      
		String sentence;
		
		while( (sentence = inFromUser.readLine()) != null) {
		
		  N = sentence.length();
		  //System.out.println("FROM USER:"+sentence);
		  for (int i=0; i<N; i+=size) {
			String sData = sentence.substring(i, i+size);
			boolean timedOut = true;
            
			// Create a byte array for sending and receiving data
			byte[] rData = new byte[ BUFFER_SIZE ];

            Packet sPacket = new Packet(sequenceNumber, sData);                  // payload: data & i (sequence number)

            // serialize the payload
            ByteArrayOutputStream baos = new ByteArrayOutputStream();   // setup stream - will be bytes
            ObjectOutputStream oos = new ObjectOutputStream(baos);      // setup to serialize Packet
            oos.writeObject(sPacket);                                 // write Packet to Object stream
            byte[] sBuf = baos.toByteArray();                           // put packet (stream) into byte buffer

			// Get the IP address of the server
			InetAddress serverIP = InetAddress.getByName( SERVER );

			System.out.println( "Sending Packet (seq_n: " + sequenceNumber + ") Payload: '" + sData + "'"); 
			DatagramPacket sDatagram = new DatagramPacket(sBuf, sBuf.length, serverIP, PORT);

			while( timedOut ){
				try{ // Send the UDP Packet to the server
					socket.send( sDatagram );

					// Receive the server's packet
                    for(int k=0; k<rData.length; k++) rData[k]=0;               //fill with zeros
					DatagramPacket rDatagram = new DatagramPacket(rData, rData.length, serverIP, PORT);
					socket.receive( rDatagram );
					byte[] rPayload = rDatagram.getData();                     // fill buffer for payload

					// serialize the payload
					ByteArrayInputStream bais = new ByteArrayInputStream(rPayload);// stream will be array of bytes
					ObjectInputStream ois = new ObjectInputStream(bais);        // setup stream 
					Packet rPacket = new Packet(0, new String(new byte[BUFFER_SIZE]));
					try {
						rPacket = (Packet) ois.readObject();                   // read and cast to Packet
					} catch (ClassNotFoundException e) {e.printStackTrace(); }  // if not packet (recover?)
					String rBuf = new String(rPacket.getPayload());         // pull out data
					int seq_no = rPacket.getSeq();                             // pull out sequence number
					System.out.println(" Received Packet (seq_n: "  + seq_no + ") Payload: '" + rBuf + "'");
					// If we receive an ack, stop the while loop
					sequenceNumber = (sequenceNumber==1)? 0 : 1;  // if seq==0 set to 1 else set to 0
					timedOut = false;
				} catch( SocketTimeoutException exception ){
					// If we don't get an ack, prepare to resend sequence number
					sPacket.incRetransmits();
					System.out.println( "Timeout (Sequence Number " + sequenceNumber + ")" );
				}
			}	
		  }
	    }	
	//socket.close();
    }
}