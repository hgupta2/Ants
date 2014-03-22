import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }
    
    /**
     * For every ant check every direction in fixed order (N, E, S, W) and move it if the tile is
     * passable.
     */
	 
	/** HashMap to store position of each ant to avoid collisions */
	private Map<Tile, Tile> orders = new HashMap<Tile, Tile>();

	/** To keep track of enemy hills */
	private Set<Tile> enemyHills = new HashSet<Tile>();
	
    /** Try to perform the move to this location*/
	private boolean doMoveDirection(Tile antLoc, Aim direction) {
        Ants ants = getAnts();
        // Track all moves, prevent collisions
        Tile newLoc = ants.getTile(antLoc, direction);
        if (ants.getIlk(newLoc).isUnoccupied() && !orders.containsKey(newLoc)) {
            ants.issueOrder(antLoc, direction);
            orders.put(newLoc, antLoc);
            return true;
        } else {
            return false;
        }
    }
	 
	
	private boolean doMoveLocation(Tile antLoc, Tile destLoc) {
        Ants ants = getAnts();
        // Track targets to prevent 2 ants to the same location
        List<Aim> directions = ants.getDirections(antLoc, destLoc);
        for (Aim direction : directions) {
            if (doMoveDirection(antLoc, direction)) {
                return true;
            }
        }
        return false;
    }
	
	private boolean takePath(Tile antLoc , Tile newLoc){
		Ants ants = getAnts();
		Aim direction = null;
		
		if (newLoc.getRow() == antLoc.getRow()) {
			if (newLoc.getCol() == antLoc.getCol()+1) direction =  Aim.EAST;
			else if (newLoc.getCol() == antLoc.getCol()-1) direction= Aim.WEST;
			else direction = antLoc.getCol() == 0 ? Aim.WEST : Aim.EAST;
		} else {
			if (newLoc.getRow() == antLoc.getRow()+1) direction = Aim.SOUTH;
			else if (newLoc.getRow() == antLoc.getRow()-1) direction = Aim.NORTH;
			else direction = antLoc.getRow() == 0 ? Aim.NORTH : Aim.SOUTH;
		}

		if (ants.getIlk(newLoc).isUnoccupied() && !orders.containsKey(newLoc)) {
            ants.issueOrder(antLoc, direction);
            orders.put(newLoc, antLoc);
            return true;
		} else {
            return false;
        }
	}
	
	/*private boolean takePath2(Tile antLoc, Tile destLoc, HashMap<Tile, Tile> backTrack){
		Ants ants = getAnts();
		Aim direction = null;
		Tile newLoc = null;
		
		while(backTrack.get(destLoc) != antLoc){
			newLoc = backTrack.get(destLoc);
			destLoc = newLoc;
		}
		
		if (newLoc.getRow() == antLoc.getRow()) {
			if (newLoc.getCol() == antLoc.getCol()+1) direction =  Aim.EAST;
			else if (newLoc.getCol() == antLoc.getCol()-1) direction= Aim.WEST;
			else direction = antLoc.getCol() == 0 ? Aim.WEST : Aim.EAST;
		} else {
			if (newLoc.getRow() == antLoc.getRow()+1) direction = Aim.SOUTH;
			else if (newLoc.getRow() == antLoc.getRow()-1) direction = Aim.NORTH;
			else direction = antLoc.getRow() == 0 ? Aim.NORTH : Aim.SOUTH;
		}
		
		if (ants.getIlk(newLoc).isUnoccupied() && !orders.containsKey(newLoc)) {
            ants.issueOrder(antLoc, direction);
            orders.put(newLoc, antLoc);
            return true;
        } else {
            return false;
        }
	}*/
	
	private void takeRandomPath(Ants ants){
		Set<Aim> directions = new HashSet<Aim>();
		directions.add(Aim.EAST);
		directions.add(Aim.WEST);
		directions.add(Aim.NORTH);
		directions.add(Aim.SOUTH);
		
		for(Tile antLoc : ants.getMyAnts()){
			if(!orders.containsValue(antLoc)){
				Tile loc = null;
				
				for(Aim aim : directions){
					if(ants.getIlk(antLoc, aim).isUnoccupied()){
						loc = ants.getTile(antLoc, aim);
						ants.issueOrder(antLoc, aim);
						orders.put(loc, antLoc);
					}
				}
			}
		}
	}
	
    private void BFS(Ants ants, TreeSet<Tile> sortedTarget, int distance, boolean keep){
    	Tile[][] mapTiles = ants.getMapTiles();
    	Map<Tile, Tile> hashTargets = new HashMap<Tile, Tile>();
        TreeSet<Tile> myAnts = new TreeSet<Tile>(ants.getMyAnts());
    	
        for(Tile target : sortedTarget){
        	Queue<Tile> queue = new LinkedList<Tile>();
        	queue.add(mapTiles[target.getRow()][target.getCol()]);
        	boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
        	boolean flag = false;
        	
        	target.setdist(0);
        	
        	while(!queue.isEmpty()){
        		Tile tile = queue.remove();
        		visited[tile.getRow()][tile.getCol()] = true;
        		Tile[] neighbors = tile.getNeighbors();
        		
        		if(tile.getdist() > distance){
        			break;
        		}
        		
        		for(Tile neighbor : neighbors){
        			if( !visited[neighbor.getRow()][neighbor.getCol()]){ 
        				neighbor.setdist(tile.getdist()+1);
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				
        				if(myAnts.contains(neighbor) 
        					&& !hashTargets.containsKey(target)
                            && !hashTargets.containsValue(neighbor)
                            //&& doMoveLocation(neighbor, target)){
                            && takePath(neighbor, tile)){
        				if(keep == true){
        					hashTargets.put(target, neighbor);
        				}
        				flag = true;
        				break;
        				}else{
        					queue.add(neighbor);
        				}
        			}
        		}
        		if(flag == true){
        			queue.clear();
        		}
        	}
        }
        
        for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    }
	
	/*public Set<Tile> exploreUnseen(Ants ants){
		Queue<Tile> exploreAnts = new LinkedList<Tile>();
		Set<Tile> myAnts = ants.getMyAnts();
		Tile[][] mapTiles = ants.getMapTiles();
		boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
		Set<Tile> exAnts = new HashSet<Tile>();
		HashMap<Tile, Boolean> explored = ants.getExploredTiles();
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				if(ants.isVisible(mapTiles[i][j]))
					explored.put(mapTiles[i][j], true);
			}
		}
		
		for(Tile tile : myAnts){
			if(!orders.containsValue(tile)){
				//visited[tile.getRow()][tile.getCol()] = true;
				exploreAnts.add(mapTiles[tile.getRow()][tile.getCol()]);
				exAnts.add(tile);
			}
		}
		
		while(!exploreAnts.isEmpty()){
			Tile target = exploreAnts.remove();
			if(target.getdist() > 10 && !unseenTiles.contains(target) && !explored.get(target)){
				explored.put(target, true);
				unseenTiles.add(target);
				continue;
			}else {
				for(Tile tile : target.getNeighbors()){
					if(!visited[tile.getRow()][tile.getCol()]){
						visited[tile.getRow()][tile.getCol()] = true;
						tile.setdist(target.getdist()+1);
						exploreAnts.add(tile);
					}
				}
			}
		}
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
				
		ants.setExploredTiles(explored);
		
		return exAnts;
	}*/
    
    private void exploreBFS(Ants ants, TreeSet<Tile> sortedAnts){
    	HashMap<Tile, Boolean> explored = ants.getExploredTiles();
    	Tile[][] mapTiles = ants.getMapTiles();
    	TreeSet<Tile> unseenTiles = new TreeSet<Tile>();
    	//HashMap<Tile, Tile> backTrack = new HashMap<Tile, Tile>();
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				if(ants.isVisible(mapTiles[i][j]) && !explored.get(mapTiles[i][j]))
					explored.put(mapTiles[i][j], true);
			}
		}
    	
    	for(Tile source : sortedAnts){
    		if(!orders.containsValue(source)){
    			Queue<Tile> queue = new LinkedList<Tile>();
    			boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    			boolean flag = false;
    			
    			queue.add(mapTiles[source.getRow()][source.getCol()]);
    			source.setdist(0);
    			//backTrack.put(source, null);
    			
    			while(!queue.isEmpty()){
    				Tile tile = queue.remove();
    				visited[tile.getRow()][tile.getCol()] = true;
            		
    				if(tile.getdist() > 20){
            			break;
            		}
    				
            		for(Tile neighbor : tile.getNeighbors()){
            			if( !visited[neighbor.getRow()][neighbor.getCol()]){
            				neighbor.setdist(tile.getdist()+1);
            				visited[neighbor.getRow()][neighbor.getCol()] = true;
            				//backTrack.put(neighbor, tile);
            				
            				if(!explored.get(neighbor)){
            					//&& takePath2(source, neighbor, backTrack)){
            					explored.put(neighbor, true);
            					unseenTiles.add(neighbor);
            					flag = true;
                				break;
            				}else{
            					queue.add(neighbor);
            				}
            			}
            		}
            		
            		if(flag == true){
            			queue.clear();
            		}
    				
    			}
    		}
    	}
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	
    	ants.setExploredTiles(explored);
    	
    	for(Tile target : unseenTiles){
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		boolean flag = false;
    		
    		queue.add(target);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
        		
        		for(Tile neighbor : tile.getNeighbors()){
        			if(!visited[neighbor.getRow()][neighbor.getCol()]){
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				if(sortedAnts.contains(neighbor)
        				   && takePath(neighbor, tile)){
        					flag = true;
        					break;
        				}else{
        					queue.add(neighbor);
        				}
        			}
        		}
        		if(flag == true){
        			queue.clear();
        		}
    		}
    	}
    }
        
    private TreeSet<Tile> findDetachedAnts(Ants ants){
    	TreeSet<Tile> myAnts = new TreeSet<Tile>();
    	TreeSet<Tile> detachedAnts = new TreeSet<Tile>();
    	Tile[][] mapTiles = ants.getMapTiles();
    	
    	for(Tile ant : ants.getMyAnts()){
    		for(Tile hill : ants.getMyHills()){
    			if(ants.getDistance(ant, hill) > 8){
    				myAnts.add(ant);
    				break;
    			}
    		}
    	}
    	
    	for(Tile myAnt : myAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		boolean flag = false;
    		
    		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
    		myAnt.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 3){
    				detachedAnts.add(myAnt);
    				break;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					if(myAnts.contains(neighbor)){
    						flag = true;
    						break;
    					}else{
    						queue.add(neighbor);
    					}
    				}
    			}
    			if(flag == true){
        			queue.clear();
        		}
    		}
    	}
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	return detachedAnts;
    }
    
	@Override
    public void doTurn() {
        Ants ants = getAnts();
        orders.clear();
        //ants.updateMapTiles();
        
        // prevent stepping on own hill
        for (Tile myHill : ants.getMyHills()) {
            orders.put(myHill, null);
        }
		
        // find close food
        BFS(ants, new TreeSet<Tile>(ants.getFoodTiles()), 15, true);
        
        // add new hills to set
        for (Tile enemyHill : ants.getEnemyHills()) {
            if (!enemyHills.contains(enemyHill)) {
                enemyHills.add(enemyHill);
            }
        }
        
		//Explore Unseen Area
        exploreBFS(ants, new TreeSet<Tile>(ants.getMyAnts()));
        
        // attack hills
        //BFS(ants, new TreeSet<Tile>(enemyHills), 30);
        /*List<Route> hillRoutes = new ArrayList<Route>();
        for (Tile hillLoc : enemyHills) {
            for (Tile antLoc : ants.getMyAnts()) {
                if (!orders.containsValue(antLoc)) {
                    int distance = ants.getDistance(antLoc, hillLoc);
                    Route route = new Route(antLoc, hillLoc, distance);
                    hillRoutes.add(route);
                }
            }
        }
		
        Collections.sort(hillRoutes);
        for (Route route : hillRoutes) {
            doMoveLocation(route.getStart(), route.getEnd());
        }*/
        
        //send idle ants to support detached ants
        TreeSet<Tile> detachedAnts = findDetachedAnts(ants);
        BFS(ants, detachedAnts, 30, false);
        
        //send to random location
        //takeRandomPath(ants);
        
        // unblock hills
        for (Tile myHill : ants.getMyHills()) {
            if (ants.getMyAnts().contains(myHill) && !orders.containsValue(myHill)) {
                for (Aim direction : Aim.values()) {
                    if (doMoveDirection(myHill, direction)) {
                        break;
                    }
                }
            }
        }
    }
}
