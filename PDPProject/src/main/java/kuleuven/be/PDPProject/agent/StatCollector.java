package kuleuven.be.PDPProject.agent;


import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import javax.sql.rowset.Predicate;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Set;

import static java.lang.Double.isNaN;


/**
 * Collects info on the roadModel
 * @needs the tick length to be 1!!
 * @needs the distances and node positions have to match !
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
    private double totalDist;

    private int feasAnt;
    private int intAnt;
    private int totalAnt;

    private long seed;
    private int nAGVs;
    private double proba;
    private double agv_speed;
    private int factor;

    public StatCollector(RoadModel roadModel, String fileName, int nAGVs, long seed, double proba, int factor, double agv_speed) {
        this.fileName = fileName;
        this.roadModel = (GraphRoadModel) roadModel;

        this.feasAnt  = 0;
        this.intAnt   = 0;
        this.totalAnt = 0;

        this.truckIDs = new LinkedList<>();
        this.truckDists = new LinkedList<>();
        this.truckCarried = new LinkedList<>();
        this.truckDelivered = new LinkedList<>();
        this.truckAccumulatedDelay = new LinkedList<>();
        this.truckAverageDelay = new LinkedList<>();

        this.totalDelay = 0L;
        this.totalDist = 0d;
        this.seed = seed;
        this.nAGVs = nAGVs;
        this.proba = proba;
        this.agv_speed = agv_speed;
        this.factor = factor;
    }

    public void setPackDelay(long delay) {
        this.totalDelay += delay;
    }

    public void runStats(TimeLapse timeLapse) {
        Set<Truck> truckSet = this.roadModel.getObjectsOfType(Truck.class);

        for (Truck t : truckSet) {
            this.truckIDs.addLast(t.getTruckID());
            this.truckDists.addLast(t.getAccumulatedDist());
            this.truckCarried.addLast(t.getHasCarried());
            this.truckDelivered.addLast(t.getHasDelivered());
            this.truckAccumulatedDelay.addLast(t.getAccumulatedDelay());
            double a = ((double) t.getAccumulatedDelay())/((double) t.getHasDelivered());
            this.truckAverageDelay.addLast(Double.isNaN(a) ? 0. : a);

            this.totalDelay += t.getAccumulatedDelay();
            this.totalDist  += t.getAccumulatedDist();
        }

        this.feasAnt  = FeasabilityAnt.feasabilityAntCounter;
        this.intAnt   = IntentionAnt.intentionAntCounter;
        this.totalAnt = this.feasAnt + this.intAnt;

        try {
            PrintWriter printer = new PrintWriter(fileName, "UTF-8");
            printer.println("========================================================================================================");
            printer.println();
            printer.println("-- END OF SIMULATION -- STATISTICS --");
            printer.println();
            printer.println("========================================================================================================");
            printer.println();
            printer.println("Number of AGVs : " + Integer.toString(this.nAGVs));
            printer.println();
            printer.println("Speed of AGVs  : " + Double.toString(this.agv_speed));
            printer.println();
            printer.println("Seed           : " + Long.toString(this.seed));
            printer.println();
            printer.println("Request proba  : " + Double.toString(this.proba));
            printer.println();
            printer.println("TimeWindow fact: "+ Integer.toString(this.factor));
            printer.println();
            printer.println("Stopped at T   : " + Long.toString(timeLapse.getTime()));
            printer.println();
            printer.println("========================================================================================================");
            printer.println();
            printer.println("AGV_IDs       : " + this.truckIDs.toString());
            printer.println();
            printer.println("AGV_TravelDist: " + this.truckDists.toString());
            printer.println();
            printer.println("AGV_Carried   : " + this.truckCarried.toString());
            printer.println();
            printer.println("AGV_Delivered : " + this.truckDelivered.toString());
            printer.println();
            printer.println("AGV_Delays    : " + this.truckAccumulatedDelay.toString());
            printer.println();
            printer.println("AGV_avgDelay  : " + this.truckAverageDelay.toString());
            printer.println();
            printer.println("FeasAnts used : " + Integer.toString(this.feasAnt));
            printer.println();
            printer.println("IntAnts used  : " + Integer.toString(this.intAnt));
            printer.println();
            printer.println("========================================================================================================");
            printer.println();
            printer.println("Total Delay: " + Long.toString(this.totalDelay));
            printer.println();
            printer.println("Total Dist : " + String.format("%5.3f", this.totalDist));
            printer.println();
            printer.println("Total Ants : " + Integer.toString(this.totalAnt));
            printer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //print objects to fileName
    }
}
