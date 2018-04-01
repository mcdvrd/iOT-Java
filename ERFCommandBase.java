package erf.wifly;

//-----------------------------------------------
// shared commands
// These are the shared commands between the
// objects in our system. This serves as a base 
// class to give a common set of commands.
//-----------------------------------------------

public class ERFCommandBase 
{

	public int NOT_SET = -99;
	
	// HTTP Server context
	protected final String MY_CONTEXT 			= "/embedrf/";
	
	// recognized commands
	protected final String START_SERVICE		= "StartService";
	protected final String STOP_SERVICE			= "StopService";
	
	protected final String START_RECORDING 		= "StartRecording";
	protected final String STOP_RECORDING 		= "StopRecording";
	
	protected final String READY_TO_WRITE		= "ReadyToWrite";
	protected final String READY_TO_READ		= "ReadyToRead";
	
}
