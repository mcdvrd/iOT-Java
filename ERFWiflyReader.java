package erf.wifly;

import erf.wifly.ERFSharedDataModel;
// access for writing through writer-singleton
import erf.wifly.ERFSingleDataWriter;

import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.Date;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


public class ERFWiflyReader extends ERFCommandBase
implements Runnable, PropertyChangeListener
{

	private ServerSocket providerSocket;
	private Socket connection = null;
	private static int packetNum = 0;
	
	// used to stop the reading
	private boolean bContinue;
	
	
	private ERFSharedDataModel sharedData;
	private static byte[] headerSequence = { (byte)0x67, (byte)0x55, (byte)0xAA };
	
	 ERFThreadPool myThreadPool = null;
	
	ERFWiflyReader() 
	{
		packetNum = 0;
		
		this.myThreadPool = new ERFThreadPool();
		
	}
	
	public void StopReader()
	{
		this.bContinue = false;
	}
	
	public void addSharedData(ERFSharedDataModel dataModel)
	{
		this.sharedData = dataModel;
	}
	

	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		
		System.out.println("ERFWiflyReader: Changed property: " + event.getPropertyName() + " [old -> "
                + event.getOldValue() + "] | [new -> " + event.getNewValue() +"]");
		
		String d = event.getPropertyName();
		if(d == READY_TO_WRITE)
		{
			this.sharedData.setReadyToRead(true);
			bContinue = true;
		}
		else
		if(d == START_RECORDING)
		{
			// set status as ready to write
			this.sharedData.setReadyToRead(true);
			bContinue = true;
			
			run();
			
		}
		else
		if(d == STOP_RECORDING)
		{
			this.sharedData.setReadyToRead(false);
			this.bContinue = false;
		}
		
	}
	
   

    // send bytes to get around a problem with the firmware
    public void sendKlugeBytes()
    {
    	try {
    	
            OutputStream out = connection.getOutputStream();
            
            InputStream is = null;
	            
	        // get a raw data stream
	        is = connection.getInputStream(); 
	        byte[] bArray = new byte[8196];
            
	        // get the hello message
            is.read(bArray);
	            
            byte[] rtxOut = new byte[2];
            rtxOut[0] = 0x54;
            rtxOut[1] = 0x01;
            
            out.write(rtxOut);
            is.read(bArray);
            
            byte[] notifyx = new byte[2];
            notifyx[0] = 0x4E;
            notifyx[1] = 0x01;
            
            out.write(notifyx);
            
            is.read(bArray);
            
            // network ID
            byte[] netMsg = new byte[4];
            netMsg[0] = 0x4D;
            netMsg[1] = netMsg[2] = netMsg[3] = 0x00;
            
            out.write(netMsg);
            is.read(bArray);
            
            
    	}
        catch(IOException ioException)
        {
            ioException.printStackTrace();
        }
        
        // And
        finally{ }
    	
    }
    
    // MCD Simulates the Reader 
    // running for Debug run()
    // debugRun()
    public void runSimulated()
    {
    	int sz = 0;
    	
    	while(bContinue)
    	{
    		// test 16-byte string
    		String testStr = new String("0123456789ABCDEF");
    		
    		byte[] dBuffer = testStr.getBytes();
    		
    		dBuffer[0] = this.headerSequence[0];
    		dBuffer[1] = this.headerSequence[1];
    		dBuffer[2] = this.headerSequence[2];
    		// make phony packet
    		ERFWiflyPacketBuffer myWriter = new ERFWiflyPacketBuffer();				 	    
		  //  Thread myThread = new Thread(myReader, "ERFWiflyPacketWriter");
		        
		    myWriter.setBuffer(dBuffer, 0, dBuffer.length);
		    this.myThreadPool.submitTask(myWriter);
		    
		    System.out.println("Packet " + packetNum);
		    packetNum++;
		    
		    try {
		    	// simulate a blocked socket waiting for bytes
				Thread.sleep(250);

			} catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		    
		   
    	}
    }
    
    // runUnBuffered
    public void run() 
    {
    	
    	try {
    		
    		if(this.providerSocket != null)
    		{
    			this.providerSocket.close();
    			this.providerSocket = null;
    		}
    		
    		if(this.connection != null)
			{
				this.connection.close();
				this.connection = null;
			}
    		
    		this.providerSocket = new ServerSocket(3344, 10);
     
            //2. Wait for connection
            System.out.println("Waiting for connection");
            
            this.connection = this.providerSocket.accept();
              
            System.out.println("Connection received from " + connection.getInetAddress().getHostName());
           
            // get around hardware/firmaware problem...
            sendKlugeBytes();
            
            byte[] extraBytes = null;
            int cnt = 0;
         
            while(bContinue)
            {
    			// null out input stream to flush old buffers
	            int toRead = 0;
	            InputStream is = null;
	            
		        // get a raw data stream
		        is = connection.getInputStream();
 	                      
 	            toRead = is.available();
 	           
 	            if(toRead == -1)
 	            	bContinue = false;
 	            
 	            if(toRead > 0)
 	            {
 	            	// create a raw input stream
 	            	DataInputStream dis = null;	 	
 	 	            dis = new DataInputStream(connection.getInputStream());
 	 	            
	 	            int bCounter;
	 	            int xtraLen = 0;
	 	            
	 	            if(extraBytes != null)
	 	            	xtraLen = extraBytes.length;
	 	            
	 	            byte[] newBuffer = new byte[toRead + xtraLen];
	 	            int rCounter = 0;
	 	            
	 	            // first add in the extra bytes
	 	            // that may be left over from
	 	            // the previous read
	 	            int i;
	 	            for(i = 0; i < xtraLen; i++)
	 	            {
	 	            	newBuffer[rCounter++] = extraBytes[i];
	 	            }
	 	            
	 	            // Now, read available data
	 	            for(bCounter = 0; bCounter < toRead; bCounter++)
	 	            {
	 	            	byte theByte = (byte)dis.read();
	 	            	newBuffer[rCounter++] = theByte;
	 	            }
 	            
	 	            extraBytes = null; // make sure it is released
	 	            
	 	            /*
	 	            StringBuilder sb = new StringBuilder();

					for (byte b : newBuffer) 
					{
				        sb.append(String.format("%02X ", b));
				    }
					
			//		System.out.println("Created a buffer, passing it off");
			//		System.out.println("Buffer > " + sb);
	 	             */
		
					
	 	            extraBytes = ParseRawBuffer(newBuffer);

	 	            // for debug - stops the reader from buffering input.
	 	       //    if(++cnt > 50)
	 	       //    	bContinue = false;
	 	            
 	            }
	    	}
    	}
    	catch (IOException e)
    	{
    		
    	}
		
    }
    
    // takes in a buffer of bytes and creates packets to be processed.
    // Any Bytes left-over are returned. Leading bytes are ignored.
    protected byte[] ParseRawBuffer(byte[] buffer)
    {
    	
    	byte[] eBytes = null;
    	
        int startIndex = 0;
    	int endIndex = 0;

    	boolean found = false;
    	
    	int idx = 0;
    	int sz;
    	
    	startIndex = 0;
    	endIndex = 0;
    	int msgPacketNum = 0;
    	
    	if(true)
    	{
    		StringBuilder buffBytes = null;
        	buffBytes = new StringBuilder();

			for (byte b : buffer) 
			{
		        buffBytes.append(String.format("%02X ", b));
		    }
			
  //      	System.out.println("ParseRawBuffer: Buffer >" + buffBytes);
    	}
    	
    	
    	while (!found && idx < buffer.length)
    	{
		
    		byte val1 = (byte)buffer[idx];
    		
    		if(val1 == (byte)headerSequence[0])
    		{
    			byte val2 = (byte)buffer[idx + 1];
    			if(val2 == (byte)headerSequence[1])
    			{	            					
    				byte val3 = (byte)buffer[idx + 2];
    				if(val3 == (byte)headerSequence[2])
    				{
    					
    					// We are at the start of a packet
    					// is there previous data?
    					startIndex = endIndex;
    					endIndex = idx;

    					sz = endIndex - startIndex;
			
						// pass buffer off
						if(startIndex != endIndex)
						{
							System.out.println("Found a packet > "  + packetNum);
							
							msgPacketNum++;
							
							byte[] byPack = new byte[sz];
							
							// get the bytes
							int i;
							for(i = 0; i < sz; i++)
								byPack[i] = buffer[startIndex + i];
						
							
					/*		StringBuilder sb = new StringBuilder();

							for (byte b : byPack) 
							{
						        sb.append(String.format("%02X ", b));
						    }
				
							System.out.println("Packet: " + packetNum);
							System.out.println("Num bytes: " + sz);
					
							System.out.println("client ID>" + ((int)buffer[idx + 3] & 0xFF));
							System.out.println("Packet > " + sb + "; bytes: " + sz);
				*/			
							// pass the packet to thread
							// for processing.
							
        					ERFWiflyPacketBuffer myPacketWriter = new ERFWiflyPacketBuffer();	
        					myPacketWriter.setBuffer(buffer, startIndex, sz);
        					
        					this.myThreadPool.submitTask(myPacketWriter);
        		// 	        Thread myThread = new Thread(myReader, "ERFWiflyPacket");	       
        		// 	        myThread.start(); 
        					
        					packetNum++;
        		 	       
    					}

    		 			idx += 3;
    						
    				}
    				else
    				{
    					idx++;
    					found = false;
    				}
    			}
    			else
				{
					idx++;
					found = false;
				}
    		}
    		else
			{
				idx++;
				found = false;
			}
    		
    		found = false;
    	}

		//
		int nLength = buffer.length - endIndex;
		eBytes = new byte[nLength];
		
		int y = 0;
		for(y = 0; y < nLength; y++)
		{
			eBytes[y] = buffer[endIndex + y];
		}
		
		return eBytes;
		
    }
    
   
	

	
}
