package dynamiClientFramework.clients;

public interface Client {
	public void trySending(Sample sample);
	public void startClient();
	public void stopClient();
	public boolean isAlive();
	
	//TO DELETE
	public void setPS();
}
