package erf.wifly;

import erf.wifly.ERFHttpServer;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


// A class to implement an 
// Observer pattern within the EmberRF service.
// It allows other classes to express interest 
// in certain events. OnEvent; all listeners in the list
// are notified.

public class ERFCommandObserver extends ERFCommandBase 
implements Runnable, HttpHandler
{
	
	// General command listener
	private List<PropertyChangeListener> listener = new ArrayList<PropertyChangeListener>();
	
	// specific command listener 
	private List<PropertyChangeListener> recordingListener = new ArrayList<PropertyChangeListener>();
	private List<PropertyChangeListener> runListener = new ArrayList<PropertyChangeListener>();
	
	private boolean bRecording;
	private boolean bServiceShouldRun;
	
	private HttpServer myHttpServer;

	ERFCommandObserver()
	{
		bRecording = false;
		bServiceShouldRun = true;
	}
	
	
	// can use from thread. Not doing so right now
	public void run()
	{
		// Open the Http ports for internet calls
		int portNum = 3345;
		
		try {
			
			this.myHttpServer = HttpServer.create(new InetSocketAddress(portNum), 0);
			HttpContext context = myHttpServer.createContext(MY_CONTEXT, this);
		//  MCD
		//	context.getFilters().add(new ERFHttpPostParse());
			myHttpServer.setExecutor(null); // creates a default executor
			myHttpServer.start();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
	}

	private void OnRESTfulCallRecieved()
	{
		// Not implemented
	}
	
	
    public void handle(HttpExchange exchange) throws IOException 
    {
    	// Use filetr class to get POST params
    //	Map params = (Map)t.getAttribute("parameters");
    	
    	URI requestedUri = exchange.getRequestURI();
    	String paramStr = requestedUri.toASCIIString();
    	
    	// get the parameters; string is MY_CONTEXT/key=value
    	String sub = MY_CONTEXT;
    	String userParams = paramStr.replace(sub, "");
    	//String args[] = userParams.split("=");
    	
    	if(userParams != null)
    	{
    		// valid input to this address
    	
    		
    		if(userParams.compareToIgnoreCase("startrecording") == 0)
    		{
    			// start recording command
    			// Stop Recording
    	    	this.pokeCommand(0,  null);
    	    	
    	    	String response = "EmbedRF Service Utility: Recording Started.";
    	        exchange.sendResponseHeaders(200, response.length());
    	        OutputStream os = exchange.getResponseBody();
    	        os.write(response.getBytes());
    	        os.close();
    		}
    		else
    		if(userParams.compareToIgnoreCase("stoprecording") == 0)
    		{
    			this.pokeCommand(1,  null);
    			
    			String response = "EmbedRF Service Utility: Recording Stopped.";
    	        exchange.sendResponseHeaders(200, response.length());
    	        OutputStream os = exchange.getResponseBody();
    	        os.write(response.getBytes());
    	        os.close();
    		}
    		else
			if(userParams.compareToIgnoreCase("stopservice") == 0)
    		{
    			// start recording command
    			// Stop Recording
    	    	this.pokeCommand(2,  null);
    	    	
    	    	String response = "EmbedRF Service Utility: Service Stopped.";
    	        exchange.sendResponseHeaders(200, response.length());
    	        OutputStream os = exchange.getResponseBody();
    	        os.write(response.getBytes());
    	        os.close();
    		}
    		
    	}
    	else
    	{	
	    	// Stop Recording
	    	this.pokeCommand(1,  null);
	    	
	        String response = "UnRecognized Command; USE EmbedRF: POST:/embedrf/key=value";
	        exchange.sendResponseHeaders(200, response.length());
	        OutputStream os = exchange.getResponseBody();
	        os.write(response.getBytes());
	        os.close();
    	}
        
    }
	

	
	
	// MCD Utility method: Remove before production.
	// allows me to push a command to be spread to other objects.
	// The args string is interpreted by the method according
	// to the code.
	public void pokeCommand(int commandCode, String args)
	{
		switch(commandCode)
		{
			case 0: // start RECORDING
			{
				this.notifyRecordingListeners(this, START_RECORDING, "false", "true");
			}
				
				break;
				
			case 1: // Stop recording
			{
				this.notifyRecordingListeners(this, STOP_RECORDING, "false", "true");
			}
			
				break;
				
			case 2:
			{
				this.notifyRunListeners(this, STOP_SERVICE, "false", "true");
			}
				break;
				
			default:
					
					break;
		}
	}
	
	
	// Allows uses to add listener
	// User will be notified of ALL events
	// this Command class recognizes.
	public void addChangeListener(PropertyChangeListener newListener) 
	{
	    listener.add(newListener);
	}

	private void notifyListeners(Object object, String property, String oldValue, String newValue) 
	{
	    for (PropertyChangeListener name : listener) 
	    {
	      name.propertyChange(new PropertyChangeEvent(this, "REST/service/x", oldValue, newValue));
	    }
	}
	
	public void addRecordingChangeListener(PropertyChangeListener newListener) 
	{
		recordingListener.add(newListener);
	}

	private void notifyRecordingListeners(Object object, String property, String oldValue, String newValue) 
	{
	    for (PropertyChangeListener name : recordingListener) 
	    {
	      name.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
	    }
	}
	
	public void addRunChangeListener(PropertyChangeListener newListener) 
	{
		runListener.add(newListener);
	}

	private void notifyRunListeners(Object object, String property, String oldValue, String newValue) 
	{
	    for (PropertyChangeListener name : runListener) 
	    {
	      name.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
	    }
	}
	
	

}
