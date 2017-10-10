package rdt;


import java.net.*;


public class Utility {
	
	public static final int MAX_NETWORK_DELAY = 200; // msec
	
	public static void udp_send (RDTSegment seg, DatagramSocket socket, 
			InetAddress ip, int port) {
			
		double d = RDT.random.nextDouble();
		if ( d < RDT.lossRate) { // simulate network loss
			System.out.println(System.currentTimeMillis()+":udp_send: Lost Segment: seqNum=" + 
					       seg.seqNum + "  ackNum=" + seg.ackNum + " ***");
			System.out.flush();
	        return;
	    }
		// prepare UDP payload 
		int payloadSize = seg.length + RDTSegment.HDR_SIZE;
		byte[] payload = new byte[payloadSize];
		seg.makePayload(payload);
	
//		// corrupt some bits
//		for ( int i = 0; i < payloadSize; i++ )
//		{
//			payload[i] = ( byte ) RDT.random.nextInt(2);	// generate random 0s or 1s
//		}
		
		// send over udp
		// simulate random network delay
		int delay = RDT.random.nextInt(MAX_NETWORK_DELAY);
		try {
			Thread.sleep(delay);
			socket.send(new DatagramPacket(payload, payloadSize, ip, port));
		} catch (Exception e) {
			System.out.println("udp_send: " + e);
		}
		
		System.out.println(System.currentTimeMillis()+":udp_send: sent Segment: seqNum=" 
					+ seg.seqNum + "  ackNum=" + seg.ackNum
					+ "   After delay= " + delay) ;
		System.out.flush();
		seg.dump();
	}
	
	/* NOTE: the following methods do NOT handle conversion from 
	network-byte order to host-byte order. The assumption is that 
	they code will run on the same architecture (Intel) */		
	public static void intToByte(int intValue, byte[] data, int idx) 
	{
		data[idx++] = (byte) ((intValue & 0xFF000000) >> 24);
		data[idx++] = (byte) ((intValue & 0x00FF0000) >> 16);
		data[idx++] = (byte) ((intValue & 0x0000FF00) >> 8);
		data[idx]   = (byte) (intValue & 0x000000FF);	
	}
	
	public static void shortToByte(short shortValue, byte[] data, int idx) 
	{
		data[idx++] = (byte) ((shortValue & 0xFF00) >> 8);
		data[idx]   = (byte) (shortValue & 0x00FF);	
	}
	
	// Caution: byte type in java has a sign (8th bit)
	// no unsigned type in java!!
	public static int byteToInt(byte[] data, int idx)
	{
		int intValue = 0, intTmp = 0;
		
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue = intTmp; 
		intValue <<= 8;
				
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue |= intTmp;
		intValue <<= 8 ; 	
			
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		idx++;
		intValue |= intTmp;
		intValue <<= 8;
			
		if ( ((int) data[idx]) < 0 ) { //leftmost bit (8th bit) is 1
			intTmp = 0x0000007F & ( (int) data[idx]);
			intTmp += 128;  // add the value of the masked bit: 2^7
		} else
			intTmp = 0x000000FF & ((int) data[idx]);
		intValue |= intTmp;
		//System.out.println(" byteToInt: " + intValue + "  " + intTmp);
		return intValue;
	}		
}
