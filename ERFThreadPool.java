package erf.wifly;

import java.util.concurrent.*;




public class ERFThreadPool {

	
	public  ExecutorService threadService;
	
	ERFThreadPool() {
		
		setUpThreadService();
	}
	
	public void setUpThreadService()
	{
		if(this.threadService == null)
			this.threadService = Executors.newSingleThreadExecutor();
		
		System.out.println("Thread Pool service created");
	}
	
	// submit a task to be acted on buy the worker-thread
	public void submitTask(ERFWiflyPacketBuffer theTask)
	{
//		System.out.println("ERFThreadPool:submitTask; Starting Task >" + theTask.threadId );
		
		threadService.execute(theTask);
		
	}

}
