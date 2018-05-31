package kuleuven.be.PDPProject.agent;

import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;

/**
 * The flags to claim a position on a {@link Node} and hence avoid collisions.
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public class Flag implements RoadUser{
	
	private GraphRoadModel roadModel;
	private String truckID;
	
	public Flag() {
		
	}
	
	public Flag(String truckID) {
		this.truckID = truckID;
	}
	
    @Override
    public void initRoadUser(RoadModel model){
        roadModel = (GraphRoadModel) model;
    }
	
	public boolean raiseFlag(String truckID) {
		if(this.truckID.equals("nobody")) {
			this.truckID = truckID;
			//System.out.print("Flag raised by ");
			//System.out.println(truckID);
			return true;
		}
		else if(this.truckID.equals(truckID))
			return true;
		else return false;
	}
	
	public boolean dropFlag(String truckID) {
		if(this.truckID.equals(truckID)) {
			this.truckID = "nobody";
			return true;
			//System.out.print("Flag dropped by ");
			//System.out.println(truckID);
		}
		else return false;
		//else if (!this.truckID.equals("nobody")){
			//System.out.print("Drop failed, ID is \"");
			//System.out.print(truckID);
			//System.out.print("\" instead of \"");
			//System.out.print(this.truckID);
			//System.out.println("\"");
		//}
	}
}
