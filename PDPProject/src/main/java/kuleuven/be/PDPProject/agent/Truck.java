package kuleuven.be.PDPProject.agent;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.LinkedList;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;
import sun.awt.image.ImageWatched;


/**
 * Taxi agent for DelegateMAS
 * TODO Has to avoid collisions and deadlocks
 * TODO Need to get critical {@link Pack}s first
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class Truck extends Vehicle {
	private Optional<Pack> curr;

	private IntentionAnt intAntFront;
	private IntentionAnt intAntBack;
	private IntentionAnt applicationAnt;
	private IntentionAnt urgentAnt;
	private Point nextPoint;
	private Point previousPoint;
	private Point currLoc;
	private Point previousLoc;

	private boolean isBackFlagDropped;
	private boolean isFrontFlagRaised;
	private boolean mustMove;
	private boolean hasChosenNewNode;
	private boolean checkPackIsForMe;
	private boolean isTrapped;

	private int hasDelivered;
	private int hasCarried;
	private long accumulatedDelay;
	private double accumulatedDist;

	RandomGenerator rng;

	private Pack tmpPack;
	private double waitingCost;
	private LinkedList<Point> tmpPath;

	String truckID;

	private LinkedList<Point> intPath;

	public Truck(Point startPosition, int capacity, double speed, String truckID, RandomGenerator rng) {
		super(VehicleDTO.builder()
				.capacity(capacity)
				.startPosition(startPosition)
				.speed(speed)
				.build());
		this.rng = rng;
		this.truckID = truckID;
		curr = Optional.absent();
		hasCarried = 0;
		hasDelivered = 0;
		accumulatedDelay = 0L;
		accumulatedDist = 0d;
		previousPoint = getStartPosition();
		nextPoint = getStartPosition();
		previousLoc = new Point(-Double.MAX_VALUE, -Double.MAX_VALUE);
		currLoc = getStartPosition();
		intPath = new LinkedList<>();
		tmpPath = new LinkedList<>();
		isBackFlagDropped = true;
		isFrontFlagRaised = false;
		waitingCost = Double.MAX_VALUE;
		tmpPack = null;
		isTrapped = false;
		hasChosenNewNode=false;
	}

	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		// Set flag under us at the beginning
		intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
		isBackFlagDropped = !intAntBack.askFlag();
	}

	public void setEmergencyMove(){
		this.mustMove = true;
	}

	public String getTruckID() {
		return truckID;
	}

	public void setIntPath(LinkedList<Point> path) {
		this.intPath = path;
	}

	public Optional<Pack> getCurr() {
		return curr;
	}

	public void setCurr(Optional<Pack> curr) {
		this.curr = curr;
	}

	public LinkedList<Point> getIntPath() {
		return intPath;
	}

	public void setNextPoint(Point nextPoint) {
		this.nextPoint = nextPoint;
	}

	public boolean isCarrying() {
            return (curr.isPresent() && this.getPDPModel().containerContains(this, curr.get()));
	}

	public boolean isCarrying(Pack pckg) {
		return this.getPDPModel().containerContains(this, pckg);
	}

	public boolean getTrapped() {
		return this.isTrapped;
	}

	public int getHasCarried() {
		return this.hasCarried;
	}

	public long getAccumulatedDelay() {
		return this.accumulatedDelay;
	}

	public double getAccumulatedDist() {
		return accumulatedDist;
	}

	public int getHasDelivered() {
		return this.hasDelivered;
	}

	// Return the path (without the first node!) to a pack that is the closest to the truck
	public LinkedList<Point> selectPackPath(Set<RoadSign> rsSet, TimeLapse time) {
		double minCost=Double.MAX_VALUE;
		LinkedList<Point> minPath = new LinkedList<>();
		for(RoadSign rs : rsSet) {
			if(!rs.indicateDest()) {
				if(rs.getCost() < minCost) {
					minPath = new LinkedList<>(rs.getPath());
					minCost = rs.getCost();
					minPath.removeLast();
				}
			}
		}
		//System.out.print("Path is ");
		//System.out.print(minPath.toString());
		return minPath;
	}

	public LinkedList<Point> getCorrespondingDestPath(Set<RoadSign> rsSet,
													  String packID){
		LinkedList<Point> path = new LinkedList<>();
		for(RoadSign rs : rsSet) {
			if(rs.indicateDest()){
				if(rs.getPackID().equals(packID)) {
					path = new LinkedList<>(rs.getPath());
					path.removeLast();
					break;
				}
			}
		}
		//System.out.print("Path is ");
		//System.out.print(path.toString());
		return path;
	}
	
	public boolean tryToMove(RoadModel rm, TimeLapse time) {
		if (!isFrontFlagRaised) {
			intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
			isFrontFlagRaised = intAntFront.askFlag();
		}
		if (isFrontFlagRaised) { // We can move
			Point p1 = rm.getPosition(this);
			rm.moveTo(this, nextPoint, time);
			Point p2 = rm.getPosition(this);
			this.accumulatedDist += Math.sqrt(Math.pow(p1.x - p2.x, 2.) + Math.pow(p1.y - p2.y, 2.));
			if (!isBackFlagDropped) {
				intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
				if (!previousPoint.equals(nextPoint))
					isBackFlagDropped = intAntBack.dropFlag();
			}
			
			if (rm.getPosition(this).equals(nextPoint)) {
				if(!intPath.isEmpty()) {
					previousPoint = rm.getPosition(this);
					nextPoint = intPath.removeLast();
				}
				
				isFrontFlagRaised = false;
				isBackFlagDropped = false;
				
				this.previousLoc = new Point(-Double.MAX_VALUE,-Double.MAX_VALUE);
			}
			return true;
		} else return false;
	}


	@Override
	public void afterTick(TimeLapse timeLapse) {
		if (checkPackIsForMe) {
			final RoadModel rm = getRoadModel();
		    boolean isAllowed = false;
			Set<RoadSign> roadSignSet = getRoadModel().getObjectsAt(this, RoadSign.class);
			for (RoadSign rs : roadSignSet)	{
				if (rs.getPack().equals(tmpPack) && !rs.indicateDest()) {
					isAllowed = (this.truckID == rs.getTruckID());
				}
			}

			if (isAllowed) {
				intPath = tmpPath;
				this.previousLoc = new Point(-Double.MAX_VALUE,-Double.MAX_VALUE);
				previousPoint = rm.getPosition(this);
				nextPoint = intPath.getLast();
				curr = Optional.fromNullable(tmpPack);
			}
		}
		checkPackIsForMe = false;
		tmpPath = new LinkedList<>();
	}

	@Override
	protected void tickImpl(TimeLapse time) {
		final RoadModel rm = getRoadModel();
		final PDPModel pm = getPDPModel();
				
		
		if (this.mustMove && intPath.isEmpty()) {
		    System.out.println(this.getTruckID()+" must move!");
		    
		    if (!hasChosenNewNode) { //|| (isTrapped && rm.getPosition(this).equals(previousPoint))) {
				Collection<Point> pointCollection = ((GraphRoadModel) getRoadModel()).getGraph()
						.getOutgoingConnections(getRoadModel().getPosition(this));
				LinkedList<Point> points = new LinkedList<>();
				points.addAll(pointCollection);
				int randI = rng.nextInt(points.size());
				Point point = points.get(randI);
				//previousPoint = nextPoint;
				previousPoint = rm.getPosition(this);
				nextPoint = point;
				hasChosenNewNode = true;
			}
		    
		    if(tryToMove(rm, time)) {
				if (rm.getPosition(this).equals(nextPoint)) {
			    	this.mustMove = false;
	                hasChosenNewNode = false;
				}
		    } else {
				isTrapped = true;
				urgentAnt = new IntentionAnt(getRoadModel(), truckID, nextPoint);
				urgentAnt.setEmergency();
		    }
		    
			//if (rm.getPosition(this).equals(this.previousLoc)) {

			//}
		}
		//if (!(mustMove && intPath.isEmpty()) || isTrapped)
		else
		{
			//if (getRoadModel().getPosition(this).equals(this.nextPoint)) {
				//isTrapped = false;
				//mustMove = false;
				//previousPoint = rm.getPosition(this);
			//}
			//System.out.print(" -- nextPoint: ");
			//if (nextPoint != null)
			//System.out.println(nextPoint.toString());

			// If no pack in target
			if (!curr.isPresent()) {
				Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
				Set<Pack> ps = rm.getObjectsAt((RoadUser) this, Pack.class);
				// If no pack on us
				if (ps.isEmpty()) {
					tmpPath = selectPackPath(rs, time);
					if (!tmpPath.isEmpty()) {
						for (RoadSign r : rs) {
							LinkedList<Point> path = new LinkedList<>(r.getPath());
							path.removeLast();
							if (path.equals(tmpPath)) {
								tmpPack = r.getPack();
								waitingCost = r.getCost();
							}
						}
						applicationAnt = new IntentionAnt(getRoadModel(), truckID, tmpPath.getFirst());
						applicationAnt.sendTruckID(truckID, tmpPack, (double) waitingCost);
						checkPackIsForMe = true;
						System.out.print(tmpPack.getPackID() + " for ");
						System.out.println(this.truckID);
					}

					if (this.previousPoint.equals(getRoadModel().getPosition(this))) {
						isFrontFlagRaised = false;
						isBackFlagDropped = false;
					}
					else {
						System.out.println("[WARNING] -- No targeted Pack and still Truck not on GraphNode...");
						//IMPORTANT in order to go to the end of your path under any circumstance !
						if (!intPath.isEmpty()) {
							System.out.println("[WARNING] -- No targeted Pack and still intention Path not empty...");
								
							tryToMove(rm, time);
						}
					}
				}
				// A pack is on us
				else {
					for (Pack p : ps) {
						//System.out.println("Ouch, a Pack just landed on me!");
						applicationAnt = new IntentionAnt(getRoadModel(), truckID, rm.getPosition(this));
						applicationAnt.sendTruckID(truckID, p, 0.);

						curr = Optional.fromNullable(p);
						intPath = getCorrespondingDestPath(rs, curr.get().getPackID());
						//System.out.print(" for truck ");
						//System.out.println(this.truckID);

						//System.out.print("Change in previous and next node for truck nb : ");
						//System.out.println(truckID);

						previousPoint = rm.getPosition(this);
						nextPoint = intPath.removeLast();
						isBackFlagDropped = false;
						isFrontFlagRaised = false;
						this.previousLoc = new Point(-Double.MAX_VALUE,-Double.MAX_VALUE);
						break;
					}
					pm.pickup(this, curr.get(), time);
					++this.hasCarried;
					curr.get().setCarried(true);
				}

			}

			// If we have a pack in target
			if (curr.isPresent() && !checkPackIsForMe) {
				final boolean inCargo = pm.containerContains(this, curr.get());

				// Sanity check
				if (!inCargo && !rm.containsObject(curr.get())) {
					curr = Optional.absent();
				}

				// We have the pack and we will deliver it
				else if (inCargo) {
					if (rm.getPosition(this).equals(this.previousLoc)) {
						if (!intPath.isEmpty())
                            System.out.println(this.getTruckID()+" says: I'm stuck Milhouse ... :'( need to go to " + intPath.getFirst().toString());
						urgentAnt = new IntentionAnt(getRoadModel(), truckID, nextPoint);
						urgentAnt.setEmergency();
					}
					
					tryToMove(rm, time);
					
					if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
							// deliver when we arrive
							pm.deliver(this, curr.get(), time);
							this.accumulatedDelay += curr.get().getDelay(time);
							++this.hasDelivered;
							curr.get().setDeposited();
							previousPoint = rm.getPosition(this);
							isBackFlagDropped = false;
							isFrontFlagRaised = false;
							curr = Optional.absent();
					}
					 //else rm.moveTo(this, nextPoint, time);
				}
				// We dont have yet the pack : go for it
				else {
				    System.out.println(this.getTruckID() + " starts to get " + this.curr.get().getPackID() + " at " + getRoadModel().getPosition(curr.get()));
				    boolean hasNegotiated = false;
					if (getRoadModel().getPosition(this).equals(this.previousLoc)){
						if (!intPath.isEmpty())
                            System.out.println(this.getTruckID()+" says: I'm stuck Milhouse ... :'( -- Must get Pack at "+ intPath.getFirst().toString());
						//urgentAnt = new IntentionAnt(getRoadModel(), truckID, nextPoint);
						//hasNegotiated = urgentAnt.negotiate(this);
					}
					//if (!hasNegotiated) {
					if(true) {
						//if (!intPath.isEmpty())
							//System.out.println("Truck " + this.getTruckID() + " is getting this pack at " + this.getIntPath().getFirst());
						
							tryToMove(rm, time);

							if (rm.equalPosition(this, curr.get())) {
								pm.pickup(this, curr.get(), time);
								++this.hasCarried;
								curr.get().setCarried(true);
								previousPoint = nextPoint;
								isBackFlagDropped = false;
								isFrontFlagRaised = false;
								Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
								intPath = getCorrespondingDestPath(rs, curr.get().getPackID());
								//System.out.print(" for truck ");
								//System.out.println(this.truckID);
							}
						
					}
				}
			}
		}
		this.previousLoc = this.currLoc;
		this.currLoc = rm.getPosition(this);
	}

}
