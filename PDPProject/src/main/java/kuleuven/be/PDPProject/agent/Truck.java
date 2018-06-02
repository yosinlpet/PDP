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
	private boolean isDeadLocked;

	private int hasDelivered;
	private int hasCarried;
	private long accumulatedDelay;
	private double accumulatedDist;
	private LinkedList<String> trucksBehindUs;
	
	RandomGenerator rng;
	private RoadModel roadModel;
	private PDPModel pdpModel;

	private Pack tmpPack;
	private double tmpCost;
	private LinkedList<Point> tmpPath;
	private LinkedList<Point> blockedPath;

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
		tmpCost = Double.MAX_VALUE;
		tmpPack = null;
		isTrapped = false;
		hasChosenNewNode=false;
		trucksBehindUs = new LinkedList<>();
		blockedPath = new LinkedList<>();
	}

	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		// Set flag under us at the beginning
        this.roadModel = pRoadModel;
        this.pdpModel = pPdpModel;
		intAntBack = new IntentionAnt(roadModel, truckID, previousPoint);
		isBackFlagDropped = !intAntBack.askFlag();
	}

	public void setEmergencyMove(LinkedList<Point> path){
		this.blockedPath = new LinkedList<>(path);
		this.mustMove = !this.curr.isPresent();
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
            return (curr.isPresent() && this.pdpModel.containerContains(this, curr.get()));
	}

	public boolean isCarrying(Pack pckg) {
		return this.pdpModel.containerContains(this, pckg);
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

	public Point getNextPoint() {
		return nextPoint;
	}

	public boolean hasCurr() {
		return curr.isPresent();
	}

	public LinkedList<String> getTrucksBehindUs() {
		return trucksBehindUs;
	}

	/**
	 * Returns the path (without the first node!) to a pack that is the closest to the truck
	 * @param rsSet set of {@link RoadSign}s at the current position
	 * @return the path to the destination
	 */
	public LinkedList<Point> selectPackPath(Set<RoadSign> rsSet) {
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


	/**
	 * Gets the <code>path</code> from (excluding) our position to the {@link Pack} or its customer
	 * @param rsSet set of {@link RoadSign}s at the current position
	 * @param packID ID of the {@link Pack} we are dealing with
	 * @param indicateDest true if we deliver the {@link Pack}, false if we are getting it
	 * @return the path to the destination
	 */
	public LinkedList<Point> getCorrespondingPath(Set<RoadSign> rsSet, String packID, boolean indicateDest){
		LinkedList<Point> path = new LinkedList<>();
		for(RoadSign rs : rsSet) {
			if(rs.indicateDest() == indicateDest && rs.getPackID().equals(packID)){
				path = new LinkedList<>(rs.getPath());
				path.removeLast();
				break;
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
		urgentAnt.setEmergency(this.trucksBehindUs, this.intPath);
	}
	
	public void addToBehindUsList(String truckID) {
		trucksBehindUs.addLast(truckID);
	}
	

	public boolean negotiate(RoadModel rm){
		urgentAnt = new IntentionAnt(rm, this.truckID, this.nextPoint);
		return urgentAnt.negotiate(this, trucksBehindUs);
	}

	/**
	 * Updates previous and next points if on a node
     * Keeps track of the total distance we ran so far
	 * @return the success of the operation: true if we moved
	 */
	public boolean tryToMove(RoadModel rm, TimeLapse time) {
		if (!isFrontFlagRaised) {
			intAntFront = new IntentionAnt(roadModel, truckID, nextPoint);
			isFrontFlagRaised = intAntFront.askFlag();
		}
		if (isFrontFlagRaised) { // We can move
			Point p1 = rm.getPosition(this);
			rm.moveTo(this, nextPoint, time);
			Point p2 = rm.getPosition(this);
			this.accumulatedDist += Math.sqrt(Math.pow(p1.x - p2.x, 2d) + Math.pow(p1.y - p2.y, 2d));
			if (!isBackFlagDropped) {
				intAntBack = new IntentionAnt(roadModel, truckID, previousPoint);
				if (!rm.getPosition(this).equals(nextPoint))
					isBackFlagDropped = intAntBack.dropFlag();
			}
			
			if (rm.getPosition(this).equals(nextPoint)) {
				if(!intPath.isEmpty()) {
					updateTargetPoints(rm, intPath.removeLast());
				}
				else
					updateTargetPoints(rm, nextPoint);
			}
			
			trucksBehindUs = new LinkedList<>(); // Reset list of trucks behind us
			return true;
			
		}
		else { 	// We cannot move
			// Check if deadlock
            System.out.println(truckID+ " pushed by: "+trucksBehindUs.toString());
			for(String trucks : trucksBehindUs) { 
				if(trucks.equals(this.truckID)) {
					isDeadLocked = true;
					break;
				}
			}
			return false;
		}
	}


	@Override
	public void afterTick(TimeLapse timeLapse) {
		if (checkPackIsForMe) {
			boolean isAllowed = false;

			if(packAppearedOnUs){ // The pack has appeared on us
				Set<Pack> packSet = roadModel.getObjectsAt(this, Pack.class);
				for(Pack p : packSet) {
					if(p.getTruckID().equals(this.truckID)) {
						isAllowed = true;
						pdpModel.pickup(this, p, timeLapse);
						++this.hasCarried;
						break;
					}
				}
			}
			else {
				Set<RoadSign> roadSignSet = roadModel.getObjectsAt(this, RoadSign.class);
				for (RoadSign rs : roadSignSet)	{
					if (rs.getPack().equals(tmpPack) && !rs.indicateDest()) {
						isAllowed = (this.truckID == rs.getTruckID());
					} 
				}
			}

			// Take the pack : its for us !
			if (isAllowed) {
				intPath = tmpPath;
				updateTargetPoints(roadModel,intPath.getLast());
				curr = Optional.fromNullable(tmpPack);
				curr.get().setCarried(packAppearedOnUs);
				packAppearedOnUs = false;
			}
		}
		checkPackIsForMe = false;
		tmpPath = new LinkedList<>();
	}

	@Override
	protected void tickImpl(TimeLapse time) {
			
				
			if(isDeadLocked) {
				System.out.println(this.truckID+" is deadlocked...");
				// Choose a new node where to move
				if (!hasChosenNewNode) {
					Collection<Point> pointCollection = ((GraphRoadModel) roadModel).getGraph()
							.getOutgoingConnections(roadModel.getPosition(this));
					LinkedList<Point> points = new LinkedList<>();
					points.addAll(pointCollection);
					for(Point p : points) {
						if(!p.equals(nextPoint)) {
							updateTargetPoints(roadModel, p);
							hasChosenNewNode = true;
							break;
						}
					}
				}
				
				// Try to move
				if (tryToMove(roadModel, time)) {
					if (roadModel.getPosition(this).equals(nextPoint)) { // we have moved to the next node
						Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
						if(curr.isPresent()) {
							boolean inCargo = pdpModel.containerContains(this, curr.get());
							if(!inCargo) {
								intPath = getCorrespondingPath(roadSignSet, curr.get().getPackID(), false);
								updateTargetPoints(roadModel, nextPoint);
							}
							else {
								intPath = getCorrespondingPath(roadSignSet, curr.get().getPackID(), true);
								updateTargetPoints(roadModel, nextPoint);
							}						
							this.mustMove = false;
							this.hasChosenNewNode = false;
							this.isDeadLocked = false;
						}
						else
							System.out.println("Deadlocked boy without targeted Pack...");
					}
				}
				else {
					sendEmergency(roadModel);
				}
			}
			// If we are waiting and someone wants to pass
			else if (this.mustMove && intPath.isEmpty()) {
				System.out.println(this.getTruckID() + " must move from " + roadModel.getPosition(this).toString()+" _1_");

				// Choose a node where to move
				if (!hasChosenNewNode) { //|| (isTrapped && rm.getPosition(this).equals(previousPoint))) {
					Collection<Point> pointCollection = ((GraphRoadModel) roadModel).getGraph()
							.getOutgoingConnections(roadModel.getPosition(this));
					LinkedList<Point> points = new LinkedList<>();
					points.addAll(pointCollection);
					LinkedList<Point> orderedPoints = new LinkedList<>();
					for (Point p : points) {
						if (blockedPath.contains(p))
							orderedPoints.addLast(p);
						else
							orderedPoints.addFirst(p);
					}
					Point point = orderedPoints.getFirst();
					updateTargetPoints(roadModel, point);
					hasChosenNewNode = true;
				}

				if (tryToMove(roadModel, time)) {
					if (roadModel.getPosition(this).equals(nextPoint)) { // we have moved to the next node
						this.mustMove = false;
						hasChosenNewNode = false;
					}
				} else {
					sendEmergency(roadModel);
				}

			} else // We can do our stuff
			{
			
				if (!this.mustMove)
					System.out.println(this.getTruckID() + " is free on " + roadModel.getPosition(this).toString());
				else
					System.out.println(this.getTruckID() + " must move from " + roadModel.getPosition(this).toString()+" _2_");

				System.out.println("prev point:"+ this.previousPoint.toString() + " -- next point: "+ this.nextPoint.toString());
				if (curr.isPresent())
					System.out.println(this.getTruckID() + "curr is present? "+curr.get().getPackID());
				System.out.println(this.getTruckID() + "must check for me? "+Boolean.toString(checkPackIsForMe));
				System.out.println(this.getTruckID() + "int path has length "+Integer.toString(intPath.size()));

				// If no pack in target
				if (!curr.isPresent()) {

					
					Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
					Set<Pack> packSet = roadModel.getObjectsAt((RoadUser) this, Pack.class);

					// If no pack on us
					if (packSet.isEmpty()) {
						tmpPath = selectPackPath(roadSignSet);
						if (!tmpPath.isEmpty()) {
							for (RoadSign r : roadSignSet) {
								LinkedList<Point> path = new LinkedList<>(r.getPath());
								path.removeLast();
								if (path.equals(tmpPath)) {
									tmpPack = r.getPack();
									tmpCost = r.getCost();
									break;
								}
							}
							sendMyIntention(roadModel, tmpPath.getFirst(), tmpPack, tmpCost);
							checkPackIsForMe = true;
						}
					}

					// A pack is on us
					else {
						for (Pack p : packSet) {
							if(p.getTruckID().equals("nobody")) {
								packAppearedOnUs = true;
								checkPackIsForMe = true;
								sendMyIntention(roadModel, roadModel.getPosition(this), p, 0d);
								tmpPath = getCorrespondingPath(roadSignSet, p.getPackID(), true);
								tmpPack = p;
								break;
							}
						}
					}
				}

				// If we have a pack in target
				if (curr.isPresent() && !checkPackIsForMe) {
				    this.mustMove = false;
				    final boolean inCargo = pdpModel.containerContains(this, curr.get());

					// Sanity check
					if (!inCargo && !roadModel.containsObject(curr.get())) {
						System.out.println("[WARNING] -- Sanity Check");
						curr = Optional.absent();
					}

					// We have the pack and we will deliver it
					else if (inCargo) {
						if (!tryToMove(roadModel, time)) {
						//	System.out.println(this.getTruckID() + " says: I'm stuck Milhouse ... :'( need to go to " + this.curr.get().getDeliveryLocation());
							sendEmergency(roadModel);
						}

						if (roadModel.getPosition(this).equals(curr.get().getDeliveryLocation())) {
							// deliver when we arrive
							pdpModel.deliver(this, curr.get(), time);
							this.accumulatedDelay += curr.get().getDelay(time);
							++this.hasDelivered;
							curr.get().setDeposited();
							curr = Optional.absent();
							updateTargetPoints(roadModel, roadModel.getPosition(this));
						}
					}

					// We dont have yet the pack : go for it
					else {
						//System.out.println(this.getTruckID() + " at " + this.roadModel.getPosition(this) + " starts to get " + this.curr.get().getPackID() + " at " + roadModel.getPosition(curr.get()));
						boolean hasNegotiated = false;

						if (!tryToMove(roadModel, time)) {
							System.out.println(this.getTruckID() + " says: I'm stuck Milhouse ... :'( -- Must get Pack at " + this.curr.get().getPickupLocation());
							hasNegotiated = negotiate(roadModel);
						}
						else
							System.out.println("try to move succeeeded");
						if (hasNegotiated) {
							updateTargetPoints(roadModel, roadModel.getPosition(this));
							//System.out.println(this.truckID + "has negotiated, can rest at "+ roadModel.getPosition(this));
						}
						else {
							if (roadModel.getPosition(this).equals(curr.get().getPickupLocation())) {
								pdpModel.pickup(this, curr.get(), time);
								++this.hasCarried;
								curr.get().setCarried(true);
								Set<RoadSign> rs = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
								intPath = getCorrespondingPath(rs, curr.get().getPackID(), true);
								updateTargetPoints(roadModel, intPath.getLast());
							}
						}

						//SANITY CHECK -- this should never happen in practice, only if there is a bug
						if (!hasNegotiated && this.intPath.isEmpty() && this.roadModel.getPosition(this) == this.nextPoint) {
							System.out.println("[WARNING] -- AIMING AT A PACK BUT NO WAY TO GET THERE -- PATH COMPUTATION");
							Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
							getCorrespondingPath(roadSignSet, truckID, inCargo);
						}
					}
				}
			}
	}
}
