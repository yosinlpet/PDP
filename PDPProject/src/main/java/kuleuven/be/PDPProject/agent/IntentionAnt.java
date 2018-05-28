package kuleuven.be.PDPProject.agent;

import java.sql.Time;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.List;
import java.util.Arrays;
import java.util.Set;

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
import com.google.common.base.Optional;
import sun.awt.image.ImageWatched;

import kuleuven.be.PDPProject.agent.*;
	
public class IntentionAnt extends Ant{
	private Optional<CollisionGraphRoadModelImpl> roadModel;
	private TimeWindow roadSignsTimeWindow;

	Point currPos;
	Queue<Point> intentionPath;
	long[] arrivalTime; // Prediction of the arrival time 
	int numberOfHops;
	String packID;
	private boolean indicateDest;
	/**
	* Constructor
	* @param pckg {@link Pack} sending the {@link FeasabilityAnt}.
	* @param p	location where to initialize the {@link FeasabilityAnt}.
	**/
	public IntentionAnt(String packID, LinkedList<Point> intentionPath, long[] arrivalTime, boolean indicateDest) {
			roadModel = Optional.absent();
			this.arrivalTime = arrivalTime;
			this.intentionPath = intentionPath;
			this.packID = packID;
			this.indicateDest = indicateDest;
			numberOfHops = 0;
			hop();
		}

		@Override
		public void initRoadUser(RoadModel model) {
		    roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
	    }

		public void hop() {
			currPos = intentionPath.poll();
			numberOfHops++;
		}
		
		public boolean runIntAnt(){
			boolean success = true;
			for(long a : arrivalTime) {
				success = success && askFlag();
				hop();
			}
			if (success)
				raiseFlag();
			return success;
		}

		/**
		 * Leaves road signs on the {@link RoadModel}.
		 */
		public boolean askFlag() {
			boolean success = false;
			Set<RoadSign> roadSigns = collectInfo(RoadSign.class);
			for(RoadSign rs : roadSigns) {
					if(rs.getNext().equals(intentionPath.element()) 
								&& (this.packID).equals(rs.getPack().getPackID())
								&& !rs.isIntentionGiven()
								&& this.indicateDest == rs.indicateDest())
						success = true;
			}
			return success;
		}
		public void raiseFlag(){
			Set<RoadSign> roadSigns = collectInfo(RoadSign.class);
			for(RoadSign rs : roadSigns) {
					if(rs.getNext().equals(intentionPath.element())
								&& (this.packID).equals(rs.getPack().getPackID())
								&& !rs.isIntentionGiven()
								&& this.indicateDest == rs.indicateDest())
					    rs.setIntention(packID, arrivalTime[numberOfHops-1]);
			}
		}
	}

