package pl.peek.kmeans.impl;

import java.util.*;

import static java.util.stream.Collectors.toMap;

/**
 * Class containing information about cluster represented by collection of {@link Cluster}
 * objects, and points contained within space represented by {@link Point} objects.
 */
public class KMeans {
    private int clusterCount;

    private Double minX;
    private Double maxX;
    private Double minY;
    private Double maxY;

    private List<Point> points;
    private List<Cluster> clusters;

    private final DistanceMethod distanceMethod;

    private final static int REPOSITION_LIMIT = 10;

    /**
     * Default constructor. Does basically nothing.
     */
    public KMeans() {
        this(0, Collections.emptyList());
    }

    /**
     * Object with default distance calculation implementation
     * sqrt( (x0 - x)^2 + (y0 - y)^2 ).
     *
     * @param clusterCount number of clusters wanted
     * @param points       collection of point in examined space.
     */
    public KMeans(int clusterCount, List<Point> points) {
        this(clusterCount, points, new EuclidDistanceMethod());
    }


    /**
     * Object with given distance calculation implementation.
     *
     * @param clusterCount   number of clusters wanted
     * @param points         collection of point in examined space
     * @param distanceMethod provided distance calculation algorithm
     */
    public KMeans(int clusterCount, List<Point> points, DistanceMethod distanceMethod) {
        if (clusterCount > points.size())
            throw new IllegalArgumentException("To many clusters required for given point list.");

        this.clusterCount = clusterCount;
        this.points = points;
        this.distanceMethod = distanceMethod;
    }

    /**
     * Assigns points to clusters based on distance between cluster and point.
     */
    public void calculateClusters() {
        computeMinMax();

        this.clusters = makeClusters(clusterCount, points);
        boolean calc = true;
        int repositions = 0;
        while (calc) {
            clearClusters();
            distributePoints();
            calc = calculateCentroids();
            if (!calc && hasEmptyClusters() && repositions < REPOSITION_LIMIT) {
                calc = true;
                repositions++;
                setRandomPosition(this.clusters.stream().filter(c -> c.getPoints().isEmpty())
                        .findFirst().get());
            }
        }
    }

    private boolean hasEmptyClusters() {
        return hasEmptyClusters(this.clusters);
    }

    private boolean hasEmptyClusters(List<Cluster> clusters) {
        return clusters.stream().anyMatch(c -> c.getPoints().isEmpty());
    }

    private void clearClusters() {
        clearClusters(this.clusters);
    }

    private boolean calculateCentroids() {
        return calculateCentroids(this.clusters);
    }

    /**
     * Calculate centroid based on points contained in cluster.
     *
     * @param clusters List of clusters to have centroids calculated.
     * @return True if centroids were different than those made.
     */
    public static boolean calculateCentroids(List<Cluster> clusters) {
        Map<Cluster, Point> centroids = clusters.stream()
                .collect(toMap(c -> c, KMeans::calculateCentroid));
        boolean ret = centroids.entrySet().stream()
                .anyMatch(e -> !e.getKey().getCentroid().isSame(e.getValue()));
        centroids.entrySet().forEach(e -> e.getKey().setCentroid(e.getValue()));
        return ret;
    }

    /**
     * Calculates centroid based on points given cluster contains.
     *
     * @param cluster Cluster
     * @return Point with calculated x, y values.
     */
    public static Point calculateCentroid(Cluster cluster) {
        double sumX = cluster.getPoints().stream().mapToDouble(Point::getX).sum();
        double sumY = cluster.getPoints().stream().mapToDouble(Point::getY).sum();

        return new Point(sumX / cluster.getPoints().size(), sumY / cluster.getPoints().size());
    }

    private void clearClusters(List<Cluster> clusters) {
        clusters.stream().forEach(Cluster::clearPoints);
    }

    private void computeMinMax() {
        minX = computeMinX(this.points);
        maxX = computeMaxX(this.points);
        minY = computeMinY(this.points);
        maxY = computeMaxY(this.points);
    }

    public static double computeMinX(List<Point> points) {
        return points.stream().map(Point::getX).min(Double::compareTo).orElse(0.0);
    }

    public static double computeMaxX(List<Point> points) {
        return points.stream().map(Point::getX).max(Double::compareTo).orElse(0.0);
    }

    public static double computeMinY(List<Point> points) {
        return points.stream().map(Point::getY).min(Double::compareTo).orElse(0.0);
    }

    public static double computeMaxY(List<Point> points) {
        return points.stream().map(Point::getY).max(Double::compareTo).orElse(0.0);
    }

    private List<Cluster> makeClusters(int clusterCount, List<Point> points) {
        if (clusterCount == 0) return Collections.emptyList();
        List<Cluster> clusters = initClusters(clusterCount);
        sharePointsBetweenClusters(clusters, points);
        return clusters;
    }

    private void sharePointsBetweenClusters(List<Cluster> clusters, List<Point> points) {
        int c = 0;
        for (Point point : points) {
            clusters.get(c).addPoint(point);
            c++;
            if (c + 1 >= clusterCount) c = 0;
        }
    }

    private List<Cluster> initClusters(int n) {
        return initClusters(n, this.minX, this.minY, this.maxX, this.maxY);
    }

    private List<Cluster> initClusters(int n, double minX, double minY, double maxX, double maxY) {
        List<Cluster> clusters = new ArrayList<>();
        if (n == 0) return clusters;
        for (int i = 0; i < n; i++) {
            Cluster cluster = new Cluster();
            setRandomPosition(cluster);
            clusters.add(cluster);
        }
        return clusters;
    }

    private void setRandomPosition(Cluster cluster) {
        setRandomPosition(cluster, minX, minY, maxX, maxY);
    }

    /**
     * Set cluster centroid based on new values generated randomly between given bounds.
     *
     * @param cluster {@link Cluster}
     * @param minX    minimal x value
     * @param minY    minimal y value
     * @param maxX    maximal x value
     * @param maxY    maximal y value
     */
    public static void setRandomPosition(Cluster cluster, double minX, double minY, double maxX,
                                         double maxY) {
        cluster.setCentroid(computeRandomPosition(minX, minY, maxX, maxY));
    }

    public static Point computeRandomPosition(double minX, double minY, double maxX, double maxY) {
        Random r = new Random();
        return new Point(
                minX + (maxX - minX) * r.nextDouble(), minY + (maxY - minY) * r.nextDouble()
        );
    }

    private void distributePoints() {
        distributePoints(this.clusters, this.points);
    }

    private void distributePoints(List<Cluster> clusters, List<Point> points) {
        distributePoints(clusters, points, this.distanceMethod);
    }

    /**
     * Assigns points to nearest cluster based on given distance method.
     *
     * @param clusters List of {@link Cluster} objects.
     * @param points   List of {@link Point} objects to distribute
     * @param calc     Distance method
     */
    public static void distributePoints(List<Cluster> clusters, List<Point> points,
                                        DistanceMethod calc) {
        points.forEach(p -> clusters
                .stream()
                .min((o1, o2) -> distance(o1, p, calc).compareTo(distance(o2, p, calc)))
                .ifPresent(cluster -> cluster.addPoint(p)));
    }

    /**
     * Calulate distance between {@link Cluster} point and given {@link Point} object.
     *
     * @param cluster {@link Cluster} object
     * @param point   {@link Point} object
     * @param method  Distance calculation implementation
     * @return Distance between points
     */
    public static Double distance(Cluster cluster, Point point, DistanceMethod method) {
        return distance(cluster.getCentroid(), point, method);
    }

    /**
     * Distance between points based on given method.
     *
     * @param p0     First {@link Point} object
     * @param p1     Second {@link Point} object
     * @param method Distance method
     * @return Distance as {@link Double} value
     */
    public static Double distance(Point p0, Point p1, DistanceMethod method) {
        return method.calcDistance(p0, p1);
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    /**
     * @return Minimal X value in point space
     */
    public Double getMinX() {
        return minX;
    }

    /**
     * @return Maximal X value in point space
     */
    public Double getMaxX() {
        return maxX;
    }

    /**
     * @return Minimal Y value in point space
     */
    public Double getMinY() {
        return minY;
    }

    /**
     * @return Maximal Y value in point space
     */
    public Double getMaxY() {
        return maxY;
    }
}