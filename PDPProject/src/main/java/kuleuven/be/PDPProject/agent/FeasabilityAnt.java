package kuleuven.be.PDPProject.agent;


import java.sql.Time;
import java.util.Collection;
import java.util.Map;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import java.util.LinkedList;


import com.github.rinde.rinsim.util.TimeWindow;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;
import sun.awt.image.ImageWatched;

public class FeasabilityAnt extends Ant implements Cloneable{
	private Optional<CollisionGraphRoadModelImpl> roadModel;
	private LinkedList<Pack> packages;
	private TimeWindow roadSignsTimeWindow;

	/**
	 * Constructor
	 * @param pckg {@link Pack} sending the {@link FeasabilityAnt}.
	 * @param p	location where to initialize the {@link FeasabilityAnt}.
	 **/
	public FeasabilityAnt(Pack pckg, Point p, TimeWindow timeWindow) {
		roadModel = Optional.absent();
		path = new LinkedList<>();
		path.add(p);
		packages = new LinkedList<>();
		packages.add(pckg);
		roadSignsTimeWindow = timeWindow;
	}

	public  FeasabilityAnt(Optional<CollisionGraphRoadModelImpl> roadModel, LinkedList<Point> path, LinkedList<Pack> packages, TimeWindow roadSignsTimeWindow)
	{
		this.roadModel = roadModel;
		super.path = path;
		this.packages = packages;
		this.roadSignsTimeWindow = roadSignsTimeWindow;
	}

	public FeasabilityAnt clone(FeasabilityAnt cloner) throws CloneNotSupportedException{
		if (!( cloner instanceof Cloneable))
		throw new CloneNotSupportedException("Invalid Cloning");
		return new FeasabilityAnt(cloner.roadModel, cloner.path, cloner.packages, cloner.roadSignsTimeWindow);
	}

	@Override
	public void initRoadUser(RoadModel model) {
	    roadModel = Optional.of((CollisionGraphRoadModelImpl) model);
    }

	@Override
	public void runAnt(){
		dropSigns();
		collectInfo();
		getClones();
	}


	/**
	 * Clones this {@link FeasabilityAnt} as many times as there are incoming nodes and moves them to these nodes.
	 * @return 	The collection of new {@link FeasabilityAnt}s on the incoming nodes, they carry the same info as the
	 * 			initial one.
	 */
	public Collection<FeasabilityAnt> getClones() {
		Point p = path.getLast();
		ListenableGraph<?> graph = roadModel.get().getGraph();
		Collection<Point> pointsCollection = graph.getIncomingConnections(p);
		LinkedList<FeasabilityAnt> clones = new LinkedList<>();

		for(Point p2 : pointsCollection){
			FeasabilityAnt clone;
			try {
                clone = clone(this);
				clone.moveTo(p2);
				clone.addToPath(p2);
				clones.add(clone);
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
        this.path.add(p);
    }

	/**
	 * Leaves road signs on the {@link RoadModel}.
	 */
	public void dropSigns() {
		for (Pack pckg : packages) {
			RoadSign roadSign = new RoadSign(pckg, path, roadSignsTimeWindow);
			roadModel.get().addObjectAt(roadSign, path.getLast());
		}
	}


}
