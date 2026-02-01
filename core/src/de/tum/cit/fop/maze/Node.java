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

    public boolean isWalkable() {
        return walkable;
    }

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
    public float getG() {
        return g;
    }

    public void setG(float g) {
        this.g = g;
    }
    public float getH() {
        return h;
    }


    public void setH(float h) {
        this.h = h;
    }

    public Node getConnection() {
        return connection;
    }

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