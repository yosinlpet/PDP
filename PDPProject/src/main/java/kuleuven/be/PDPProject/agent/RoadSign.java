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

import java.util.LinkedList;

//TODO: implement the intention flag;
//TODO: implement the danger zone;
//TODO: edit the cost metric (total arc length);

/**
 * RoadSigns are information dropped by {@link FeasabilityAnt}s; each sign carries
 * info about where to go to find a {@link Pack} and when it expires.
 * It has to die after a few ticks
 */
public class RoadSign implements RoadUser, TickListener {
    
	private GraphRoadModel roadModel;
    private Pack pack;
    private Point next;
    private int cost;
    private TimeWindow timeWindow;
    
    private String agentID;
    private String packID;
    private long until;
    private TimeWindow twIntention;
    private boolean intentionGiven; 
    private String intentionGivenToID;
    private TimeLapse timeLapse;
    private boolean tlSet;
    private LinkedList<Point> path;
    private boolean isOver;
    private boolean indicateDest;
    private LinkedList<Long> distList;

    public RoadSign() {}
    
    public RoadSign(Pack pack, LinkedList<Point> path,
    		TimeWindow timeWindow, boolean indicateDest, LinkedList<Long> distList){
        this.timeWindow = timeWindow;
        this.pack = pack;
        this.distList = distList;
        this.isOver = false;
        LinkedList<Point> path_ = new LinkedList<>(path);
        this.path = new LinkedList<>(path);
        cost = path.size();
        path_.removeLast();
        next = path_.getLast();
        this.packID = pack.getPackID();
        intentionGiven = false;
        tlSet = false;
        this.indicateDest = indicateDest;
    }

    @Override
    public void initRoadUser(RoadModel model){
        roadModel = (GraphRoadModel) model;
    }
    
    public void setIntention(String agentID, long until) {
    	twIntention = TimeWindow.create(timeLapse.getTime(), until);
    	intentionGiven = true;
    	intentionGivenToID = agentID;
    }
    
    public String getPackID() {
    	return packID;
    }
    
    public String intentionGivenTo() {
    	return intentionGivenToID;
    }
    
    public boolean isIntentionGiven() {
    	return intentionGiven;
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
    
    public int getCost(){
        return cost;
    }

    public long getPackExpirationTime() {
        return pack.getTimeWindow().end();
    }
    
    public boolean indicateDest() {
    	return indicateDest;
    }

    public LinkedList<Long> getDistList() {
        return distList;
    }
    
    @Override
    public void tick(TimeLapse timeLapse)
    {
    	if(!tlSet) {
    		this.timeLapse = timeLapse;
    		tlSet = true;
    	}
       if (this.pack.isDeposited()) {
           if (!this.isOver) {
               roadModel.removeObject(this);
               this.isOver = true;
           }
       }
              
       // If intention expired
       //if(!twIntention.isIn(timeLapse.getTime())){
    	//   intentionGiven = false;
       //}
    }
    @Override
    public void afterTick(TimeLapse timeLapse){}

}
