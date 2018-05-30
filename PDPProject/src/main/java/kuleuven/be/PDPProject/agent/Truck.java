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
import sun.awt.image.ImageWatched;


/**
 * Implementation of a very simple taxi agent. It moves to the closest customer,
 * picks it up, then delivers it, repeat.
 *
 * @author Rinde van Lon
 */
public class Truck extends Vehicle {
	private static final double SPEED = 1;
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
	private boolean mustAfterTick;

	private Pack waitingPack;
	private double waitingCost;
	private LinkedList<Point> waitingPath;

	String truckID;

	private LinkedList<Point> intPath;

	public Truck(Point startPosition, int capacity, String truckID) {
		super(VehicleDTO.builder()
				.capacity(capacity)
				.startPosition(startPosition)
				.speed(SPEED)
				.build());
		this.truckID = truckID;
		curr = Optional.absent();
		previousPoint = getStartPosition();
		intPath = new LinkedList<>();
		waitingPath = new LinkedList<>();
		isBackFlagDropped = true;
		isFrontFlagRaised = false;
		waitingCost = Double.MAX_VALUE;
		waitingPack = null;
	}

	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		// Set flag under us at the beginning
		intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
		isBackFlagDropped = !intAntBack.askFlag();
	}

	public void setEmergencyMove(boolean emergencyMove){
		this.mustMove = emergencyMove;
	}

	public String getTruckID() {
		return truckID;
	}

	// Return the path (without the first node!) to a pack that is the closest to the truck
	public LinkedList<Point> selectPackPath(Set<RoadSign> rsSet, TimeLapse time) {
		int minCost=Integer.MAX_VALUE;
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


	@Override
	public void afterTick(TimeLapse timeLapse) {
		if (mustAfterTick) {
		    boolean isAllowed = false;
			Set<RoadSign> roadSignSet = getRoadModel().getObjectsAt(this, RoadSign.class);
			for (RoadSign rs : roadSignSet)	{
				if (rs.getPack().equals(waitingPack) && !rs.indicateDest()) {
					isAllowed = (this.truckID == rs.getTruckID());
				}
			}

			if (isAllowed) {
				intPath = waitingPath;
				nextPoint = intPath.getLast();
				curr = Optional.fromNullable(waitingPack);
			}
		}
		mustAfterTick = false;
		waitingPath = new LinkedList<>();

	}

	@Override
	protected void tickImpl(TimeLapse time) {
		final RoadModel rm = getRoadModel();
		final PDPModel pm = getPDPModel();
		//System.out.print(truckID);
		//System.out.print(" has back flag dropped ? ");
		//System.out.print(isBackFlagDropped);
		//System.out.print(" has front flag raised ? ");
		//System.out.println(isFrontFlagRaised);
		//System.out.print(" has targeted object ? ");
		//System.out.println(curr.isPresent());
		//System.out.print("intention path: ");
		//System.out.print(intPath.toString());
		//System.out.print(" -- currPoint: ");
		//System.out.print(rm.getPosition(this).toString());
		//System.out.print(" -- prevPoint: ");
		//System.out.print(previousPoint.toString());
		// compute next and previous point
		if (this.mustMove && intPath.isEmpty()) {
		    if (nextPoint.equals(previousPoint)) {
				Collection<Point> pointCollection = ((GraphRoadModel) getRoadModel()).getGraph()
						.getOutgoingConnections(getRoadModel().getPosition(this));
				LinkedList<Point> points = new LinkedList<>();
				points.addAll(pointCollection);
				Point point = points.getFirst();
				previousPoint = nextPoint;
				nextPoint = point;
			}
			else if (rm.getPosition(this).equals(nextPoint)) {
		    	this.mustMove = false;
		    	previousPoint = nextPoint;
		    	isBackFlagDropped = false;
                isFrontFlagRaised = false;
			}

			if (!isFrontFlagRaised) {
				intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
				isFrontFlagRaised = intAntFront.askFlag();
			}
			if (isFrontFlagRaised) {
				rm.moveTo(this, nextPoint, time);
				if (!isBackFlagDropped) {
					intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
					if (!previousPoint.equals(nextPoint))
						isBackFlagDropped = intAntBack.dropFlag();
				}
			}
		}
		else {
			if (!intPath.isEmpty()) {
				if (rm.getPosition(this).equals(nextPoint)) {

					//System.out.print("Change in previous and next node for truck nb : ");
					//System.out.println(truckID);
					isFrontFlagRaised = false;
					isBackFlagDropped = false;
					previousPoint = nextPoint;
					nextPoint = intPath.removeLast();
				}
			}

			//System.out.print(" -- nextPoint: ");
			//if (nextPoint != null)
			//System.out.println(nextPoint.toString());

			// If no pack in target
			if (!curr.isPresent()) {
				Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
				Set<Pack> ps = rm.getObjectsAt((RoadUser) this, Pack.class);
				// If no pack on us
				if (ps.isEmpty()) {
					waitingPath = selectPackPath(rs, time);
					if (!waitingPath.isEmpty()) {
						for (RoadSign r : rs) {
							LinkedList<Point> path = new LinkedList<>(r.getPath());
							path.removeLast();
							if (path.equals(waitingPath)) {
								waitingPack = r.getPack();
								waitingCost = r.getCost();
							}
						}
						applicationAnt = new IntentionAnt(getRoadModel(), truckID, waitingPath.getFirst());
						applicationAnt.sendTruckID(truckID, waitingPack, (double) waitingCost);
						mustAfterTick = true;
					}
					//System.out.print(" for truck ");
					//System.out.println(this.truckID);

					//IMPORTANT in order to go to the end of your path under any circumstance !
					if (!intPath.isEmpty()) {
						if (!isFrontFlagRaised) {
							intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
							isFrontFlagRaised = intAntFront.askFlag();
						}
						if (isFrontFlagRaised) {
							rm.moveTo(this, nextPoint, time);
							if (!isBackFlagDropped) {
								intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
								if (!previousPoint.equals(nextPoint))
									isBackFlagDropped = intAntBack.dropFlag();
							}
						}
					}
				}
				// A pack is on us
				else {
					for (Pack p : ps) {
						System.out.println("Ouch, a Pack just landed on me!");
						applicationAnt = new IntentionAnt(getRoadModel(), truckID, rm.getPosition(this));
						applicationAnt.sendTruckID(truckID, p, 0.);

						curr = Optional.fromNullable(p);
						intPath = getCorrespondingDestPath(rs,
								curr.get().getPackID());
						//System.out.print(" for truck ");
						//System.out.println(this.truckID);

						//System.out.print("Change in previous and next node for truck nb : ");
						//System.out.println(truckID);

						previousPoint = rm.getPosition(this);
						nextPoint = intPath.removeLast();
						isBackFlagDropped = false;
						isFrontFlagRaised = false;
						break;
					}
					pm.pickup(this, curr.get(), time);
					curr.get().setCarried(true);
				}

			}

			// If we have a pack in target
			if (curr.isPresent() && !mustAfterTick) {
				final boolean inCargo = pm.containerContains(this, curr.get());

				// Sanity check
				if (!inCargo && !rm.containsObject(curr.get())) {
					curr = Optional.absent();
				}

				// We have the pack and we will deliver it
				else if (inCargo) {
					if (previousPoint.equals(this.previousPoint)) {
						urgentAnt = new IntentionAnt(getRoadModel(), truckID, nextPoint);
						urgentAnt.setEmergency();
					}
					if (!isFrontFlagRaised) {
						intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
						isFrontFlagRaised = intAntFront.askFlag();
					}
					if (isFrontFlagRaised) {
						rm.moveTo(this, nextPoint, time);
						if (!isBackFlagDropped) {
							intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
							if (!previousPoint.equals(nextPoint))
								isBackFlagDropped = intAntBack.dropFlag();
						}
						if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
							// deliver when we arrive
							pm.deliver(this, curr.get(), time);
							curr.get().setDeposited();
							previousPoint = rm.getPosition(this);
							isBackFlagDropped = false;
							isFrontFlagRaised = false;
							curr = Optional.absent();
						}
					} //else rm.moveTo(this, nextPoint, time);
				}
				// We dont have yet the pack : go for it
				else {

					if (!isFrontFlagRaised) {
						intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
						isFrontFlagRaised = intAntFront.askFlag();
					}
					if (isFrontFlagRaised) {
						rm.moveTo(this, nextPoint, time);
						intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
						if (!isBackFlagDropped) {
							if (!previousPoint.equals(nextPoint))
								isBackFlagDropped = intAntBack.dropFlag();
						}

						if (rm.equalPosition(this, curr.get())) {
							pm.pickup(this, curr.get(), time);
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
	}
}
