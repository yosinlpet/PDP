package kuleuven.be.PDPProject.agent;

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
  private static final double SPEED = 0.5;
  private Optional<Pack> curr;

  private IntentionAnt intAntFront;
  private IntentionAnt intAntBack;
  private Point nextPoint;
  private Point previousPoint;

  private boolean hasRaisedFlag;
  private boolean abandon;

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
    intPath = new LinkedList<Point>();
    hasRaisedFlag = false;
    abandon=false;
  }

  // Return the path (without the first node!) to a pack that is the closest to the truck
  public LinkedList<Point> selectPackPath(Set<RoadSign> rsSet, TimeLapse time) {
		  int minCost=Integer.MAX_VALUE;
		  LinkedList<Point> minPath = new LinkedList<>();
		  for(RoadSign rs : rsSet) {
			  if(!rs.indicateDest()) {
				  nextPoint = rs.getNext(); // ??
				  if(rs.getCost() < minCost) {
					  minPath = rs.getPath();
					  minCost = rs.getCost();
					  curr = Optional.fromNullable(rs.getPack());
					  try {
						  minPath.removeLast();
					  } catch (NoSuchElementException e){
					  	System.out.print("Bad boy is at :");
					  	System.out.print(getRoadModel().getPosition(this));
					  	System.out.print(" for ID :");
					  	System.out.println(this.truckID);
					  	throw new NoSuchElementException("Ok ta mer");
					  }
				  }
			  }
		  }
		  System.out.print("Path is ");
		  System.out.print(minPath.toString());
		  return minPath;
	  }

  public LinkedList<Point> getCorrespondingDestPath(Set<RoadSign> rsSet,
		  											String packID){
	  LinkedList<Point> path = new LinkedList<>();
	  for(RoadSign rs : rsSet) {
		  if(rs.indicateDest()){
			  if(rs.getPackID().equals(packID)) {
				  path = rs.getPath();
				  path.removeLast();
				  break;
			  }
		  }
	  }
	  System.out.print("Path is ");
	  System.out.print(path.toString());
	  return path;
  }


  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();

	  // compute next and previous point
	 if(!intPath.isEmpty()) {
		  if(rm.getPosition(this).equals(nextPoint) && !abandon) {

			  System.out.print("Change in previous and next node for truck nb : ");
			  System.out.println(truckID);

			  previousPoint = nextPoint;
			  nextPoint = intPath.removeLast();
		  }
	  }

    // If no pack in target
    if (!curr.isPresent()) {
    	if(hasRaisedFlag && abandon) {
    		intAntFront = new IntentionAnt(getRoadModel(), truckID, nextPoint);
    		if (intAntFront.askFlag()) {
				rm.moveTo(this, nextPoint, time);
				intAntBack = new IntentionAnt(getRoadModel(), truckID, previousPoint);
				intAntBack.dropFlag();
			}

    		if(rm.getPosition(this).equals(nextPoint))
    			abandon = false;
    	}
    	else {
	    	 Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
	    	 Set<Pack> ps = rm.getObjectsAt((RoadUser) this, Pack.class);
	         // If no pack on us
	         if(ps.isEmpty()) {
				 intPath =  selectPackPath(rs, time);
				 System.out.print(" for truck ");
				 System.out.println(this.truckID);
		         if(!intPath.isEmpty()) {
		        	 intAntFront = new IntentionAnt(getRoadModel(),truckID,nextPoint);
		        	 if(intAntFront.askFlag()) {
		        		 rm.moveTo(this, nextPoint, time);
						 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
						 intAntBack.dropFlag();

		        	 } else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
		        		 rm.moveTo(this, nextPoint, time);
						 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
						 intAntBack.dropFlag();

		        	 } else {

		        	 }
		         }
	         }
	         // A pack is on us
	         else {
	        	 for(Pack p : ps) {
	        		 curr = Optional.fromNullable(p);
	        		 intPath = getCorrespondingDestPath(rs,
								curr.get().getPackID());
					 System.out.print(" for truck ");
					 System.out.println(this.truckID);

                     System.out.print("Change in previous and next node for truck nb : ");
                     System.out.println(truckID);

	        		 previousPoint = rm.getPosition(this);
	        		 nextPoint = intPath.removeLast();
	        		 break;
	        	 }
	        	 pm.pickup(this, curr.get(), time);
	        	 curr.get().isCarried();
	         }
    	}
    }

    // If we have a pack in target
    if(curr.isPresent()) {
    	final boolean inCargo = pm.containerContains(this, curr.get());

    	// Sanity check
        if (!inCargo && !rm.containsObject(curr.get())) {
            curr = Optional.absent();
            abandon = true;
          }

        // We have the pack and we will deliver it
        else if (inCargo) {
        	 intAntFront = new IntentionAnt(getRoadModel(),truckID,nextPoint);
        	 if(intAntFront.askFlag()) {
        		 rm.moveTo(this, nextPoint, time);
				 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
				 intAntBack.dropFlag();

                 if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
                     // deliver when we arrive
                     pm.deliver(this, curr.get(), time);
					 curr.get().setDeposited();
					 previousPoint = rm.getPosition(this);
                     curr = Optional.absent();
                 }
        	 }
        	 else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
        		 rm.moveTo(this, nextPoint, time);
				 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
				 intAntBack.dropFlag();

        	 } //else rm.moveTo(this, nextPoint, time);
          }
        // We dont have yet the pack : go for it
        else {
         	 intAntFront = new IntentionAnt(getRoadModel(),truckID,nextPoint);
         	 if(intAntFront.askFlag()) {
         		 hasRaisedFlag = true;
         		 rm.moveTo(this, nextPoint, time);
				 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
				 intAntBack.dropFlag();

         		 if(rm.equalPosition(this, curr.get())) {
	        		pm.pickup(this, curr.get(), time);
					curr.get().isCarried();

	        		Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
	        		intPath = getCorrespondingDestPath(rs,curr.get().getPackID());
					 System.out.print(" for truck ");
					 System.out.println(this.truckID);
         		 }
        	} else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
       		 rm.moveTo(this, nextPoint, time);
				 intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
				 intAntBack.dropFlag();

       	 	}
        }
        }
    }

}
