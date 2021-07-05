package dynamiClientFramework.clients;

public class DMQTTClientCreator implements DynamicClientCreator {

	@Override
	public Client createDynamicClient(String destination, String acceptorAddress) {
		return new DynamicMQTTClient(destination, acceptorAddress);
	}

}
