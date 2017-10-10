package rdt;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RDT 
{
	// variables
	
	public static int MSS = 10 + RDTSegment.HDR_SIZE; // Max segment size in bytes
	public static int RTO = 500; // Retransmission Timeout in msec
	public static final int ERROR = -1;
	public static final int MAX_BUF_SIZE = 3;  
	public static final int GBN = 1;   // Go back N protocol
	public static final int SR = 2;    // Selective Repeat
	public static final int SR_FC = 3;   // Selective Repeat with flow control
	public static final int MAX_SEQ_NUM = ( int ) ( Math.pow( 2, 32 ) );	// maximum number of sequence numbers allowed, see see RFC 793
	public static final int SOCKET_TIMEOUT = 360000;	// socket timeout set to 360000msec or 360 sec
	
	public static int protocol = GBN;
	private static int nextUnusedSeqNum = 0;
	private static int ackNumOfLastSegmentACKed = 0;
	
	public static double lossRate = 0.0;
	public static Random random = new Random(); 
	public static Timer timer = new Timer();	
	
	private DatagramSocket socket; 
	private InetAddress dst_ip;
	private int dst_port;
	private int local_port; 
	
	private RDTBuffer sndBuf;
	private RDTBuffer rcvBuf;
	
	private ReceiverThread rcvThread;  
		
	
	
	
	
	// RDT default constructor
	// parameters: 	dst_hostname_ - destination host name
	//				dst_port_ - destination port
	//				protocol_ - either GBN or SR
	RDT (String dst_hostname_, int dst_port_, 
			int local_port_, int protocol_) 
	{
		local_port = local_port_;
		dst_port = dst_port_;
		protocol = protocol_;
		try
		{
			socket = new DatagramSocket( local_port );
			dst_ip = InetAddress.getByName( dst_hostname_ );
		}
		
		catch ( IOException e ) 
		{
			System.out.println( "RDT constructor: " + e );
		}
		
		
		sndBuf = new RDTBuffer( MAX_BUF_SIZE );
		if ( protocol == GBN ) { rcvBuf = new RDTBuffer( 1 ); }
		else { rcvBuf = new RDTBuffer( MAX_BUF_SIZE ); }
		rcvThread = new ReceiverThread( rcvBuf, sndBuf, socket, dst_ip, dst_port );
		rcvThread.start();
	}
	
	
	
	
	
	// RDT constructor with customizable send and receive buffer sizes
	// parameters: 	dst_hostname_ - destination host name
	//				dst_port_ - destination port
	//				sndBufSize - send buffer size
	//				rcvBufSize - receive buffer size
	//				protocol_ - either GBN or SR
	RDT ( String dst_hostname_, int dst_port_, 
			int local_port_, int sndBufSize, 
			int rcvBufSize, int protocol_)
	{
		local_port = local_port_;
		dst_port = dst_port_;
		protocol = protocol_;
		try
		{
			socket = new DatagramSocket( local_port );
			dst_ip = InetAddress.getByName( dst_hostname_ );
		} 
		 
		catch ( IOException e )
		{
			System.out.println( "RDT constructor: " + e );
		}
		 
	
		sndBuf = new RDTBuffer( sndBufSize );
		if ( protocol == GBN ) { rcvBuf = new RDTBuffer( rcvBufSize ); }
		else { rcvBuf = new RDTBuffer( rcvBufSize ); }
		rcvThread = new ReceiverThread( rcvBuf, sndBuf, socket, dst_ip, dst_port );
		rcvThread.start();
	}
	
	
	
	// helper functions - getters and setters
	
	// public static void function: sets the loss rate
	// parameter: rate - a decimal fraction double
	public static void setLossRate( double rate ) { lossRate = rate; }
	
	
	
	// public static void function: updates the next unused sequence number accordingly
	public static void updateNextUnusedSeqNum( ) { nextUnusedSeqNum++; }
	
	
	// public static int function: returns the next unused sequence number
	// sequence number will start again from 0 when MAX_SEQ_NUM is reached
	public static int getNextUnusedSeqNum() { return nextUnusedSeqNum % MAX_SEQ_NUM; }
	
	
	
	// public static void function: updates the last segment in buffer that has been ACKed
	public static void updateAckNumOfLastSegmentACKed() { ackNumOfLastSegmentACKed++; }
		
	
	// public static int function: returns the last segment in buffer that has been ACKed
	public static int getAckNumOfLastSegmentACKed() { return ackNumOfLastSegmentACKed; }

	
	// public static void function: change the maximum segment size
	// parameter - mss - maximum segment size
	public static void setMSS( int mss ) { MSS = mss + RDTSegment.HDR_SIZE; }

	
	// public static void function: change the protocol
	// parameter: protocol_ - either SR or GBN
	public static void setProtocol( int protocol_ ) { protocol = protocol_; }

	
	// public static void function: updates the last segment in buffer that has been ACKed
	// parameter: rto - Retransmission Timeout in msec
	public static void setRTO( int rto ) { RTO = rto; }
	
	
	
	
	
	// public int function: sends application data using UDP
	// parameters: 	appData - application data in bytes
	//				appDataSize - application data size
	// returns: total number of sent bytes  
	public int send( byte[] appData, int appDataSize ) 
	{	
		// base case: no segments to send
		if ( appData.length <= 0 || appData == null ) { return 0; }
		
		
		
		// otherwise, if data can be divided into more than 1 segment
		// do the following
		
		
		// divide data into segments	
		int numSegmentsRequired = ( int ) Math.ceil( ( double ) appDataSize / 
				( MSS - RDTSegment.HDR_SIZE ) );	 // number of segments required to transmit data
		int i = 0;
		int k = 0;
		RDTSegment segment[] = new RDTSegment[numSegmentsRequired];
		
		
		segment[k]= new RDTSegment();
		for ( int j = 0; j < appDataSize; j++ )
		{
			segment[k].data[i] = appData[j];
			segment[k].length++;
			i++;
				
			// if i has reached max segment size - hdr size
			// or if no more data to divide up
			if ( i == MSS - RDTSegment.HDR_SIZE || j == appDataSize - 1 )
			{
//				System.out.println( "index i = " + i );
				segment[k].seqNum = getNextUnusedSeqNum();
				segment[k].rcvWin = sndBuf.size;

//				// I took this out since the flags header is used for closing/FIN
//				if ( numSegmentsRequired == 1 ) // if no data fragmentation
//				{
//					segment[k].flags = 0;	// NO MORE fragments on the way
//				}
//					
//				else if ( numSegmentsRequired > 1 )	// if data fragmentation. i.e. more than one segment
//				{
//					if ( j == appDataSize - 1 )
//					{
//						segment[k].flags = 0;	// NO MORE fragments on the way
//					}
//						
//					else
//					{
//						segment[k].flags = 1;	// MORE fragments on the way
//					}
//				}
					
				segment[k].checksum = segment[k].computeChecksum();
				updateNextUnusedSeqNum();
//				System.out.println( "segment length is " + segment.length );
//				System.out.println( "The next unused sequence number is " + getNextUnusedSeqNum() );
//				System.out.println( "Segment contains");
//				segment[k].dump();
				k++;
				
				if ( k < numSegmentsRequired )	// condition to prevent out of bounds exception
				{
					segment[k]= new RDTSegment();
				}
				
				// move on to the next segment and repeat
				i = 0;
			}
		}
		
		
		
				
		// while application still sending segments
		RDTSegment currentSegmentInBufferToSend;
		TimeoutHandler retransmit;
		@SuppressWarnings("unused")
		long startTime, elapsedTime;
		
		
		i = 0;
		while ( i < numSegmentsRequired )
		{
			// put each segment into sndBuf
			sndBuf.putNext( segment[i] );
//			System.out.println( "\n\n----------------------------------------------------------------------------" );
//			System.out.println( "\n+++segment " + segment[i] + " placed in buffer. Dumping segment contents" );
//			segment[i].dump();	
				
				
			// send using udp_send()
			// get the next segment stored in buffer and send it if it is not null
			currentSegmentInBufferToSend = sndBuf.getNext();
			if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == segment[i] )
			{
//				System.out.println( "\n---Segment " + currentSegmentInBufferToSend + " fetched from buffer." );
//				System.out.println( "Dumping segment contents" );
//				currentSegmentInBufferToSend.dump();
				Utility.udp_send( currentSegmentInBufferToSend, socket, dst_ip, dst_port );	
				
				
				// schedule segment(s) for timeout
				// for both GBN and SR cases
				// schedule to retransmit if timeout interval RTO milliseconds has been exceeded	
				// repeating operation every RTO milliseconds if ACK not received
				retransmit = new TimeoutHandler(sndBuf, currentSegmentInBufferToSend, socket, dst_ip, dst_port, protocol);
				timer.schedule( retransmit, (long) RTO, (long) RTO );
				startTime = System.currentTimeMillis();	// start the timer	
				
				
				// if ACK received before timeout, while loop breaks
				// retransmission will be cancelled
				while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RTO ) 
				{
					if ( currentSegmentInBufferToSend.ackReceived )
					{
						retransmit.cancel();
//						System.out.println("\n++++++++++++++ stop retransmit schedule for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
//						System.out.println("++++++++++++++ ACK has been received for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
//						currentSegmentInBufferToSend.dump();
//						System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
						
						// if no timeout occurs and flow control option selected
						// doing an additive increase
						if ( protocol == SR_FC )
						{
							int newWindowSize = sndBuf.size + 1;	
							RDTSegment[] tempBuf = new RDTSegment[newWindowSize];
							
							// copy everything in the new buffer and 
							// replace the buffer
							System.arraycopy(sndBuf.buf, 0, tempBuf, 0, sndBuf.buf.length);
							sndBuf.buf = tempBuf;	
						}
						
						break;
					}	
				}
						
				// retransmit timeoutHandler will resend segment automatically once segment times out
			}

				
			i++;	// increment and repeat process for remaining segments
		}
		
		return appDataSize;
	}
	
	
	
	
	
	// called by app
	// receive one segment at a time
	// returns number of bytes copied in buf
	public int receive ( byte[] appBuffer, int appBuffSize )
	{
		RDTSegment segReceived;
		int numBytesReceived = 0;
		
		
		// if there are segments in the receive buffer
		while ( ( segReceived = rcvBuf.getNext() ) != null )
		{
			try
			{
				// copy the contents into the application layer buffer
				// and update numBytesReceived
				System.arraycopy( segReceived.data, 0, appBuffer, 0, segReceived.data.length);	
				numBytesReceived += segReceived.data.length;
			}
				
			catch (IndexOutOfBoundsException e)
			{
				System.out.println("Application buffer overflow at index " + e);
			}
		}
		
		return numBytesReceived;
	}
	
	
	
	
	// called by app
	public void close()
	{
		// TCP-style connection termination process
		
//		if application has no more data to send
//			while there is still data to transmit
//				send data
//				wait for receiver to ACK data
//			send the FIN bit
//			if ACK received from receiver
//				wait for FIN + ACK from receiver
//				send ACK to receiver
//				delete record of connection
//		else
//			do nothing
		
		
		if ( socket != null && !socket.isClosed() )
		{
			RDTSegment finSeg = new RDTSegment();
			RDTSegment currentSegmentInBufferToSend;
			@SuppressWarnings("unused")
			long startTime, elapsedTime;
			TimeoutHandler retransmit;
			
			finSeg.data = null;
			finSeg.flags = getNextUnusedSeqNum();
			finSeg.seqNum = finSeg.flags;
//			finSeg.ackNum = 0;
//			finSeg.length = 0;
//			finSeg.rcvWin = rcvBuf.size;
//			finSeg.ackReceived = false;
			finSeg.checksum = finSeg.computeChecksum();
			sndBuf.putNext(finSeg);
			currentSegmentInBufferToSend = sndBuf.getNext();
			
			// send FIN bit
			if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == finSeg )
			{
				Utility.udp_send(currentSegmentInBufferToSend, socket, dst_ip, dst_port);	// send using udp_send() (consumer)
//				currentSegmentInBufferToSend.dump();
						
				// schedule to retransmit if timeout interval RTO milliseconds has been exceeded	
				// repeating operation every RTO milliseconds if ACK not received
				retransmit = new TimeoutHandler(sndBuf, currentSegmentInBufferToSend, socket, dst_ip, dst_port, protocol);
				timer.schedule( retransmit, (long) RTO, (long) RTO );
				startTime = System.currentTimeMillis();	// start the timer
					
				// if ACK received before timeout, while loop breaks
				// retransmission will be cancelled
				while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RTO ) 
				{
					if ( currentSegmentInBufferToSend.ackReceived )
					{
						retransmit.cancel();
//						System.out.println("\n++++++++++++++ stop retransmit schedule for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
//						System.out.println("++++++++++++++ ACK has been received for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
//						System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
						break;
					}	
				}
						
				// retransmit timeoutHandler will resend segment automatically once segment times out
				
				
				
				if ( rcvThread.terminateThread )
				{
					try
					{
						socket.setSoTimeout( SOCKET_TIMEOUT + RDT.RTO );	// set socket timeout
//						Thread.sleep( SOCKET_TIMEOUT );
					}
					
					catch (SocketException e)
					{
//						System.out.println(" UDP error ");
					} 
					
					
					socket.close();	// close the connection
					System.out.println( "Socket is closed? " + socket.isClosed() );			
				}
			}
		}
	}
	
}  // end RDT class 





class RDTBuffer 
{
	public RDTSegment[] buf;
	public int size;	
	public int base;
	public int next;
	public Semaphore semMutex; // for mutual exclusion
	public Semaphore semFull; // #of full slots
	public Semaphore semEmpty; // #of Empty slots
	
	RDTBuffer (int bufSize) 
	{
		buf = new RDTSegment[bufSize];
		for ( int i = 0; i < bufSize; i++ )	{ buf[i] = null; }
		size = bufSize;
		base = next = 0;
		semMutex = new Semaphore(1, true);
		semFull =  new Semaphore(0, true);
		semEmpty = new Semaphore(bufSize, true);
	}

	
	
	// Put a segment in the next available slot in the buffer
	public void putNext( RDTSegment seg ) 
	{	
		try 
		{
			semEmpty.acquire(); // wait for an empty slot 
			semMutex.acquire(); // wait for mutex
				buf[next % size] = seg;
				next++;
			semMutex.release();
			semFull.release(); // increase #of full slots
		} 
		
		catch(InterruptedException e)
		{
			System.out.println("Buffer put(): " + e);
		}
	}
	
	
	
	// return the next in-order segment
	public RDTSegment getNext() 
	{
		boolean updateBase = false;	// updateBuf flag

		RDTSegment nextSegment = null;
		try {
			semEmpty.release(); // release empty slot 
			semMutex.acquire(); // wait for mutex
			
//			// if base case, buffer empty, set updateBase flag to false	
//			if ( ( next = base ) == 0 ){ updateBase = false; }
			
			nextSegment = buf[base % size];
			
			if ( next > base )
			{
				updateBase = true;
			}
			
			if ( updateBase == true ) { base++; }
			
			semMutex.release();
			semFull.acquire(); // decrease #of full slots
		} catch(InterruptedException e) {
			System.out.println("Buffer put(): " + e);
		}
		
		return nextSegment; 
	}
	
	
	
	// Put a segment in the *right* slot based on seg.seqNum
	// used by receiver in Selective Repeat
	public void putSeqNum( RDTSegment seg )
	{
		try 
		{
			semEmpty.acquire(); // wait for an empty slot 
			semMutex.acquire(); // wait for mutex
				buf[seg.seqNum % size] = seg;
				next++;
			semMutex.release();
			semFull.release(); // increase #of full slots
		} 
		
		catch(InterruptedException e)
		{
			System.out.println("Buffer put(): " + e);
		}
	}
	

	
	
	
	// for debugging, prints contents in buffer
	public void dump() 
	{
		System.out.println("Dumping the buffer ...");
		for (int i = 0; i < size; i++)
		{ 
			System.out.print(buf[i] + " "); 
		}
	}
} // end RDTBuffer class





class ReceiverThread extends Thread 
{
	DatagramSocket socket;
	DatagramPacket packet;
	InetAddress dst_ip;
	int dst_port;
	RDTBuffer rcvBuf, sndBuf;
	RDTSegment retrievedSegment;
	int tempByteBufferSize = RDT.MSS;
	byte[] tempByteBuffer = new byte[tempByteBufferSize];
	boolean terminateThread = false;
	RDTSegment previousSuccessSegACK = null;
	int retrievedSegmentChecksum;
	
	// receiver thread constructor
	ReceiverThread (RDTBuffer rcv_buf, RDTBuffer snd_buf, DatagramSocket s, 
			InetAddress dst_ip_, int dst_port_) 
	{
		rcvBuf = rcv_buf;
		sndBuf = snd_buf;
		socket = s;
		dst_ip = dst_ip_;
		dst_port = dst_port_;
	}	
	
	
	
	public void run()
	{		
		while ( !terminateThread )
		{
			// clear the tempByteBuffer
			// create a packet with it
			// receive incoming packet from the socket
			//Arrays.fill( tempByteBuffer, 0x00 );	// why is this not working?
			for ( int i = 0; i < tempByteBufferSize; i++ ) { tempByteBuffer[i] = ( byte ) 0; }
			packet = new DatagramPacket(tempByteBuffer, tempByteBufferSize);
			try
			{
				socket.receive( packet );
			}
			
			catch ( IOException e )
			{
				System.out.println( "Socket " + e + ": failed to receive packet" );
				
			}
			

			
			
			// make a segment from the packet payload bytes
			// and verify checksum
			retrievedSegment = new RDTSegment();
			retrievedSegmentChecksum = retrievedSegment.checksum;
			retrievedSegment.checksum = 0;
			makeSegment( retrievedSegment, packet.getData() );
//			System.out.println( "\n~~~Segment " + retrievedSegment + " has been received. Dumping segment contents" );
//			retrievedSegment.dump();
			
			
			
			
			if ( retrievedSegment.isValid( retrievedSegmentChecksum ) )	// if checksum is correct
			{				
				if ( retrievedSegment.containsAck() )
				{
					// for both GBN and SR
					// ACK the packet accordingly
					int correspondingSeqNum = retrievedSegment.ackNum - 1;
					
					// if correspondingSeqNum <= 0, drop the packet since valid ACKs start at 1 (condition to deal with base case)
						
					// if the correspondingSeqNum >= 0 (implying ACK in retrieved segment is valid since seqNum = ackNum - 1 >= 0 )
					// and the sequence number in the buffer pairs with the retrieved segment ACK
					if ( correspondingSeqNum >= 0 && 
							sndBuf.buf[correspondingSeqNum % sndBuf.size].seqNum == correspondingSeqNum )
					{
						sndBuf.buf[correspondingSeqNum % sndBuf.size].ackReceived = true;
					}			
				}
				
				else if ( retrievedSegment.containsData() )
				{
					// put the retrievedSegment in the buffer accordingly
					if ( RDT.protocol == RDT.GBN )
					{								
//						System.out.println( "RDT.getAckNumOfLastSegmentACKed() is: " +  RDT.getAckNumOfLastSegmentACKed() );
						
						// if base case ( nothing stored ), or if the highest sequence number in buffer
						// pairs with the incoming ACK, this implies packet arrived in order. Archive it
						// otherwise drop it ( do nothing )
						int retrievedSeqNum = retrievedSegment.seqNum;
						int seqNumOfLastSegmentACKed = RDT.getAckNumOfLastSegmentACKed() - 1;

						
						
						// if when rcvBuf is empty 
						// i.e. retrievedSeqNum = 0 making seqNumOfLastSegmentACKed < 0 or 
						// if the sequence number follows the last one that got ACKed
						// i.e. ( seqNumOfLastSegmentACKed = retrievedSeqNum - 1)
						if ( seqNumOfLastSegmentACKed == retrievedSeqNum - 1 )
						{
							rcvBuf.putNext( retrievedSegment );
							System.out.println( "\n----------------------------------------------------------------" );
							System.out.println( "\nnew segment retrieved \n\n\n" );
							
							
							// send an ACK back to the sender so it can send send the next segment
							RDTSegment segACK = new RDTSegment();
							segACK.data = null;
							segACK.seqNum = RDT.getNextUnusedSeqNum();
							segACK.ackNum = retrievedSegment.seqNum + 1;
							segACK.rcvWin = rcvBuf.size;
							segACK.ackReceived = false;
							segACK.checksum = segACK.computeChecksum();
							
							
//							System.out.println( "\n$$$ACK segment " + segACK + " has been created." );
//							System.out.println( "Dumping segment contents" );
//							segACK.dump();
							
							// put segment in sndBuf
							sndBuf.putNext( segACK );
							
							// get the next segment stored in buffer and send it
							RDTSegment currentSegmentInBufferToSend = sndBuf.getNext();
							if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == segACK )
							{
								Utility.udp_send(currentSegmentInBufferToSend, socket, dst_ip, dst_port);
//								System.out.println( "\n***ACK segment " +  currentSegmentInBufferToSend + " has been sent." );
//								System.out.println( "---------------------------------------------------------------------\n\n\n" );
								
								previousSuccessSegACK = currentSegmentInBufferToSend;
								RDT.updateAckNumOfLastSegmentACKed();
								RDT.updateNextUnusedSeqNum();
								
							}
						}
						
						
						// otherwise the same packet is received ( seqNumOfLastSegmentACKed == retrievedSeqNum )
						// or if packet is not received in order
						// segment dropped, send old ACK to notify sender to resend correct segment
						else
						{
							if ( previousSuccessSegACK != null )
							{
								Utility.udp_send(previousSuccessSegACK, socket, dst_ip, dst_port);
//								System.out.println( "\n***ACK segment " +  previousSuccessSegACK + " has been sent." );
//								System.out.println( "---------------------------------------------------------------------\n\n\n" );
							}
						}				
					}
					
					
					else // if ( RDT.protocol == RDT.SR_FC or RDT.SR)
					{												
						// Archive incoming packet if within receiving window range
						// otherwise drop it
						int retrievedSeqNum = retrievedSegment.seqNum;
						if ( retrievedSeqNum <= 0 ) { retrievedSeqNum = 0; }	// condition to deal with base case
						
						
						// if empty rcvBuf or if flag = 0, implying it can be overwritten
						if ( retrievedSeqNum == 0 || rcvBuf.buf[retrievedSeqNum  % rcvBuf.size] == null )
						{
							// implies traffic is good occurs and flow control option selected
							// do an additive increase here
							if ( RDT.protocol == RDT.SR_FC )
							{
								int newWindowSize = rcvBuf.size + 1;	
								RDTSegment[] tempBuf = new RDTSegment[newWindowSize];
								
								// copy everything in the new buffer and 
								// replace the buffer
								System.arraycopy(rcvBuf.buf, 0, tempBuf, 0, rcvBuf.buf.length);
								rcvBuf.buf = tempBuf;
							}
							
							rcvBuf.putSeqNum( retrievedSegment );
							System.out.println( "\n----------------------------------------------------------------" );
							System.out.println( "\nnew segment retrieved \n\n\n" );
	
						
							// send an ACK back to the sender so it can send send the next segment
							RDTSegment segACK = new RDTSegment();
							segACK.data = null;
							segACK.seqNum = RDT.getNextUnusedSeqNum();
							segACK.ackNum = retrievedSeqNum + 1;
							segACK.rcvWin = rcvBuf.size;
							segACK.ackReceived = false;
							segACK.checksum = segACK.computeChecksum();
							
							System.out.println( "\n$$$ACK segment " + segACK + " has been created." );
							System.out.println( "Dumping segment contents" );
							segACK.dump();
							
							// put segment in sndBuf
							sndBuf.putNext( segACK );
							
							// get the next segment stored in buffer and send it
							RDTSegment currentSegmentInBufferToSend = sndBuf.getNext();
							
							if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == segACK )
							{
								Utility.udp_send(currentSegmentInBufferToSend, socket, dst_ip, dst_port);
//								System.out.println( "\n***ACK segment " +  currentSegmentInBufferToSend + " has been sent." );
//								System.out.println( "---------------------------------------------------------------------\n\n\n" );
								
								previousSuccessSegACK = currentSegmentInBufferToSend;
							}
						}
						
						// otherwise,
						// segment dropped, send old ACK to notify sender to resend correct segment
						else	// if updatepreviousSuccessSegACKFlag = false
						{
							if ( previousSuccessSegACK != null )
							{
								Utility.udp_send(previousSuccessSegACK, socket, dst_ip, dst_port);
//								System.out.println( "\n***ACK segment " +  previousSuccessSegACK + " has been sent." );
//								System.out.println( "---------------------------------------------------------------------\n\n\n" );
							}
						}
					}
				}
				
				
				// if FIN exists, implying close is called
				else if ( !retrievedSegment.containsData() && 
						!retrievedSegment.containsAck() &&
						retrievedSegment.flags == retrievedSegment.seqNum )
				{
					RDTSegment segACK_FIN = new RDTSegment();
					segACK_FIN.data = null;
					segACK_FIN.seqNum = RDT.getNextUnusedSeqNum();
					segACK_FIN.ackNum = retrievedSegment.flags + 1;
					segACK_FIN.rcvWin = rcvBuf.size;
					segACK_FIN.ackReceived = false;
					segACK_FIN.checksum = segACK_FIN.computeChecksum();
					RDT.updateNextUnusedSeqNum();
					
					System.out.println( "\n$$$ACK segment " + segACK_FIN + " has been created." );
					System.out.println( "Dumping segment contents" );
					segACK_FIN.dump();
					
					// put segment in sndBuf
					sndBuf.putNext( segACK_FIN );
					
					// get the next segment stored in buffer and send it
					RDTSegment currentSegmentInBufferToSend = sndBuf.getNext();
					if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == segACK_FIN )
					{
						Utility.udp_send(currentSegmentInBufferToSend, socket, dst_ip, dst_port);
//						System.out.println( "\n***ACK segment (FIN)" +  currentSegmentInBufferToSend + " has been sent." );
//						System.out.println( "---------------------------------------------------------------------\n\n\n" );
						
						terminateThread = true;
					}
				}
			}
			
			
			else	// if checksum is incorrect, do nothing ( segment dropped )
			{
				System.out.println( "Checksum is incorrect.  Segment Dropped");
			}
		}
		
		
		if ( terminateThread == true )
		{
			if ( socket != null && !socket.isClosed() )
			{
				RDTSegment finSeg = new RDTSegment();
				RDTSegment currentSegmentInBufferToSend;
				@SuppressWarnings("unused")
				long startTime, elapsedTime;
//				TimeoutHandler retransmit;
				
				finSeg.data = null;
				finSeg.flags = RDT.getNextUnusedSeqNum();
				finSeg.seqNum = finSeg.flags;
//				finSeg.ackNum = 0;
//				finSeg.length = 0;
//				finSeg.rcvWin = rcvBuf.size;
//				finSeg.ackReceived = false;
				finSeg.checksum = finSeg.computeChecksum();
				sndBuf.putNext( finSeg );
				currentSegmentInBufferToSend = sndBuf.getNext();
				
				// send FIN bit
				if ( currentSegmentInBufferToSend != null && currentSegmentInBufferToSend == finSeg)
				{
					Utility.udp_send(currentSegmentInBufferToSend, socket, dst_ip, dst_port);	// send using udp_send() (consumer)
					
//					// schedule to retransmit if timeout interval RTO milliseconds has been exceeded	
//					// repeating operation every RTO milliseconds if ACK not received
//					retransmit = new TimeoutHandler(sndBuf, currentSegmentInBufferToSend, socket, dst_ip, dst_port, RDT.protocol);
//					RDT.timer.schedule( retransmit, (long) RDT.RTO, (long) RDT.RTO );
//					startTime = System.currentTimeMillis();	// start the timer
//						
//					// if ACK received before timeout, while loop breaks
//					// retransmission will be cancelled
//					while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RDT.RTO ) 
//					{
//						if ( currentSegmentInBufferToSend.ackReceived )
//						{
//							retransmit.cancel();
////							System.out.println("\n++++++++++++++ stop retransmit schedule for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
////							System.out.println("++++++++++++++ ACK has been received for segment " + currentSegmentInBufferToSend + " BEFORE Timeout: " );
////							System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
//							break;
//						}	
//					}
//							
//					// retransmit timeoutHandler will resend segment automatically once segment times out
					
					try
					{
						socket.setSoTimeout( RDT.SOCKET_TIMEOUT + RDT.RTO );	// set socket timeout
//						Thread.sleep( SOCKET_TIMEOUT );
					}
						
					catch (SocketException e)
					{
//						System.out.println(" UDP error ");
					} 
						
						
					socket.close();	// close the connection
					System.out.println( "Socket is closed? " + socket.isClosed() );	
				}
			}
		}
		
	}
	
	
//	 create a segment from received bytes 
	void makeSegment(RDTSegment seg, byte[] payload)
	{
		seg.seqNum = Utility.byteToInt(payload, RDTSegment.SEQ_NUM_OFFSET);
		seg.ackNum = Utility.byteToInt(payload, RDTSegment.ACK_NUM_OFFSET);
		seg.flags  = Utility.byteToInt(payload, RDTSegment.FLAGS_OFFSET);
		seg.checksum = Utility.byteToInt(payload, RDTSegment.CHECKSUM_OFFSET);
		seg.rcvWin = Utility.byteToInt(payload, RDTSegment.RCV_WIN_OFFSET);
		seg.length = Utility.byteToInt(payload, RDTSegment.LENGTH_OFFSET);
		//Note: Unlike C/C++, Java does not support explicit use of pointers! 
		// we have to make another copy of the data
		// This is not efficient in protocol implementation
		for ( int i=0; i< seg.length; i++ )
		{
			seg.data[i] = payload[i + RDTSegment.HDR_SIZE]; 
		}	
	}
	
} // end ReceiverThread class

