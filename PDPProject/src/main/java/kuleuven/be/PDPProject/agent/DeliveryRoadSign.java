package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import java.util.LinkedList;

public class DeliveryRoadSign extends RoadSign{

    public DeliveryRoadSign(Pack pack, LinkedList<Point> path, TimeWindow timeWindow) {
        super(pack, path, timeWindow);
    }

}

