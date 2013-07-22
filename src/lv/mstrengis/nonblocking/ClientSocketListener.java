package lv.mstrengis.nonblocking;

import java.nio.ByteBuffer;

public interface ClientSocketListener {
	public void onData(Socket client, ByteBuffer data, int bytesRead);
	public void onDisconnect(Socket client);
	public void onConnected(Socket client);
}
