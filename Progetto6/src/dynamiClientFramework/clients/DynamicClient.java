package dynamiClientFramework.clients;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dynamiClientFramework.clients.exceptions.InvalidPropertyException;
import dynamiClientFramework.clients.exceptions.InvalidSampleTTLException;
import dynamiClientFramework.polling.PollingService2;


public abstract class DynamicClient implements Client{
	
	//TO TURN PRIVATE
	public enum State {NORMAL, CONGESTION};
	public enum Strategy {DROP, AGGREGABLE};
	public enum Operations {MIN, MAX, SUM, MEAN};
	
	private int EPSILON;
	private int BUFFER_DIM;
	private boolean aggressiveStrategy = false;
	private int MAX_BUFFER_DIM;
	private long TTL;	
	private long POLLING_PERIOD;
	private State status;
	private Strategy strategy;
	private Operations operation;
	protected List<Sample> sendBuffer;
	private	PollingService ps; 
	private int currMessageCount;
	private String destination, acceptorAddress;
	
	//TO DELETE: TEST ONLY
	private int aggregateCount = 0;	
	private int aggregateSent = 0;	
	private int invalidCount = 0;
	
	public DynamicClient(String destination, String acceptorAddress) {
		this.destination=destination;
		this.acceptorAddress=acceptorAddress;
		loadProperties();
		MAX_BUFFER_DIM = BUFFER_DIM * 2;
		sendBuffer = new ArrayList<Sample>();
		status = State.NORMAL;
		currMessageCount = 0;
		ps = createPollingService(POLLING_PERIOD);
	}
	

	//PUBLIC INTERFACE*****************************/
	public void trySending(Sample sample){
		switch(status) {
		case NORMAL: 
			if(!sendBuffer.isEmpty()) emptyBuffer();
			sendMessage(sample);
			break;
		case CONGESTION:
			handleStrategy(sample);
			break;
		}
	}
	
	public void startClient() {
		startConnection();
		ps.startPolling();
	}
	
	public void stopClient() {
		ps.stopPolling();
		closeConnection();
	}
	
	public abstract boolean isAlive();

	protected abstract void sendMessage(Sample sample);
	
	protected abstract void startConnection();

	protected abstract void closeConnection();
	
	protected abstract PollingService createPollingService(long pollingPeriod);
	//*********************************************/
	
	private void loadProperties() {
		String filename = "config.properties";
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            Properties prop = new Properties();
            if (input == null) throw new FileNotFoundException();
            
            prop.load(input);
            EPSILON = Integer.parseInt(prop.getProperty("epsilon"));
            if(EPSILON<=0) throw new InvalidPropertyException("Epsilon must be greater than 0.");

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

	private void handleStrategy(Sample sample) {
		switch(strategy) {
			case DROP: 
				drop(sample);
				break;
			case AGGREGABLE:
				aggregate(sample);
				break;
		}
	}
	
	private void drop(Sample sample) {
		if(sendBuffer.size() < BUFFER_DIM) sendBuffer.add(sample);
		else emptyBuffer();
	}
	
	private void aggregate(Sample sample) {
		aggregateCount++;
	    sendBuffer.add(sample);
		if(sendBuffer.size()>=BUFFER_DIM){
	         Serializable value = computeAggregation();
	         if(value!=null) {
	        	 try {
					sendMessage(new Sample(value, TTL));
				} catch (InvalidSampleTTLException e) {
					e.printStackTrace();
				}
	     		aggregateSent++;
	         }
	         sendBuffer.clear();
		}
		if(aggressiveStrategy && BUFFER_DIM<MAX_BUFFER_DIM) BUFFER_DIM = MAX_BUFFER_DIM;
		else if(!aggressiveStrategy && BUFFER_DIM==MAX_BUFFER_DIM) BUFFER_DIM /=2;
	}
	 
	private Serializable computeAggregation() {
		switch(operation) {
			case MIN: return computeMin();
			case MAX: return computeMax();
			case SUM: return computeSum();
			case MEAN: return computeMean();
			default: return null;
		}
	}
	
	private Serializable computeMin() {
		double min = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value < min) min = value;
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return min;
		else return null;
	}
	
	private Serializable computeMax() {
		double max = (double) sendBuffer.get(0).getValue();
		int validCount = 0;
		for(int i=1; i<sendBuffer.size(); i++) {
			Sample s = sendBuffer.get(i);
			if(s.isValid()) {
				double value = (double) s.getValue();
				if(value > max) max = value;
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return max;
		else return null;
	}
	
	private Serializable computeSum() {
		double sum = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				sum += (Double) s.getValue();
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0)//aggregate at least two samples
			return sum;
		else return null;
	}
	
	private Serializable computeMean() {
		double mean = 0.0;
		int validCount = 0;
		for(Sample s: sendBuffer) {
			if(s.isValid()) {
				mean += (Double) s.getValue();
				validCount++;
			}else invalidCount++;
		}
		if(validCount>0) {
			//aggregate at least two samples
			mean = mean / (double) validCount;
			return mean;
		}
		else return null;
	}
	
	private void emptyBuffer() {
		for(Sample sample: sendBuffer)
			if(sample.isValid()) {
				sendMessage(sample);
				aggregateCount--;
			}
			else invalidCount++;
		sendBuffer.clear();
	} 
			
	/**
	 * Sets congestion status checking messageCount
	 * @param messageCount number of current messages within the destination queue.
	 */
	public void updateQueueStatus(int messageCount) {	
		int delta = (messageCount - currMessageCount);
		currMessageCount=messageCount;
		//System.out.println("Delta: " + delta);
		switch(status) {
			case NORMAL: 
				if(messageCount>EPSILON) status=State.CONGESTION;
				break;
			case CONGESTION:
				if(delta<=0) {
					if(messageCount<=EPSILON) status=State.NORMAL;
					aggressiveStrategy=false;
				}
				else if(delta>0) aggressiveStrategy=true;
				break;
			}
	}

	
	//TO DELETE: FOR TEST ONLY
	public void setPS() {
		this.ps = new PollingService2(this, POLLING_PERIOD, true);
	}
	
	public String getDestination() {
		return destination;
	}

	public String getAddress() {
		return acceptorAddress;
	}
		
	public int getBUFFER_DIM() {
		return BUFFER_DIM;
	}

	public boolean doesAggressiveStrategy() {
		return aggressiveStrategy;
	}

	public State getStatus() {
		return status;
	}

	public Strategy getStrategy() {
		return strategy;
	}

	public int getCurrMessageCount() {
		return currMessageCount;
	}

	public int getAggregateCount() {
		return aggregateCount;
	}
	
	public int getAggregateSent() {
		return aggregateSent;
	}
	
	public int getInvalidCount() {
		return invalidCount;
	}
	
	public List<Sample> getSendBuffer() {
		return sendBuffer;
	}
	
}
