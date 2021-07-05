package adaptiveProducerFramework.adaptiveProducer;

import java.io.Serializable;

import adaptiveProducerFramework.producers.exceptions.InvalidSampleTTLException;

public class Sample implements Serializable{

	private static final long serialVersionUID = 1L;
	private Serializable value;
	private long TTL;
	private long timestamp;

	/**
	 * Data wrapper object
	 * @param value data payload
	 * @param ttl time to live expressed in milliseconds; defines if a sample is still valid.
	 * @throws InvalidSampleTTLException ttl must be greater than 0
	 */
	public Sample(Serializable value, long ttl)throws InvalidSampleTTLException{
		if(ttl<=0) throw new InvalidSampleTTLException();
		this.value = value;
		this.TTL = ttl;
		timestamp = System.currentTimeMillis();
	};

	public String toString() {
		return value.toString();
	}

	/**
	 * Checks if a sample is expired or still valid, using ttl.
	 * @return true if a sample is still valid, false otherwise.
	 */
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
}
