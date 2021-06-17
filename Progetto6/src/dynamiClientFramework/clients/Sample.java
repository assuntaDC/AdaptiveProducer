package dynamiClientFramework.clients;

import java.io.Serializable;
import dynamiClientFramework.clients.exceptions.InvalidSampleTTLException;

public class Sample implements Serializable{

	private static final long serialVersionUID = 1L;
	private Serializable value;
	private long TTL;
	private long timestamp;
	
	public Sample(Serializable value, long ttl)throws InvalidSampleTTLException{
		if(ttl<=0) throw new InvalidSampleTTLException();
		this.value = value;
		this.TTL = ttl;
		timestamp = System.currentTimeMillis();
	};
		
	public String toString() {
		return value.toString();
	}
	
	public boolean isValid() {
		boolean valid=false;
		if((System.currentTimeMillis() - timestamp)<=TTL) valid=true;
		return valid;
	}

	public Serializable getValue() {
		return value;
	}

	public void setValue(Serializable value) {
		this.value = value;
	}

	public long getTTL() {
		return TTL;
	}

	public void setTTL(long ttl) {
		TTL = ttl;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
