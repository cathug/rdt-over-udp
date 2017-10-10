package rdt;



public class TestClient 
{

	public TestClient() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		 if (args.length != 3) {
	         System.out.println("Required arguments: dst_hostname dst_port local_port");
	         return;
	      }
		 String hostname = args[0];
	     int dst_port = Integer.parseInt(args[1]);
	     int local_port = Integer.parseInt(args[2]);
	     	      
	     

	     RDT rdt = null;
	     int dataSize;
//	     byte[] buf = new byte[RDT.MSS];
	     byte[] data;
	     
	     // choose a test case
	     int testCase = 7;
	     
	     switch ( testCase )
	     {
	     	case 1:
	     		// send a long 45 byte message
		   	    // sndBufSize = rcvBufSize = 1
		   	    // 0 loss rate
	     		RDT.setLossRate( 0.0 );
		   	    dataSize = 45;
		   	    rdt = new RDT( hostname, dst_port, local_port, 1, 1, RDT.GBN );
		   	    data = new byte[dataSize];
		   	    for ( int i = 0; i < 10; i++ ) { data[i] = 7; }
		   	    for ( int i = 10; i < 20; i++ ) { data[i] = 6; }
		   	 	for ( int i = 20; i < 30; i++ ) { data[i] = 5; }
		   	 	for ( int i = 30; i < 40; i++ ) { data[i] = 4; }
		   	 	for ( int i = 40; i < dataSize; i++ ) { data[i] = 3; }
		   	    rdt.send( data, dataSize );

	     		break;   
	     
	     	case 2:	// GBN with small message and 0 loss rate
	     		// send a small 10 byte message
	     		// sndBufSize = rcvBufSize = 10
	     		// 0 loss rate
	     		dataSize = 10;
	     		RDT.setLossRate( 0.0 );
	     		rdt = new RDT( hostname, dst_port, local_port, 10, 10, RDT.GBN );
	     		data = new byte[dataSize];
	     		for (int i = 0; i < dataSize; i++) { data[i] = 9; } 
	     		rdt.send( data, dataSize );

	     		break;
	
	     	case 3:	// GBN with long message and 0 loss rate
	     		// send a long 45 byte message
		   	    // sndBufSize = rcvBufSize = 3
		   	    // 0 loss rate
	     		RDT.setLossRate( 0.0 );
		   	    dataSize = 45;
		   	    rdt = new RDT( hostname, dst_port, local_port, 3, 3, RDT.GBN );
		   	    data = new byte[dataSize];
		   	    for ( int i = 0; i < 10; i++ ) { data[i] = 7; }
		   	    for ( int i = 10; i < 20; i++ ) { data[i] = 6; }
		   	 	for ( int i = 20; i < 30; i++ ) { data[i] = 5; }
		   	 	for ( int i = 30; i < 40; i++ ) { data[i] = 4; }
		   	 	for ( int i = 40; i < dataSize; i++ ) { data[i] = 3; }
		   	    rdt.send( data, dataSize );

		   	    break;
   
	     	case 4:	// GBN with one-way losses
		     	
		   	    // send 5 small back to back messages
		   	    // 40% loss rate
		   	    dataSize = 10;
		   	    rdt = new RDT( hostname, dst_port, local_port, 10, 10, RDT.GBN );
		   	    data = new byte[dataSize];
		   	    RDT.setLossRate( 0.4 );
		   	    for ( byte j = 9; j >= 5; j-- )
		   	    {
		   	    	for ( int i = 0; i < dataSize; i++ ) { data[i] = j; }
		   	    	rdt.send( data, dataSize );
		   	    }

	     		break;	
	     		
	     	case 5:	// GBN with two-way losses
		     	
		   	    // send 5 small back to back messages
		   	    // 80% loss rate
		   	    dataSize = 10;
		   	    rdt = new RDT( hostname, dst_port, local_port, 1, 1, RDT.GBN );
		   	    data = new byte[dataSize];
		   	    RDT.setLossRate( 0.8 );
		   	    for ( byte j = 9; j >= 8; j-- )
		   	    {
			   	    for ( int i = 0; i < dataSize; i++ ) { data[i] = j; }
			   	    rdt.send( data, dataSize );
		   	    }

	     		break;
	     		
	     	case 6:	// Selective Repeat with 0 loss rate
	     		
	     		// send 0 loss rate
	     		dataSize = 10;
	     		rdt = new RDT( hostname, dst_port, local_port, 10, 10, RDT.SR );
	     		data = new byte[dataSize];
	     		RDT.setLossRate( 0.0 );
	   	     	for ( byte j = 9; j >= 4; j-- )
	   	     	{
	   	    	 	for ( int i = 0; i < dataSize; i++ ) { data[i] = j; }
	   	    	 	rdt.send( data, dataSize );
	   	     	}

	     		break;
	     		
	     	case 7:
	     		
		   	    // case 7 : Selective Repeat with two-way losses
		   	    // 40% loss rate
	     		dataSize = 45;
	     		RDT.setLossRate( 0.4 );
		   	    rdt = new RDT( hostname, dst_port, local_port, 6, 6, RDT.SR );
		   	    data = new byte[dataSize];
		   	    for ( byte j = 9; j >= 0; j-- )
		   	    {
		   	    	for ( int i = 0; i < dataSize; i++ ) { data[i] = j; }
		   	    	rdt.send( data, dataSize );
		   	    }
		   	    
		   	    break;
		   	    
	     	case 8:
	     		
		   	    // case 8 : Selective Repeat + flow control with two-way losses
		   	    // 40% loss rate
	     		dataSize = 45;
	     		RDT.setLossRate( 0.4 );
		   	    rdt = new RDT( hostname, dst_port, local_port, 6, 6, RDT.SR_FC );
		   	    data = new byte[dataSize];
		   	    for ( byte j = 9; j >= 0; j-- )
		   	    {
		   	    	for ( int i = 0; i < dataSize; i++ ) { data[i] = j; }
		   	    	rdt.send( data, dataSize );
		   	    }
		   	    
		   	    break;
	     }
	     

	 
	     
//	     System.out.println(System.currentTimeMillis() + ":Client has sent all data " );
//	     System.out.flush();
	
//	     System.out.println("Client has called rdt.receive");
//	     rdt.receive(buf, RDT.MSS);
	     
//	     rdt.close();
	     System.out.println("Client is done " );
	}

}
