package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.R.bool;
import android.R.integer;
import android.R.string;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class SimpleDhtProvider extends ContentProvider {
	//----database variables
		private static final String DbName="SimpleDHTDB";
		private static final String TableName="SimpleDHTTable";
		private static final int version=1;
		SQLiteDatabase SQLitedb;
		SQLiteQueryBuilder QueryBuilder;
		public static Context context;
		static final Uri  providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
		public static final String KEY="key";
		public static final String VALUE="value";
		public static final String lDumpQryType="@";
		public static final String gDumpQryType="*";
		public static String QueryfromServer="*server*";
		public static HashMap<String, String> globalDB= new HashMap<String, String>();
		public static boolean waitFlag=true;
		
		//----hash values
		static String myPortHash=null;
 		static String myPredessorHash=null;
 		static String mySuccessorHash=null;
 		static String keyHash=null;
 		static String myEmulator=null;
 		static String myPreEmulator= null;
 		static String mySucEmulator= null;
		
		//----message passing variables
		static String myPort;
		static final String REMOTE_PORT0 = "11108";
	    static final String REMOTE_PORT1 = "11112";
	    static final String REMOTE_PORT2 = "11116";
	    static final String REMOTE_PORT3 = "11120";
	    static final String REMOTE_PORT4 = "11124";
	    static final int SERVER_PORT = 10000;
	    
	    //---msg types
	    static final String msgTypeJoin="Join";
	    static final String msgTyperesponse="Response";
	    static final String msgTypekeyValue="keyValue";
	    static final String msgTypeQuery="*";
	    static final String msgTypeDelete="delete";
	    
	    static int myPredecessor=0;
	    static int mySuccessor=0;
	    public class DbClass extends SQLiteOpenHelper
		{
			private static final String SQL_CREATE = "CREATE TABLE " +
					TableName +
		            "(" +                           // The columns in the table
		            " key String PRIMARY KEY, " +
		            " value String"+ ")";
			
			
			
			/*private static final String SQL_CREATE =
					"CREATE TABLE GrpMessengerTable (key String PRIMARY KEY,value String)";*/
			
			public DbClass(Context context) {
				super(context, DbName, null, version);
				// TODO Auto-generated constructor stub
			}

			@Override
			public void onCreate(SQLiteDatabase db) {
				// TODO Auto-generated method stub
				 db.execSQL(SQL_CREATE);
				 
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				// TODO Auto-generated method stub
				
			}
		}
		
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
    	int deleted=0;
    	    deleted=SQLitedb.delete(TableName, KEY+"="+selection, null);
       if(deleted==0)
       {

   		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeDelete,myPort,Integer.toString(mySuccessor),selection,"-","-","-",myPort);
   		
       }
       return 1;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

   

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
    	 context=getContext();
         DbClass dbHelper= new DbClass(context);
     	SQLitedb= dbHelper.getWritableDatabase();
     
     	//get current port
    	TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        
        myPredecessor=Integer.parseInt(myPort);
        mySuccessor=Integer.parseInt(myPort);
        
        myEmulator= getEmulator(myPort);
 		myPreEmulator= getEmulator(Integer.toString(myPredecessor));
 		mySucEmulator= getEmulator(Integer.toString(mySuccessor));
        
        try {
			myPortHash= genHash(myEmulator);
			 myPredessorHash=genHash(myPreEmulator);
			 mySuccessorHash= genHash(mySucEmulator);
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        
      //send request to 5554 to join
        if(!(myPort.equalsIgnoreCase(REMOTE_PORT0)))
        {
        	//message format:
        	//type,fromPort,toPort,file_name, content, predecessor,successor
        	Log.e("myPort", msgTypeJoin+" "+myPort+" "+REMOTE_PORT0);
        	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeJoin,myPort,REMOTE_PORT0,"-","-","-","-");
        }
        /*else
        {
        	ChordRing[0]= true;
        }*/
        
      //start server...
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
           
            Log.e("Server Creation", "Can't create a ServerSocket");
        }
    
        return true;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
    	String Contentkey=values.getAsString(KEY);
 		String Contentval=values.getAsString(VALUE);
 		String actualKey=null;
 		try {
 			synchronized(this)
			{
				this.wait(10);
			}
 			if(Contentkey.startsWith("*ModKey*"))
 	 		{
 	 			actualKey=Contentkey.substring(8);
 	 		}
 	 		else
 	 		{
 	 			actualKey=Contentkey;
 	 		}
 	 		String ActualKeyHash= genHash(actualKey);
 	 		
 	 		if(Integer.parseInt(myPort)==myPredecessor && Integer.parseInt(myPort)==mySuccessor)
 	 	    {
 	 			//Log.e(myPort, msgReceivedArr[1]);
 	 			
 	 			Log.e("1 node inser", Contentkey+"   "+Contentval);
 	 	    	SQLitedb.insert(TableName, null, values);
 	 	        Log.v("insert", values.toString());
 	 	        return uri;
 	 	    }
 	 		else
 	 		{
 	 		/*if(myPredecessor==mySuccessor)
 	 		{
 	 			if(Contentkey.startsWith("*ModKey*"))
 	 	 	    {
 	 	 	    	
 	 	 	    	ContentValues cv = new ContentValues();
 	 	 			cv.put(KEY, Contentkey.substring(8));
 	 	 			cv.put(VALUE, Contentval);
 	 	 			
 	 	 			SQLitedb.insert(TableName, null, cv);
 	 	 	        Log.v("insert", cv.toString());
 	 	 	        return uri;
 	 	 	    }
 	 			else
 	 			{
 	 			if(myPortHash.compareTo(ActualKeyHash)>=0)
 	 			{
 	 				Log.e("1 node inser", Contentkey+"   "+Contentval);
 	 	 	    	SQLitedb.insert(TableName, null, values);
 	 	 	        Log.v("insert", values.toString());
 	 	 	        return uri;
 	 			}
 	 			else
 	 			{
 	 				Contentkey="*ModKey*"+Contentkey;	
 	 	           new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 	 	         
 	 			}
 	 			}
 	 		}*/
 	 		//else
 	 		//{	
 	 		
 	 			if(Contentkey.startsWith("*ModKey*"))
 	 	 	    {
 	 	 	    	
 	 	 	    	ContentValues cv = new ContentValues();
 	 	 			cv.put(KEY, Contentkey.substring(8));
 	 	 			cv.put(VALUE, Contentval);
 	 	 			
 	 	 			SQLitedb.insert(TableName, null, cv);
 	 	 	        Log.v("insert", cv.toString());
 	 	 	        return uri;
 	 	 	    }
 	 			else
 	 			{
 	 			if(myPortHash.compareTo(ActualKeyHash)>=0)
 	 		    {
 	 			if(ActualKeyHash.compareTo(myPredessorHash)>0)
 	 			{
 	 				Log.e("1 node inser", Contentkey+"   "+Contentval);
 	 	 	    	SQLitedb.insert(TableName, null, values);
 	 	 	        Log.v("insert", values.toString());
 	 	 	        return uri;
 	 			}
 	 			else
 	 			{
 	 				if(myPortHash.compareTo(myPredessorHash)<0)
 	 				{
 	 					Log.e("1 node inser", Contentkey+"   "+Contentval);
 	 	 	 	    	SQLitedb.insert(TableName, null, values);
 	 	 	 	        Log.v("insert", values.toString());
 	 	 	 	        return uri;
 	 				}
 	 				else
 	 				{
 	 				//message format:
 	 	        	//type,fromPort,toPort,file_name, content, predecessor,successor
 	 				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 	 				}
 	 				
 	 			}
 	 		}
 	 		else
 	 		{
 	 			//if(myPortHash.compareTo(myPredessorHash)>0)
 	 			//{
 	 				if(myPortHash.compareTo(mySuccessorHash)>0)
 	 				{
 	 					Contentkey="*ModKey*"+Contentkey;
 							Log.e("myPort" +msgTypekeyValue+" "+myPort+" "+Integer.toString(mySuccessor)+" "+Contentkey,Contentval);
 							//message format:
 					        //type,fromPort,toPort,file_name, content, predecessor,successor
 							 new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 						
 	 				}
 	 				else
 	 				{
 	 					//message format:
 	     	        	//type,fromPort,toPort,file_name, content, predecessor,successor
 	 					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 	 	 				
 	 				}
 	 			//}
 	 			//else
 	 			//{
 	 				//message format:
 	 	        	//type,fromPort,toPort,file_name, content, predecessor,successor
 	 			//	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 	 				
 	 		//	}
 	 			
 	 			
 	 		}
 	 	}
 	 		//}	
		} 
 	 		synchronized(this)
			{
				this.wait(10);
			}
 		}catch (Exception e) {
			// TODO: handle exception
		}
 		
 		
 		
 		
 		/*	
 	    if(Integer.parseInt(myPort)==myPredecessor && Integer.parseInt(myPort)==mySuccessor)
 	    {
 	    	Log.e("1 node inser", Contentkey+"   "+Contentval);
 	    	SQLitedb.insert(TableName, null, values);
 	        Log.v("insert", values.toString());
 	        return uri;
 	    }
 	    else if(Contentkey.startsWith("*ModKey*"))
 	    {
 	    	
 	    	ContentValues cv = new ContentValues();
 			cv.put(KEY, Contentkey.substring(8));
 			cv.put(VALUE, Contentval);
 			
 			SQLitedb.insert(TableName, null, cv);
 	        Log.v("insert", cv.toString());
 	        return uri;
 	    }
 	    else
 	    {
 	    	try {
 				 
 				 keyHash=genHash(Contentkey);
 				 
 				 if(keyHash.compareTo(myPortHash)<0)
 				 {
 					 if(keyHash.compareTo(myPredessorHash)>=0)
 					 {
 						 SQLitedb.insert(TableName, null, values);
 					        Log.v("insert", values.toString());
 					        return uri;
 					 }
 					 else
 					 {
 						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 					 }
 				 }
 				 else
 				 {
 					 if(myPortHash.compareTo(mySuccessorHash)>0)
 					 {
 						Contentkey="*ModKey*"+Contentkey;
 						Log.e("myPort" +msgTypekeyValue+" "+myPort+" "+Integer.toString(mySuccessor)+" "+Contentkey,Contentval);
 						//message format:
 				        //type,fromPort,toPort,file_name, content, predecessor,successor
 						 new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 					 }
 					 else
 					 {
 						Log.e("myPort" +msgTypekeyValue+" "+myPort+" "+Integer.toString(mySuccessor)+" "+Contentkey,Contentval);
 						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypekeyValue,myPort,Integer.toString(mySuccessor),Contentkey,Contentval,"-","-");
 	 				 }
 					 
 				 }
 				 
 			} catch (NoSuchAlgorithmException e) {
 				// TODO Auto-generated catch block
 				e.printStackTrace();
 			}
 	    }*/
 		return uri;
        
    }
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        // TODO Auto-generated method stub
    	QueryBuilder = new SQLiteQueryBuilder();
    	QueryBuilder.setTables(TableName);
    /*	synchronized(this)
		{
			try {
				this.wait(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
    	
   if(selection.equalsIgnoreCase(lDumpQryType))
   {
	   String sql= "Select * from "+TableName;
	  
       Cursor cursor= SQLitedb.rawQuery(sql, null);
		//Log.v("query", selection);
			
       return cursor;
	   
   }
   //else
  // {
    	if(selection.equalsIgnoreCase(gDumpQryType))
    	{
    		if(Integer.parseInt(myPort)==myPredecessor && Integer.parseInt(myPort)==mySuccessor)
     	    {
    			String sql= "Select * from "+TableName;
    		    //Cursor cursor = SQLitedb.query(TableName,columns,"key = "+"'"+selection+"'", null, null, null, null);
    	       Cursor cursor= SQLitedb.rawQuery(sql, null);
    			Log.v("query", selection);
    				
    	       return cursor;
     	    }
    		else
    		{
    		String sql= "Select * from "+TableName;
    	    //Cursor cursor = SQLitedb.query(TableName,columns,"key = "+"'"+selection+"'", null, null, null, null);
            Cursor cursor= SQLitedb.rawQuery(sql, null);
    		Log.v("query", selection);
    		while(cursor.moveToNext()){

				int keyIndex = cursor.getColumnIndex("key");
				int valueIndex = cursor.getColumnIndex("value");

				String key = cursor.getString(keyIndex);
				String value= cursor.getString(valueIndex);
				globalDB.put(key, value);
    		}
    		
    		//send to successor
    		
    		
    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeQuery,myPort,Integer.toString(mySuccessor),"-","-","-","-",myPort);
    		
    	/*	synchronized(this)
			{
				try {
					this.wait(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
    		*/
    		while(waitFlag)
    		{
    			
    		}
    		 String[] columns={"key","value"};
    		 MatrixCursor resultCursor = new MatrixCursor(columns);
    			for(Entry<String, String> entry : globalDB.entrySet()) {
    				resultCursor.addRow(new Object[] { entry.getKey(), entry.getValue() });
		     		 }
    			//globalDB.clear();
            return resultCursor;
    	}
    }
    	if ((!selection.equalsIgnoreCase(gDumpQryType)) &&(!selection.equalsIgnoreCase(lDumpQryType))){
    		
    		if(Integer.parseInt(myPort)==myPredecessor && Integer.parseInt(myPort)==mySuccessor)
            {
    			 String[] columns={"key","value"};
                        String sql= "Select * from "+TableName;
                    Cursor cursor = SQLitedb.query(TableName,columns,"key = "+"'"+selection+"'", null, null, null, null);
             //  Cursor cursor= SQLitedb.rawQuery(sql, null);
                        Log.v("query", selection);

               return cursor;
            }
                else
                {
                String sql= "Select * from "+TableName;
            //Cursor cursor = SQLitedb.query(TableName,columns,"key = "+"'"+selection+"'", null, null, null, null);
            Cursor cursor= SQLitedb.rawQuery(sql, null);
                Log.v("query", selection);
                while(cursor.moveToNext()){

                                int keyIndex = cursor.getColumnIndex("key");
                                int valueIndex = cursor.getColumnIndex("value");

                                String key = cursor.getString(keyIndex);
                                String value= cursor.getString(valueIndex);
                                globalDB.put(key, value);
                }

                //send to successor

                new ClientTask().doInBackground(msgTypeQuery,myPort,Integer.toString(mySuccessor),"-","-","-","-",myPort);
                //new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeQuery,myPort,Integer.toString(mySuccessor),"-","-","-","-",myPort);
             /*   synchronized(this)
				{
					try {
						this.wait(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}*/
                while(waitFlag)
                {

                }

                String[] columns={"key","value"};
                MatrixCursor resultCursor = new MatrixCursor(columns);
                       if(globalDB.containsKey(selection))
                       {
                               resultCursor.addRow(new Object[] {selection, globalDB.get(selection) });
                               Log.e("cursor","");
                       }
                       return resultCursor;


               }
    		
    	
    	}
    		
   //}	
    	return null;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

    	@Override
       protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
           // int portNo[]={11108,11112,11116,11120,11124};
            ObjectInputStream inStream = null;
          //  BufferedReader br;
			try {
				while(true)
				{
				inStream = new ObjectInputStream(serverSocket.accept().getInputStream());
				MessageClass msgRecObj=(MessageClass)inStream.readObject();
				
		     	//message format:
	        	//type,fromPort,toPort,file_name, content, predecessor,successor
		     	
		     	String msgReceivedArr[]={msgRecObj.msgType,msgRecObj.fromPort,msgRecObj.toPort,msgRecObj.receivedKey,msgRecObj.receivedValue,msgRecObj.predecessor,msgRecObj.successor};
		     	
		     	String newNodeHash=genHash(getEmulator(msgReceivedArr[1].trim()));
		     	
		     	if(msgReceivedArr[0].equalsIgnoreCase(msgTypeJoin))
		     	{
		     		if(Integer.parseInt(myPort)==myPredecessor && Integer.parseInt(myPort)==mySuccessor)
		     	    {
		     			Log.e(myPort, msgReceivedArr[1]);
		     			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",myPort,myPort);	
		     			
		     			myPredecessor=Integer.parseInt(msgReceivedArr[1].trim());
		 	            myPreEmulator=getEmulator(Integer.toString(myPredecessor));
		 	            myPredessorHash=genHash(myPreEmulator);
		 	          
		 	            
		 	            mySuccessor=Integer.parseInt(msgReceivedArr[1].trim());
		 	            mySucEmulator=getEmulator(Integer.toString(mySuccessor));
		 	            mySuccessorHash=genHash(mySucEmulator);
		 	           Log.e("1st condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		 	            
		     	    }
		     		//else
		     		//{
		     		/*if(myPredecessor==mySuccessor)
		     		{
		     			if(myPortHash.compareTo(newNodeHash)>0)
		     			{
		     				
		     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",Integer.toString(myPredecessor),myPort);
		     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,Integer.toString(myPredecessor),"-","-","-",msgReceivedArr[1]);
			 	            myPredecessor=Integer.parseInt(msgReceivedArr[1].trim());
			 	            myPreEmulator=getEmulator(Integer.toString(myPredecessor));
			 	            myPredessorHash=genHash(myPreEmulator);
			 	           Log.e("2st condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		     			}
		     			else
		     			{
		     				
			 	           new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",myPort,Integer.toString(mySuccessor));
		     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,Integer.toString(mySuccessor),"-","-",msgReceivedArr[1],"-");
		     				mySuccessor=Integer.parseInt(msgReceivedArr[1].trim());
			 	            mySucEmulator=getEmulator(Integer.toString(mySuccessor));
			 	            mySuccessorHash=genHash(mySucEmulator);
			 	           Log.e("3rd condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		     			}
		     		}*/
		     		else
		     		{	
		     		if(myPortHash.compareTo(newNodeHash)>0)
		     		{
		     			if(newNodeHash.compareTo(myPredessorHash)>0)
		     			{
		     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",Integer.toString(myPredecessor),myPort);
		     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,Integer.toString(myPredecessor),"-","-","-",msgReceivedArr[1]);
			 	            myPredecessor=Integer.parseInt(msgReceivedArr[1].trim());
			 	            myPreEmulator=getEmulator(Integer.toString(myPredecessor));
			 	            myPredessorHash=genHash(myPreEmulator);
			 	           Log.e("4tt condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		     			}
		     			else
		     			{
		     				if(myPortHash.compareTo(myPredessorHash)<0)
		 	 				{
		     					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",Integer.toString(myPredecessor),myPort);
		     					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,Integer.toString(myPredecessor),"-","-","-",msgReceivedArr[1]);
				 	            myPredecessor=Integer.parseInt(msgReceivedArr[1].trim());
				 	            myPreEmulator=getEmulator(Integer.toString(myPredecessor));
				 	            myPredessorHash=genHash(myPreEmulator);
		 	 				}
		 	 				else
		 	 				{
		 	 				//message format:
			     	        	//type,fromPort,toPort,file_name, content, predecessor,successor
			     	        	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeJoin,msgReceivedArr[1],Integer.toString(mySuccessor),"-","-","-","-");
		 	 				}
		     				
		     			}
		     		}
		     		else
		     		{
		     			//if(myPortHash.compareTo(myPredessorHash)>0)
		     			//{
		     				if(myPortHash.compareTo(mySuccessorHash)>0)
		     				{
		     					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,msgReceivedArr[1],"-","-",myPort,Integer.toString(mySuccessor));
			     				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTyperesponse,myPort,Integer.toString(mySuccessor),"-","-",msgReceivedArr[1],"-");
				 	            mySuccessor=Integer.parseInt(msgReceivedArr[1].trim());
				 	            mySucEmulator=getEmulator(Integer.toString(mySuccessor));
				 	            mySuccessorHash=genHash(mySucEmulator);
				 	           Log.e("5tt condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		     				}
		     				else
		     				{
		     					//message format:
			     	        	//type,fromPort,toPort,file_name, content, predecessor,successor
			     	        	new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeJoin,msgReceivedArr[1],Integer.toString(mySuccessor),"-","-","-","-");
		     				}
		     			//}
		     			//else
		     		    //{
		     				//message format:
		     	        	//type,fromPort,toPort,file_name, content, predecessor,successor
		     	        	//new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeJoin,msgReceivedArr[1],Integer.toString(mySuccessor),"-","-","-","-");
		     			//}
		     		}
		     	}
		     	//}
		     		
		     	}
		     	
		     	if(msgReceivedArr[0].equalsIgnoreCase(msgTyperesponse))
		     	{
		     		if(!msgReceivedArr[5].equalsIgnoreCase("-"))
		     		{
		     			myPredecessor=Integer.parseInt(msgReceivedArr[5]);
		     			 myPreEmulator=getEmulator(Integer.toString(myPredecessor));
			 	            myPredessorHash=genHash(myPreEmulator);
			 	           
		     			
		     		}
		     		if(!msgReceivedArr[6].equalsIgnoreCase("-"))
		     		{
		     			mySuccessor=Integer.parseInt(msgReceivedArr[6]);
		     			 mySucEmulator=getEmulator(Integer.toString(mySuccessor));
			 	            mySuccessorHash=genHash(mySucEmulator);
		     		}
		     		 Log.e("response 1st condition",myPort+ "    "+ myPredecessor+"   "+mySuccessor);
		     	}
		     	
		     	if(msgReceivedArr[0].equalsIgnoreCase(msgTypekeyValue))
		     	{
		     		ContentValues keyValueToInsert = new ContentValues();
    		        
  		            keyValueToInsert.put("key",msgReceivedArr[3].trim() );
  		            keyValueToInsert.put("value",msgReceivedArr[4].trim());
  		            Uri newUri1 = context.getContentResolver().insert(
  		      		    providerUri,    // assume we already created a Uri object with our provider URI
  		      		    keyValueToInsert
  		      		    );    
		     	}
		     	if(msgReceivedArr[0].equalsIgnoreCase(msgTypeQuery))
		     	{
		     		 
		     		for(Entry<String, String> entry : msgRecObj.gDumpQueryResult.entrySet()) {
			               globalDB.put(entry.getKey(), entry.getValue());
			     		 }	
		     		if(msgRecObj.initiator.equalsIgnoreCase(myPort))
		     		{
		     			waitFlag=false;
		     		}
		     		else
		     		{
		     			Uri.Builder uriBuilder = new Uri.Builder();
		     			uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
		     			uriBuilder.scheme("content");
		     			Uri abc=uriBuilder.build();	
		     		Cursor resultCursor12 =context.getContentResolver().query(providerUri,null,lDumpQryType,null,null);
		     			//Uri.Builder uriBuilder = new Uri.Builder();
		     			//uriBuilder.authority("edu.buffalo.cse.cse486586.simpledht.provider");
		     			//uriBuilder.scheme("content");
		     			//Uri abc=uriBuilder.build();
		     			//String sql= "Select * from "+TableName;
		     			//Cursor resultCursor12= SQLitedb.rawQuery(sql, null);
		     	//	resultCursor.moveToFirst();
		     		
		     			while(resultCursor12.moveToNext()){

						int keyIndex = resultCursor12.getColumnIndex("key");
						int valueIndex = resultCursor12.getColumnIndex("value");

						String key = resultCursor12.getString(keyIndex);
						String value= resultCursor12.getString(valueIndex);
						globalDB.put(key, value);
						//send to successor
		     		}
			    		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgTypeQuery,myPort,Integer.toString(mySuccessor),"-","-","-","-",msgRecObj.initiator);
			    		
					
		     		}
		     		
		     	}
		     	if(msgRecObj.msgType.equalsIgnoreCase(msgTypeDelete))
		     	{
		     		context.getContentResolver().delete(providerUri, msgRecObj.receivedKey, null);
		     	}
		     	
				//publishProgress(br.readLine());
		     	
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
			//	e.printStackTrace();
				//Log.e(TAG, "error in displaying content");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
        	
         	
           return null;
        }

       
		protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
        	return;
        }
    }
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
        	int portToSend= Integer.parseInt(msgs[2]);
        	//String messageToSend=msgs[0]+" "+msgs[1]+" "+msgs[2]+" "+msgs[3]+" "+msgs[4]+" "+msgs[5]+" "+msgs[6];
        	MessageClass msgObj;
        	//if(msgs[0].equalsIgnoreCase(msgTypeDelete))
        	//{
        		//msgObj=new MessageClass(msgs[0], msgs[1], msgs[2], msgs[3], msgs[4], msgs[5], msgs[6],msgs[7],globalDB);
            	
        	//}
        	if(msgs[0].equalsIgnoreCase(msgTypeQuery))
        	{
        		msgObj=new MessageClass(msgs[0], msgs[1], msgs[2], msgs[3], msgs[4], msgs[5], msgs[6],msgs[7],globalDB);
        	}
        	else
        	{	
        		 msgObj=new MessageClass(msgs[0], msgs[1], msgs[2], msgs[3], msgs[4], msgs[5], msgs[6],"-",new HashMap<String, String>());
        	}
        	ObjectOutputStream outToServer=null;
        	try{
        		
                 Socket socket=new Socket("10.0.2.2",portToSend);
                // PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                    outToServer = new ObjectOutputStream(socket.getOutputStream());
                    
                    outToServer.writeObject(msgObj);
                    
                    outToServer.flush();
                    outToServer.close();
               //  pw.println(messageToSend); 
                // pw.flush(); 
                 //pw.close();
                        
                     //Thread.sleep(1000);
                     socket.close();
                       
                  // }
               } catch (UnknownHostException e) {
                  // Log.e(TAG, "ClientTask UnknownHostException");
               } catch (IOException e) {
                  // Log.e(TAG, "ClientTask socket IOException");
               }
        	
			return null;
    }

}
    private String getEmulator(String AVD) {
    	
    	if(AVD.equalsIgnoreCase(REMOTE_PORT0))
        {
        	return "5554";
        }
        if(AVD.equalsIgnoreCase(REMOTE_PORT1))
        {
        	return "5556";
        }
        if(AVD.equalsIgnoreCase(REMOTE_PORT2))
        {
        	return "5558";
        }
        if(AVD.equalsIgnoreCase(REMOTE_PORT3))
        {
        	return "5560";
        }
        if(AVD.equalsIgnoreCase(REMOTE_PORT4))
        {
        	return "5562";
        }
        return "";
	}
}
