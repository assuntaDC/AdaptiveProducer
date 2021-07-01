package dynamiClientFramework.clients;

public interface Client {
	/**
	 * Try sending a message taking into account queue congestion and intervening with predefined strategies.
	 * @param sample Data to send
	 */
	public void trySending(Sample sample);
	
	/**
	 * Connect client to topic/queue.
	 */
	public void startClient();
	
	/**
	 * Disconnect client from topic/queue.
	 */
	public void stopClient();
	
}
