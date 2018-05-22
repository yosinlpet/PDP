package kuleuven.be.PDPProject;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import javax.measure.unit.SI;

import kuleuven.be.PDPProject.agent.*;

import java.lang.Package;

public class Project 
{
	private static final double VEHICLE_LENGTH = 2d;
	private static final int NUM_AGVS = 40;
	private static final long TEST_END_TIME = 10 * 60 * 1000L;
	private static final int TEST_SPEED_UP = 16;
    
    private Project() {
    	
    	
    }

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
    	run(false);
    }
    
    /**
     * Runs the project.
     * @param testing If true the example will run in testing mode,
     *          automatically starting and stopping itself such that it can be run
     *          from a unit test.
     */
    public static void run(boolean testing) {
    	  	
        View.Builder viewBuilder = View.builder()
        	      		.with(WarehouseRenderer.builder()
								.withNodes()
								.withOneWayStreetArrows()
        	      		.withMargin(VEHICLE_LENGTH))
        	      		.with(AGVRenderer.builder()
        	      		.withDifferentColorsForVehicles())
                        .with(PackageRenderer.builder()
                        .withDifferentColorsForPackages());

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
        	if (i == 0)
        	    sim.register(new Package(sim.getRandomGenerator()));
        }
        
        sim.start();
    }
	
}
