package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.LinkedList;

// import sun.management.Agent;

/*
* Implements the abstract class of ants, i.e. delegate MAS
* c.f. Hanif, Holvoet, Rinde  2011: Delegate MAS for Large Scale and Dynamic PDP: A Case Study
*/

public abstract class Ant implements RoadUser{
        protected LinkedList<Point> path;
        protected Optional<CollisionGraphRoadModelImpl> roadModel;

        protected Ant() {
            path = new LinkedList<>();
        }

    /**
     * Displaces the {@link Ant} at position <code>p</code>.
     * Must be a correct position ! Relevant with initial position of the {@link Ant}: in/outComing Node.
     * @param p position to go to.
     */
    public void moveTo(Point p) {
        path.add(p);
    }
    /**
     * Collects information at position <code>p</code>.
     */
    public void collectInfo(){
        Point p = path.getLast();
        ImmutableSet<RoadUser> roadUserSet = roadModel.get().getRoadUsersOnNode(p);
        System.out.println(roadUserSet.toString());

        //Pack pckg = roadUserSet.asList().get(0); //Under construction
        //if (!packages.contains(pckg)) {
        //	packages.add(pckg);
        //}
    }
    public void runAnt(){}
}
