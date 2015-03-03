package edu.buffalo.cse.cse486586.simpledht;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


public class MessageClass  implements Serializable{
	//message format:
 	//type,fromPort,toPort,file_name, content, predecessor,successor
 	
	 public String msgType;
	 public String fromPort;
	 public String toPort;
	 public String receivedKey;
	 public String receivedValue;
	 public String predecessor;
	 public String successor;
	 public String initiator;
	 public HashMap<String,String> gDumpQueryResult = new HashMap<String, String>();
	 
	 
	 
	 
	 public MessageClass(String msgType,String fromPort, String toPort,String receivedKey,String receivedValue,String predecessor,String successor,String initiator, HashMap hash_map)
	 {
		 this.msgType=msgType;
		 this.fromPort=fromPort;
		 this.toPort=toPort;
		 this.receivedKey=receivedKey;
		 this.receivedValue=receivedValue;
		 this.predecessor=predecessor;
		 this.successor=successor;
		 this.initiator=initiator;
		 this.gDumpQueryResult.putAll(hash_map);
	 }
	

}
