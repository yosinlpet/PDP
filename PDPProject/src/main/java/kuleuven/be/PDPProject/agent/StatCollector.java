package kuleuven.be.PDPProject.agent;


import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;

import javax.sql.rowset.Predicate;
import java.util.LinkedList;
import java.util.Set;


/**
 * Collects info on the roadModel
 */
public class StatCollector {
    private GraphRoadModel roadModel;
    private String fileName;
    private LinkedList<String> truckIDs;

    private LinkedList<Double> truckDists;
    private LinkedList<Integer> truckCarried;
    private LinkedList<Long> truckAccumulatedDelay;
    private LinkedList<Double> truckAverageDelay;
    private LinkedList<Integer> truckDelivered;
    private long totalDelay;

    private long seed;
    private int nAGVs;
    private double proba;

    public StatCollector(RoadModel roadModel, String fileName, int nAGVs, long seed, double proba) {
        this.fileName = fileName;
        this.roadModel = (GraphRoadModel) roadModel;

        this.truckIDs = new LinkedList<>();
        this.truckDists = new LinkedList<>();
        this.truckCarried = new LinkedList<>();
        this.truckDelivered = new LinkedList<>();
        this.truckAccumulatedDelay = new LinkedList<>();
        this.truckAverageDelay = new LinkedList<>();

        this.totalDelay = 0L;
        this.seed = seed;
        this.nAGVs = nAGVs;
        this.proba = proba;
    }

    public void setPackDelay(long delay) {
        this.totalDelay += delay;
    }

    public void runStats() {
        Set<Truck> truckSet = this.roadModel.getObjectsOfType(Truck.class);

        for (Truck t : truckSet) {
            this.truckIDs.addLast(t.getTruckID());
            //this.truckDists.addLast(t.getDist());
            this.truckCarried.addLast(t.getHasCarried());
            this.truckDelivered.addLast(t.getHasDelivered());
            this.truckAccumulatedDelay.addLast(t.getAccumulatedDelay());
            this.truckAverageDelay.addLast(((double) t.getAccumulatedDelay())/((double) t.getHasDelivered()));

            this.totalDelay += t.getAccumulatedDelay();
        }
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println();
        System.out.println("-- END OF SIMULATION -- STATISTICS --");
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println();
        System.out.println("Number of AGVs: "+Integer.toString(this.nAGVs));
        System.out.println();
        System.out.println("Seed          : "+Long.toString(this.seed));
        System.out.println();
        System.out.println("Request proba : "+Double.toString(this.proba));
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println();
        System.out.println("AGV_IDs      : "+this.truckIDs.toString());
        System.out.println();
        System.out.println("AGV_Carried  : "+this.truckCarried.toString());
        System.out.println();
        System.out.println("AGV_Delivered: "+this.truckDelivered.toString());
        System.out.println();
        System.out.println("AGV_Delays   : "+this.truckAccumulatedDelay.toString());
        System.out.println();
        System.out.println("AGV_avgDelay : "+this.truckAverageDelay.toString());
        System.out.println();
        System.out.println("========================================================================================================");
        System.out.println();
        System.out.println("Total Delay: "+Long.toString(this.totalDelay));

        //print objects to fileName
    }
}
