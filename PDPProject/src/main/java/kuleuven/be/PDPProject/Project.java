/*
 * Copyright (C) 2011-2018 Rinde R.S. van Lon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kuleuven.be.PDPProject;
import com.github.rinde.rinsim.core.model.road.DynamicGraphRoadModelImpl;
import com.github.rinde.rinsim.core.model.time.TimeModel;
import kuleuven.be.PDPProject.agent.*;


import static com.google.common.collect.Maps.newHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Nullable;
import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Monitor;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
//import com.github.rinde.rinsim.examples.taxi.TaxiRenderer.Language;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.github.rinde.rinsim.ui.renderers.PDPModelRenderer;
import com.github.rinde.rinsim.util.TimeWindow;


/**
 * Pick-up and Delivery Problem with DelegateMAS
 * @author Denis Dumoulin
 * @author Remi Chauvenne
 */
public final class Project {

  private static final int NUM_AGVS = 5;
  private static final int NUM_PCKGS = 0;

  private static final int SPEED_UP = 1;
  private static final double NEW_CUSTOMER_PROB = .05;

  private static final String MAP_FILE = "/testGraph.dot";
  private static final Map<String, Graph<MultiAttributeData>> GRAPH_CACHE = newHashMap();

  private static final long TEST_STOP_TIME = 20 * 60 * 1000;
  private static final int TEST_SPEED_UP = 64;
  
  private static final int factor = 5;
  private static final long END = 2000;
  private static final long SEED = 2399L;

  private static final double AGV_SPEED = .2d;
  private static final int AGV_CAPACITY = 1;

  private static final String OUTPUT_FILE = ".logfiles/run_nAGV"+String.format("%02d", NUM_AGVS)+"_seed"+String.format("%02d", SEED)+".log";

  public Project() {}

  /**
   * Starts the {@link Project}.
   * @param args The first option may optionally indicate the end time of the
   *          simulation.
   */
  public static void main(@Nullable String[] args) {
    final long endTime = args != null && args.length >= 1 ? Long.parseLong(args[0]) : END;

    final String graphFile = args != null && args.length >= 2 ? args[1] : MAP_FILE;

    final long seed = args != null && args.length >= 3 ? Long.parseLong(args[2]) : SEED;

    run(false, endTime, graphFile, null /* new Display() */, null, null, seed);
  }

  /**
   * Run the example.
   * @param testing If <code>true</code> enables the test mode.
   */
  public static void run(boolean testing) {
    run(testing, Long.MAX_VALUE, MAP_FILE, null, null, null, SEED);
  }

  /**
   * Starts the example.
   * @param testing Indicates whether the method should run in testing mode.
   * @param endTime The time at which simulation should stop.
   * @param graphFile The graph that should be loaded.
   * @param display The display that should be used to show the ui on.
   * @param m The monitor that should be used to show the ui on.
   * @param list A listener that will receive callbacks from the ui.
   * @return The simulator instance.
   */
  public static Simulator run(boolean testing, final long endTime,
      String graphFile,
      @Nullable Display display, @Nullable Monitor m, @Nullable Listener list, long seed) {
	  
	  int packID = 0;

    final View.Builder view = createGui(testing, display, m, list);

    final Simulator simulator = Simulator.builder()
      .addModel(RoadModelBuilders.staticGraph(loadGraph(graphFile)).withDistanceUnit(SI.METER).withSpeedUnit(SI.METERS_PER_SECOND))
      .addModel(DefaultPDPModel.builder())
            .addModel(TimeModel.builder().withTickLength(1).withTimeUnit(SI.SECOND))
      .addModel(view)
      .setRandomSeed(seed)
      .build();
    
    final RandomGenerator rng = simulator.getRandomGenerator();

    final RoadModel roadModel = simulator.getModelProvider().getModel(
      RoadModel.class);

    final StatCollector statCollector = new StatCollector(roadModel, OUTPUT_FILE, NUM_AGVS, SEED, NEW_CUSTOMER_PROB);

    // add depots, taxis and parcels to simulator
      LinkedList<Point> positions = new LinkedList<>();
    for (int i = 0; i < NUM_AGVS; i++) {
        Point position;
        if (positions.isEmpty()) {
            position = roadModel.getRandomPosition(rng);
            positions.addLast(position);
        }
        else {
            do
                position = roadModel.getRandomPosition(rng);
            while (positions.contains(position));
            positions.addLast(position);
        }
    	simulator.register(new Truck(position,AGV_CAPACITY, AGV_SPEED,"Truck-"+String.format("%03d", i), rng));
    }
    
 
    for (int i = 0; i < NUM_PCKGS; i++) {
    	Point destination;
        Point departure = roadModel.getRandomPosition(rng);
        do {
            destination = roadModel.getRandomPosition(rng);
        }
        while (destination.equals(departure));
		Measure<Double, Length> md = roadModel.getDistanceOfPath(
				roadModel.getShortestPathTo(departure, destination));
		
		long distanceShortestPath = (long) md.doubleValue(md.getUnit());

		long maxDeliveryTime =
				Math.round(distanceShortestPath*factor/AGV_SPEED);

		TimeWindow tw = TimeWindow.create(0, maxDeliveryTime);
    	simulator.register(new Pack(
    				Parcel.builder(departure, destination)
    					.deliveryTimeWindow(tw)
    					.buildDTO(), rng,"Pack-"+ String.format("%03d", packID),simulator, endTime, statCollector));
    	packID++;
    }

    simulator.addTickListener(new TickListener() {
    int packID = NUM_PCKGS;
      @Override
      public void tick(TimeLapse time) {
        if (time.getStartTime() >= endTime) {
            statCollector.runStats(time);
            simulator.stop();
        } else if (rng.nextDouble() < NEW_CUSTOMER_PROB) {

            Point destination;
        	Point departure = roadModel.getRandomPosition(rng);
        	do {
                destination = roadModel.getRandomPosition(rng);
            }
        	while (destination.equals(departure));

        	
    		Measure<Double, Length> md = roadModel.getDistanceOfPath(
    				roadModel.getShortestPathTo(departure, destination));
    		
    		long distanceShortestPath = (long) md.doubleValue(md.getUnit());

    		long maxDeliveryTime =
                    Math.round(distanceShortestPath*factor/AGV_SPEED);

    		TimeWindow tw = TimeWindow.create(time.getStartTime(),
    						time.getStartTime()+maxDeliveryTime);
        		 		
        	simulator.register(new Pack(
    				Parcel.builder(departure, destination)
    					.deliveryTimeWindow(tw)
    					.buildDTO(), rng,"Pack-"+ String.format("%03d",packID),simulator, endTime, statCollector));
        	packID++;
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    simulator.start();

    return simulator;
  }

  static View.Builder createGui(
      boolean testing,
      @Nullable Display display,
      @Nullable Monitor m,
      @Nullable Listener list) {

    View.Builder view = View.builder()
      .with(GraphRoadModelRenderer.builder().withDirectionArrows().withNodeCircles().withMargin(1))
      .with(RoadUserRenderer.builder()
        .withImageAssociation(Truck.class, "/multix.png")
        .withImageAssociation(Pack.class, "/issou.png"))
          //.withImageAssociation(RoadSign.class, "/roadSign.png"))
      .with(PDPModelRenderer.builder())
      .withTitleAppendix("Issou!");

    if (testing) {
      view = view.withAutoClose()
        .withAutoPlay()
        .withSimulatorEndTime(TEST_STOP_TIME)
        .withSpeedUp(TEST_SPEED_UP);
    } else if (m != null && list != null && display != null) {
      view = view.withMonitor(m)
        .withSpeedUp(SPEED_UP)
        .withResolution(m.getClientArea().width, m.getClientArea().height)
        .withDisplay(display)
        .withCallback(list)
        .withAsync()
        .withAutoPlay()
        .withAutoClose();
    }
    return view;
  }

  // load the graph file
  static Graph<MultiAttributeData> loadGraph(String name) {
    try {
      if (GRAPH_CACHE.containsKey(name)) {
        return GRAPH_CACHE.get(name);
      }
      final Graph<MultiAttributeData> g = DotGraphIO
        .getMultiAttributeGraphIO(
          Filters.selfCycleFilter())
        .read(
          Project.class.getResourceAsStream(name));
      GRAPH_CACHE.put(name, g);
      return g;
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}