package dynamiClientFramework.clients;

public interface DynamicClientCreator {
	public Client createDynamicClient(String destination, String acceptorAddress);
}
