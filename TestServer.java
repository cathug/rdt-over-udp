package rdt;



public class TestServer {

	public TestServer() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		 if (args.length != 3)
		 {
	         System.out.println("Required arguments: dst_hostname dst_port local_port");
	         return;
	     }
		 
		 String hostname = args[0];
	     int dst_port = Integer.parseInt(args[1]);
	     int local_port = Integer.parseInt(args[2]);	    
	     RDT rdt;
	     int protocol = RDT.GBN;
	     int sndBufSize = 3;
	     int rcvBufSize = 3;
	     
	     
	     // choose a test case
	     int testCase = 7;
	     	     
	     switch ( testCase )
	     {
	     	case 1:// GBN with small message and 0 loss rate
	     		protocol = RDT.GBN;
	     		RDT.setLossRate( 0.0 );
	     		sndBufSize = rcvBufSize = 1;
	     		break;
	     
	     	case 2:	// GBN with small message and 0 loss rate
	     		protocol = RDT.GBN;
	     		RDT.setLossRate( 0.0 );
	     		sndBufSize = rcvBufSize = 10;
	     		break;
	     		
	     	case 3: // GBN with long message and 0 loss rate
	     		protocol = RDT.GBN;
	     		RDT.setLossRate( 0.0 );
	     		sndBufSize = rcvBufSize = 3;
	     		break;
	     		
		    case 4: // GBN with one-way losses
		    	protocol = RDT.GBN;
		    	RDT.setLossRate( 0.0 );
	     		sndBufSize = rcvBufSize = 10;
		    	break;

		    case 5: // GBN with two-way losses
		    	protocol = RDT.GBN;
		    	RDT.setLossRate( 0.8 );
	     		sndBufSize = rcvBufSize = 1;
		    	break;
		    	
		    case 6: // Selective Repeat with 0 loss rate
		    	protocol = RDT.SR;
		    	RDT.setLossRate( 0.0 );
	     		sndBufSize = rcvBufSize = 10;
		    	break;
		    	
		    case 7: // Selective Repeat with two-way losses
		    	protocol = RDT.SR;
		    	RDT.setLossRate( 0.4 );
		    	sndBufSize = rcvBufSize = 6;
		    	break;
		    
		    case 8: // Selective Repeat + flow control with two-way losses
		    	protocol = RDT.SR_FC;
		    	RDT.setLossRate( 0.4 );
		    	sndBufSize = rcvBufSize = 6;
		    	break;
	     }
	     
	     
	     rdt = new RDT(hostname, dst_port, local_port, sndBufSize, rcvBufSize, protocol);
	     
	     byte[] buf = new byte[500];  	     
	     System.out.println("Server is waiting to receive ... " );
	
	     
	     while (true)
	     {
	    	 int size = rdt.receive(buf, RDT.MSS);
	    	 for (int i=0; i<size; i++)
	    		 System.out.print(buf[i]);
	    	 System.out.println(" ");
	    	 System.out.flush();
	     } 
	}
}
