package kuleuven.be.PDPProject.agent;


import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer.AbstractTypedCanvasRenderer;
import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

public final class PackageRenderer extends AbstractTypedCanvasRenderer<RoadUser> {
    private static final int DEFAULT_COLOR = 3;
    private final CollisionGraphRoadModel model;
    private final RenderHelper helper;
    private final Map<RoadUser, PackageRenderer.PackageUI> packages;
    private final ImmutableSet<PackageRenderer.VizOptions> vizOptions;
    private int packageCounter;
    private final Iterator<Integer> colors = Iterators.cycle(new Integer[]{9, 3, 5, 13, 11, 7, 10, 4, 6, 14, 12, 8});

    PackageRenderer(CollisionGraphRoadModel m, ImmutableSet<PackageRenderer.VizOptions> options) {
        this.model = m;
        this.helper = new RenderHelper();
        this.packages = Collections.synchronizedMap(new LinkedHashMap());
        this.packageCounter = 0;
        this.vizOptions = options;
    }

    public void renderStatic(GC gc, ViewPort vp) {
    }

    public void renderDynamic(GC gc, ViewPort vp, long time) {
        this.helper.adapt(gc, vp);
        Map var5 = this.packages;
        synchronized(this.packages) {
            Map<RoadUser, Point> objMap = this.model.getObjectsAndPositions();
            Iterator var7 = this.packages.values().iterator();

            while(var7.hasNext()) {
                PackageRenderer.PackageUI v = (PackageRenderer.PackageUI)var7.next();
                v.update(gc, vp, this.helper, objMap);
            }

        }
    }

    public boolean register(RoadUser mru) {
        int color = this.vizOptions.contains(PackageRenderer.VizOptions.USE_DIFFERENT_COLORS) ? (Integer)this.colors.next() : 2;
        PackageRenderer.PackageUI v = new PackageRenderer.PackageUI(mru, this.model, color, this.vizOptions, this.packageCounter++);
        Verify.verify(this.packages.put(mru, v) == null);
        return true;
    }

    public boolean unregister(RoadUser ru) {
        ((PackageRenderer.PackageUI)Verify.verifyNotNull(this.packages.remove(ru))).dispose();
        return true;
    }

    @CheckReturnValue
    public static PackageRenderer.Builder builder() {
        return PackageRenderer.Builder.create();
    }

    static class PackageUI {
        static final int DOT_SIZE_PX = 4;
        static final int ROTATION_OFFSET_DEG = 90;
        static final int ROTATION_MAX_DEG = 180;
        final CollisionGraphRoadModel model;
        final RoadUser pckg;
        Point position;
        double angle;
        final int color;
        final Set<PackageRenderer.VizOptions> vizOptions;
        final int creationNumber;
        double scale = 1.0D;
        Optional<Image> image;

        PackageUI(RoadUser ru, CollisionGraphRoadModel m, int c, Set<PackageRenderer.VizOptions> t, int num) {
            this.pckg = ru;
            this.model = m;
            this.angle = 0.0D;
            this.color = c;
            this.vizOptions = t;
            this.creationNumber = num;
            this.position = new Point(0.0D, 0.0D);
            this.image = Optional.absent();
        }

        void dispose() {
            if (this.image.isPresent()) {
                ((Image)this.image.get()).dispose();
            }

        }

        Image createImage(GC gc, ViewPort vp) {
            if (this.image.isPresent()) {
                ((Image)this.image.get()).dispose();
            }

            int length = (int)(this.model.getVehicleLength() * vp.scale);
            int width = length / 4;
            int frontSize = length / 16;
            Image img = new Image(gc.getDevice(), width, length);
            GC igc = new GC(img);
            igc.setBackground(gc.getDevice().getSystemColor(this.color));
            igc.fillPolygon(new int[]{frontSize, 0, width - frontSize, 0, width, frontSize, 0, frontSize});
            igc.setBackground(gc.getDevice().getSystemColor(15));
            igc.fillRectangle(0, frontSize, width, length - frontSize);
            if (this.vizOptions.contains(PackageRenderer.VizOptions.CREATION_NUMBER)) {
                String string = Integer.toString(this.creationNumber);
                double factor = (double)width / (double)igc.stringExtent(string).x;
                Font initialFont = igc.getFont();
                FontData[] fontData = initialFont.getFontData();

                for(int i = 0; i < fontData.length; ++i) {
                    fontData[i].setHeight((int)((double)fontData[i].getHeight() * factor));
                }

                Font newFont = new Font(gc.getDevice(), fontData);
                igc.setFont(newFont);
                org.eclipse.swt.graphics.Point finalTextSize = igc.stringExtent(string);
                int xOffset = (int)((double)(width - finalTextSize.x) / 2.0D);
                int yOffset = frontSize + (int)((double)(length - frontSize - finalTextSize.y) / 2.0D);
                igc.drawText(string, xOffset, yOffset, true);
                newFont.dispose();
            }

            if (this.vizOptions.contains(PackageRenderer.VizOptions.PACKAGE_ORIGIN)) {
                igc.setBackground(gc.getDevice().getSystemColor(3));
                igc.setForeground(gc.getDevice().getSystemColor(1));
                igc.fillOval(width / 2 - 2, length / 2 - 2, 4, 4);
                igc.drawOval(width / 2 - 2, length / 2 - 2, 4, 4);
            }

            igc.dispose();
            return img;
        }

        void update(GC gc, ViewPort vp, RenderHelper helper, Map<RoadUser, Point> objMap) {
            if (objMap.containsKey(this.pckg)) {
                this.position = (Point)objMap.get(this.pckg);
                Optional<? extends Connection<?>> conn = this.model.getConnection(this.pckg);
                if (!this.image.isPresent() || this.scale != vp.scale) {
                    this.scale = vp.scale;
                    this.image = Optional.of(this.createImage(gc, vp));
                }

                if (conn.isPresent()) {
                    this.angle = PointUtil.angle((Connection)conn.get());
                }

                int x = vp.toCoordX(this.position.x);
                int y = vp.toCoordY(this.position.y);
                Transform transform = new Transform(gc.getDevice());
                transform.translate((float)x, (float)y);
                transform.rotate((float)(90.0D + this.angle * 180.0D / 3.141592653589793D));
                transform.translate((float)(-(x + ((Image)this.image.get()).getBounds().width / 2)), (float)(-(y + ((Image)this.image.get()).getBounds().height / 2)));
                gc.setTransform(transform);
                gc.drawImage((Image)this.image.get(), x, y);
                gc.setTransform((Transform)null);
                transform.dispose();
                if (this.vizOptions.contains(PackageRenderer.VizOptions.COORDINATES)) {
                    helper.setBackgroundSysCol(7);
                    helper.setForegroundSysCol(2);
                    gc.drawString(String.format("%1.2f,%1.2f", this.position.x, this.position.y), vp.toCoordX(this.position.x), vp.toCoordY(this.position.y));
                }

            }
        }
    }

    public abstract static class Builder extends AbstractModelBuilder<PackageRenderer, RoadUser> {
        private static final long serialVersionUID = -8359744710512375486L;

        Builder() {
            this.setDependencies(new Class[]{CollisionGraphRoadModel.class});
        }

        abstract ImmutableSet<PackageRenderer.VizOptions> vizOptions();

        @CheckReturnValue
        public PackageRenderer.Builder withPackageCreationNumber() {
            return create(PackageRenderer.VizOptions.CREATION_NUMBER, this.vizOptions());
        }

        @CheckReturnValue
        public PackageRenderer.Builder withPackageCoordinates() {
            return create(PackageRenderer.VizOptions.COORDINATES, this.vizOptions());
        }

        @CheckReturnValue
        public PackageRenderer.Builder withDifferentColorsForPackages() {
            return create(PackageRenderer.VizOptions.USE_DIFFERENT_COLORS, this.vizOptions());
        }

        @CheckReturnValue
        public PackageRenderer.Builder withPackageOrigin() {
            return create(PackageRenderer.VizOptions.PACKAGE_ORIGIN, this.vizOptions());
        }

        public PackageRenderer build(DependencyProvider dp) {
            CollisionGraphRoadModel rm = (CollisionGraphRoadModel)dp.get(CollisionGraphRoadModel.class);
            return new PackageRenderer(rm, this.vizOptions());
        }

        static PackageRenderer.Builder create() {
            return new AutoValue_PackageRenderer_Builder(Sets.immutableEnumSet(ImmutableSet.of()));
        }

        static PackageRenderer.Builder create(PackageRenderer.VizOptions one, Iterable<PackageRenderer.VizOptions> more) {
            return new AutoValue_PackageRenderer_Builder(Sets.immutableEnumSet(one, (Enum[])Iterables.toArray(more, PackageRenderer.VizOptions.class)));
        }
    }

    static enum VizOptions {
        COORDINATES,
        CREATION_NUMBER,
        PACKAGE_ORIGIN,
        USE_DIFFERENT_COLORS;

        private VizOptions() {
        }
    }
}
