package com.barobot;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BarobotMain extends Activity {
    // Layout Views
	private static BarobotMain instance;
    private ListView mConversationView;

    // Array adapter for the conversation thread
    ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		instance = this;
        // Set up the window layout
        setContentView(R.layout.main);

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        /*
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener( new TextView.OnEditorActionListener() {
	        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
	            // If the action is a key-up event on the return key, send the message
		            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
		            	Constant.log(Constant.TAG, "END onEditorAction+++");
		                String message = view.getText().toString();
		                queue.getInstance().send(message);
		            }
		            Constant.log(Constant.TAG, "END onEditorAction");
	            return true;
	        }
	    });
	    */

		boolean res = queue.getInstance().connectADB();
		if(res == false ){
			Constant.log("Seeeduino ADK", "Unable to start TCP server" );
			System.exit(-1);
		}
		this.runTimer();
    }

    // test okresowego wywo�ywania polece�
    private void runTimer() {
		// TODO Auto-generated method stub
    	TimerTask scanTask;
    	final Handler handler = new Handler();
    	Timer t = new Timer();
    	scanTask = new TimerTask() {
    	    public void run() {
    	            handler.post(new Runnable() {
	                    public void run() {
	                    	Constant.log("RUNNABLE", "TICK" );
	                    }
    	           });
    	    }};
    	t.schedule(scanTask, 300, 5000);
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
             if( queue.getInstance().checkBT() == false ){
                Toast.makeText(this, "Bluetooth jest niedost�pny", Toast.LENGTH_LONG).show();
                finish();
            }
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, Constant.REQUEST_CONNECT_DEVICE_SECURE);
            return true;

	    case R.id.debug_mode_window:
            serverIntent = new Intent(this, DebugWindow.class);
            startActivityForResult(serverIntent, Constant.REQUEST_BEBUG_WINDOW);
	        return true;
	    }

        return false;
    }
    @Override
    public void onStart() {
        super.onStart();
        if( queue.getInstance().checkBT() != false ){
	        int res = queue.startBt();
	        if( res == 34){		// jesli jest wlaczony
	        	 queue.getInstance().setupBT( this);
	        }else if( res == 12){	// jesli wymaga wlaczenia to wroci do onActivityResult
	            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableIntent, Constant.REQUEST_ENABLE_BT);
	        }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        queue.resume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        queue.stop();
    }

    // The Handler that gets information back from the BluetoothChatService
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case Constant.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case Constant.STATE_CONNECTED:
                    mConversationArrayAdapter.clear();
                    break;
                case Constant.STATE_CONNECTING:
                    break;
                case Constant.STATE_LISTEN:
                case Constant.STATE_NONE:
                    break;
                }
                break;
            case Constant.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Constant.log(Constant.TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case Constant.REQUEST_BEBUG_WINDOW:
        	Constant.log(Constant.TAG, "REQUEST_BEBUG_WINDOW");
            break;

        case Constant.REQUEST_CONNECT_DEVICE_SECURE:
        	Constant.log(Constant.TAG, "REQUEST_CONNECT_DEVICE_SECURE");
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            	queue.connectBTDevice(data);
            }
            break;

        case Constant.REQUEST_ENABLE_BT:
        	Constant.log(Constant.TAG, "REQUEST_ENABLE_BT " + resultCode);
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up session
                queue.getInstance().setupBT( this);
            } else {
                // User did not enable Bluetooth or an error occurred
                Constant.log(Constant.TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	//Any update to UI can not be carried out in a non UI thread like the one used
	//for Server. Hence runOnUIThread is used.
	public void setText(final int target, final String result) {
		if(result!=null){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					TextView bt = (TextView) findViewById(target);	    	
					bt.setText(result);
				}
			});
		}
	}
	public static BarobotMain getInstance(){
		return instance;
	}
	/*
    @Override
    public synchronized void onPause() {
        super.onPause();
        Constant.log(Constant.TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        Constant.log(Constant.TAG, "-- ON STOP --");
    }
*/
	public void addToList3(final String string) {
		final BarobotMain parent = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				parent.mConversationArrayAdapter.add("robot:  " + string );
			}
		});
			
	}
	
	
}