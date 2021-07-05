package dynamiClientFramework.clients;

public class DJMSClientCreator implements DynamicClientCreator {

	@Override
	public Client createDynamicClient(String destination, String acceptorAddress) {
		return new DynamicJMSClient(destination, acceptorAddress);
	}

}
