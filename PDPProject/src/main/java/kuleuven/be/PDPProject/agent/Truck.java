package kuleuven.be.PDPProject.agent;

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
  private static final double SPEED = 1000d;
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
		  for(RoadSign r : rsSet) {
			  if(!r.indicateDest()) {
				  nextPoint = r.getNext(); // ??
				  if(r.getCost() < minCost) {
					  minPath = r.getPath();
					  minCost = r.getCost();
					  curr = Optional.fromNullable(r.getPack());
					  minPath.removeLast();
				  }
			  }
		  }
		  return minPath;
	  }
  
  public LinkedList<Point> getCorrespondingDestPath(Set<RoadSign> rsSet, 
		  											String packID){
	  LinkedList<Point> path = new LinkedList<>();
	  for(RoadSign r : rsSet) {
		  if(r.indicateDest()){
			  if(r.getPackID().equals(packID)) {
				  path = r.getPath();
				  path.removeLast();
				  break;
			  }
		  }
	  }
	  return path;
  }
  
  
  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();
    
    intAntBack = new IntentionAnt(getRoadModel(),truckID,previousPoint);
    intAntBack.dropFlag();

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
    		rm.moveTo(this, nextPoint, time);
    		if(rm.getPosition(this).equals(nextPoint)) 
    			abandon=false;
    	}
    	else {
	    	 Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
	    	 Set<Pack> ps = rm.getObjectsAt((RoadUser) this, Pack.class);
	         intPath =  selectPackPath(rs, time);
	         // If no pack on us
	         if(ps.isEmpty()) {
		         if(!intPath.isEmpty()) {
		        	 intAntFront = new IntentionAnt(getRoadModel(),truckID,nextPoint);
		        	 if(intAntFront.askFlag()) {
		        		 rm.moveTo(this, nextPoint, time);
		        	 } else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
		        		 rm.moveTo(this, nextPoint, time);
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
	        		 break;
	        	 }
	        	 pm.pickup(this, curr.get(), time);
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
                 if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
                     // deliver when we arrive
                     curr.get().setDeposited();
                     pm.deliver(this, curr.get(), time);
                     curr = Optional.absent();
                 }
        	 } else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
        		 rm.moveTo(this, nextPoint, time);
        	 } //else rm.moveTo(this, nextPoint, time);
          } 
        // We dont have yet the pack : go for it
        else {
         	 intAntFront = new IntentionAnt(getRoadModel(),truckID,nextPoint);
         	 if(intAntFront.askFlag()) {
         		 hasRaisedFlag = true;
         		 rm.moveTo(this, nextPoint, time);
         		 if(rm.equalPosition(this, curr.get())) {
	        		pm.pickup(this, curr.get(), time);
	        		Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
	        		intPath = getCorrespondingDestPath(rs,curr.get().getPackID());
         		 }
        	} else if(Point.distance(rm.getPosition(this), nextPoint) < 0.5){
       		 rm.moveTo(this, nextPoint, time);
       	 	}
        }
        }
    }

    public long[] getArrivalTime(TimeLapse timeLapse, LinkedList<Long> distList){
        long currTime = timeLapse.getTime();
        //long[] arrivalTime = new long[distList.size()];
        long[] arrivalTime = new long[1];
        int i = 0;
        //for (long a : distList){
            //arrivalTime[i] = currTime+Math.round(a/SPEED);
        //   ++i;
        //}
        arrivalTime[0] = currTime+Math.round(distList.getLast()/SPEED);

        return arrivalTime;
    }
}
