package de.tum.cit.fop.maze;

import java.lang.reflect.Array;
import java.util.*;

import static de.tum.cit.fop.maze.TiledToPropertiesConverter.TYPE_WALL;

/**
 * Implements A* pathfinding algorithm for maze navigation.
 * This class creates a grid of nodes from map data and finds optimal paths
 * between two points. It includes path smoothing using line-of-sight checks
 * to reduce unnecessary waypoints and create more natural movement.

 * The pathfinding uses Manhattan distance as the heuristic and only allows
 * 4-directional movement (no diagonals).
 *
 */
public class PathFinder {
    private Node [][] nodeGrid;
    private static final int TILE_SIZE = 16;
    int mapWidth = 0;
    int mapHeight =0;
    /**
     * Constructs a PathFinder from map data.
     * Creates a node grid where each node corresponds to a tile in the map.
     * Nodes are marked as walkable if their map value is non-zero.
     *
     * @param mapData 2D array where 0 = wall, non-zero = walkable
     */
    public PathFinder(int [][] mapData){
        this.mapWidth= mapData.length;
        this.mapHeight= mapData[0].length;
        this.nodeGrid = new Node[mapWidth][mapHeight];
        for(int x =0; x<mapWidth ; x++){
            for(int y =0; y<mapHeight; y++){
                Node node =new Node(x,y);
                if (mapData[x][y]!=TYPE_WALL){
                    node.setWalkable(true);
                }
                nodeGrid[x][y]= node;
            }
        }
    }
    /**
     * Gets all walkable neighboring nodes of a given node.
     * Only considers 4-directional movement (up, down, left, right).
     * Diagonal movement is not allowed.
     *
     * @param node The node whose neighbors to find
     * @return ArrayList of walkable neighboring nodes
     */
    public ArrayList<Node> getNeighbors(Node node){
        ArrayList<Node> neighbors = new ArrayList<>();
        if(node.x+1<mapWidth){
            Node neighbor1 = nodeGrid[node.x+1][node.y];
            if(neighbor1.isWalkable()){
                neighbors.add(neighbor1);
            }
        }
        if(node.x-1>=0){
            Node neighbor2 = nodeGrid[node.x-1][node.y];
            if(neighbor2.isWalkable()){
                neighbors.add(neighbor2);
            }
        }
        if(node.y+1<mapHeight){
            Node neighbor3 = nodeGrid[node.x][node.y+1];
            if(neighbor3.isWalkable()){
                neighbors.add(neighbor3);
            }
        }
        if(node.y-1>=0){
            Node neighbor4 = nodeGrid[node.x][node.y-1];
            if(neighbor4.isWalkable()){
                neighbors.add(neighbor4);
            }
        }
        return neighbors;

    }
    /**
     * Finds the shortest path between two points using A* algorithm.
     * Converts pixel coordinates to tile coordinates, then uses A* to find
     * the optimal path. Returns an empty list if no path exists or if the
     * destination is unwalkable.
     *
     * @param x1 Starting X coordinate in pixels
     * @param y1 Starting Y coordinate in pixels
     * @param x2 Target X coordinate in pixels
     * @param y2 Target Y coordinate in pixels
     * @return ArrayList of nodes representing the path, or empty list if no path found
     */
    public ArrayList<Node> findPath(float x1,float y1,float x2, float y2){
        int startX = Math.max(0, Math.min((int)(x1 / TILE_SIZE), mapWidth - 1));
        int startY = Math.max(0, Math.min((int)(y1 / TILE_SIZE), mapHeight - 1));
        int endX = Math.max(0, Math.min((int)(x2 / TILE_SIZE), mapWidth - 1));
        int endY = Math.max(0, Math.min((int)(y2 / TILE_SIZE), mapHeight - 1));
        Node startNode= nodeGrid[startX][startY];
        Node endNode= nodeGrid[endX][endY];
        if (startNode == null || endNode == null || !endNode.isWalkable()) {
            return new ArrayList<>();
        }


        for(int x=0;x<mapWidth;x++){
            for(int y=0;y<mapHeight;y++){
                Node tempNode=nodeGrid[x][y];
                tempNode.setG(Float.MAX_VALUE);
                tempNode.setConnection(null);
            }
        }
        startNode.setG(0);
        startNode.setH(getDistance(startNode,endNode));
        PriorityQueue<Node> openNodes = new PriorityQueue<>();
        HashSet<Node> closedNodes = new HashSet<>();
        openNodes.add(startNode);
        ArrayList<Node> path=new ArrayList<>();
        while(!openNodes.isEmpty()){
            Node current = openNodes.poll();
            if(current.equals(endNode)){
                while (current!=null){
                path.add(current);
                current=current.getConnection();

                }
                Collections.reverse(path);

                return path;
            }
            else {
                closedNodes.add(current);
                ArrayList<Node>neighbors=getNeighbors(current);
                for(Node neighbor : neighbors){
                    if(closedNodes.contains(neighbor)){
                        continue;
                    }
                    if(neighbor.getG()>current.getG()+1){
                        openNodes.remove(neighbor);
                        neighbor.setG(current.getG() + 1);
                        neighbor.setConnection(current);
                        neighbor.setH(getDistance(neighbor, endNode));
                        openNodes.add(neighbor);
                    }
                }

            }
        }
        Collections.reverse(path);
        if(!path.isEmpty()) path.remove(0);

        return path;
    }
    /**
     * Calculates Manhattan distance between two nodes.
     * Used as the heuristic function for A* pathfinding.
     * Manhattan distance is the sum of absolute differences in coordinates.
     *
     * @param a First node
     * @param b Second node
     * @return Manhattan distance as an integer
     */
    public int getDistance(Node a,Node b){
        return Math.abs(a.x-b.x)+Math.abs(a.y-b.y);
    }
    /**
     * Checks if there's a clear line of sight between two nodes.
     * Uses Bresenham's line algorithm to check if all tiles between
     * two nodes are walkable. Used for path smoothing.
     *
     * @param a Starting node
     * @param b Target node
     * @return true if clear line of sight exists, false if blocked by walls
     */
    private boolean hasLineOfSight(Node a, Node b) {
        int x0 = a.x;
        int y0 = a.y;
        int x1 = b.x;
        int y1 = b.y;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int curX = x0;
        int curY = y0;

        while (true) {
            if (!nodeGrid[curX][curY].isWalkable()) return false;

            if (curX == x1 && curY == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                curX += sx;
            }
            if (e2 < dx) {
                err += dx;
                curY += sy;
            }
        }
        return true;
    }
    /**
     * Smooths a path by removing unnecessary waypoints.
     * Uses line-of-sight checks to skip intermediate waypoints when a
     * direct path is available. This creates more natural, direct movement
     * instead of strict grid-based pathfinding.
     * For example, a path like (0,0) → (1,0) → (2,0) → (2,1) → (2,2)
     * might be smoothed to (0,0) → (2,0) → (2,2) if line of sight allows.
     *
     * @param path The original path from A* algorithm
     * @return Smoothed path with fewer waypoints
     */
    public ArrayList<Node> smoothPath(ArrayList<Node> path) {
        if (path.size() <= 2) return path;

        ArrayList<Node> smoothed = new ArrayList<>();
        smoothed.add(path.get(0)); // Keep the start

        int current = 0;
        while (current < path.size() - 1) {
            int nextVisible = current + 1;
            for (int i = current + 2; i < path.size(); i++) {
                if (hasLineOfSight(path.get(current), path.get(i))) {
                    nextVisible = i;
                } else {
                    break;
                }
            }
            smoothed.add(path.get(nextVisible));
            current = nextVisible;
        }
        return smoothed;
    }
}
