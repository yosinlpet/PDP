package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import java.util.LinkedList;

public class PackRoadSign extends RoadSign{

    public PackRoadSign(Pack pack, LinkedList<Point> path, TimeWindow timeWindow) {
        super(pack, path, timeWindow);
    }

}
