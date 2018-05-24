package kuleuven.be.PDPProject.agent;


import java.util.Collection;
import java.util.Map;

import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import java.util.Queue;
import java.util.LinkedList;


import javax.measure.unit.SI;
import javax.xml.soap.Node;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.google.common.base.Optional;

public class FeasabilityAnt implements Ant, Cloneable{
	private Optional<CollisionGraphRoadModelImpl> roadModel;
	private LinkedList<Point> path;



	/**
	 * Constructor
	 * @param p	point where to initialize the FeasabilityAnt.
	 **/
	public FeasabilityAnt(Point p) {
		roadModel = Optional.absent();
		path = new LinkedList<>();
		path.add(p);
	}

	@Override
	public void initRoadUser(RoadModel model) {
	}


	public void moveTo(Point p) {
		path.add(p);
	}

	/**
	 * Clones this {@link FeasabilityAnt} as many times as there are incoming nodes and moves them to these nodes.
	 * @return 	The collection of new {@link FeasabilityAnt}s on the incoming nodes, they carry the same info as the
	 * 			initial one.
	 */
	public Collection<FeasabilityAnt> getClones() {
		CollisionGraphRoadModelImpl a = Optional.of(roadModel);
		Point p = path.getLast();
		Graph<Node> g = a.getGraph();
		Collection<Point> pointsCollection = g.getIncomingConnections(p);
		LinkedList<FeasabilityAnt> clones = new LinkedList<>();

		for(Point p : pointsCollection){
			FeasabilityAnt clone = this.clone();
			clone.moveTo(p);
			clones.add(clone);
		}
		return clones;
	}

	public void dropSeeds();
	public void getInfo();

}
