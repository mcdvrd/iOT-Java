package erf.wifly;

import erf.wifly.ERFSharedDataModel;


import erf.wifly.ERFAccessPoint;

import java.util.ArrayList;
import java.util.Collections;

import javax.xml.parsers.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


//-----------------------------------------------------------
// Singleton class to Write EmbedRF data files.
// This class opens a File for writing and begins to accept
// data-packets for output.
//-----------------------------------------------------------
public class ERFSingleDataWriter extends ERFCommandBase
implements Runnable, PropertyChangeListener  
{
	
	private final static boolean UseDreamPlug = false;
	
	private int initiatorId = NOT_SET;
	public int currentId = NOT_SET;
	public int currentIdx = 0;
	
	private FileOutputStream outStream;
	private boolean bShouldContinue = true;
	
	private ERFSharedDataModel sharedData;
	
	private  String configFileName = null;
	private  final String wConfigFileName = "C:\\Users\\jacobsp\\ERFConfiguration.xml";
	
	private  final String uConfigFileName = "/usr/mcd/ERFConfiguration.xml"; 
	
	// MCD test file name
	// private static final String FILE_NAME = "\\Users\\mcd\\test.txt";

	private  String FILE_NAME = null;
	private  String wFILE_NAME = "C:\\Users\\jacobsp\\test.txt";
	private  String uFILE_NAME = "/usr/mcd/test.txt";
 	
	// our list of access points from the config file
	public ArrayList<ERFAccessPoint> apList;
	
	// Lock object to prevent 
	// threads from writing simultaneously
	private final Object wLock = new Object();
	
	//--------------------------------------------------------
	// Singleton instance
	// Constructor : accessor
	private static ERFSingleDataWriter instance = null;

	public static ERFSingleDataWriter getInstance()
	{
	    if(instance == null)
	    {
	       instance = new ERFSingleDataWriter();
	      
	    }
	      return instance;
	 }
	
	protected ERFSingleDataWriter()
	{
		if(!UseDreamPlug)
		{
			this.configFileName = this.wConfigFileName;
			this.FILE_NAME = this.wFILE_NAME;
		}
		else
		{
			this.configFileName = this.uConfigFileName; 
			this.FILE_NAME = this.uFILE_NAME;
		}
		
		ReadConfigurationInformation();
	}
	
	
	//--------------------------------------------
	// 
	//--------------------------------------------
	private void ReadConfigurationInformation()
	{
		this.apList = null;
		this.apList = new ArrayList<ERFAccessPoint>();
		
		ReadConfigFile();	
	}
	
	
	public void OpenOutputFile(String path) 
	{
		try {
			outStream = new FileOutputStream(path);
			
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	public void CloseOutputFile()
	{
		if(outStream != null)
		{
			try {
				
				outStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	//----------------------------------------------------------
	// WritePacketBytes()
	// Write the data for one packet out to a Data file. This
	// in volves using the list of access points to determine
	// if there have been any missed packets. If a packet(s)
	// is found to be out of sequence, the missing sequences 
	// are replaced with INVALID_VALUE values.
	//----------------------------------------------------------
	public void WritePacketBytes(int clientId, byte[] bytesToWrite)
	{
		// we cannot do anything without this information
		// so bail out.
		if(this.initiatorId == NOT_SET)
			return;
		
		int listNum = this.apList.size();
			
		// Which client ID are we at?
		if(this.sharedData.IsWriting() == true)
		{
			// We have already started writing
			// Is this clientId the next expected packet?
			ERFAccessPoint obj = null;
			
			int nxtIndex;
			if(this.currentIdx < (listNum - 1))
				nxtIndex = this.currentIdx + 1;
			else
				nxtIndex = 0;
			
			obj = (ERFAccessPoint)this.apList.get(nxtIndex);
			
			if(obj.ID != clientId)
			{

				// What have we missed?
				int start = nxtIndex;
				int end = this.GetIndexForClientId(clientId);
				
				int span = 0;
				int span2 = 0;
				if(start <= end)
				{
					span = (end - start);
				}
				else
				{
					span = listNum - end;
					span2 = start;
				}
			
				// we are out of order
				System.out.println("Error; packet is out of order. Should be: " + obj.ID + "; found: " + clientId);
				System.out.println("Start >" + start + " End >" + end + " CurrenIdx >" + this.currentIdx);
				System.out.println("Span >" + span + " Span2 >" + span2);
				
				int newCount = 0;
				// hate crummy iterators in Java
				for(ERFAccessPoint ap : apList)
				{
					if(span2 > 0)
					{
						if(newCount < span2)
							EmitDummyBytes(newCount);
						
					}
					else
					{
						// MCD ??
						if((newCount >= start) && (newCount < (end)))
							EmitDummyBytes(newCount);	
					}
					
					newCount++;
				}
				
			
				// We have caught-up now; 
				// write the original record
				this.WriteBytes(bytesToWrite);
				
			}
			else
			{
				this.WriteBytes(bytesToWrite);
			
				// Are we at the end of cycle?
				if(this.currentIdx == this.apList.size())
				{
					
					this.WriteNewline();
				}
			}
			
			this.currentIdx = this.GetIndexForClientId(clientId);
			
		}
		else
			if(clientId == this.initiatorId)
				this.sharedData.setWriting(true);
		
		return;
		
	}
	
	//---------------------------------------------
	// Emit dummy records to fill-in for lost data 
	// packets.
	//
	//---------------------------------------------
	public void EmitDummyBytes(int idx)
	{
		
		System.out.println("EmitDummyBytes to replace packet for ----->" + idx);
		
		ERFAccessPoint ap = this.apList.get(idx);
		int PAD_VALUE = -99;
		StringBuilder sb = null;
		sb = new StringBuilder();
		
		if(ap.IsInitiator != 1)
		{
			// Not the initiator
			// two paths
			sb.append(String.format(
				"%5d %5d %5d %5d %5d %5d %5d", 
				PAD_VALUE, PAD_VALUE, PAD_VALUE, PAD_VALUE, 
				PAD_VALUE, PAD_VALUE, PAD_VALUE));
		}
		else
		{
			// the initiator
			// Only one-path
			sb.append(String.format(
				"%5d %5d %5d %5d", 
				PAD_VALUE, PAD_VALUE, PAD_VALUE, PAD_VALUE));
		}

		this.WriteBytes(sb.toString().getBytes());
	}
	
	
	//--------------------------------------------
	// Writes bytes to outstream 
	// in thread-safe manner.
	//--------------------------------------------
	public void WriteBytes(byte[] bytes)
	{
		try {
			
			synchronized(wLock)
			{
				if((outStream != null) &&  (bytes.length > 0))
				{
					outStream.write(bytes);
					outStream.flush();
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	//--------------------------------------------
	// Writes string to outstream 
	// in (hopefully) thread-safe manner.
	//--------------------------------------------
	public void WriteString(String str)
	{
		try {
			
			synchronized(wLock)
			{
				if((outStream != null) &&  (str.length() > 0))
				{
					outStream.write(str.getBytes("UTF-16"));
		//			outStream.flush();
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	//--------------------------------------------
		// Writes string to outstream 
		// in (hopefully) thread-safe manner.
		//--------------------------------------------
		public void WriteNewline()
		{
			try {
				
				synchronized(wLock)
				{
					if(outStream != null)
					{
						byte[] nl = new byte[2];
						nl[0] = '\n';
						nl[1] = '\r';
						outStream.write(nl);
						
						outStream.flush();
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	
	
	
	///--------------------------------------------
	// Called on Thread.start()
	//--------------------------------------------
	public void run () 
	{
		// use the thread-start
		OpenOutputFile(FILE_NAME);
		
		this.sharedData.setReadyToWrite(true);
		
	}
	
	public void StopWriter()
	{
		this.bShouldContinue = false;
	}
	
	public int GetPathPrimaryForAccessPont(int clientID)
	{
		for(ERFAccessPoint ap : apList)
		{
			// find the accesspoint 
			// with the passed client id
			if(ap.ID == clientID)
				return ap.primaryPath;
		}
		
		// otherwise
		return NOT_SET;
	}
	
	public int GetPathSecondaryForAccessPont(int clientID)
	{
		for(ERFAccessPoint ap : apList)
		{
			// find the accesspoint 
			// with the passed client id
			if(ap.ID == clientID)
				return ap.secondaryPath;
		}
		
		// otherwise
		return NOT_SET;
	}
	
	
	public int GetIndexForClientId(int clientID)
	{
		int cnt = 0;
		int ret = NOT_SET;
		
		for(ERFAccessPoint ap : apList)
		{
			// find the accesspoint 
			// with the passed client id
			if(ap.ID == clientID)
			{
				ret = cnt;
				break;
			}
			else
				cnt++;
		}
		
		// otherwise
		return ret;
	}
	
	
	// Use the default config-file name
	public void ReadConfigFile()
	{
		ReadConfigFileXML(configFileName);
	}

	//--------------------------------------------
	// Read XML Configuration file and build
	// a list of the access points sorted by
	// delay time.
	//--------------------------------------------
	public void ReadConfigFileXML(String configFileName) 
	{
		 try 
		 {
		      DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			 
	 		  domFactory.setNamespaceAware(true); 
			  DocumentBuilder builder = domFactory.newDocumentBuilder();
			  
			  try {
					 
					 Document doc = builder.parse(configFileName);
					 
					 BuildLists(doc);
					 
				 }
				 catch(SAXException e)
				 {
					 System.out.println("SAX Error " + e.toString());
					 e.printStackTrace();
				 }
			  	 catch(IOException e)
			  	 {
			  		 System.out.println("IO Error " + e.toString()); 
			  		 e.printStackTrace();
			  	 }
			
		 }
		 catch (ParserConfigurationException e)
		 {
			 System.out.println("XML Parse Error; ReadConfigFile()");
			 e.printStackTrace();
		 }
		
		}
	
		// Create the data structures to enable processing
		// of data signals from the Wifly Node
		public void BuildLists(Document xmlDoc)
		{
			// read the access-point information
			// from the doc
			NodeList nList = xmlDoc.getElementsByTagName("accesspoint");

			if(nList.getLength() >= 0)
			{
				int i;
				
				NodeList napList = xmlDoc.getElementsByTagName("ap");
				
				int numPoints = napList.getLength();
				
				for(i = 0; i < numPoints; i++)
				{
					Node theNode = napList.item(i);
					Element appElement = (Element)theNode;
					
					// make an accesspoint object to model this entry
					ERFAccessPoint ap = new ERFAccessPoint();
					
					// now fill this information from the xml dom		
					ap.ID = 			(int) Integer.parseInt(getElementValue("ID", appElement));
					ap.LocX = 			(int) Integer.parseInt(getElementValue("locx", appElement));
					ap.LocY = 			(int) Integer.parseInt(getElementValue("locy", appElement));
					ap.LocZ = 			(int) Integer.parseInt(getElementValue("locz", appElement));
					ap.TxID = 			(int) Integer.parseInt(getElementValue("TxID", appElement));
					ap.RxID = 			(int) Integer.parseInt(getElementValue("RxID", appElement));
					ap.IsInitiator = 	(int) Integer.parseInt(getElementValue("IsInitiator", appElement));
					ap.Delay = 			(int) Integer.parseInt(getElementValue("delay", appElement));
					ap.RoomID = 		(int) Integer.parseInt(getElementValue("roomID", appElement));
					
					// is it the initiator? If so, set it for the class
					if(ap.IsInitiator != 0)
						this.initiatorId = ap.ID;
					
					// add this item to the list
					this.apList.add(ap);
					
					System.out.println("Found AccessPoint[" + i + "] ->" + ap.ID);

				}
				
		//		System.out.println("");
				// print out found accesspoints
				Collections.sort(apList);
				
				// to see the list
				/*
				for(ERFAccessPoint ap : apList)
				{
					System.out.println("AccessPoint>");
					System.out.println("ID >" + ap.ID);
					System.out.println("Delay >" + ap.Delay);
					System.out.println();
					
				}
				*/
				
				// go through the list and create the primary and secondary paths
				int cnt = 2;
				
				for(ERFAccessPoint ap : apList)
				{
					// int value here...
					if(ap.IsInitiator != 1)
					{
						ap.primaryPath = cnt;
						ap.secondaryPath = cnt + 1;
						cnt += 2;
					}
					else
					{
						ap.primaryPath = 1;
						ap.secondaryPath = NOT_SET;
					}
				}
				
			}
			
		}
		
		private static String getElementValue(String tag, Element element) 
		{

			NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();

			Node node = (Node) nodes.item(0);

			return node.getNodeValue();

		}
	//---------------------------------------------------------
	
	public void addSharedData(ERFSharedDataModel dataModel)
	{
		this.sharedData = dataModel;
	}
	

	// Property change recieved from the shared data model
	@Override
	public void propertyChange(PropertyChangeEvent event) 
	{
		
		System.out.println("ERFDataWriter: Changed property: " + event.getPropertyName() + " [old -> "
                + event.getOldValue() + "] | [new -> " + event.getNewValue() +"]");
		
		// Start/Stop message sent
		if(event.getPropertyName() == "START_RECORDING")
			// Start/stop recording
				this.sharedData.setReadyToWrite(true);
		else
			if(event.getPropertyName() == "STOP_RECORDING")
				this.sharedData.setReadyToWrite(false);
	}
		

	
} // Class
