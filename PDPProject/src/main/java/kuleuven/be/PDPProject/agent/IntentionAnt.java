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
	
public class IntentionAnt extends Ant{

	Point node;
	String truckID;
	static int intentionAntCounter = 0;

	public IntentionAnt() {

	}
	/**
	* Constructor
    **/
	public IntentionAnt(RoadModel roadModel, String truckID, Point node) {

			this.node = node;
			super.roadModel = (GraphRoadModel) roadModel;
			super.roadModel.addObjectAt(this, node);
			this.truckID = truckID;
			intentionAntCounter++;
			if(intentionAntCounter%100==0) {
				System.out.print("Number of intention ants already created : ");
				System.out.println(intentionAntCounter);
			}
		}
	

		@Override
		public void initRoadUser(RoadModel model) {
		}

		/**
		 * Leaves road signs on the {@link RoadModel}.
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
					//System.out.println(node);
					return true;
				}else return false;
			}
		}

		public boolean dropFlag() {
			Set<Flag> flagSet = roadModel.getObjectsAt((RoadUser) this, Flag.class);
			if(flagSet.size()==0) {
				return false;
			}
			else {
				Flag flag = new Flag();
				for(Flag f2 : flagSet) {flag=f2;}
				return flag.dropFlag(truckID);
				//System.out.print("Flag dropped by ");
				//System.out.print(truckID);
				//System.out.print(" at ");
				//System.out.println(node);
			}
		}

		public void sendTruckID(String truckID, Pack pack, double cost) {
			Set<Pack> packSet = this.roadModel.getObjectsAt(this, Pack.class);
			for (Pack p : packSet) {
				System.out.println(pack.toString() +" and " +p.toString() + "should be the same to candidate");
				if (p.equals(pack))
					pack.setCandidate(truckID, cost);
			}
		}

		public void setEmergency() {
			Set<Truck> truckSet = this.roadModel.getObjectsAt(this, Truck.class);
			for (Truck t : truckSet) {
			    System.out.println("Truck found: "+ t.getTruckID());
				t.setEmergencyMove(true);
			}
		}
	}

