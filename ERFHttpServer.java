package erf.wifly;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;



public class ERFHttpServer implements Runnable
{
	private boolean bShouldContinue = true;
	private boolean bServerStarted = false;
	
	private HttpServer server = null; 
	
	private int portNum = 3345;

	
	
	ERFHttpServer(int portNum, HttpHandler handlerObject) throws IOException
	{
		//ConfigureServer(portNum);
		bServerStarted = ConfigureServer(portNum, handlerObject);
		
	}
	
	
	public boolean ConfigureServer(int portNum, HttpHandler ob) throws IOException
	{
		
		server = HttpServer.create(new InetSocketAddress(portNum), 0);
		server.createContext("/erf/test", ob);
	     
		return true;
	}
	
	
	@Override
	public void run() {
	
			server.setExecutor(null); // creates a default executor
		    server.start();
	
	}

}
