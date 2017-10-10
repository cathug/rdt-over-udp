package rdt;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

class TimeoutHandler extends TimerTask {
	RDTBuffer sndBuf;
	RDTSegment seg; 
	DatagramSocket socket;
	InetAddress ip;
	int port;
	int protocol;
	long startTime, elapsedTime;
	int newWindowSize;
	RDTSegment[] tempBuf;
	
	TimeoutHandler (RDTBuffer sndBuf_, RDTSegment s, DatagramSocket sock, 
			InetAddress ip_addr, int p, int protocol_) {
		sndBuf = sndBuf_;
		seg = s;
		socket = sock;
		ip = ip_addr;
		port = p;
		protocol = protocol_;
	}
	
	public void run()
	{
		if ( seg.ackReceived )	// if the ACK has been received at the RTO-th millisecond
		{
			this.cancel();
//			System.out.println("\n++++++++++++++ stop retransmit schedule for segment " + seg + " BEFORE Timeout: " );
//			System.out.println("++++++++++++++ ACK has been received for segment " + seg + " BEFORE Timeout: " );
//			seg.dump();
//			System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
		}
		
		else // if ACK not received
		{
			System.out.println( "\n\n\nExecuting TimeoutHandler" );
			System.out.println( System.currentTimeMillis() + ":Timeout for seg: " + seg.seqNum );
			System.out.flush();
			
			switch(protocol)
			{
				case RDT.GBN:
					Utility.udp_send( seg, socket, ip, port );	// resend packet
//					System.out.println("\n++++++++++++++ Resending segment: " + seg + "\n\n");
//					seg.dump();
						
					startTime = System.currentTimeMillis();	// start the timer
						
					// cancel the retransmission if ack is received within timeout interval
					while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RDT.RTO )
					{
						if ( seg.ackReceived )
						{
							this.cancel();	
//							System.out.println("\n++++++++++++++ stop retransmit schedule for segment INSIDE TimeoutHandler: " + seg );
//							System.out.println("++++++++++++++ ACK received for segment INSIDE TimeoutHandler: " + seg);
//							seg.dump();
//							System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
							break;
						}
					}
					
					break;
					
				case RDT.SR:
					Utility.udp_send( seg, socket, ip, port );	// resend packet
//					System.out.println("\n++++++++++++++ Resending segment: " + seg + "\n\n");
//					seg.dump();
						
					startTime = System.currentTimeMillis();	// start the timer
						
					// cancel the retransmission if ack is received within timeout interval
					while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RDT.RTO )
					{
						if ( seg.ackReceived )
						{
							this.cancel();	
//							System.out.println("\n++++++++++++++ stop retransmit schedule for segment INSIDE TimeoutHandler: " + seg );
//							System.out.println("++++++++++++++ ACK received for segment INSIDE TimeoutHandler: " + seg);
//							seg.dump();
//							System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
							break;
						}
					}
					
					break;

				
					
				// updating the window size if flow control options are selected
				// we use multiplicative decrease when timeout occurs
				case RDT.SR_FC:
					newWindowSize = sndBuf.size / 2;
					if ( newWindowSize < 1 ) { newWindowSize = 1; }	// make sure window size can no smaller than 1
					tempBuf = new RDTSegment[newWindowSize];
					sndBuf.buf = tempBuf;	// drop everything
					
					Utility.udp_send( seg, socket, ip, port );	// resend packet
//					System.out.println("\n++++++++++++++ Resending segment: " + seg + "\n\n");
//					seg.dump();
						
					startTime = System.currentTimeMillis();	// start the timer
						
					// cancel the retransmission if ack is received within timeout interval
					while ( ( elapsedTime = System.currentTimeMillis() - startTime ) < RDT.RTO )
					{
						if ( seg.ackReceived )
						{
							this.cancel();	
//							System.out.println("\n++++++++++++++ stop retransmit schedule for segment INSIDE TimeoutHandler: " + seg );
//							System.out.println("++++++++++++++ ACK received for segment INSIDE TimeoutHandler: " + seg);
//							seg.dump();
//							System.out.println("--------------------------------------------------------------------------------------\n\n\n" );
							break;
						}
					}
					break;
					
				default:
					System.out.println("Error in TimeoutHandler:run(): unknown protocol");
			}
		}	
	}
} // end TimeoutHandler class

