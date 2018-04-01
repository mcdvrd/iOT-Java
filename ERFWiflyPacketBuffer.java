//------------------------------------------------------
// ERFWiflyPacketBuffer: 
//------------------------------------------------------

package erf.wifly;

import erf.wifly.ERFSingleDataWriter;

// this class is run as a thread task.
public class ERFWiflyPacketBuffer extends ERFCommandBase implements Runnable
{

/////////////////////////////////////////////////////////
	// byte array to hold the packet data
	byte[] myBuffer;
	
	String rowStr;
	
	// Synchronized
    int threadId;
    // for accessing this id....
    private final Object idLock = new Object();
    
    // packet header to search for
 	 public static byte[] headerSequence = { (byte)0x67, (byte)0x55, (byte)0xAA };
 	
 /////////////////////////////////////////////////////////	 
    
	ERFWiflyPacketBuffer()
	{
		// init the packet object
		myBuffer = null;
		threadId = -1;
	}

	
	 public void setThreadId(int newThreadId)
	 {
		 synchronized (idLock) 
		 {
			 threadId = newThreadId;
		 }
	 }
	 
	 
	 public int getThreadId()
	 {
		 synchronized (idLock) 
		 {
			 return this.threadId;
		 }
	 }
	 
	 
	 // copy the entire passed buffer
	public void setBuffer(byte[] newBuffer)
	{
		this.myBuffer = newBuffer.clone();
	}
	

	//---------------------------------------
	// Copy just a part of the passed buffer
	// this picks out a packet from a multi-
	// packet transmission.
	//---------------------------------------
	public void setBuffer(byte[] theBuffer, int start, int sz)
	{ 

		if(this.myBuffer != null)
			this.myBuffer = null;
	
		this.myBuffer = new byte[sz];
		
		if((start + sz) <= theBuffer.length)
		{
			int i;
			for (i = 0; i < sz; i++)
			{
				this.myBuffer[i] = theBuffer[i + start];
			}
		}
	}
	
	//--------------------------------
	// Task start function
	// This called by the ThreadPool
	// to start a task.
	//--------------------------------
	@Override
	public void run() 
	{
		// MCD: debugging
	//	PrintBuffer();
		
		ParseWiflyPacket();
		
	}
	
	//----------------------------
	// prints my internal buffer
	// contents FOR DEBUG
	//----------------------------
	public void PrintBuffer()
	{
		// MCD - write to data writer
		ERFSingleDataWriter.getInstance().WriteBytes(this.myBuffer);
	}

	
	//------------------------------------------------
	// this just looks for the header and then prints
	// the clientID byte for identification.
	// DEPRECATED
	//------------------------------------------------
	public String ParseWiflyPacket()
	{
		String result = new String();
		
	//	System.out.println("ParseWiflyBuffer: " + this.myBuffer);
		int numBytes = myBuffer.length;
	//	int rIdx = 0;
	//	int packetStart = 0;
	//	int packetEnd = 0;
		
		boolean found = false;
		int i;
		int rIdx = 0;
		
		for(i = 0; i < numBytes; i++)
		{
			found = false;
			byte theByte = myBuffer[i];
			
			if(theByte == headerSequence[0])
			{
				theByte = myBuffer[i+1];
				if(theByte == headerSequence[1])
				{
					theByte = myBuffer[i + 2];
					found = true;
				}
			}
			
			if(found)
			{
				byte targetByte =  this.myBuffer[i+3];
				
				// print byte-value as an unsigned int.
				// Java has no unsigned support ...Doh!
				int tb = ((int) targetByte & 0xFF);
				
				int pLQI = 	((int)this.myBuffer[9] & 0xFF);
				int pRSSI = ((int)this.myBuffer[14] & 0xFF);
				int sRSSI =  ((int)this.myBuffer[6] & 0xFF);
				int sLQI =  ((int)this.myBuffer[7] & 0xFF);
					
				// System.out.println("Parsed ClientID > " + tb + " ThreadId > " + this.threadId);
				int primary = ERFSingleDataWriter.getInstance().GetPathPrimaryForAccessPont(tb);
				int secondary = ERFSingleDataWriter.getInstance().GetPathSecondaryForAccessPont(tb);
				// System.out.println("Primary: " + primary + " Secondary: " + secondary);
				
				//ERFDataWriter.getInstance().WriteBytes(this.myBuffer);
				
				StringBuilder sb = new StringBuilder();
				
				if(secondary != NOT_SET)
				{
					// Not the initiator
					// two paths
					sb.append(String.format(
						"%6d %5d %5d %5d %5d %5d %5d", 
						tb, primary, pLQI, pRSSI, 
						    secondary, sLQI, sRSSI));
				}
				else
				{
					// Not the initiator
					// Only one-path
					sb.append(String.format(
						"%4d %4d %4d %4d", 
						tb, primary, pLQI, pRSSI));
				}

			//	ERFDataWriter.getInstance().WriteString(sb.toString());
				
				ERFSingleDataWriter.getInstance().WritePacketBytes(tb, sb.toString().getBytes());
			}
			
			rIdx++;
			
		 }

		return result;
	}
}
