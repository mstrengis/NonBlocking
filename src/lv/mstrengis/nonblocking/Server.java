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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Server {
	private final HashMap<SelectionKey, Client> mClientsMap = new HashMap<SelectionKey, Client>();
	private Selector mSelector;
	private SocketListener mSocketListener;
	public Server(int port, SocketListener socketListener){
		try {
			mSocketListener = socketListener;
			mSelector = Selector.open();
			ServerSocketChannel server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.socket().bind(new InetSocketAddress(port));
			server.register(mSelector, SelectionKey.OP_ACCEPT);
			
			System.out.println("");
			System.out.println("Server started");
			
			for (;;) {
				mSelector.select();
				Set<?> keys = mSelector.selectedKeys();
				Iterator<?> i = keys.iterator();
				while (i.hasNext()) {
					SelectionKey key = (SelectionKey) i.next();
					i.remove();
					
					if (key.isAcceptable()) {
						SocketChannel client = server.accept();
						client.configureBlocking(false);
						
						new Client(client, mSelector, mClientsMap, new SocketListener() {
							@Override
							public void onData(Client client, ByteBuffer data, int bytesRead) {
								mSocketListener.onData(client, data, bytesRead);
							}
							@Override
							public void onDisconnect(Client client) {
								mClientsMap.remove(client.mSocket.keyFor(mSelector));
								mSocketListener.onDisconnect(client);
							}
							@Override
							public void onConnected(Client client) {
								mClientsMap.put(client.mSocket.keyFor(mSelector), client);
								mSocketListener.onConnected(client);
							}
						});
						continue;
					}

					if (key.isReadable()) {
						Client client = mClientsMap.get(key);
						if(client != null){
							client.read();
						}
						continue; 
					}
					
					if(key.isWritable()){
						Client client = mClientsMap.get(key);
						if(client != null){
							if(client.mWriteBuffers.size() > 0){
								ByteBuffer[] stockArr = new ByteBuffer[mClientsMap.get(key).mWriteBuffers.size()];
							    stockArr = mClientsMap.get(key).mWriteBuffers.toArray(stockArr);
							    client.mSocket.write(stockArr);
							    client.mWriteBuffers.clear();
							    
							    client.register(SelectionKey.OP_READ);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
}