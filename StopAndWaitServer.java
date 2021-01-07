/**
 * A UDP Server
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
import java.util.*;

class StopAndWaitServer{
	private static final int BUFFER_SIZE = 2048;
	private static final int PORT = 6789;
    private static final int SEQUENCE_NUMBER = 0;

	public static void main(String[] args) throws IOException {
        Integer expected_seq_no = SEQUENCE_NUMBER;
	    
		// Create a server socket  // DatagramSocket serverSocket = new DatagramSocket( PORT );
	    NoisyDatagramSocket socket = new NoisyDatagramSocket( PORT );
		
		Packet rPacket = new Packet(0, new String(new byte[BUFFER_SIZE]));

    	// Set up byte arrays for sending/receiving data
        byte[] rBuf = new byte[ BUFFER_SIZE ];
           
        // Infinite loop to check for connections 
        while(true){
			// Get the received packet
			DatagramPacket rDatagram = new DatagramPacket( rBuf, rBuf.length );
			socket.receive( rDatagram );

            byte[] rData = rDatagram.getData();                      	// get payload from UPD datagram
            ByteArrayInputStream bais = new ByteArrayInputStream(rData);// datagram payload is serialized
            ObjectInputStream ois = new ObjectInputStream(bais);        // setup stream
            try {
                rPacket = (Packet) ois.readObject();                    // cast read object to Packet
            } catch (ClassNotFoundException e) {e.printStackTrace(); }
    
            // Get packet's IP and port
            InetAddress IPAddress = rDatagram.getAddress();
            int port = rDatagram.getPort();

          	// Get the message from the packet
            String payload = new String(rPacket.getPayload());         // get sent data
            int seq_no = rPacket.getSeq();                             // get sequence number
            System.out.println("FROM CLIENT: '" + payload + "' sequence number: " + seq_no );
            Packet sPacket;
            if(seq_no == expected_seq_no) {
				// receive expected datagram so incr seq_no
                System.out.println("    Received expected sequence_number - sending acknowledgement\n"); 
                expected_seq_no = (expected_seq_no==1)? 0 : 1;  // if seq==0 set to 1 else set to 0
            } else {      
				// We did NOT get correct packet (sequence number was wrong)
				// do NOT incr seq_no but set payload to dummy value
                System.out.println("DUPLICATE - expecting sequence number: "+expected_seq_no
                           +" RECEIVED seq: "+seq_no); 
                payload = "DUPLICATE";				// reset payload	
            }
            // Make Packet for ACK - send same data back
            sPacket = new Packet(seq_no, payload, State.Acked);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();   // setup stream - will be bytes
            ObjectOutputStream oos = new ObjectOutputStream(baos);     	// setup to serialize Packet
            oos.writeObject(sPacket);                               	// write Packet to Object stream
            byte[] sData = baos.toByteArray();                         	// data for UPD datagram (to send)

            // Make Datagram  - Send data back to the client
            DatagramPacket sDatagram = new DatagramPacket( sData, sData.length, IPAddress, port );
            socket.send( sDatagram );

       	}
	}
}