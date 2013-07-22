NonBlocking
===========

Easy to use nonblocking socket server and non blocking socket client. 
This example shows how to use server side when 1 socket command consists of
4bytes unsigned integer size of data string and the rest of it is a data string you can create your own 
protocol if you want.


Usage:
______

  	new Server(9510, new SocketListener(){
		  	private byte[] packetBuffer;
				private int DEFAULT_BUFFER_SIZE = 4, nextSize = DEFAULT_BUFFER_SIZE, packetBufferSize = 0, packetBufferLength = 0, needToRead = 0;
				@Override
				public void onData(Client client, ByteBuffer data, int bytesRead) {
					// f.e your command consists of unsigned integer as length of command and rest of the command is just a json string
		  			boolean error = false;
					java.nio.ByteBuffer bbuffer = java.nio.ByteBuffer.allocate(nextSize);
					byte[] buffer = null;
					if (!error) {
						buffer = new byte[bytesRead];
						bbuffer.get(buffer);
						
						if (packetBufferSize < packetBufferLength + bytesRead) {
							byte[] tmp = new byte[packetBufferLength + bytesRead];
							packetBufferSize = packetBufferLength + bytesRead;
							if (packetBufferLength > 0) {
								try {
									System.arraycopy(packetBuffer, 0, tmp, 0, packetBufferLength);
								} catch (Exception e) {
									e.printStackTrace();
									error = true;
								}
							}
							packetBuffer = tmp;
						}
					}
		
					if (packetBuffer == null) {
						error = true;
					}
		
					if (!error) {
						System.arraycopy(buffer, 0, packetBuffer, packetBufferLength, bytesRead);
						packetBufferLength += bytesRead;
					}
					
					if (!error) {
						try {
							while (packetBufferLength > 4) {
								long i1 = (int) (packetBuffer[0]) & 0xFF;
								long i2 = (int) (packetBuffer[1]) & 0xFF;
								long i3 = (int) (packetBuffer[2]) & 0xFF;
								long i4 = (int) (packetBuffer[3]) & 0xFF;
								needToRead = (int) ((i4 << 24) + (i3 << 16) + (i2 << 8) + i1) + 4;
								if (needToRead <= packetBufferLength && needToRead > 0 && packetBuffer != null) {
									byte[] work = new byte[needToRead];
									System.arraycopy(packetBuffer, 4, work, 0, needToRead - 4);
									String dataFromClient = new String(work, 0, needToRead - 4, "UTF-8");
									//use JSONObject notify connections save to db or whatever you want
									nextSize = DEFAULT_BUFFER_SIZE;
									if (!error) {
										if (packetBufferLength > 0) {
											packetBufferLength -= needToRead;
											System.arraycopy(packetBuffer, needToRead, packetBuffer, 0, packetBufferLength);
										}
									}
								} else {
									nextSize = needToRead - packetBufferLength;
									break;
								}
							}
						} catch (Exception e) {
							error = true;
								e.printStackTrace();
							}
						}
			
						if (error) {
							client.disconnect();
						}
					}
			
					@Override
					public void onDisconnect(Client client) {
					  // TODO notify other connecitons this client is disconnected and remove from your previously defined clients ArrayList
					}
			
					@Override
					public void onConnected(Client client) {
					  // TODO save your clients in some kind of ArrayList<Client> clients
					}
			});
			
			
_________
Writing to clients:

			String queue = "{\"rq\":\"ping\"}"; //Just a sample use JSONObject().put("rq", "ping").toString() instead
			
			byte[] bytes = queue.getBytes();
			ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4); 
			int length = bytes.length; 
			
			//first four bytes is length of a command as unsigned integer
			buffer.put((byte) (length & 0xFF));
			buffer.put((byte) ((length >>> 8) & 0xFF));
			buffer.put((byte) ((length >>> 16) & 0xFF));
			buffer.put((byte) ((length >>> 24) & 0xFF));
			
			//rest of command is data
			buffer.put(bytes);
			
			buffer.flip(); //dont forget about flipping data before writing it to client

			client.write(buffer);
