package kuleuven.be.PDPProject.agent;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;
import sun.security.util.Length;

import javax.measure.Measure;
import java.util.Collections;
import java.util.LinkedList;

//TODO: implement the danger zone or path breaking...;

/**
 * RoadSigns are information dropped by {@link FeasabilityAnt}s; each sign carries
 * info about where to go to find a {@link Pack} and when it expires.
 * It dies when the {@link Pack} is either carried (if <code>indicateDest</code>) or delivered.
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class RoadSign implements RoadUser, TickListener {
    
	private GraphRoadModel roadModel;
    private Pack pack;
    private Point next;
    private double cost;

    private String truckID;
    private String packID;
    private LinkedList<Point> path;
    private boolean isOver;
    private boolean indicateDest;


    public RoadSign(Pack pack, LinkedList<Point> path,
    		TimeWindow timeWindow, boolean indicateDest, String truckID){
        this.pack = pack;
        this.isOver = false;
        LinkedList<Point> path_ = new LinkedList<>(path);
        this.path = new LinkedList<>(path);
        path_.removeLast();
        next = path_.getLast();
        this.packID = pack.getPackID();
        this.indicateDest = indicateDest;
        this.truckID = truckID;
    }

    @Override
    public void initRoadUser(RoadModel model){
        roadModel = (GraphRoadModel) model;
        LinkedList<Point> path_ = new LinkedList<>(path);
        Collections.reverse(path_);
        this.cost = roadModel.getDistanceOfPath(path_).doubleValue(roadModel.getDistanceOfPath(path_).getUnit());
    }
    
    public void setTruckID(String truckID) {
    	this.truckID = truckID;
    }

    public String getTruckID() {
        return truckID;
    }

    public String getPackID() {
    	return packID;
    }
    
    public Pack getPack() {
        return pack;
    }

    public Point getNext() {
        return next;
    }

    public LinkedList<Point> getPath() {
        return path;
    }
    
    public double getCost(){
        return cost;
    }

    public long getPackExpirationTime() {
        return pack.getTimeWindow().end();
    }
    
    public boolean indicateDest() {
    	return indicateDest;
    }

    @Override
    public void tick(TimeLapse timeLapse)
    {
       if (this.pack.isDeposited() || (!this.indicateDest && this.pack.isCarried())) {
           if (!this.isOver) {
               roadModel.removeObject(this);
               this.isOver = true;
           }
       }
    }

    @Override
    public void afterTick(TimeLapse timeLapse){}

}
