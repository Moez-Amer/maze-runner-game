package de.tum.cit.fop.maze;

/**
 * Represents a single tile/node in the pathfinding grid.
 * <p>
 * This class is used by the A* pathfinding algorithm to track tile positions,
 * costs, and connections. Each node stores its grid coordinates, walkability
 * status, and the A* algorithm values (g, h, and f costs). Implements
 * Comparable to allow priority queue sorting based on f-cost during pathfinding.
 * </p>
 */
public class Node implements Comparable<Node>{
    public int x;
    public int y;
    private float g;
    private float h;
    private float f;
    private Node connection;
    private boolean walkable;

    /**
     * Constructs a new Node at the specified grid coordinates.
     * Initializes all pathfinding costs to 0 and sets the node as
     * non-walkable by default. Walkability must be set explicitly
     * based on map data.
     *
     * @param x X coordinate in the tile grid.
     * @param y Y coordinate in the tile grid.
     */
    public Node(int x,int y){
        this.x=x;
        this.y=y;
        this.g=0f;
        this.h=0f;
        this.connection=null;
        this.f=0f;
        this.walkable=false;
    }

    /**
     * Checks whether this node can be traversed during pathfinding.
     *
     * @return true if the node is walkable.
     */
    public boolean isWalkable() {
        return walkable;
    }

    /**
     * Sets whether this node can be traversed during pathfinding.
     *
     * @param walkable true to mark the node as walkable, false for a wall.
     */
    public void setWalkable(boolean walkable) {
        this.walkable = walkable;
    }

    /**
     * Returns the total estimated cost (f) for the A* algorithm.
     * Calculated as the sum of the movement cost from the start (g)
     * and the heuristic estimate to the goal (h).
     *
     * @return The f-cost value (g + h).
     */
    public float getF(){
        return g+h;
    }

    /**
     * Returns the movement cost from the start node to this node.
     *
     * @return The g-cost value.
     */
    public float getG() {
        return g;
    }

    /**
     * Sets the movement cost from the start node to this node.
     *
     * @param g The new g-cost value.
     */
    public void setG(float g) {
        this.g = g;
    }

    /**
     * Returns the heuristic estimate from this node to the goal.
     *
     * @return The h-cost value.
     */
    public float getH() {
        return h;
    }

    /**
     * Sets the heuristic estimate from this node to the goal.
     *
     * @param h The new h-cost value.
     */
    public void setH(float h) {
        this.h = h;
    }

    /**
     * Returns the parent node in the shortest path found so far.
     * Used to reconstruct the full path once the goal is reached.
     *
     * @return The connected parent Node, or null if this is the start node.
     */
    public Node getConnection() {
        return connection;
    }

    /**
     * Sets the parent node in the shortest path found so far.
     *
     * @param connection The parent Node to connect to.
     */
    public void setConnection(Node connection) {
        this.connection = connection;
    }

    /**
     * Compares this node to another based on f-cost for priority queue ordering.
     * Lower f-cost nodes are considered higher priority.
     *
     * @param other The other Node to compare against.
     * @return A negative value if this node has a lower f-cost, positive if higher,
     *         or 0 if equal.
     */
    @Override
    public int compareTo(Node other){
        return Float.compare(this.getF(),other.getF());
    }
}