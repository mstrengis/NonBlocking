/*
 * Copyright (C) 2013 mstrengis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lv.mstrengis.nonblocking;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class Client {
	ArrayList<ByteBuffer> mWriteBuffers = new ArrayList<ByteBuffer>();
	SocketChannel mSocket;
	HashMap<SelectionKey, Client> mClientsMap; 
	private Selector mSelector;
	private SocketListener mSocketListener;

	public Client(SocketChannel socket, Selector selector, HashMap<SelectionKey, Client> clientsMap, SocketListener listener) {
		mClientsMap = clientsMap;
		mSocket = socket;
		mSelector = selector;
		mSocketListener = listener;
		
		try{
			register(SelectionKey.OP_READ);
			mSocketListener.onConnected(this);
		}catch(Exception e){
			e.printStackTrace();
		}
	} 
	
	public void register(int op){
		SelectionKey key = mSocket.keyFor(mSelector);
		mClientsMap.remove(key);
		SelectionKey keyNew = null;
		try {
			keyNew = mSocket.register(mSelector, op);
			mClientsMap.put(keyNew, this);
		} catch (ClosedChannelException e) {
			disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(key!=null && !key.equals(keyNew)){
			key.cancel();
		}
	}
	
	public void write(ByteBuffer buffer){
		mWriteBuffers.add(buffer);
		register(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}

	public boolean read() {
		int read = 0;
		boolean error = false;
		java.nio.ByteBuffer bbuffer = java.nio.ByteBuffer.allocate(500);
		try {
			read = mSocket.read(bbuffer);
			bbuffer.flip();
			mSocketListener.onData(this, bbuffer, read);
		} catch (Exception e) {
			e.printStackTrace();
			error = true;
		}

		if (read < 1 && !error) {
			error = true;
		}

		if (error) {
			disconnect();
		}
		
		return !error;
	}
	
	public void disconnect(){
		SelectionKey key = mSocket.keyFor(mSelector);
		key.cancel();
		try {
			mSocket.socket().shutdownInput();
		} catch (Exception e) {}

		try {
			mSocket.socket().shutdownOutput();
		} catch (Exception e) {}
		
		mSocketListener.onDisconnect(this);
	}
}