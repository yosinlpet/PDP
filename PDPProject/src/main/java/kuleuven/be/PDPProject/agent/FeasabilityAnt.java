package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.core.Simulator;
import java.sql.Time;
import java.util.*;
import javax.measure.Measure;
import javax.measure.quantity.Length;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;


import com.github.rinde.rinsim.util.TimeWindow;
import com.sun.xml.internal.bind.v2.TODO;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;
import sun.awt.image.ImageWatched;

import javax.measure.Measure;

/**
 * Ants in order to send information from {@link Pack}s and Customers to the environment.
 * The information is dropped by means of {@link RoadSign}s.
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class FeasabilityAnt extends Ant implements Cloneable{
	private Pack pckg;
	private TimeWindow roadSignsTimeWindow;
	
	private Simulator sim;

	private boolean indicateDest;
	
	public static int feasabilityAntCounter = 0;

	private Map<String,Point> hashMap = new HashMap<String,Point>(); 
	/**
	 * Constructor
	 * @param pckg {@link Pack} sending the {@link FeasabilityAnt}.
	 * @param p	location where to initialize the {@link FeasabilityAnt}.
	 **/
	public FeasabilityAnt(Pack pckg, Point p, TimeWindow timeWindow, 
			Simulator sim, boolean indicateDest){
		path = new LinkedList<>();
		path.add(p);
		hashMap.put(p.toString(),p);
		this.pckg = pckg;
		roadSignsTimeWindow = timeWindow;
		roadModel = (GraphRoadModel) pckg.getRoadModel();
		roadModel.addObjectAt(this, p);
		this.sim = sim;
		this.indicateDest = indicateDest;
		feasabilityAntCounter++;
	}

	//  Constructor used for George cloning
	public  FeasabilityAnt(GraphRoadModel roadModel, LinkedList<Point> path, Pack pckg,
					TimeWindow roadSignsTimeWindow, Map<String,Point> hashMap, 
					Simulator sim, boolean indicateDest)
	{
		super.roadModel = roadModel;
		super.path = new LinkedList<Point>(path);
		this.hashMap = hashMap;
		this.pckg = pckg;
		this.roadSignsTimeWindow = roadSignsTimeWindow;
		this.sim = sim;
		this.indicateDest = indicateDest;
		
		feasabilityAntCounter++;
		if(feasabilityAntCounter%100==0) {
			System.out.print("Number of feasability ants already created : ");
			System.out.println(feasabilityAntCounter);
		}
	}


	public FeasabilityAnt clone(FeasabilityAnt cloner) throws CloneNotSupportedException{
		if (!( cloner instanceof Cloneable))
		throw new CloneNotSupportedException("Invalid Cloning");
		return new FeasabilityAnt(cloner.roadModel, cloner.path, cloner.pckg,
				cloner.roadSignsTimeWindow,cloner.hashMap, cloner.sim, cloner.indicateDest);
	}

	@Override
	public void initRoadUser(RoadModel model) {
	  //  roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
    }

	public LinkedList<Point> getPath() {
		return this.path;
	}

	public Map<String,Point> getHashMap() {
		return hashMap;
	}

	/**
	 * Tells the Feasibility Ant to place {@link RoadSign}s on the map.
	 * @param clones list of the other created ants for the breadth-first algorithm of placing new {@link RoadSign}s.
	 */
	public void runAnt(LinkedList<FeasabilityAnt> clones){
	    if (clones.size() > 0) {
			LinkedList<FeasabilityAnt> newClones = new LinkedList<>(clones);
			FeasabilityAnt clone = newClones.removeFirst();								//BreadthFirst: remove element at beginning
			clone.dropSigns();															//Actual dropping of the signs
			for (FeasabilityAnt c : clone.getClones())
				newClones.addLast(c);													//BreadthFirst: add children at rear of the queue
			runAnt(newClones);															//BreadthFirst: and run the code with the new queue
		}
	}

	/**
	 * In the same fashion as <code>runAnt</code>, edit the truckID on the {@link RoadSign}s.
	 * @param clones list of clones for breadth-first editing
	 * @param truckID the <code>truckID</code> to write on the {@link RoadSign}
	 */
	public void updateTruckID(LinkedList<FeasabilityAnt> clones, String truckID) {
		if (clones.size() >0) {
			LinkedList<FeasabilityAnt> newClones = new LinkedList<>(clones);
			FeasabilityAnt clone = newClones.removeFirst();

			Set<RoadSign> roadSignSet = this.roadModel.getObjectsAt(clone, RoadSign.class); //Collect the RoadSigns at this position
			for (RoadSign rs : roadSignSet) {
				if (!rs.indicateDest() && rs.getPack().equals(this.pckg))					//Make sure it indicates the Pack
					rs.setTruckID(truckID);													//Do the job & edit truckID
			}
			for (FeasabilityAnt c : clone.getClones())
				newClones.addLast(c);
			updateTruckID(newClones, truckID);
		}
	}
	
	/**
	 * Clones this {@link FeasabilityAnt} as many times as there are incoming nodes and moves them to these nodes.
	 * @return  the list of clones
	 */
	public LinkedList<FeasabilityAnt> getClones() {
		Point p = path.getLast();												//current position is last in path
		Graph<?> graph = roadModel.getGraph();
		Collection<Point> pointsCollection = graph.getIncomingConnections(p);
		LinkedList<FeasabilityAnt> clones = new LinkedList<>();

		for(Point p2 : pointsCollection){
			FeasabilityAnt c;
			try {
                c = clone(this);												//clone the ant and...
                c.addToPath(p2);												//add put it on position p
				if(!c.getHashMap().containsKey(p2.toString())) {				//can't exist already
					clones.add(c);
					roadModel.addObjectAt(c, p2);								//then we seriously create it
					c.getHashMap().put(p2.toString(),p2);						//and memorize we just did it
				}
				
			}
			catch (CloneNotSupportedException e) {
				System.out.println("[EXCEPTION CAUGHT] -- Impossible to clone!");
			}
		}
		return clones;
	}
	
	/**
	 * Leaves road signs on the {@link RoadModel}.
	 */
	public void dropSigns() {
			RoadSign roadSign = new RoadSign(pckg, path, roadSignsTimeWindow, indicateDest, "nobody");
			roadModel.addObjectAt(roadSign, path.getLast());
            sim.register(roadSign);
	}
	

    /**
     * Add a point to the path.
     * @param p point to be added.
     */
    public void addToPath(Point p) {
        path.add(p);
    }
}
