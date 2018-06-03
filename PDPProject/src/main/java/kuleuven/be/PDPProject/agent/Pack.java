package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;
import kuleuven.be.PDPProject.agent.FeasabilityAnt;

import java.util.Map;

/**
 * Actual packages to be delivered to Customers
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class Pack extends Parcel implements TickListener{

	public String packID;
	private final RandomGenerator rng;
	private final double CRITIC = 0.5;

	// States
	private boolean carried;
	private boolean deposited;
	private boolean fragile;
	private boolean isBestTruckKnown;
	private boolean isCritical;

	private long endTime;
	private StatCollector statCollector;

	// Positions
	private Point departure;
	private Point destination;

	private TimeWindow deliveryTW;
	private String truckID;
	
	private Simulator sim;
	private Map<String, Double> candidates;

	private PDPModel pdpModel;
	private RoadModel roadModel;
	ParcelDTO dto; 
	
	private FeasabilityAnt fAntDeparture;
	private FeasabilityAnt fAntDestination;
	
    public Pack(ParcelDTO dto, RandomGenerator r, String packID, Simulator sim, long endTime, StatCollector statCollector) {
        super(dto,packID);
        this.packID = packID;

        rng = r;
        this.departure = dto.getPickupLocation();
        this.destination = dto.getDeliveryLocation();

		this.carried = false;
		this.deposited = false;
		this.fragile = rng.nextDouble() > 0.5;

		this.isBestTruckKnown = false;
		this.isCritical = false;
		this.candidates = new LinkedHashMap<>();
		this.truckID = new String("nobody");
		this.deliveryTW = dto.getDeliveryTimeWindow();
		this.sim = sim;
		this.endTime = endTime;
		this.statCollector = statCollector;
		
    }
    
    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    	this.roadModel = pRoadModel;
    	this.pdpModel = pPdpModel;
    	//create feasibility ants leaving dropsigns to the pack
    	fAntDeparture = new FeasabilityAnt(this, departure, deliveryTW, sim,false);
    	fAntDeparture.runAnt(fAntDeparture.getClones());

		//create feasibility ants leaving dropsigns to the Customer
    	fAntDestination = new FeasabilityAnt(this, destination, deliveryTW, sim,true);
    	fAntDestination.runAnt(fAntDestination.getClones());
    }

	@Override
	public RoadModel getRoadModel() {return roadModel;}

	public boolean isCarried() {
		return carried;
	}
	
	public void setCarried(boolean b) {
		carried = b;
	}
	
	public boolean isDeposited() {
		return deposited;
	}
	
	public void setDeposited() {
		deposited = true;
	}
	
	public boolean isFragile(){
		return fragile;
	}
	
	public String getPackID() {
		return packID;
	}
	
	public boolean isTimeOver(TimeLapse timeLapse) {
		if(deliveryTW.isAfterEnd(timeLapse.getTime())){
			return true;
		}
		return false;
	}
	
	public boolean isCritical(TimeLapse timeLapse) {
		if(deliveryTW.isIn(timeLapse.getTime()) 
					&& timeLapse.getTime()-deliveryTW.begin() > CRITIC*deliveryTW.length()){
			return true;
		}
		return false;
	}

	public boolean isCritical() {
    	return isCritical;
	}

	public TimeWindow getTimeWindow() {
		return dto.getDeliveryTimeWindow();
	}

	/**
	 * Selects the closest {@link Truck} for delivery
	 * @return the <code>truckID</code> of the {@link Truck}
	 */
	public String selectTruck() {
    	double minCost = Double.MAX_VALUE;
    	String minKey = new String("nobody");
       for (String key: candidates.keySet())
	   {
	   		double val = candidates.get(key);						//computes the candidate with the smallest cost
	   		if (val < minCost) {
				minKey = key;
				minCost = val;
	   		}
	   }
	   System.out.println(minKey + " has cost: " + Double.toString(minCost));
	   return minKey;
	}

	/**
	 * Adds a candidate to the candidate list
	 * @param truckID ID of the candidate
	 * @param cost cost of the candidate
	 */
	public void setCandidate(String truckID, double cost) {
        if (!this.isBestTruckKnown)
            this.candidates.put(truckID, cost);
	}

	public String getTruckID() {
    	return this.truckID;
	}

	public long getDelay(TimeLapse timeLapse) {
		return Math.max(timeLapse.getTime() - this.deliveryTW.end(), 0L);
	}

	/**
	 * Sends ants immediately in order to change {@link RoadSign}s' truckID
	 * @param truckID ID of the kidnapper
	 */
	public void kidnapping(String truckID) {
		FeasabilityAnt ant = new FeasabilityAnt(this, departure, deliveryTW, sim, false);
		ant.updateTruckID(ant.getClones(), truckID);
		this.truckID = truckID;
		this.isBestTruckKnown = true;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (candidates.size() > 0 && !isBestTruckKnown) {
			this.truckID = selectTruck();
            this.candidates = new LinkedHashMap<>();
            FeasabilityAnt ant = new FeasabilityAnt(this, departure, deliveryTW, sim, false);
            ant.updateTruckID(ant.getClones(), truckID);
            this.candidates = new LinkedHashMap<>();
            this.isBestTruckKnown = true;
        }
        if (timeLapse.getTime()%5 == 0 && pdpModel.getParcelState(this).equals(PDPModel.ParcelState.AVAILABLE))
        	System.out.println(this.truckID + " must grab " + this.packID + " at position "+ this.getPickupLocation().toString());

		if (!isCritical)
			this.isCritical = isCritical(timeLapse);
		if (timeLapse.getTime() == this.endTime - sim.getTimeStep() && !pdpModel.getParcelState(this).equals(PDPModel.ParcelState.DELIVERED)) {
			long delay = timeLapse.getTime() + sim.getTimeStep() - this.deliveryTW.end();
			delay = Math.max(0L, delay);
			System.out.println(this.packID + " sends its delay: "+Long.toString(delay));
			statCollector.setPackDelay(delay);
		}
	}
	
	@Override
	public void afterTick(TimeLapse timeLapse) {
		  
	}
}
