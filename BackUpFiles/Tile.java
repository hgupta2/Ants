/**
 * Represents a tile of the game map.
 */
public class Tile implements Comparable<Tile> {
    private final int row;
    
    private final int col;
    
    private Tile[] neighbors;
    
    private int distance;
    
    /**
     * Creates new {@link Tile} object.
     * 
     * @param row row index
     * @param col column index
     */
    public Tile(int row, int col) {
        this.row = row;
        this.col = col;
        neighbors = new Tile[4];
        distance = 0;
    }
    
    /**
     * Returns neighbors of the Tile.
     * 
     * @return neighbors
     */
    public Tile[] getNeighbors(){
    	return neighbors;
    }
    
    /**
     * Returns row index.
     * 
     * @return row index
     */
    public int getRow() {
        return row;
    }
    
    /**
     * Returns column index.
     * 
     * @return column index
     */
    public int getCol() {
        return col;
    }
    
    /**
     * Returns distance.
     * 
     * @return distance
     */
    public int getdist() {
        return distance;
    }
    
    /**
     * Sets a new distance.
     * 
     * @param distance
     */
    public void setdist(int dist) {
        distance = dist;
    }
    
    /** 
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Tile o) {
        return hashCode() - o.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return row * Ants.MAX_MAP_SIZE + col;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        boolean result = false;
        if (o instanceof Tile) {
            Tile tile = (Tile)o;
            result = row == tile.row && col == tile.col;
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return row + " " + col;
    }
    
    /**
     * Set neighbors of the Tile at position i.
     * 
     */    
    public void setNeighbors(int i, Tile tile){
    	neighbors[i] = tile;
    }
    
    /**
     * remove Water tile from neighbors
     * 
     */
    public void removeNeighbor(Tile tile){
    	if(neighbors.length > 1){
	    	Tile[] newNeighbors = new Tile[neighbors.length - 1];
	    	int i = 0;
	    	for(Tile neighbor : getNeighbors()){
	    		if( neighbor != tile)
	    			newNeighbors[i++] = neighbor;
	    	}
	    	
	    	neighbors = newNeighbors;
    	}
    }
    
}
