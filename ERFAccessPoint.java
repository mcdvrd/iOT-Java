package erf.wifly;



// Class to model the AccessPoint data in a list
public class ERFAccessPoint implements Comparable<ERFAccessPoint> {
	
	// my own fields
	int primaryPath;
	int secondaryPath;
	
	// hardware fields
	 public int ID;
     public int Delay;
     public int LocX;
     public int LocY;
     public int LocZ;

     public int TxID;
     public int RxID;
     public int RoomID;
     
     public int IsInitiator;
     
     // Simple sort.
     @Override
     public int compareTo(ERFAccessPoint other)
     {
         return ((this.Delay < other.Delay) ? 1 : 0);
     }

}
