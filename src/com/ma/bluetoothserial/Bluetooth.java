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
* 这是主要的活动显示当前聊天会话。
*/
public class Bluetooth extends Activity {
    // Debugging
    private static final String TAG = "BluetoothSmartHome";
    private static final boolean D = true;
    
    // 从 BluetoothChatService 处理程序发送的消息类型
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String PREFS_NAME = "Bluetooth";
    public static final String DEVICE_NAME = "DeviceName";
    public static final String TOAST = "toast";
   
    // Intent request codes意向请求代码
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    // Layout Views布局视图
    // private TextView mTitle;
	private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    
    // 连接的设备名称
    private String mConnectedDeviceName = null;
    // 本地蓝牙适配器
    private BluetoothAdapter mBluetoothAdapter = null;
    //发送服务
    private BluetoothService mBluetoothService = null;
    
    public void init() {
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 如果空适配器那么蓝牙不受支持
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙设备不可用！", Toast.LENGTH_LONG).show();
            //......................
            finish();
            return;
        }
        
        // 跳转到蓝牙列表页面
        Intent serverIntent = new Intent(this, DeviceList.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        
        // 如果BT未启动，请求启用它。
        // 然后将调用期间 onActivityResult
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
            
        	//只有当开始是STATE_NONE，表明还没真正连接
        	if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
        		// 启动蓝牙设备服务
        		mBluetoothService.start();
        	}
        }
    }
    
    private void setupControl() {
        //初始化执行蓝牙连接 BluetoothChatService
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
    	//停止蓝牙聊天服务
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
     * 发送数据
     * @param message:A string of text to send.
     */
    @SuppressWarnings("unused")
	private void sendMessage(String message) {
        // 检查是否真正连上
        if (mBluetoothService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // 检查有什么实际发送
        if (message.length() > 0) {
            // 获取消息字节并写入 BluetoothChatService
        	byte[] send = message.getBytes();  
            //stringToChar( );
        	mBluetoothService.write(send);       
        }
    }
    
    // 是从该 BluetoothChatService 重新获取信息显示在UI的处理程序
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
                    			"本机没有找到蓝牙硬件或驱动存在问题",Toast.LENGTH_SHORT)
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
                //保存连接设备名称
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "已成功连接到： "
                               + mConnectedDeviceName + "可以开始操纵了！", Toast.LENGTH_SHORT).show();
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
        		// 获取设备地址
        		String address = data.getExtras()
                                     .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
        		Toast.makeText(this, "该设备MAC地址为--->"+address, Toast.LENGTH_SHORT).show();
        		//获取设备对象
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
        	//当启用蓝牙的请求返回时
        	if (resultCode == Activity.RESULT_OK) 
        	{
        		// 蓝牙现已启用
        		setupControl();
        	}
        	else 
        	{
        		// 用户没有启用蓝牙或发生错误
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