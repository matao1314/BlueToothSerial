package com.ma.bluetoothserial;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
* 此活动将作为一个对话框出现。 它列出了配对的所有设备和
* 在区域中的后发现中检测到的设备。 当选择一个设备
* 通过在的用户设备的 MAC 地址发送回父
* Activity in the result Intent.。
*/
public class DeviceList extends Activity {
	// Debugging
	private static final String TAG = "DeviceList";
	private static final boolean D = true;

	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(D) Log.e(TAG, "++ ON Create ++");		
		setContentView(R.layout.device_list);		
		// Set result CANCELED incase the user backs out
		setResult(Activity.RESULT_CANCELED);
		// Initialize the button to perform device discovery
		Button scanButton = (Button) findViewById(R.id.button_scan);
		
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doDiscovery();//Search bluetooth devices
				v.setVisibility(View.GONE);
			}
		});

		// 一个Adapter是已配对的，一个是未配对的
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);//Already paired
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);//Not paired

		// 把已配对设备放到ListView 
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		//把新发现的设备放到 ListView
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
		
		// Get the local Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			}
		} else {
			String noDevices = getResources().getText(R.string.none_paired).toString();
			mPairedDevicesArrayAdapter.add(noDevices);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Make sure we're not doing discovery anymore
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}
		// Unregister broadcast listeners
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void doDiscovery() {
		if (D) Log.d(TAG, "doDiscovery()");
		// Indicate scanning in the title
		setProgressBarIndeterminateVisibility(true);
		//setTitle(R.string.scanning);
		// Turn on sub-title for new devices
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}
		// Request discover from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// 取消搜索，准备选择设备进行连接
			mBtAdapter.cancelDiscovery();

			// 获取设备的MAC地址
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			// 把MAC address用Intent回传到主Activity
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

			// 结束当前Activity并回传MAC
			setResult(Activity.RESULT_OK, intent);
			finish();
		}
	};
	//BroadcastReceiver监听设备，当设备发现后，重新设置TITLE
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);					
				// If it's already paired, skip it, because it's been listed already					
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {						
					mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				}
				// When discovery is finished, change the Activity title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};
	
	
	public void btn_device_back(View v) {     //标题栏 返回按钮
      	this.finish();
      }
	
}