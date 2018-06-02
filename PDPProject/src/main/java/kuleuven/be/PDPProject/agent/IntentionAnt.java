package kuleuven.be.PDPProject.agent;

import java.sql.Time;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.Arrays;
import java.util.Set;

import com.github.rinde.rinsim.core.model.road.GraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import java.util.LinkedList;


import com.github.rinde.rinsim.util.TimeWindow;
import com.sun.xml.internal.bind.v2.TODO;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.google.common.base.Optional;
import sun.awt.image.ImageWatched;

import kuleuven.be.PDPProject.agent.*;

/**
 * Ants sent by {@link Truck}s for expressing their wishes.
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class IntentionAnt extends Ant{

	Point node;
	String truckID;
	public static int intentionAntCounter = 0;

	/**
	 * Constructor
	 **/
	public IntentionAnt(RoadModel roadModel, String truckID, Point node) {

		this.node = node;
		super.roadModel = (GraphRoadModel) roadModel;
		super.roadModel.addObjectAt(this, node);
		this.truckID = truckID;
		intentionAntCounter++;
	}

	@Override
	public void initRoadUser(RoadModel model) {
	}

	/**
	 * Raise a flag is no {@link Truck} claims the position
	 */
	public boolean askFlag() {
		Set<Flag> flag = roadModel.getObjectsAt((RoadUser) this, Flag.class);

		// If no flag yet
		if(flag.size()==0) {
			Flag f = new Flag(truckID);
			roadModel.addObjectAt(f, node);
			//System.out.print("Flag created by ");
			//System.out.print(truckID);
			//System.out.print(" at ");
			//System.out.println(node);
			return true;
		}
		// If flag : try to change the name
		else {
			Flag f = new Flag();
			for(Flag f2 : flag) {f=f2;}
			if(f.raiseFlag(truckID)) {
				//System.out.print("Flag raised by ");
				//System.out.print(truckID);
				//System.out.print(" at ");
				// System.out.println(node);
				return true;
			}else return false;
		}
	}


	/**
	 * Removes the flag
	 * @return the success or failure of the operation
	 */
	public boolean dropFlag() {
		Set<Flag> flagSet = roadModel.getObjectsAt((RoadUser) this, Flag.class);
		if(flagSet.size()==0) {
			return false;
		}
		else {
			Flag flag = new Flag();
			for(Flag f2 : flagSet) {flag=f2;}
			//System.out.print("Flag dropped by ");
			//System.out.print(truckID);
			//System.out.print(" at ");
			//System.out.println(node);
			return flag.dropFlag(truckID);
		}
	}

	/**
	 * Sends the <code>truckID</code> of the {@link Truck} to the closest {@link Pack}.
	 * @param truckID the ID
	 * @param pack the {@link Pack} we candidate for
	 * @param cost the cost
	 */
	public void sendTruckID(String truckID, Pack pack, double cost) {
		Set<Pack> packSet = this.roadModel.getObjectsAt(this, Pack.class);
		for (Pack p : packSet) {
			if (p.equals(pack))
				pack.setCandidate(truckID, cost);
		}
	}

	/**
	 * Tells the {@link Truck} in front of us that we are in a hurry...
     * @return true if the {@link Truck} blocking us can move
	 */
	public boolean setEmergency() {
		Set<Truck> truckSet = this.roadModel.getObjectsAt(this, Truck.class);
		for (Truck t : truckSet) {
			if (t.getIntPath().size() == 0) {
				t.setEmergencyMove();
				return t.getTrapped();
			}
		}
		return false;
	}

	/**
	 * Gives the {@link Truck} in front of us the intention to get {@link Pack} we have to go get.
	 * @param truck ourselves
	 * @return the success or failure of the operation
	 */
	public boolean negotiate(Truck truck){
		LinkedList<Point> myPath = new LinkedList<>(truck.getIntPath());
		Set<Truck> truckSet = this.roadModel.getObjectsAt(this, Truck.class);
		if (myPath.isEmpty()) {
			// if empty, the truck is on the Pack and will just take it.
			for (Truck t : truckSet) {
				if (t.getIntPath().size() == 0 && !t.isCarrying() && !t.hasCurr()) {
				    t.setCurr(truck.getCurr());
				    truck.setCurr(Optional.<Pack>absent());
				    return true;
                }
			}
		}
		else {
			//else, we need to do fancy stuff to make sure the former Truck won't be wanting the same Pack as the new Truck.
			Set<RoadSign> roadSignSet = this.roadModel.getObjectsAt(this, RoadSign.class);
            myPath.removeLast();											//We can remove the candidate's position from the path
			for (Truck t : truckSet) {
				if (t.getIntPath().size() == 0 && !t.isCarrying() && !t.hasCurr()) {
					t.setCurr(truck.getCurr());
					Pack pack = truck.getCurr().get();
					pack.setBestTruckKnown(false);
					for (RoadSign rs : roadSignSet){
						if (rs.getPackID() == pack.getPackID() && !rs.indicateDest())
							t.sendMyIntention(roadModel, pack.getPickupLocation(), pack, rs.getCost());
					}
					t.setIntPath(myPath);
					System.out.println(t.getTruckID() + " gets pack of " + truck.getTruckID() + " _2_");
					truck.setIntPath(new LinkedList<Point>());
					truck.setCurr(Optional.<Pack>absent());
					return true;
				}
			}
		}
		return false;
	}
}
