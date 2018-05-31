package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

// import sun.management.Agent;

/**
 * Implements the abstract class of ants, i.e. delegate MAS
 * c.f. Hanif, Holvoet, Rinde  2011: Delegate MAS for Large Scale and Dynamic PDP: A Case Study
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public abstract class Ant implements RoadUser{
    protected LinkedList<Point> path;
    protected GraphRoadModel roadModel;

    protected Ant() {
        path = new LinkedList<>();
    }
}
