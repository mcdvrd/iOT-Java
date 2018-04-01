package erf.wifly;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class ERFSharedDataModel extends ERFCommandBase
{

	
	private boolean bWriterStarted;
	private boolean bReaderStarted;
	private boolean bReadyToWrite;
	private boolean bReadyToRead;
	
	public boolean bRecording;
	
	private List<PropertyChangeListener> writerListeners = new ArrayList<PropertyChangeListener>();
	private List<PropertyChangeListener> readerListeners = new ArrayList<PropertyChangeListener>();
	
	ERFSharedDataModel()
	{
		// init the shared variables
		this.bWriterStarted = false;
		this.bReaderStarted = false;
		this.bReadyToWrite = false;
		this.bReadyToRead = false;
	}
	
	 //////////////////// Read ////////////////////////
	
	public boolean IsReading()
	{
		return this.bReaderStarted;
	}
	
	public void setReadyToRead(boolean val) 
	{
		bReadyToRead = val;
	    notifyReaderListeners(this, bReadyToRead);
	}
	
	public void setReading(boolean val)
	{
		bReaderStarted = val;
	}

	 private void notifyReaderListeners(Object object, boolean bVal) 
	 {
		    for (PropertyChangeListener name : readerListeners) {
		      name.propertyChange(new PropertyChangeEvent(this, READY_TO_READ, !bReadyToRead, bReadyToRead));
		    }
	 }

	 public void addReaderChangeListener(PropertyChangeListener newListener) 
	 {
		  readerListeners.add(newListener);
	 }

     //////////////////// Write ////////////////////////
	 
	 public boolean IsWriting()
	 {
		 return this.bWriterStarted;
	 }
	 
	 public void setWriting(boolean val)
		{
			bWriterStarted = val;
		}
		
		public void setReadyToWrite(boolean val) 
		{
			bReadyToWrite = val;
		    notifyReaderListeners(this, bReadyToRead);
		}
		
	
	private void notifyWriterListeners(Object object, boolean bVal) 
	{
		    for (PropertyChangeListener name : writerListeners) {
		      name.propertyChange(new PropertyChangeEvent(this, READY_TO_WRITE, !bReadyToWrite, bReadyToWrite));
		    }
	}
	
	 public void addWriterChangeListener(PropertyChangeListener newListener) 
	{
		  writerListeners.add(newListener);
	}

	

}
