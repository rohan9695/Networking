/**
 * A NoisyDatagramSock Class
 * Extends DatagramSocket 
 *    Add drops and delays configurable 
 * Uses: 
 *    Externan file anmed net.properties.txt
 * 
 * Last modified:  Malcolm 20171005
 *
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Properties;
import java.util.Random;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NoisyDatagramSocket extends DatagramSocket {
  private static double VER = 0.09;
  private static String PROP = "net.properties.txt";
  private static String logFile = "NoisyDatagramSocket.logfile.txt";
  private static long start_time = System.currentTimeMillis();
  
  private int packet_mtu = 1500;                    // max size of packet
  private int packet_droprate = 30;                 // 0=> 0% packet loss, 25=> 25% packet loss. etc
  private int[] packet_drop = new int[0];           // array of packer numbers to drop.  e.g. every 3rd {3,6,9,12,15,...}
  private int packet_delay_min = 10;                // minimum packer delay milliseconds to wait before send
  private int packet_delay_max = 20;                // maximum packet delay  ''                      ''
  private Random rand = new Random(System.currentTimeMillis());
  private static boolean dump = true;               // verbose output - for verbos set to true

  private static long packet_counter = 1L;          // keep track of packets - enumberate them

  //Formatter for timestamp
  private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
  
  public NoisyDatagramSocket(SocketAddress paramSocketAddress) throws SocketException {
    super(paramSocketAddress);                      // call real constructor
    setuplogfile();
    loadProperties();                               // read config file and set variables (packet_droprate, min, max packet delay, etc)
  }
  
  public NoisyDatagramSocket(int paramInt, InetAddress paramInetAddress) throws SocketException {
    super(paramInt, paramInetAddress);              // call real constructor;
    setuplogfile();
    loadProperties();                               // read config file and set variables (packet_droprate, min, max packet delay, etc)
  }
  
  public NoisyDatagramSocket(DatagramSocketImpl paramDatagramSocketImpl) throws SocketException {
    super(paramDatagramSocketImpl);                  // call real constructor;
    setuplogfile();
    loadProperties();                               // read config file and set variables (packet_droprate, min, max packet delay, etc)
  }
  
  public NoisyDatagramSocket(int paramInt) throws SocketException {
    super(paramInt);                                 // call real constructor;
    setuplogfile();
    loadProperties();                               // read config file and set variables (packet_droprate, min, max packet delay, etc)
  }
  
  public NoisyDatagramSocket() throws SocketException {
    setuplogfile();
    loadProperties();                               // read config file and set variables (packet_droprate, min, max packet delay, etc)
  }
  
  public int getSendBufferSize() throws SocketException {
    return packet_mtu;                              // return max packet size
  }
  
  public void setSendBufferSize(int paramInt) throws SocketException {
    throw new SocketException("MTU is adjusted within the net.properties file");   
  }                                                 // can't set this variable with builtin (super's) method
  
 
  private void rawSend(DatagramPacket paramDatagramPacket) throws IOException {
    super.send(paramDatagramPacket);                // bypass our overwritten send and use builtin (real) send
  }                                                 // no packet drop or delay
  

  public void send(DatagramPacket paramDatagramPacket) throws IOException {
    synchronized (paramDatagramPacket) {                            // thead safe
      if (paramDatagramPacket.getLength() > packet_mtu) {           // if packet size too big throw error
         throw new IOException("Packet length exceeds MTU");
      }
     
      boolean bool=false;                                           // bool is used for drop / no drop choice (default no drop)
      int N = rand.nextInt(100)+1;                                  // rand num for use in drop calc
      
      bool = (packet_droprate > N);                                 // do we drop or not?

      for (int i = 0; i < packet_drop.length; i++) {                // check list for enumerated packets to drop 
        if (packet_counter - 1 == packet_drop[i])                   // is it a packet we drop?
          bool = true;                                              // yes - we are going to drop it
      }
  
      try {   
         Thread.sleep((long) 1 );
      }  catch (Exception e) {;}

      new senderThread(this, paramDatagramPacket, packet_counter - 1L, bool);       // create a thread to send the packet
      packet_counter += 1L;                                         // incr counter
    }
  }
  
   private void setuplogfile(){
      try {
         File file = new File(logFile);
         FileOutputStream fos = new FileOutputStream(file);
         PrintStream ps = new PrintStream(fos);
         System.setErr(ps);
      } catch (FileNotFoundException e) {}
   }

  private void loadProperties()  {                                  // read config file and set variables (drop_rate, max, min delay, etc)
    try {
      Properties localProperties = new Properties();
      localProperties.load(new FileInputStream(PROP));
                                                                    // parse list of packets to drop - if any
      String[] arrayOfString = localProperties.getProperty("packet.droprate", "0").split(",");
      
      if (arrayOfString.length == 1) {                              // if length one the value is percentage
        packet_droprate = Integer.parseInt(arrayOfString[0]);       // convert and set
      }
      else {                                                        // length is not one so we have a list of packets to drop
        packet_drop = new int[arrayOfString.length];                // create a new list for internal use
        for (int i = 0; i < arrayOfString.length; i++) {            // 
          packet_drop[i] = Integer.parseInt(arrayOfString[i]);      // add value to list
        }
      }                                                            // now set variables
      packet_delay_min = Integer.parseInt(localProperties.getProperty("packet.delay.minimum", "0"));
      packet_delay_max = Integer.parseInt(localProperties.getProperty("packet.delay.maximum", "0"));
      packet_mtu = Integer.parseInt(localProperties.getProperty("packet.mtu", "1500"));
      packet_mtu = (packet_mtu <= 0 ? Integer.MAX_VALUE : packet_mtu);

      if(dump) {                                                // if drop by verbose (output settings)
         System.err.println("packet_droprate:"+packet_droprate);
         System.err.println("packet_delay_min:"+packet_delay_min);
         System.err.println("packet_delay_max:"+packet_delay_max);
         System.err.println("packet_mtu:"+packet_mtu);
         System.err.print("Dropping packets: ");
         for (int i = 0; i < packet_drop.length; i++) {
            System.err.print( "drop_packet["+i+"]:"+packet_drop[i] +", " );
            System.err.print( packet_drop[i] +", " );
         }
         System.err.println(" ");
        dump=false;                                             // verbose output but only first time loads config file
      }
      
    } catch (Exception e){ e.printStackTrace(); }
  }
  

  private class senderThread extends Thread {
    NoisyDatagramSocket socket;
    
    DatagramPacket packet;
    long id;
    boolean drop;                                           // to drop or not to drop - that is the question
    
    senderThread(NoisyDatagramSocket paramSocket, DatagramPacket paramDatagramPacket, long paramLong, boolean paramBoolean) {
      socket = paramSocket;                                      // existing socket 
      packet = new DatagramPacket(paramDatagramPacket.getData(),    // make datagram - add payload
                                  paramDatagramPacket.getLength(),  // length of payload
                                  paramDatagramPacket.getAddress(), // to address 
                                  paramDatagramPacket.getPort());   // to port
      id = paramLong;
      drop = paramBoolean;
      start();    				  // ok, do it - do the send
								  // we are writing this for Threads 
								  // do it like we do threads (call start())
    }
    public void run(){
      try {                                                         // if delay set sleep for awhile
         long delay = (long) (packet_delay_min + rand.nextFloat() * (packet_delay_max - packet_delay_min));
         if(delay != 0 ) {
            try {
               String payload = new String(((Packet) Serializer.toObject(packet.getData())).getPayload());
               //System.err.println(" delaying send "+delay + "ms for packet " + ((Packet) Serializer.toObject(packet.getData())) 
               //   + payload);
                System.err.println(simpleDateFormat.format(new Date()) + ": Delaying send "+delay + "ms for packet " 
                    + ((Packet) Serializer.toObject(packet.getData())) + payload);

            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         sleep( delay );
        try {
            if(drop){
               String payload = new String(((Packet) Serializer.toObject(packet.getData())).getPayload());
               //System.err.println(" dropping datagram: " + ((Packet) Serializer.toObject(packet.getData())) + payload );
               System.err.println(simpleDateFormat.format(new Date()) + ": Dropping datagram: " + ((Packet) Serializer.toObject(packet.getData())) + payload );
            }
            if (!drop) socket.rawSend(packet);                        // send if real send method
        } catch (Exception localException) {
          localException.printStackTrace();
        }
      } catch (InterruptedException localInterruptedException) {}
   }
  }
}