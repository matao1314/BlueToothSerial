package com.ma.bluetoothserial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {
	// Debugging
	private static final String TAG = "BluetoothService";
	private static final boolean D = true;

	// Name for the SDP record when creating server socket
	private static final String NAME = "BluetoothService";

	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

	// Member fields
	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote device

	/**
	* Constructor. Prepares a new BluetoothChat session.
	* @param context The UI Activity Context
	* @param handler A Handler to send messages back to the UI Activity
	*/
	public BluetoothService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	* Set the current state of the chat connection
	* @param state An integer defining the current connection state
	*/
	private synchronized void setState(int state) {
		if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
			mState = state;

			// Give the new state to the Handler so the UI Activity can update
			mHandler.obtainMessage(Bluetooth.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	* Return the current connection state. */
	public synchronized int getState() {
		return mState;
	}

	/**
	* Start the chat service. Specifically start AcceptThread to begin a
	* session in listening (server) mode. Called by the Activity onResume() */
	public synchronized void start() {
		if (D) Log.d(TAG, "start");
	
		// 取消所有线程意图以创建一个连接
		//if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		
		// 取消当前线程来运行一个连接
		// if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		/*
		// 通过BluetoothServerSocket，开始一个线程
		if (mAcceptThread == null) {
		mAcceptThread = new AcceptThread();
		mAcceptThread.start();
		}*/
		//if(mConnectThread==null)
		// {
		// mConnectThread = new ConnectThread(null);
		// mConnectThread.start();
		// }
		setState(STATE_LISTEN);
	}

	/**
	* Start the ConnectThread to initiate a connection to a remote device.
	* @param device The BluetoothDevice to connect
	*/
	public synchronized void connect(BluetoothDevice device) {
		if (D) Log.d(TAG, "connect to: " + device);
	
		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	* 开始一个ConnectedThread，开始管理蓝牙连接
	* @param socket The BluetoothSocket on which the connection was made
	* @param device The BluetoothDevice that has been connected
	*/
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
		if (D) Log.d(TAG, "connected");
	
		// 取消已完成连接的线程
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		
		// Cancel any thread currently running a connection
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
	
		// Cancel the accept thread because we only want to connect to one device
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
	
		// 开启连接到服务器线程
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
	
		// 把MAC地址发回 UI Activity
		Message msg = mHandler.obtainMessage(Bluetooth.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(Bluetooth.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	
		setState(STATE_CONNECTED);
	}

	/**
	* Stop all threads
	*/
	public synchronized void stop() {
		if (D) Log.d(TAG, "stop");
		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
		setState(STATE_NONE);
	}
	
	/**
	* Write to the ConnectedThread in an unsynchronized manner
	* @param out The bytes to write
	* @see ConnectedThread#write(byte[])
	*/
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED) return;
			r = mConnectedThread;
		}
		// Perform the write unsynchronized
		r.write(out);
	}

	public void read(){
		ConnectedThread r;
		
		synchronized (this) {
			if(mState != STATE_CONNECTED) return;
			r = mConnectedThread;
		}
		r.run();
	}
	/**
	* 连接失败时在UI上显示.
	*/
	private void connectionFailed() {
		setState(STATE_LISTEN);
		
		// 失败信息发回Activity
		Message msg = mHandler.obtainMessage(Bluetooth.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(Bluetooth.TOAST, "无法连接到设备，请确认下位机蓝牙功能是否正常");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	/**
	* 丢失连接时在UI上显示.
	*/
	private void connectionLost() {
		setState(STATE_LISTEN);
	
		// 失败信息发回Activity
		Message msg = mHandler.obtainMessage(Bluetooth.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(Bluetooth.TOAST, "与目标设备连接丢失");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	/**
	* This thread runs while listening for incoming connections. It behaves
	* like a server-side client. It runs until a connection is accepted
	* (or until cancelled).
	*/
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;
		
		@SuppressWarnings("unused")
		public AcceptThread() {
			BluetoothServerSocket tmp = null;
	
			// Create a new listening server socket
			try {
				tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
	
		}
	
		public void run() {
			if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;
			
			// Listen to the server socket if we're not connected
			while (mState != STATE_CONNECTED) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket=mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}
	
				// 如果连接已经接受
				if (socket != null) {
					synchronized (BluetoothService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// 如果没有准备好或者无连接，则终止Socket
							try {
								socket.close();
							} catch (IOException e) {
								Log.e(TAG, "Could not close unwanted socket", e);
							}
							break;
						}
					}
				}
			}
			if (D) Log.i(TAG, "END mAcceptThread");
		}
		
		public void cancel() {
			if (D) Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}
	
	
	/**
	* This thread runs while attempting to make an outgoing connection
	* with a device. It runs straight through; the connection either
	* succeeds or fails.
	*/
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;
	
		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;
	
			//通过蓝牙设备从蓝牙连接中获取一个socket
	
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}
	
		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("连接线程");
	
			// 当关闭连接后，设备可见性设为不可见
			mAdapter.cancelDiscovery();
	
			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// 关闭Socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG, "unable to close() socket during connection failure", e2);
				}
				// Start the service over to restart listening mode
				BluetoothService.this.start();
				return;
			}
	
			// Reset the ConnectThread because we're done
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}
	
			// Start the connected thread
			connected(mmSocket, mmDevice);
		}
	
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
	
	/**
	 * 该线程在取得连接后执行
	 * It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
	
		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
	
			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
	
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
	
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;
			
			//保持监听
			while (true) {
				try {
					// 从InputStream读取
					bytes = mmInStream.read(buffer);
	
					// Send the obtained bytes to the UI Activity
					mHandler.obtainMessage(Bluetooth.MESSAGE_READ, bytes, -1, buffer)
					.sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}
	
		/**
		 * Write to the connected OutStream.
		 * @param buffer The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);
				// 把发送的内容在UI上显示
				mHandler.obtainMessage(Bluetooth.MESSAGE_WRITE, -1, -1, buffer)
				.sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}
	
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}