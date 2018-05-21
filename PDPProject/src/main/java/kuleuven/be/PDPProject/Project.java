package kuleuven.be.PDPProject;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import javax.measure.unit.SI;

public class Project 
{
	private static final double VEHICLE_LENGTH = 2d;
	private static final int NUM_AGVS = 20;
	private static final long TEST_END_TIME = 10 * 60 * 1000L;
	private static final int TEST_SPEED_UP = 16;
	
    private final RandomModel rndModel;
    private final CommModel commModel;
    private final PDPModel pdpModel;
    private final RoadModel roadModel;
    
    private Project() {}

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
    	run(false,1000L);
    }
    
    /**
     * Runs the project.
     * @param testing If true the example will run in testing mode,
     *          automatically starting and stopping itself such that it can be run
     *          from a unit test.
     */
    public static void run(boolean testing, long seed) {
    	  	
        rndModel = RandomModel.create(seed);
        commModel = CommModel.builder().build();
        pdpModel = DefaultPDPModel.create();
        roadModel = CollisionGraphRoadModel.builder(GraphCreator.createGraph())
                	.setVehicleLength(VEHICLE_LENGTH)
                	.build();
    	
        View.Builder viewBuilder = View.builder()
        	      		.with(WarehouseRenderer.builder()
        	      		.withMargin(VEHICLE_LENGTH))
        	      		.with(AGVRenderer.builder()
        	      		.withDifferentColorsForVehicles());

        if (testing) {
        	viewBuilder = viewBuilder.withAutoPlay()
        	        	.withAutoClose()
        	        	.withSimulatorEndTime(TEST_END_TIME)
        	        	.withTitleAppendix("TESTING")
        	        	.withSpeedUp(TEST_SPEED_UP);
        } else {
        	viewBuilder = viewBuilder.withTitleAppendix("AGV example");
        }
    	
        final Simulator sim = Simulator.builder()
        				.addModel(
        					RoadModelBuilders.dynamicGraph(GraphCreator.createSimpleGraph())
        				.withCollisionAvoidance()
        				.withDistanceUnit(SI.METER)
        				.withVehicleLength(VEHICLE_LENGTH))
        				.addModel(viewBuilder)
        				.build();
        
        for (int i = 0; i < NUM_AGVS; i++) {
        	sim.register(new AgvAgent(sim.getRandomGenerator()));
        }
        
        sim.start();
    }
	
}
