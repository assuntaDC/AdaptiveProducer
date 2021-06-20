package dynamiClientFramework.clients;

public interface DynamicClientCreator {
	/**
	 * Hide Dynamic Client creation.
	 * @param destination queue/topic to connect to-
	 * @param acceptorAddress Artemis valid acceptor address.
	 * @return a Client instance.
	 */
	public Client createDynamicClient(String destination, String acceptorAddress);
}
