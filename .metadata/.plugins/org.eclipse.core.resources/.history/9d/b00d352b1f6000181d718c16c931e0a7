package kuleuven.be.PDPProject.agent;


import java.sql.Time;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import java.util.LinkedList;


import com.github.rinde.rinsim.util.TimeWindow;
import com.sun.xml.internal.bind.v2.TODO;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;
import sun.awt.image.ImageWatched;

public class FeasabilityAnt extends Ant implements Cloneable{
	private LinkedList<Pack> packages;
	private TimeWindow roadSignsTimeWindow;

	private Map<String,Point> hashMap = new HashMap<String,Point>(); 
	
	/**
	 * Constructor
	 * @param pckg {@link Pack} sending the {@link FeasabilityAnt}.
	 * @param p	location where to initialize the {@link FeasabilityAnt}.
	 **/
	public FeasabilityAnt(Pack pckg, Point p, TimeWindow timeWindow){
		path = new LinkedList<>();
		path.add(p);
		hashMap.put(p.toString(),p);
		packages = new LinkedList<>();
		packages.add(pckg);
		roadSignsTimeWindow = timeWindow;
		roadModel = (GraphRoadModel) pckg.getRM();

	}

	//  Constructor used for George cloning
	public  FeasabilityAnt(GraphRoadModel roadModel, LinkedList<Point> path, LinkedList<Pack> packages, TimeWindow roadSignsTimeWindow, Map<String,Point> hashMap)
	{
		super.roadModel = roadModel;
		super.path = new LinkedList<Point>(path);
		this.hashMap = hashMap;
		this.packages = new LinkedList<Pack>(packages);
		this.roadSignsTimeWindow = roadSignsTimeWindow;
	}

	public FeasabilityAnt clone(FeasabilityAnt cloner) throws CloneNotSupportedException{
		if (!( cloner instanceof Cloneable))
		throw new CloneNotSupportedException("Invalid Cloning");
		return new FeasabilityAnt(cloner.roadModel, cloner.path, cloner.packages, cloner.roadSignsTimeWindow,cloner.hashMap);
	}

	@Override
	public void initRoadUser(RoadModel model) {
	  //  roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
    } 

	@Override
	public void runAnt(){
		Collection<FeasabilityAnt> clones = getClones();
		//System.out.println("Signs dropped");
		for (FeasabilityAnt clone : clones) {
			clone.dropSigns();	
		}
		for (FeasabilityAnt clone : clones) {
			clone.runAnt();
		}

		
		//dropSigns();
	
		//clone.runAnt();
		//collectInfo(RoadSign.class);
		//System.out.println("Collected tameeeer");
	}

	public Map<String,Point> getHashMap() {
		return hashMap;
	}
	
	/**
	 * Clones this {@link FeasabilityAnt} as many times as there are incoming nodes and moves them to these nodes.
	 * @return 	The collection of new {@link FeasabilityAnt}s on the incoming nodes, they carry the same info as the
	 * 			initial one.
	 */
	public Collection<FeasabilityAnt> getClones() {
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
					c.getHashMap().put(p2.toString(),p2);
					//System.out.println("Cloned at :"+p2.toString());
					
				}
				
			}
			catch (CloneNotSupportedException e) {
				System.out.println("[EXCEPTION CAUGHT] -- Impossible to clone!");
			}
		}
		return clones;
	}

    /**
     * Add a point to the path.
     * @param p point to be added.
     */
    public void addToPath(Point p) {
        path.add(p);
    }

	/**
	 * Leaves road signs on the {@link RoadModel}.
	 */
	public void dropSigns() {
		for (Pack pckg : packages) {
			RoadSign roadSign = new RoadSign(pckg, path, roadSignsTimeWindow);
			roadModel.addObjectAt(roadSign, path.getLast());
		}
	}

}
