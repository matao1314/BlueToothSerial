package com.ma.bluetoothserial;

import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
/**
* ������Ҫ�Ļ��ʾ��ǰ����Ự��
*/
public class Bluetooth extends Activity {
    // Debugging
    private static final String TAG = "BluetoothSmartHome";
    private static final boolean D = true;
    
    // �� BluetoothChatService ��������͵���Ϣ����
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String PREFS_NAME = "Bluetooth";
    public static final String DEVICE_NAME = "DeviceName";
    public static final String TOAST = "toast";
   
    // Intent request codes�����������
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    // Layout Views������ͼ
    // private TextView mTitle;
	private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    
    // ���ӵ��豸����
    private String mConnectedDeviceName = null;
    // ��������������
    private BluetoothAdapter mBluetoothAdapter = null;
    //���ͷ���
    private BluetoothService mBluetoothService = null;
    
    public void init() {
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        // ��ȡ��������������
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // �������������ô��������֧��
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "�����豸�����ã�", Toast.LENGTH_LONG).show();
            //......................
            finish();
            return;
        }
        
        // ��ת�������б�ҳ��
        Intent serverIntent = new Intent(this, DeviceList.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        
        // ���BTδ������������������
        // Ȼ�󽫵����ڼ� onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
        	Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBluetoothService == null) 
            	setupControl();
        }
    }
   
    @Override
    public synchronized void onResume() {
    	super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        if (mBluetoothService != null) {
            
        	//ֻ�е���ʼ��STATE_NONE��������û��������
        	if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
        		// ���������豸����
        		mBluetoothService.start();
        	}
        }
    }
    
    private void setupControl() {
        //��ʼ��ִ���������� BluetoothChatService
    	mBluetoothService = new BluetoothService(this, mHandler);
    }
    
    @Override
    public synchronized void onPause() {
    	super.onPause();
    	if(D) Log.e(TAG, "- ON PAUSE -");
    }
    
    @Override
    public void onStop() {
    	super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }
    @Override
    public void onDestroy() {
    	super.onDestroy();   	
    	//ֹͣ�����������
    	if (mBluetoothService != null)
    	{
    		mBluetoothService.stop();
    	}
    	
    	if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    @SuppressWarnings("unused")
	private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    /**
     * ��������
     * @param message:A string of text to send.
     */
    @SuppressWarnings("unused")
	private void sendMessage(String message) {
        // ����Ƿ���������
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // �����ʲôʵ�ʷ���
        if (message.length() > 0) {
            // ��ȡ��Ϣ�ֽڲ�д�� BluetoothChatService
        	byte[] send = message.getBytes();  
            //stringToChar( );
        	mBluetoothService.write(send);       
        }
    }
    
    // �ǴӸ� BluetoothChatService ���»�ȡ��Ϣ��ʾ��UI�Ĵ������
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D)  Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    //mTitle.setText(R.string.title_connected_to);
                    //mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothService.STATE_CONNECTING:
                    //mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothService.STATE_LISTEN:
                case BluetoothService.STATE_NONE:
                    //mTitle.setText(R.string.title_not_connected);
                    BluetoothAdapter cwjBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    
                    if(cwjBluetoothAdapter == null)
                    {
                    	Toast.makeText(Bluetooth.this,
                    			"����û���ҵ�����Ӳ����������������",Toast.LENGTH_SHORT)
                    			.show();
                    }
                    if (!cwjBluetoothAdapter.isEnabled()) 
                    {
                    	Intent TurnOnBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    	startActivityForResult(TurnOnBtIntent, REQUEST_ENABLE_BT);
                    }
                    break;
                }
                break;
            case MESSAGE_WRITE:
            
                break;
            case MESSAGE_READ:
            
                break;
            case MESSAGE_DEVICE_NAME:
                //���������豸����
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "�ѳɹ����ӵ��� "
                               + mConnectedDeviceName + "���Կ�ʼ�����ˣ�", Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D)  Log.d(TAG, "onActivityResult " + resultCode);
        
        switch (requestCode) 
        {
        case REQUEST_CONNECT_DEVICE:  
        	if (resultCode == Activity.RESULT_OK) 
        	{
        		// ��ȡ�豸��ַ
        		String address = data.getExtras()
                                     .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
        		Toast.makeText(this, "���豸MAC��ַΪ--->"+address, Toast.LENGTH_SHORT).show();
        		//��ȡ�豸����
        		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                try 
                {
                	device.createRfcommSocketToServiceRecord(uuid);
                } 
                catch (IOException e1) 
                {
                	e1.printStackTrace();
                }
                // Attempt to connect to the device
                mBluetoothService.connect(device);   
        	}
        	break;
        case REQUEST_ENABLE_BT:
        	//���������������󷵻�ʱ
        	if (resultCode == Activity.RESULT_OK) 
        	{
        		// ������������
        		setupControl();
        	}
        	else 
        	{
        		// �û�û������������������
        		Log.d(TAG, "BT not enabled");
        		Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
        		finish();
        	}
        }
    }
    
    public static char[] stringToChar(String str) {
    	char[] sendStr;
    	String[] itemStr = str.split(" ");
    	sendStr = new char[itemStr.length];
    	for (int i = 0; i < itemStr.length; i++) 
    	{
    		char ch = (char) Integer.parseInt(itemStr[i], 16);
    		sendStr[i] = ch;
    	}
    	return sendStr;
    	
    }

    public static String bytesToHexString(byte[] bytes) {
        String result = "";
        for (int i = 0; i < bytes.length; i++) {
            String hexString = Integer.toHexString(bytes[i] & 0xFF);
            if (hexString.length() == 1) {
                hexString = '0' + hexString;
            }
            result += hexString.toUpperCase();
        }
        return result;
    }   
    
    
    public ListView getmConversationView() {
		return mConversationView;
	}

 
    public EditText getmOutEditText(){
		return mOutEditText;
    }
    
	public void setmOutEditText(EditText mOutEditText) {
		this.mOutEditText = mOutEditText;
	}

	public Button getmSendButton() {
		return mSendButton;
	}

	public void setmSendButton(Button mSendButton) {
		this.mSendButton = mSendButton;
	}
}