package adaptiveProducerFramework.producers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import adaptiveProducerFramework.producers.exceptions.InvalidPropertyException;
import adaptiveProducerFramework.producers.exceptions.InvalidSampleTTLException;


public abstract class AdaptiveProducer implements Producer{

	private enum State {NORMAL, CONGESTED};
	private enum Strategy {DROP, AGGREGABLE};
	private enum Operations {MIN, MAX, SUM, MEAN};

	private int EPSILON;
	private int BUFFER_DIM;
	private int CONSECUTIVE_DECREASES;
	private boolean aggressiveStrategy;
	private int MAX_BUFFER_DIM;
	private long TTL;	
	private long POLLING_PERIOD;
	private State status;
	private Strategy strategy;
	private Operations operation;
	protected List<Sample> sendBuffer;
	private	PollingService ps; 
	private int currMessageCount, decreaseCount;
	private String destination, acceptorAddress;

	/**
	 * Create a producer instance and set up its properties.
	 * @param destination queue/topic name to connect to
	 * @param acceptorAddress valid Artemis acceptor address
	 */
	public AdaptiveProducer(String destination, String acceptorAddress) {
		this.destination=destination;
		this.acceptorAddress=acceptorAddress;
		loadProperties();
		MAX_BUFFER_DIM = BUFFER_DIM * 2;
		aggressiveStrategy=false;
		sendBuffer = new ArrayList<Sample>();
		status = State.NORMAL;
		currMessageCount = 0;
		decreaseCount = 0;
		ps = createPollingService(POLLING_PERIOD);
	}


	//PUBLIC INTERFACE*****************************/
	public void trySending(Sample sample){
		switch(status) {
		case NORMAL: 
			if(!sendBuffer.isEmpty()) emptyBuffer();
			sendMessage(sample);
			break;
		case CONGESTED:
			handleStrategy(sample);
			break;
		}
	}

	public void startProducer() {
		startConnection();
		ps.startPolling();
	}

	public void stopProducer() {
		//Send all messages left
		while(!sendBuffer.isEmpty()) emptyBuffer();
		ps.stopPolling();
		closeConnection();
	}
	//***********************************************/

	/**
	 * Send a message based on the connector chosen by the producer.
	 * @param sample Data to send
	 */
	protected abstract void sendMessage(Sample sample);

	/**
	 * Start connection to topic/queue based on the connector chosen by the producer.
	 */
	protected abstract void startConnection();

	/**
	 * Close connection to topic/queue based on the connector chosen by the producer.
	 */
	protected abstract void closeConnection();
	//*********************************************/


	/**
	 * Create a PollingService object to check and update queue status.
	 * @param pollingPeriod expressed in milliseconds.
	 * @return PollingService instance.
	 */
	private PollingService createPollingService(long pollingPeriod) {
		return new PollingService(this, pollingPeriod);
	}

	/**
	 * Load properties from configuration file located in resources.
	 */
	private void loadProperties() {
		String filename = "config.properties";
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
			Properties prop = new Properties();
			if (input == null) throw new FileNotFoundException();

			prop.load(input);
			EPSILON = Integer.parseInt(prop.getProperty("epsilon"));
			if(EPSILON<=0) throw new InvalidPropertyException("Epsilon must be greater than 0.");

			CONSECUTIVE_DECREASES = Integer.parseInt(prop.getProperty("consecutiveDecreases"));
			if(CONSECUTIVE_DECREASES<=0) throw new InvalidPropertyException("ConsecutiveDecreases must be greater than 0.");

			BUFFER_DIM = Integer.parseInt(prop.getProperty("bufferDim"));
			if(BUFFER_DIM<=0) throw new InvalidPropertyException("Buffer dim must be greater than 0.");

			POLLING_PERIOD = Long.parseLong(prop.getProperty("pollingPeriod"));
			if(POLLING_PERIOD<=0) throw new InvalidPropertyException("Polling period must be greater than 0.");

			TTL = Long.parseLong(prop.getProperty("TTL"));
			if(TTL<=0) throw new InvalidPropertyException("Sample TTL must be greater than 0.");

			switch(prop.getProperty("strategy")) {
			case "AGGREGABLE": strategy=Strategy.AGGREGABLE; break;
			case "DROP": strategy= Strategy.DROP; break;
			default: throw new InvalidPropertyException("Strategy malformed. Use: AGGREGABLE or DROP."); 
			}

			switch(prop.getProperty("aggregationType")) {
			case "MIN": operation=Operations.MIN; break;
			case "MAX": operation=Operations.MAX; break;
			case "SUM": operation=Operations.SUM; break;
			case "MEAN": operation=Operations.MEAN; break;
			default: throw new InvalidPropertyException("Aggregation type malformed. Use: MAX, MIN, SUM or MEAN."); 
			}  
		} catch (FileNotFoundException ex) {
			System.err.println("Sorry, unable to find " + filename);
		} catch (IOException e) {
			System.err.println("Property file malformed.");
		} catch (InvalidPropertyException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Sets congestion status checking message count and history variation.
	 * @param number of current messages within the destination queue.
	 */
	public void updateQueueStatus(int messageCount) {	
		int delta = (messageCount - currMessageCount);
		currMessageCount=messageCount;
		switch(status) {
		case NORMAL: 
			if(messageCount>EPSILON) status=State.CONGESTED;
			decreaseCount = 0;
			break;
		case CONGESTED:
			if(delta<=0) {
				decreaseCount++;
				if(decreaseCount >= CONSECUTIVE_DECREASES) {
					if(messageCount<=EPSILON) status=State.NORMAL;
					aggressiveStrategy=false;	
				}
			}
			else if(delta>0) {
				aggressiveStrategy=true;
				decreaseCount = 0;
			}

			break;
		}

		//If strategy must be more aggressive, we need to extend buffer dimension.
		//If strategy must be less aggressive, we need to reduce buffer dimension;
		//before resizng it, we must be sure there are no more sample than buffer half dim.
		//In this case, we have to aggregate sample or empty the buffer, otherwise we can reduce its length.
		if(aggressiveStrategy && BUFFER_DIM<MAX_BUFFER_DIM) BUFFER_DIM = MAX_BUFFER_DIM;
		else if(!aggressiveStrategy && BUFFER_DIM==MAX_BUFFER_DIM) {
			if(sendBuffer.size()==BUFFER_DIM) {
				if(strategy==Strategy.AGGREGABLE) aggregate();
				else emptyBuffer();
			}
			BUFFER_DIM /=2;
		}
	}

	/**
	 * Execute the chosen strategy to reduce congestion.
	 * @param sample
	 */
	private void handleStrategy(Sample sample) {
		switch(strategy) {
		case DROP: 
			drop(sample);
			break;
		case AGGREGABLE:
			sendBuffer.add(sample);
			aggregate();
			break;
		}
	}

	/**
	 * Send messages left into sendBuffer, then clear it.
	 */
	private void emptyBuffer() {
		//If message number is smaller than half of BUFFER_DIM, send all of them.
		int i;
		if(sendBuffer.size()<=BUFFER_DIM/2) {
			for(Sample sample: sendBuffer) {
				if(sample.isValid()) sendMessage(sample);
			}
			sendBuffer.clear();
		}
		//if message number is greater than half of BUFFER_DIM, send some of them
		else {
			for(i=0; i<BUFFER_DIM/2; i++){
				Sample sample = sendBuffer.get(i);
				if(sample!=null && sample.isValid()) sendMessage(sample);
			}
			for(int j = i; j>=0; j--) sendBuffer.remove(j);
		}
	} 


	/**
	 * Store messages into sendBuffer until it's not full, then try to send valid ones.  
	 * @param sample Data to store in sendBuffer
	 */
	private void drop(Sample sample) {
		if(sendBuffer.size() < BUFFER_DIM) sendBuffer.add(sample);
		else {
			emptyBuffer();
			sendBuffer.add(sample);
		}
	}

	/**
	 * Store messages into sendBuffer until it's not full, then compute aggregation on valid ones.
	 * @param sample Data to aggregate
	 */
	private void aggregate() {
		if(sendBuffer.size()>=BUFFER_DIM){
			Serializable value = computeAggregation();
			if(value!=null) {
				try {
					sendMessage(new Sample(value, TTL));
				} catch (InvalidSampleTTLException e) {
					e.printStackTrace();
				}
			}
			sendBuffer.clear();
		}
	}

	/**
	 * Compute aggregation operation suggested by user.
	 * @return
	 */
	private Serializable computeAggregation() {
		switch(operation) {
		case MIN: return computeMin();
		case MAX: return computeMax();
		case SUM: return computeSum();
		case MEAN: return computeMean();
		default: return null;
		}
	}

	/**
	 * Send valid samples with lowest value as aggregation result.
	 * @return Lowest value among valid samples.
	 */
	private Serializable computeMin() {
		double min = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value < min) min = value;
				validCount++;
			}
		}
		if(validCount>0)return min;
		else return null;
	}

	/**
	 * Send valid samples with highest value as aggregation result.
	 * @return Highest value among valid samples.
	 */
	private Serializable computeMax() {
		double max = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value > max) max = value;
				validCount++;
			}
		}
		if(validCount>0)return max;
		else return null;
	}

	/**
	 * Send sum of the valid samples as aggregation result.
	 * @return Valid samples sum.
	 */
	private Serializable computeSum() {
		double sum = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				sum += (double) s.getValue();
				validCount++;
			}
		}
		if(validCount>0)return sum;
		else return null;
	}

	/**
	 * Send mean of the valid samples as aggregation result
	 * @return Mean of the valid samples.
	 */
	private Serializable computeMean() {
		double mean = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				mean += (double) s.getValue();
				validCount++;
			}
		}
		if(validCount>0) {
			mean = mean / (double) validCount;
			return mean;
		}
		else return null;
	}


	public String getDestination() {
		return destination;
	}

	public String getAddress() {
		return acceptorAddress;
	}

}
