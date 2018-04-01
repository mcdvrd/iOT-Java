package erf.wifly;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// just for this main class 
import erf.wifly.ERFCommandBase;


public class EmbedRFMainSvc extends ERFCommandBase
implements PropertyChangeListener
{
	
	private  ERFCommandObserver myObserver;
	private  ERFSingleDataWriter myDataWriter;
	private  ERFWiflyReader myDataReader;
	

	/**
	 * @param args
	 * 
	 * Entry point for the EmbedRF Service Utility
	 * This version supports the EmbedRF service 
	 * utility for the EmbedRF Wifly Node.
	 */
	public static void main(String[] args) 
	{
		
		// Start of Service
		
		EmbedRFMainSvc myMain = new EmbedRFMainSvc();
		myMain.startUp();

	}
	
	public void startUp()
	{
		
			// Create shared data model between the Read/Writer
			ERFSharedDataModel sharedRWData = new ERFSharedDataModel();
		
			// Single-Data writer is a Singleton()
			myDataWriter = ERFSingleDataWriter.getInstance();
			
			// OK, we know we will only 
			// have One reader... for now
			myDataWriter.addSharedData(sharedRWData);
			
			myDataReader = new ERFWiflyReader();
			myDataReader.addSharedData(sharedRWData);
			
			sharedRWData.addReaderChangeListener(myDataWriter);
			sharedRWData.addWriterChangeListener(myDataReader);
			
			// register observers for events	
			// Tell command observer we want to know about 
			// the start/stop recording events.
			myObserver = new ERFCommandObserver();
		//	myObserver.run();
			
			// this class wants to hear about:
			myObserver.addRecordingChangeListener(this);
			myObserver.addRunChangeListener(this);
			
			// the data reader:
			myObserver.addRecordingChangeListener(myDataReader);
			
			Thread observerThread  = new Thread(myObserver);
			observerThread.start();
			
			// start the data writer.
			// dataWriterThread.start();
			myDataWriter.run();
			
			// MCD Do some debug:
			// Issue direct commands to test
//			myObserver.pokeCommand(0,  "false, true");
//			myObserver.pokeCommand(1,  null);
				
	}


	// Event; Message callbacks from the Observer() model.
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		//
		System.out.println("EmbedRFMainSvc: Changed property: " + event.getPropertyName() + " [old -> "
                + event.getOldValue() + "] | [new -> " + event.getNewValue() +"]");
		
		// We are told to start/stop recording
		if(event.getPropertyName() == START_RECORDING)
		{
			// not responding here; see reader/writer
		}
		else
			// I can read this event here; or at the writer/reader level
			// depending on what I want to do.
	    if(event.getPropertyName() == STOP_RECORDING)
		{
			// convince myself that I can get called here too
		}
	    else
	    if(event.getPropertyName() == STOP_SERVICE)
	    {
	    	// kill this process
	    	this.myDataWriter.StopWriter();
	    	this.myDataReader.StopReader();
	    	
	    	// Bye...
	    	System.exit(0);
	    	
	    	
	    }
	}

}
