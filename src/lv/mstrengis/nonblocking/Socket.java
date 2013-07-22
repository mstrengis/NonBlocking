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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Socket {
	private Selector mSelector;
	private SocketChannel mSocket;
	private ArrayList<ByteBuffer> mWriteBuffers = new ArrayList<ByteBuffer>();
	private ClientSocketListener mSocketListener;
	public Socket(String host, int port, ClientSocketListener socketListener) throws IOException{
		mSocketListener = socketListener;
		mSelector = Selector.open();
		mSocket = SocketChannel.open();
		mSocket.configureBlocking(false);
		mSocket.connect(new InetSocketAddress(host, port));
		register(SelectionKey.OP_READ);
		
		mSocketListener.onConnected(this);
		
		for(;;){
			mSelector.select();
			Set<?> keys = mSelector.selectedKeys();
			Iterator<?> i = keys.iterator();
			while (i.hasNext()) {
				SelectionKey key = (SelectionKey) i.next();
				i.remove();
				
				if (key.isReadable()) {
					ByteBuffer buffer = ByteBuffer.allocate(500);
					int bytesRead = mSocket.read(buffer);
					mSocketListener.onData(this, buffer, bytesRead);
					continue; 
				}
				
				if(key.isWritable()){
					if(mWriteBuffers.size() > 0){
						ByteBuffer[] stockArr = new ByteBuffer[mWriteBuffers.size()];
					    stockArr = mWriteBuffers.toArray(stockArr);
					    mSocket.write(stockArr);
					    mWriteBuffers.clear();
					    
					    register(SelectionKey.OP_READ);
					}
				}
			}
		}
	}
	
	public void write(ByteBuffer buffer){
		mWriteBuffers.add(buffer);
		register(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	}
	
	private void register(int op){
		SelectionKey key = mSocket.keyFor(mSelector);
		SelectionKey keyNew = null;
		try {
			keyNew = mSocket.register(mSelector, op);
		} catch (ClosedChannelException e) {
			disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(key!=null && !key.equals(keyNew)){
			key.cancel();
		}
	}
	
	public void disconnect(){
		mSocketListener.onDisconnect(this);
		try{
			mSocket.socket().shutdownInput();
		}catch(Exception e){
			
		}
		
		try{
			mSocket.socket().shutdownOutput();
		}catch(Exception e){
			
		}
		
		try {
			mSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
