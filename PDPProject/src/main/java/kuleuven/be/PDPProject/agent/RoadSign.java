package kuleuven.be.PDPProject.agent;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

import java.util.LinkedList;

/**
 * RoadSigns are information dropped by {@link FeasabilityAnt}s; each sign carries
 * info about where to go to find a {@link Pack} and when it expires.
 * It has to die after a few ticks
 */
public class RoadSign implements RoadUser, TickListener {
    private Optional<CollisionGraphRoadModelImpl> roadModel;
    private Pack pack;
    private Point next;
    private TimeWindow timeWindow;
    public RoadSign(Pack pack, LinkedList<Point> path, TimeWindow timeWindow){
        roadModel = Optional.absent();
        this.timeWindow = timeWindow;
        this.pack = pack;
        LinkedList<Point> path_ = path;
        path_.removeLast();
        next = path_.getLast();
    }

    @Override
    public void initRoadUser(RoadModel model){
        roadModel = model;
    }

    public Pack getPack() {
        return pack;
    }

    public Point getNext() {
        return next;
    }

    public long getPackExpirationTime() {
        return pack.getTimeWindow().end();
    }
    @Override
    public void Tick(TimeLapse timeLapse)
    {
       if (timeLapse.getTime() > timeWindow.end())
            roadModel.get().removeObject(this);
    }
    @Override
    public void afterTick(TimeLapse timeLapse){}
}
