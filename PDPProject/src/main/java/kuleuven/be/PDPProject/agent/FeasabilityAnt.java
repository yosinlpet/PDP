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

public class FeasabilityAnt extends Ant implements Cloneable{
	private Pack pckg;
	private TimeWindow roadSignsTimeWindow;
	
	private Simulator sim;

	private boolean indicateDest;
	
	private static int feasabilityAntCounter = 0;

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
		roadModel = (GraphRoadModel) pckg.getRM();
		roadModel.addObjectAt(this, p);
		this.sim = sim;
		this.indicateDest = indicateDest;
		feasabilityAntCounter++;
		if(feasabilityAntCounter%100==0) {
			System.out.print("Number of feasability ants already created : ");
			System.out.println(feasabilityAntCounter);
		}
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
//TODO: make sure runAnt(clones) is a breadth-first algorithm.
	public void runAnt(LinkedList<FeasabilityAnt> clones){
	    if (clones.size() >0) {
			LinkedList<FeasabilityAnt> newClones = new LinkedList<>(clones);
			FeasabilityAnt clone = newClones.removeFirst();
			clone.dropSigns();
			for (FeasabilityAnt c : clone.getClones())
				newClones.addLast(c);
			runAnt(newClones);
		}
	}
	
	public void updateTruckID(LinkedList<FeasabilityAnt> clones, String truckID) {
		if (clones.size() >0) {
			LinkedList<FeasabilityAnt> newClones = new LinkedList<>(clones);
			FeasabilityAnt clone = newClones.removeFirst();

			Set<RoadSign> roadSignSet = this.roadModel.getObjectsAt(clone, RoadSign.class);
			for (RoadSign rs : roadSignSet) {
				if (!rs.indicateDest() && rs.getPack().equals(this.pckg))
					rs.setTruckID(truckID);
			}
			for (FeasabilityAnt c : clone.getClones())
				newClones.addLast(c);
			updateTruckID(newClones, truckID);
		}
	}
	
	/**
	 * Clones this {@link FeasabilityAnt} as many times as there are incoming nodes and moves them to these nodes.
	 * @return 	The collection of new {@link FeasabilityAnt}s on the incoming nodes, they carry the same info as the
	 * 			initial one.
	 */
	public LinkedList<FeasabilityAnt> getClones() {
		Point p = path.getLast();
		Graph<?> graph = roadModel.getGraph();
		Collection<Point> pointsCollection = graph.getIncomingConnections(p);
		LinkedList<FeasabilityAnt> clones = new LinkedList<>();

		for(Point p2 : pointsCollection){
			FeasabilityAnt c;
			try {
                c = clone(this);
                c.addToPath(p2);
				if(!c.getHashMap().containsKey(p2.toString())) {	
					clones.add(c);
					roadModel.addObjectAt(c, p2);
					c.getHashMap().put(p2.toString(),p2);
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
			RoadSign roadSign = new RoadSign(pckg, path, roadSignsTimeWindow, indicateDest, getDistList(), "nobody");
			sim.register(roadSign);
			roadModel.addObjectAt(roadSign, path.getLast());	
	}
	

    /**
     * Add a point to the path.
     * @param p point to be added.
     */
    public void addToPath(Point p) {
        path.add(p);
    }

	public LinkedList<Point> getPath() {
		return this.path;
	}
	

	public Map<String,Point> getHashMap() {
		return hashMap;
	}

	public LinkedList<Long> getDistList() {
    	LinkedList<Long> dl = new LinkedList<>();
    	LinkedList<Point> p = new LinkedList<>(path);
		LinkedList<Point> p2 = new LinkedList<>(path);
		p2.removeLast();
    	while (p.size()> 1) {
    		LinkedList<Point> a = new LinkedList<Point>();
    		a.addLast(p.getLast());
    		a.addLast(p2.getLast());
			Measure<Double, Length> md = roadModel.getDistanceOfPath(a);
			dl.addLast((long) md.doubleValue(md.getUnit()));
			p2.removeLast();
			p.removeLast();
		}
		return dl;
	}
}
