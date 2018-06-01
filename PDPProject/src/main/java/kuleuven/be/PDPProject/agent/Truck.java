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
	private boolean packAppearedOnUs;

	private int hasDelivered;
	private int hasCarried;
	private long accumulatedDelay;

	RandomGenerator rng;

	private Pack tmpPack;
	private double tmpCost;
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
		previousPoint = getStartPosition();
		nextPoint = getStartPosition();
		previousLoc = new Point(-Double.MAX_VALUE, -Double.MAX_VALUE);
		currLoc = getStartPosition();
		intPath = new LinkedList<>();
		tmpPath = new LinkedList<>();
		isBackFlagDropped = true;
		isFrontFlagRaised = false;
		tmpCost = Double.MAX_VALUE;
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
		return path;
	}
	
	public void updateTargetPoints(RoadModel rm, Point nextPoint) {
		this.previousPoint = rm.getPosition(this);
		this.nextPoint = nextPoint;
		isFrontFlagRaised = false;
		isBackFlagDropped = false;
	}
	
	public void sendMyIntention(RoadModel rm, Point packetPos, Pack targetPack, double cost) {
		applicationAnt = new IntentionAnt(rm, this.truckID, packetPos);
		applicationAnt.sendTruckID(truckID, targetPack, cost);
	}
	
	public void sendEmergency(RoadModel rm) {
		urgentAnt = new IntentionAnt(rm, this.truckID, this.nextPoint);
		urgentAnt.setEmergency();
	}
	
	// Return true if we moved
	// Update next and previous point if we are on a node
	public boolean tryToMove(RoadModel rm, TimeLapse time) {
		if (!isFrontFlagRaised) {
			intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
			isFrontFlagRaised = intAntFront.askFlag();
		}
		if (isFrontFlagRaised) { // We can move
			rm.moveTo(this, nextPoint, time);
			if (!isBackFlagDropped) {
				intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
				if (!rm.getPosition(this).equals(nextPoint))
					isBackFlagDropped = intAntBack.dropFlag();
			}
			
			if (rm.getPosition(this).equals(nextPoint)) {
				if(!intPath.isEmpty()) {
					updateTargetPoints(rm, intPath.removeLast());
				}
			}
			return true;
		} else return false;
	}


	@Override
	public void afterTick(TimeLapse timeLapse) {
		if (checkPackIsForMe) {
			boolean isAllowed = false;
			final RoadModel rm = getRoadModel();
			final PDPModel pm = getPDPModel();
			
			if(packAppearedOnUs){ // The pack has appeared on us
				Set<Pack> packSet = rm.getObjectsAt(this, Pack.class);
				for(Pack p : packSet) {
					if(p.getTruckID().equals(this.truckID)) {
						isAllowed = true;
						pm.pickup(this, p, timeLapse);
						packAppearedOnUs = false;
						this.hasCarried++;
						break;
					}
				}
			}
			else {
				Set<RoadSign> roadSignSet = getRoadModel().getObjectsAt(this, RoadSign.class);
				for (RoadSign rs : roadSignSet)	{
					if (rs.getPack().equals(tmpPack) && !rs.indicateDest()) {
						isAllowed = (this.truckID == rs.getTruckID());
					} 
				}
			}
			
			// Take the pack : its for us !
			if (isAllowed) {
				intPath = tmpPath;
				updateTargetPoints(rm,intPath.getLast());
				curr = Optional.fromNullable(tmpPack);
				curr.get().setCarried(true);
			}
		}
		checkPackIsForMe = false;
		tmpPath = new LinkedList<>();
	}

	@Override
	protected void tickImpl(TimeLapse time) {
		final RoadModel rm = getRoadModel();
		final PDPModel pm = getPDPModel();
				
		// If we are waiting and someone want to pass
		if (this.mustMove && intPath.isEmpty()) {
		    System.out.println(this.getTruckID()+" must move!");
		    
		    // Choose a node where to move
		    if (!hasChosenNewNode) { //|| (isTrapped && rm.getPosition(this).equals(previousPoint))) {
				Collection<Point> pointCollection = ((GraphRoadModel) getRoadModel()).getGraph()
						.getOutgoingConnections(getRoadModel().getPosition(this));
				LinkedList<Point> points = new LinkedList<>();
				points.addAll(pointCollection);
				int randI = rng.nextInt(points.size());
				Point point = points.get(randI);
				updateTargetPoints(rm,point);
				hasChosenNewNode = true;
			}
		    
		    if(tryToMove(rm, time)) {
				if (rm.getPosition(this).equals(nextPoint)) { // we have moved to the next node
			    	this.mustMove = false;
	                hasChosenNewNode = false;
				}
		    } else {
		    	sendEmergency(rm);
		    }
		    
		}
		else // We can do our stuff 
		{
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
								tmpCost = r.getCost();
								break;
							}
						}
						sendMyIntention(rm,tmpPath.getFirst(),tmpPack,tmpCost);
						checkPackIsForMe = true;
						System.out.print(tmpPack.getPackID() + " for ");
						System.out.println(this.truckID);
					}
				}
				
				// A pack is on us
				else {
					for (Pack p : ps) {
						packAppearedOnUs = true;
						checkPackIsForMe = true;
						sendMyIntention(rm,rm.getPosition(this),p,0d);
						tmpPath = getCorrespondingDestPath(rs, p.getPackID());
						tmpPack = p;
						break;
					}
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
					if (!tryToMove(rm, time) && !intPath.isEmpty()) {
                        System.out.println(this.getTruckID()+" says: I'm stuck Milhouse ... :'( need to go to " + intPath.getFirst().toString());
						sendEmergency(rm);
					}
					
					if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
							// deliver when we arrive
							pm.deliver(this, curr.get(), time);
							//this.accumulatedDelay += curr.get().getDelay(time);
							this.hasDelivered++;
							curr.get().setDeposited();
							curr = Optional.absent();
							updateTargetPoints(rm, rm.getPosition(this));
					}
				}
				
				// We dont have yet the pack : go for it
				else {
				    System.out.println(this.getTruckID() + " starts to get " + this.curr.get().getPackID() + " at " + getRoadModel().getPosition(curr.get()));
				    boolean hasNegotiated = false;
					
					if(!tryToMove(rm, time) && !intPath.isEmpty()) {
						 System.out.println(this.getTruckID()+" says: I'm stuck Milhouse ... :'( -- Must get Pack at "+ intPath.getFirst().toString());
						 sendEmergency(rm);
					}
					
					if (rm.getPosition(this).equals(curr.get().getPickupLocation())) {
						pm.pickup(this, curr.get(), time);
						this.hasCarried++;
						curr.get().setCarried(true);
						Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
						intPath = getCorrespondingDestPath(rs, curr.get().getPackID());
						updateTargetPoints(rm, intPath.getLast());
					}	
				}
			}
		}
	}

}
