package kuleuven.be.PDPProject;

import java.util.Map;
import static com.google.common.collect.Lists.newArrayList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
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

public class GraphCreator {
    static final int LEFT_CENTER_U_ROW = 4;
    static final int LEFT_CENTER_L_ROW = 5;
    static final int LEFT_COL = 4;
    static final int RIGHT_CENTER_U_ROW = 2;
    static final int RIGHT_CENTER_L_ROW = 4;
    static final int RIGHT_COL = 0;

    private static final double VEHICLE_LENGTH = 2d;
    
    public GraphCreator() {	
    	
    }

    static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows, Point offset) {
    final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable.builder();
      for (int c = 0; c < cols; c++) {
        for (int r = 0; r < rows; r++) {
          builder.put(r, c, new Point(
            offset.x + c * VEHICLE_LENGTH * 2,
            offset.y + r * VEHICLE_LENGTH * 2));
        }
      }
      return builder.build();
    }

    static ListenableGraph<LengthData> createSimpleGraph() {
      final Graph<LengthData> g = new TableGraph<>();

      final Table<Integer, Integer, Point> matrix = createMatrix(8, 6,
        new Point(0, 0));

      for (int i = 0; i < matrix.columnMap().size(); i++) {

        final Iterable<Point> path;
        if (i % 2 == 0) {
          path = Lists.reverse(newArrayList(matrix.column(i).values()));
        } else {
          path = matrix.column(i).values();
        }
        Graphs.addPath(g, path);
      }

      Graphs.addPath(g, matrix.row(0).values());
      Graphs.addPath(g, Lists.reverse(newArrayList(matrix.row(
        matrix.rowKeySet().size() - 1).values())));

      return new ListenableGraph<>(g);
    }

    static ListenableGraph<LengthData> createGraph() {
      final Graph<LengthData> g = new TableGraph<>();

      final Table<Integer, Integer, Point> leftMatrix = createMatrix(5, 10,
        new Point(0, 0));
      for (final Map<Integer, Point> column : leftMatrix.columnMap().values()) {
        Graphs.addBiPath(g, column.values());
      }
      Graphs.addBiPath(g, leftMatrix.row(LEFT_CENTER_U_ROW).values());
      Graphs.addBiPath(g, leftMatrix.row(LEFT_CENTER_L_ROW).values());

      final Table<Integer, Integer, Point> rightMatrix = createMatrix(10, 7,
        new Point(30, 6));
      for (final Map<Integer, Point> row : rightMatrix.rowMap().values()) {
        Graphs.addBiPath(g, row.values());
      }
      Graphs.addBiPath(g, rightMatrix.column(0).values());
      Graphs.addBiPath(g, rightMatrix.column(rightMatrix.columnKeySet().size()
        - 1).values());

      Graphs.addPath(g,
        rightMatrix.get(RIGHT_CENTER_U_ROW, RIGHT_COL),
        leftMatrix.get(LEFT_CENTER_U_ROW, LEFT_COL));
      Graphs.addPath(g,
        leftMatrix.get(LEFT_CENTER_L_ROW, LEFT_COL),
        rightMatrix.get(RIGHT_CENTER_L_ROW, RIGHT_COL));

      return new ListenableGraph<>(g);
    }
}

