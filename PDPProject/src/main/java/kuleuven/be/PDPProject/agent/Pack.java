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
import java.util.Map;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;
import kuleuven.be.PDPProject.agent.FeasabilityAnt;

import java.util.Map;


public class Pack extends Parcel implements TickListener{

	public String packID;
	private final RandomGenerator rng;
	private final double CRITIC = 0.8;

	// States
	private boolean carried;
	private boolean deposited;
	private boolean fragile;
	private boolean timeIsOver;
	private boolean isCritical;
	private boolean isBestTruckKnown;

	// Positions
	private Point departure;
	private Point position; 
	private Point destination;

	private RoadModel roadModel;
	private PDPModel pdpModel;
	private TimeWindow deliveryTW;
	private String truckID;
	
	private Simulator sim;
	private Map<String, Double> candidates;
	
	private RoadModel rm;
	ParcelDTO dto; 
	
	private FeasabilityAnt fAntDeparture;
	private FeasabilityAnt fAntDestination;
	
    public Pack(ParcelDTO dto, RandomGenerator r, String packID, Simulator sim) {
        super(dto,packID);
        this.packID = packID;

        rng = r;
        this.departure = dto.getPickupLocation();
        this.destination = dto.getDeliveryLocation();
        System.out.println("Pack created!");
        
		this.carried = false;
		this.deposited = false;
		this.fragile = rng.nextDouble() > 0.5;

		this.isBestTruckKnown = false;
		this.timeIsOver = false;
		this.candidates = new LinkedHashMap<>();
		this.truckID = new String("nobody");
		this.deliveryTW = dto.getDeliveryTimeWindow();
		
		this.sim = sim;
		
    }
    
    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    	this.rm = pRoadModel;
    	
    	fAntDeparture = new FeasabilityAnt(this, departure, deliveryTW, sim,false);
    	fAntDeparture.runAnt(fAntDeparture.getClones());
    	
    	fAntDestination = new FeasabilityAnt(this, destination, deliveryTW, sim,true);
    	fAntDestination.runAnt(fAntDestination.getClones());
    }
   
    public RoadModel getRM() {
    	return rm;
    }
	
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
	
	public TimeWindow getTimeWindow() {
		return dto.getDeliveryTimeWindow();
	}

	public String selectTruck() {
    	double minCost = Double.MAX_VALUE;
    	String minKey = new String("nobody");
       for (String key: candidates.keySet())
	   {
	   		double val = candidates.get(key);
	   		if (val < minCost) {
	   			System.out.println("key = " + key);
				minKey = key;
				minCost = val;
			}
	   }
	   return minKey;
	}

	public void setCandidate(String truckID, double cost) {
        if (!this.isBestTruckKnown)
            this.candidates.put(truckID, cost);
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (candidates.size() > 0)
		{
			this.truckID = selectTruck();
            this.candidates = new LinkedHashMap<>();
            FeasabilityAnt ant = new FeasabilityAnt(this, departure, deliveryTW, sim, false);
            ant.updateTruckID(ant.getClones(), truckID);
            this.isBestTruckKnown = true;
        }
		if(isCritical(timeLapse)) {
			this.isCritical = true;
		}
		if(isTimeOver(timeLapse)) {
			this.timeIsOver = true;
		}
	}
	
	@Override
	public void afterTick(TimeLapse timeLapse) {
		  
	}

	@Override
	public RoadModel getRoadModel() {return roadModel;}
}
