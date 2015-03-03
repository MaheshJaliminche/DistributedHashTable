package edu.buffalo.cse.cse486586.simpledht;


import android.R.string;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {
	Uri providerUri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
	public static final String lDumpQryType="@";
	public static final String gDumpQryType="*";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        
        final Button ldump = (Button) findViewById(R.id.button1);

        ldump.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	Cursor resultCursor =getContentResolver().query(providerUri,null,lDumpQryType,null,null);
            	 TextView tv1 = (TextView) findViewById(R.id.textView1);
                 tv1.setMovementMethod(new ScrollingMovementMethod());
            	while(resultCursor.moveToNext()){

					int keyIndex = resultCursor.getColumnIndex("key");
					int valueIndex = resultCursor.getColumnIndex("value");

					String key = resultCursor.getString(keyIndex);
					String value= resultCursor.getString(valueIndex);
					
					tv1.append("\t"+key+"---"+value+"\n");

					//	System.out.println("PRINTING: "+"< "+key+" : "+value+" >");
						
				}
				

            }
        });
        final Button gdump = (Button) findViewById(R.id.button1);

        gdump.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	
            	Cursor resultCursor =getContentResolver().query(providerUri,null,gDumpQryType,null,null);
            	 TextView tv1 = (TextView) findViewById(R.id.textView1);
                 tv1.setMovementMethod(new ScrollingMovementMethod());
            	while(resultCursor.moveToNext()){

					int keyIndex = resultCursor.getColumnIndex("key");
					int valueIndex = resultCursor.getColumnIndex("value");

					String key = resultCursor.getString(keyIndex);
					String value= resultCursor.getString(valueIndex);
					
					tv1.append("\t"+key+"---"+value+"\n");

					//	System.out.println("PRINTING: "+"< "+key+" : "+value+" >");
						
				}
				

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

}
