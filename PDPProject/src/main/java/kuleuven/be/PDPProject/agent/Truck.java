package kuleuven.be.PDPProject.agent;

import java.util.Set;
import java.util.LinkedList;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
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
  
  private IntentionAnt intAnt;
  
  private LinkedList<Point> intPath;

  public Truck(Point startPosition, int capacity) {
    super(VehicleDTO.builder()
      .capacity(capacity)
      .startPosition(startPosition)
      .speed(SPEED)
      .build());
    curr = Optional.absent();

  }
  
  // Return the path to a pack that is the closest to the truck
  public LinkedList<Point> selectPackPath(Set<RoadSign> rsSet, TimeLapse time) {
		  int minCost=Integer.MAX_VALUE;
		  LinkedList<Point> minPath = new LinkedList<>();
		  RoadSign rsMin = new RoadSign();
		  IntentionAnt copy = new IntentionAnt(); 
		  for(RoadSign r : rsSet) {
			  if(!r.indicateDest()) {
				  intAnt = new IntentionAnt(r.getPackID(), r.getPath(), 
						  getArrivalTime(time, r.getDistList()), false);	  
				  if(r.getCost() < minCost && intAnt.askFlag()) {
					  copy = intAnt;
					  minPath = r.getPath();
					  minCost = r.getCost();
					  curr = Optional.fromNullable(r.getPack());
					  rsMin = r;
				  }
				  copy.runIntAnt();
				  break;
			  }
		  }
		  return minPath;
	  }
  
  public LinkedList<Point> getCorrespondingDestPath(Set<RoadSign> rsSet, 
		  											String packID, TimeLapse time){
	  LinkedList<Point> path = new LinkedList<>();
	  for(RoadSign r : rsSet) {
		  if(r.indicateDest()){
			  intAnt = new IntentionAnt(r.getPackID(), r.getPath(), 
					  getArrivalTime(time, r.getDistList()), true);
			  if(r.getPackID().equals(packID) && intAnt.askFlag()) {
				  path = r.getPath();
				  intAnt.runAnt();
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
    
    // If no pack in target
    if (!curr.isPresent()) {
    	 Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
         intPath =  selectPackPath(rs, time);
         if(!intPath.isEmpty()) {
             rm.moveTo(this, intPath.getFirst(), time);
    	 }
    }
    
    // If we have a pack in target
    if(curr.isPresent()) {
    	final boolean inCargo = pm.containerContains(this, curr.get());
    	
    	// Sanity check
        if (!inCargo && !rm.containsObject(curr.get())) {
            curr = Optional.absent();
          } 
        // We have the pack and we will deliver it
        else if (inCargo) {
        	  rm.moveTo(this, intPath.getFirst(), time);
              if (rm.getPosition(this).equals(curr.get().getDeliveryLocation())) {
                  // deliver when we arrive
                  curr.get().setDeposited();
                  pm.deliver(this, curr.get(), time);
                  curr = Optional.absent();
                }
          } 
        // We dont have yet the pack : go for it
        else {
          	rm.moveTo(this, intPath.getFirst(), time);
        	if(rm.equalPosition(this, curr.get())) {
        		pm.pickup(this, curr.get(), time);
        		Set<RoadSign> rs = rm.getObjectsAt((RoadUser) this, RoadSign.class);
        		intPath = getCorrespondingDestPath(rs,curr.get().getPackID(), time);
        		rm.moveTo(this, intPath.getFirst(), time);
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
