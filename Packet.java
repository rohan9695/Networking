// The packet class to build packet for datagram.

import java.io.Serializable;

public class Packet implements Serializable {
    private int seq; 		// the seq number
    private byte[] payload; 	// the datagram
    private int len; 		// the length of data
    public State state; 	// the state of packet  Ready,Sent, etc
    public int reTransmits = 0; // the times of retransmits

     // no default constructor
    private Packet() { }

    public Packet(Packet p) {
        this.len = p.len;
        this.payload = p.payload;
        this.state = p.state;
        this.reTransmits = p.reTransmits;
    }

    
    public Packet(int seq, byte[] payload, State state) {
        this.seq = seq;
        this.payload = payload;
        this.state = state;
        this.len = payload.length;
    }

    public Packet(int seq, String strPayload, State state) {
        this.seq = seq;
        this.payload = strPayload.getBytes();
        this.state = state;
        this.len = this.payload.length;
    }
    
    public Packet(int seq, byte [] Payload) {
        this.seq = seq;
        this.payload = Payload;
        this.state = State.Ready;
        this.len = this.payload.length;
    }

    public Packet(int seq, String strPayload) {
        this.seq = seq;
        this.payload = strPayload.getBytes();
        this.state = State.Ready;
        this.len = this.payload.length;
    }

    public void setSeq(int seq) { this.seq = seq; }

    public void setReady() { this.state = State.Ready; }
    public void setAck(boolean ack) { this.state = State.Acked; }
    public void setAck() { this.state = State.Acked; }
    public void setSent() { this.state = State.Sent; }
    public void setAcknowledgment() { this.state = State.Acknowledgment; }
    public void setReceived() { this.state = State.Received; }
    public void setLost() { this.state = State.Lost; }
    public void setCorrupt() { this.state = State.Corrupt; }
    public void setResent() { this.state = State.Resent; }
    public void setLast() { this.state = State.Last; }

 
    public void setState(State s) { this.state = s; }
    
    public State getState() { return this.state; }

    public void setPayLoad(byte[] payload) { this.payload = payload; }
	
    public int getRetransmits(){ return reTransmits;}
	
    public void incRetransmits() {reTransmits++;}

    public byte[] getPayload() { return payload; }

    public String getData() { return payload.toString(); }

    public int getSeq() { return seq; }

    public boolean isAcked() { return (state == State.Acked); }

    public String toString() {
        return ("Seq: " + seq + " 's state is " + state.toString() + " , it is " + reTransmits + " times to retransmits");
    }

}


// the state class used to represent current state of a packet

enum State {
    Ready(4),Sent(5), Acknowledgment(7), Acked(10), Received(13), Lost(15), Corrupt(20), Resent(25), Last(50) ;
    private int value; 		// the value of enum state

    private State(int value) { this.value = value; }
    private State() { this.value = 4; }

    public String toString() {
        switch (this.value){
            case 4  : return "Ready";
            case 5  : return "Sent";
            case 7  : return "Acknowledgment";
            case 10 : return "Acked";
            case 13 : return "Received";
            case 15 : return "Lost";
            case 20 : return "Corrupt";
            case 25 : return "Resent";
            case 50 : return "Last";
        }
        return "";
    }
}