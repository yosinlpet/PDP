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

	private boolean isBackFlagDropped;
	private boolean isFrontFlagRaised;
	private boolean mustMove;
	private boolean hasChosenNewNode;
	private boolean hasChosenNewNodeDL;
	private boolean checkPackIsForMe;
	private boolean isTrapped;
	private boolean packAppearedOnUs;
	private boolean isDeadLocked;

	private int hasDelivered;
	private int hasCarried;
	private long accumulatedDelay;
	private double accumulatedDist;
	private LinkedList<String> trucksBehindUs;
	private long lastUpdate;

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
		intPath = new LinkedList<>();
		tmpPath = new LinkedList<>();
		isBackFlagDropped = true;
		isFrontFlagRaised = false;
		tmpCost = Double.MAX_VALUE;
		tmpPack = null;
		isTrapped = false;
		hasChosenNewNode=false;
		hasChosenNewNodeDL=false;
		trucksBehindUs = new LinkedList<>();
		blockedPath = new LinkedList<>();
		lastUpdate = 0L;
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
		this.intPath = new LinkedList<>(path);
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

	public boolean isCarrying() {
		return (curr.isPresent() && this.pdpModel.containerContains(this, curr.get()));
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

	public boolean hasCurr() {
		return curr.isPresent();
	}

	protected void deliver(TimeLapse timeLapse) {
		pdpModel.deliver(this, curr.get(), timeLapse);
		this.accumulatedDelay += curr.get().getDelay(timeLapse);
		++this.hasDelivered;
		curr.get().setDeposited();
		curr = Optional.absent();
	}

	protected void pickUp(TimeLapse timeLapse, Pack pack, boolean setPath) {
		pdpModel.pickup(this, pack, timeLapse);
		++this.hasCarried;
		pack.setCarried(true);
		if (setPath) {
			Set<RoadSign> rs = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
			intPath = getCorrespondingPath(rs, pack.getPackID(), true);
		}
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

	public void updateTargetPoints(RoadModel rm, Point nextPoint, TimeLapse timeLapse) {
		this.previousPoint = rm.getPosition(this);
		this.nextPoint = nextPoint;
		this.lastUpdate = timeLapse.getTime();
		this.isFrontFlagRaised = false;
		this.isBackFlagDropped = false;
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


	public boolean negotiate(RoadModel rm, TimeLapse timeLapse){
		urgentAnt = new IntentionAnt(rm, this.truckID, this.nextPoint);
		return urgentAnt.negotiate(this, trucksBehindUs, timeLapse);
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
					updateTargetPoints(rm, intPath.removeLast(), time);
				}
				else
					updateTargetPoints(rm, nextPoint, time);
			}

			trucksBehindUs = new LinkedList<>(); // Reset list of trucks behind us
			return true;

		}
		else { 	// We cannot move
			// Check if deadlock
			//System.out.println(truckID+ " pushed by: "+trucksBehindUs.toString());
			for(String trucks : trucksBehindUs) {
				if(trucks.equals(this.truckID)) {
					isDeadLocked = true;
					System.out.println(this.truckID + " is in type 1 deadlock.");
					break;
				}
			}
			if (!isDeadLocked) {
				if (time.getTime() - this.lastUpdate > 60) {
					isDeadLocked = true;
					System.out.println(this.truckID + " is in type 2 deadlock.");
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
						this.pickUp(timeLapse, p, false);
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
				intPath = new LinkedList<>(tmpPath);
				updateTargetPoints(roadModel,intPath.getLast(), timeLapse);
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
		System.out.print("Tick: "+ Long.toString(time.getTime())+ " | "+this.truckID + " | " + roadModel.getPosition(this) + " | ");

		if(isDeadLocked) {
			System.out.print("DL | ");
			if (!hasChosenNewNodeDL) {																					// Choose a new node where to move
				Collection<Point> pointCollection = ((GraphRoadModel) roadModel).getGraph()
						.getOutgoingConnections(roadModel.getPosition(this));
				LinkedList<Point> points = new LinkedList<>();
				points.addAll(pointCollection);
				for(Point p : points) {
					if(!p.equals(nextPoint)) {
						updateTargetPoints(roadModel, p, time);
						System.out.print("has "+ p.toString()+" | ");
						hasChosenNewNodeDL = true;
						this.intPath = new LinkedList<>();
						break;
					}
					else
						System.out.print("can't "+p.toString()+ " | ");
				}
			}

			if (tryToMove(roadModel, time)) {																			// Try to move
				System.out.print("escaping | ");
				if (!hasChosenNewNodeDL) {
					this.isDeadLocked = false;
					System.out.print("escaped | ");
				}
				else if (roadModel.getPosition(this).equals(nextPoint)) { 											// we have moved to the next node
					System.out.print("escaped | ");
					Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
					if(curr.isPresent()) {
						boolean inCargo = pdpModel.containerContains(this, curr.get());
						intPath = getCorrespondingPath(roadSignSet, curr.get().getPackID(), inCargo);
						System.out.println();
						System.out.println(truckID + " has new path: " + intPath.toString());
						updateTargetPoints(roadModel, (intPath.isEmpty()) ? nextPoint : intPath.getLast(), time);
					}
					else {
						updateTargetPoints(roadModel, nextPoint, time);
						System.out.print("NoCurr | ");
					}
					this.mustMove = false;
					this.hasChosenNewNodeDL = false;
					this.isDeadLocked = false;
				}
				System.out.print("chosen: "+ Boolean.toString(hasChosenNewNodeDL)+" | ");
			}
			else {
				System.out.print("no Esc | ");
				if (curr.isPresent() && !pdpModel.containerContains(this, curr.get()))
					this.negotiate(roadModel, time);
				else
					sendEmergency(roadModel);
			}
			System.out.println("pp "+ this.previousPoint.toString() + " -- np "+ this.nextPoint.toString() + " | ");
		}
		else if (this.mustMove && intPath.isEmpty()) { // && !curr.isPresent()) {										// If we are waiting and someone wants to pass
			System.out.print("must move  | ");

			if (!hasChosenNewNode) {																					// Choose a node where to move
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
				updateTargetPoints(roadModel, point, time);
				hasChosenNewNode = true;
			}

			if (tryToMove(roadModel, time)) {
				if (roadModel.getPosition(this).equals(nextPoint)) { 											// we have moved to the next node
					this.mustMove = false;
					this.hasChosenNewNode = false;
				}
			}
			else {
				this.sendEmergency(roadModel);
			}
			System.out.println("pp "+ this.previousPoint.toString() + " -- np "+ this.nextPoint.toString() + " | ");
		}
		else {

			if (!this.mustMove)
				System.out.print("is free | ");
			else
				System.out.print("must move | ");

			System.out.print("pp "+ this.previousPoint.toString() + " -- np "+ this.nextPoint.toString() + " | ");
			System.out.print("must check: "+Boolean.toString(checkPackIsForMe)+" | ");
			System.out.print("path length: "+Integer.toString(intPath.size())+" | ");
			if (curr.isPresent()) {
			    boolean inCargo = pdpModel.containerContains(this, curr.get());
				String cargoString = inCargo ? " in cargo" : " in transit";
				String pointString = inCargo ? curr.get().getDeliveryLocation().toString() : curr.get().getPickupLocation().toString();
				System.out.println("curr " + curr.get().getPackID() + " at " + pointString + cargoString + " | ");
			}
			else
				System.out.println("");

			if (!curr.isPresent()) {																					// If no pack in target


				Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
				Set<Pack> packSet = roadModel.getObjectsAt((RoadUser) this, Pack.class);

				if (packSet.isEmpty()) {																				// If no pack on us
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

				else {																									// A pack is on us
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

			if (curr.isPresent() && !checkPackIsForMe) {																// If we have a pack in target
				this.mustMove = false;
				final boolean inCargo = pdpModel.containerContains(this, curr.get());

				if (!inCargo && !roadModel.containsObject(curr.get())) {												// Sanity check
					System.out.println("[WARNING] -- Sanity Check");
					curr = Optional.absent();
				}

				else if (inCargo) {																						// We have the pack and we will deliver it
					if (!tryToMove(roadModel, time))
						sendEmergency(roadModel);

					if (roadModel.getPosition(this).equals(curr.get().getDeliveryLocation())) {                // deliver when we arrive
                        this.deliver(time);
						updateTargetPoints(roadModel, roadModel.getPosition(this), time);
					}
				}

				else {																									// We dont have yet the pack : go for it
					boolean hasNegotiated = false;

					if (!tryToMove(roadModel, time))
						hasNegotiated = negotiate(roadModel, time);

					if (hasNegotiated)
						updateTargetPoints(roadModel, roadModel.getPosition(this), time);
					else {
						if (roadModel.getPosition(this).equals(curr.get().getPickupLocation())) {               //pick up the thing
							this.pickUp(time, curr.get(), true);
							updateTargetPoints(roadModel, intPath.getLast(), time);
						}
					}

					//SANITY CHECK -- this should never happen in practice, only if there is a bug
					if (!hasNegotiated && this.intPath.isEmpty() && this.roadModel.getPosition(this) == this.nextPoint) {
						System.out.println("[WARNING] -- AIMING AT A PACK BUT NO WAY TO GET THERE -- PATH COMPUTATION");
						Set<RoadSign> roadSignSet = roadModel.getObjectsAt((RoadUser) this, RoadSign.class);
						intPath = getCorrespondingPath(roadSignSet, truckID, inCargo);
					}
				}
			}
		}
	}
}
