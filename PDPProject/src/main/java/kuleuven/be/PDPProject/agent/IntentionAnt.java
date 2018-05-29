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
	private TimeWindow roadSignsTimeWindow;

	LinkedList<Point> intentionPath;
	LinkedList<Point> currPath;
	long[] arrivalTime; // Prediction of the arrival time 
	int numberOfHops;
	String packID;
	private boolean indicateDest;

	public IntentionAnt() {
		
	}
	/**
	* Constructor
	* @param packID {@link Pack} sending the {@link FeasabilityAnt}.
	* @param indicateDest flag to know if the road sign si for deliveryRoadSign or PackRoadSign
    * @param arrivalTime list of forecast arrival times
    **/
	public IntentionAnt(GraphRoadModel roadModel, String packID, LinkedList<Point> intentionPath, long[] arrivalTime, boolean indicateDest) {
			this.arrivalTime = arrivalTime;
			this.currPath = new LinkedList<>(intentionPath);
            this.intentionPath = intentionPath;
			this.packID = packID;
			super.roadModel = roadModel;
			this.indicateDest = indicateDest;
			super.roadModel.addObjectAt(this, currPath.getLast());
			numberOfHops = 0;
		}

		@Override
		public void initRoadUser(RoadModel model) {
		    //roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
	    }

		public void hop() {
	    	roadModel.removeObject(this);
			currPath.removeLast();
			roadModel.addObjectAt(this, currPath.getLast());
			numberOfHops++;
		}

		public void resetHop(){
			numberOfHops = 0;
			currPath = new LinkedList<>(this.intentionPath);
			roadModel.removeObject(this);
			roadModel.addObjectAt(this, intentionPath.getLast());
		}

		public void runIntAnt(TimeLapse time){
			for (long a : arrivalTime) {
				raiseFlag(a, time);
			}
		}

		/**
		 * Leaves road signs on the {@link RoadModel}.
		 */
		public boolean askFlag() {
			boolean s1 = true;
			boolean s2 = false;
			for(long a : arrivalTime) {
				s2 = false;
				Set<RoadSign> roadSigns = collectInfo();
				System.out.println(roadSigns.size());
				for(RoadSign rs : roadSigns) {
						if(!rs.isIntentionGiven() && indicateDest == rs.indicateDest()) {
						    System.out.println("[SUCCESS]");
							s2 = true;
							break;
						}
				}
				s1 = s1 && s2;
				hop();
			}
			resetHop();
			return s1;
		}
		
		
		public void raiseFlag(long a, TimeLapse time){
			Set<RoadSign> roadSigns = collectInfo();
			System.out.println(a);
			for(RoadSign rs : roadSigns) {
					if((this.packID).equals(rs.getPack().getPackID()))
					    rs.setIntention(packID, a, time);
			}
		}
	}

