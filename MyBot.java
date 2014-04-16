import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import javax.print.Doc;
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
	private Set<Tile> orderAnts = new HashSet<Tile>();

	/** Ants object */
	private Ants ants;
	
	/** Map object to track of tiles */
	private Tile[][] mapTiles;
	
	/** My ants object to store my ants on the map */
	private Set<Tile> myAnts = new HashSet<Tile>();
	
	/** Enemy ants object to store all my enemy ants	 */
	private Set<Tile> enemyAnts = new HashSet<Tile>();
	
	/** My hills object  */
	private Set<Tile> myHills =  new HashSet<Tile>();
	
	/** To keep track of enemy hills */
	private Set<Tile> enemyHills = new HashSet<Tile>();
	private Set<Tile> enemyHillsRazed = new HashSet<Tile>();
	boolean hillRazed = false;
	
	/** Food Tiles to keep track of food */
	private Set<Tile> foodTiles = new HashSet<Tile>();
	
	// things to remember between turns
	private Map<Tile, Aim> antStraight = new HashMap<Tile, Aim>();
	private Map<Tile, Aim> antLefty = new HashMap<Tile, Aim>();
	
	//Fighting Ants
	List<Tile> myFightAnts = new ArrayList<Tile>();
	List<Tile> enemyFightAnts = new ArrayList<Tile>();
	
	//Closest Enemy Ant
	HashMap<Tile, Tile> distMap = new HashMap<Tile, Tile>();
	
	//For max step of multi mini max
	HashMap<Tile, Tile> currTo = new HashMap<Tile, Tile>(); 
	Set<Tile> path = new HashSet<Tile>();
	
	//For min step of multi mini max
	HashMap<Tile, Tile> enemyCurrTo = new HashMap<Tile, Tile>(); 
	Set<Tile> enemyPath = new HashSet<Tile>();
	
	//Store dangered ants
	Set<Tile> dangeredAnts = new HashSet<Tile>();
	
	//Direct Enemies
	HashMap<Tile, ArrayList<Tile>> antGammaEnemies = new HashMap<Tile, ArrayList<Tile>>();
	HashMap<Tile, ArrayList<Tile>> enemyAntGammaEnemies = new HashMap<Tile, ArrayList<Tile>>();
	
	//For minimax algo
	private boolean timeOut;
	private boolean doCut; 
	private int bestPrecValue;
	private long startTime; 
	
	private boolean takePath(Tile antLoc , Tile newLoc){
		if(antLoc == null || newLoc == null){
			return false;
		}
		
		if(enemyHills.contains(antLoc)){
			enemyHillsRazed.add(antLoc);
			hillRazed = true;
			//enemyHills.clear();
		}
		
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
            orderAnts.add(antLoc);
            return true;
		} else {
            return false;
        }
	}
	
	private boolean takeRandomPath(Ants ants){
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
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private Set<Tile> findSafeTiles(Set<Tile> targets){
		Set<Tile> storeDangerTiles = new HashSet<Tile>();
		
		for(Tile target : targets){
			Queue<Tile> queue = new LinkedList<Tile>();
        	queue.add(mapTiles[target.getRow()][target.getCol()]);
        	boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
        	int myAntCount = 0;
        	int enemyAntCount = 0;
        	
        	target.setdist(0);
        	
        	while(!queue.isEmpty()){
        		Tile tile = queue.remove();
        		visited[tile.getRow()][tile.getCol()] = true;
        		Tile[] neighbors = tile.getNeighbors();
        		
        		if(tile.getdist() > 5){
        			if((myAntCount - enemyAntCount) < 0){
        				storeDangerTiles.add(target);
        				break;
        			}
        			continue;
        		}
        		
        		for(Tile neighbor : neighbors){
        			if( !visited[neighbor.getRow()][neighbor.getCol()]){ 
        				neighbor.setdist(tile.getdist()+1);
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				
        				if(myAnts.contains(neighbor)){
        					myAntCount++;        				
        				}else if(enemyAnts.contains(neighbor)){
        					enemyAntCount++;
        				}
        				queue.add(neighbor);
        			}
        		}
        	}
		}
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
		
		for(Tile target : storeDangerTiles){
			targets.remove(target);
		}
		return targets;
	}
	
    private void foodBFS(Set<Tile> sortedTarget, int distance){
    	sortedTarget = findSafeTiles(sortedTarget);
    	
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
        			continue;
        		}
        		
        		for(Tile neighbor : neighbors){
        			if( !visited[neighbor.getRow()][neighbor.getCol()]){ 
        				neighbor.setdist(tile.getdist()+1);
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				
        				if(myAnts.contains(neighbor) 
        					&& !hashTargets.containsKey(target)
                            && !hashTargets.containsValue(neighbor)
                            && !orderAnts.contains(neighbor)
                            && takePath(neighbor, tile)){
        				hashTargets.put(target, neighbor);
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
	
	private void exploreBFS2(Ants ants, TreeSet<Tile> sortedAnts){
    	HashMap<Tile, Boolean> explored = ants.getExploredTiles();
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
    			if(ants.getDistance(ant, hill) > 20){
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
    
    private void enemyHillsBFS(Set<Tile> targets, int count, int distance){
    	targets = findSafeTiles(targets);
    	
    	for(Tile target : targets){
    		boolean flag = false;
    		Queue<Tile> queue = new LinkedList<Tile>();
        	boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[target.getRow()][target.getCol()]);
    		target.setdist(0);
    		    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > distance){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(myAnts.contains(neighbor) && !orderAnts.contains(neighbor) && takePath(neighbor, tile)){
    						count--;
    						if(count <= 0){
    							flag = true;
    							break;
    						}
    					}
    					queue.add(neighbor);
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
    	
    	if(hillRazed == true){
    		enemyHills.clear();
    		hillRazed = false;
    	}
    }
    
    private void incExploreValue(){
    	for(Tile[] row : mapTiles){
    		for (Tile tile : row){
    			tile.setExpVal(tile.getExpVal()+1);
    		}
    	}
    	
    	//System.out.println(mapTiles[new Random().nextInt(ants.getRows())][new Random().nextInt(ants.getRows())].getExpVal() + ">>>>>>>>>>>");
    }
    
    private void initExplore(){
    	for(Tile myAnt : myAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
        	boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
        	
        	queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
    		myAnt.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 10){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					mapTiles[neighbor.getRow()][neighbor.getCol()].setExpVal(0);
    					queue.add(neighbor);
    				}
    			}
    		}
    	}
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    }
    
    private void exploreBFS(Tile myAnt){
    	Queue<Tile> queue = new LinkedList<Tile>();
    	boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    	HashMap<Tile, Integer> values = new HashMap<Tile, Integer>();
    	
		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
		myAnt.setdist(0);
    			
		while(!queue.isEmpty()){
			Tile tile = queue.remove();
			visited[tile.getRow()][tile.getCol()] = true;
			
			if(tile.getdist() > 10){
				values.put(tile, tile.getExpVal());	
				continue;
			}
    				
			for(Tile neighbor : tile.getNeighbors()){
				if(!visited[neighbor.getRow()][neighbor.getCol()]){
					visited[neighbor.getRow()][neighbor.getCol()] = true;
					neighbor.setdist(tile.getdist()+1);
					queue.add(neighbor);
				}
			}
			
    	}
		
		int max = 0;
		Tile dest = null;
		for(Map.Entry<Tile, Integer> entry : values.entrySet()){
			if(entry.getValue() > max){
				max = entry.getValue();
				dest = entry.getKey();
			}
		}
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
				visited[i][j] = false;
			}
		}
		
		if(dest != null){
			queue.clear();
			queue.add(mapTiles[dest.getRow()][dest.getCol()]);
			dest.setdist(0);
			
			while(!queue.isEmpty()){
				Tile tile = queue.remove();
				visited[tile.getRow()][tile.getCol()] = true;
				
				if(tile.getdist() > 15){
					continue;
				}
				
				for(Tile neighbor : tile.getNeighbors()){
					if(!visited[neighbor.getRow()][neighbor.getCol()]){
						visited[neighbor.getRow()][neighbor.getCol()] = true;
						neighbor.setdist(tile.getdist()+1);
						
						if(neighbor.equals(myAnt) && takePath(neighbor, tile)){
							//System.out.println("----------"+neighbor+"sssssssssssss"+tile+"----------");
							queue.clear();
							break;
						}else{
							queue.add(neighbor);
						}
					}
				}
			}
		}
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    }
    
    private void exploreBorderTiles(){
    	Set<Tile> allAnts = new HashSet<Tile>();
    	//System.out.println(myAnts.size() + "---------" +enemyAnts.size());
    	allAnts.addAll(myAnts);
    	allAnts.addAll(enemyAnts);
    	//System.out.println(allAnts.size() + ">>>>>>>>>>>>");
    	Set<Tile> allVisibleTiles = new HashSet<Tile>();
    	
    	for(Tile ant : allAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
        	boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		queue.add(mapTiles[ant.getRow()][ant.getCol()]);
    		ant.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 10){
    				continue;
    			}else{
    				allVisibleTiles.add(tile);
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					queue.add(neighbor);
    				}
    			}
    		}
    	}
    	
    	Set<Tile> borderTiles = new HashSet<Tile>();
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	
    	for(Iterator<Tile> it=allVisibleTiles.iterator(); it.hasNext(); ){
    		Tile tile = it.next();
    		for(Tile neighbor : tile.getNeighbors()){
    			if(!allVisibleTiles.contains(neighbor)){
    				borderTiles.add(neighbor);
    			}
    		}
    	}
    	
    	foodBFS(borderTiles, 15);
    }
    
    private void leftyBotExplore(){
    	Set<Tile> destinations = new HashSet<Tile>();
		Map<Tile, Aim> newStraight = new HashMap<Tile, Aim>();
		Map<Tile, Aim> newLefty = new HashMap<Tile, Aim>();
		for (Tile location : myAnts) {
			if(!orders.containsValue(location)){
				// send new ants in a straight line
				if (!antStraight.containsKey(location) && !antLefty.containsKey(location)) {
					Aim direction;
					if (location.getRow() % 2 == 0) {
						if (location.getCol() % 2 == 0) {
							direction = Aim.NORTH;
						} else {
							direction = Aim.SOUTH;
						}
					} else {
						if (location.getCol() % 2 == 0) {
							direction = Aim.EAST;
						} else {
							direction = Aim.WEST;
						}
					}
					antStraight.put(location, direction);
				}
				// send ants going in a straight line in the same direction
				if (antStraight.containsKey(location)) {
					Aim direction = antStraight.get(location);
					Tile destination = ants.getTile(location, direction);
					if (ants.getIlk(destination).isPassable()) {
						if (ants.getIlk(destination).isUnoccupied() && !destinations.contains(destination)) {
							ants.issueOrder(location, direction);
							newStraight.put(destination, direction);
							destinations.add(destination);
						} else {
							// pause ant, turn and try again next turn
							newStraight.put(location, direction.left());
							destinations.add(location);
						}
					} else {
						// hit a wall, start following it
						antLefty.put(location, direction.right());
					}
				}
				// send ants following a wall, keeping it on their left
				if (antLefty.containsKey(location)) {
					Aim direction = antLefty.get(location);
					List<Aim> directions = new ArrayList<Aim>();
					directions.add(direction.left());
					directions.add(direction);
					directions.add(direction.right());
					directions.add(direction.behind());
					// try 4 directions in order, attempting to turn left at corners
					for (Aim new_direction : directions) {
						Tile destination = ants.getTile(location, new_direction);
						if (ants.getIlk(destination).isPassable()) {
							if (ants.getIlk(destination).isUnoccupied() && !destinations.contains(destination)) {
								ants.issueOrder(location, new_direction);
								newLefty.put(destination, new_direction);
								destinations.add(destination);
								break;
							} else {
								// pause ant, turn and send straight
								newStraight.put(location, direction.right());
								destinations.add(location);
								break;
							}
						}
					}
				}
			}
		}
		antStraight = newStraight;
		antLefty = newLefty;
    }
    
    private void approachEnemyBFS(Set<Tile> targets){
    	Set<Tile> targetTiles = new HashSet<Tile>();
    	
    	for(Tile target : targets){
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean flag = false;
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[target.getRow()][target.getCol()]);
    		target.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 4){
    				targetTiles.add(tile);
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(!targetTiles.contains(neighbor)){
    						targetTiles.remove(neighbor);
    					}
    					queue.add(neighbor);
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
    	
    	//System.out.println(targetTiles.size() + ">>>>>>>>>>>>>>>>>");
    	foodBFS(targetTiles, 12);
    }
    
    private void approachEnemyBFS2(){
    	HashMap<Tile, Tile> enemyLocations = new HashMap<Tile, Tile>();
    	
    	for(Tile myAnt : myAnts){
    		if(orderAnts.contains(myAnt)){
    			continue;
    		}
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean flag = false;
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
    		myAnt.setdist(0);
    		    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			    			
    			if(tile.getdist() > 20){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(enemyAnts.contains(neighbor)){
    						enemyLocations.put(myAnt, neighbor);
    						flag = true;
    						break;
    					}
    					queue.add(neighbor);
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
    	
    	HashMap<Tile, Tile> appLocations = new HashMap<Tile, Tile>();
    	
    	for(Map.Entry<Tile, Tile> entry : enemyLocations.entrySet()){
    		Tile myAnt = entry.getKey();
    		
    		Queue<Tile> queue = new LinkedList<Tile>();
    		
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
    		myAnt.setdist(0);
    		Tile[] antNeighbors = myAnt.getNeighbors();
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			    			
    			if(Euclidean(tile, entry.getValue()) < 5){
    				boolean flag = false;
    				for(Tile temp : antNeighbors){
    					if(temp!=null && temp.equals(tile) && orderAnts.contains(tile)){
    						flag = true;
    						break;
    					}
    				}
    				if(flag == true){
    					continue;	//why continue just let the ant stay where it is
    				}
    				appLocations.put(myAnt, tile);
    				break;
    			}else if(tile.getdist() > 20){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					queue.add(neighbor);
    				}
    			}
    		}
    	}
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	
    	//System.out.println("-----------ApproachEnemies-----------" + appLocations.size());
    	
    	for(Map.Entry<Tile, Tile> entry : appLocations.entrySet()){
    		Tile target = entry.getValue();
    		//System.out.println("-----------ApproachedAnt-----------" + entry.getKey().getRow() + "," + entry.getKey().getCol());
    		
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean flag = false;
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[target.getRow()][target.getCol()]);
    		target.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			    			
    			if(tile.getdist() > 20){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(entry.getKey().equals(neighbor) 
    							&& !orderAnts.contains(neighbor)
    							&& takePath(neighbor, tile)){
    						//System.out.println("-----------ApproachedTile-----------" + neighbor.getRow() + "," + neighbor.getCol());
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
    
    private int Euclidean(Tile source, Tile target){
    	int temp = (int) Math.sqrt(((source.getRow() - target.getRow()) * (source.getRow() - target.getRow())) + ((source.getCol() - target.getCol()) * (source.getCol() - target.getCol())));
    	return temp;
    }
    
    private boolean safeMove(Tile myAnt, Tile enemyAnt){
    	Queue<Tile> queue = new LinkedList<Tile>();
		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
		
		int myAntCount = 0;
		int enemyAntCount = 0;
		
		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
		while(!queue.isEmpty()){
			Tile tile = queue.remove();
			visited[tile.getRow()][tile.getCol()] = true;
			
			if(tile.getdist() > 3){
				break;
			}
			
			for(Tile neighbor : tile.getNeighbors()){
				if(!visited[neighbor.getRow()][neighbor.getCol()]){
					visited[neighbor.getRow()][neighbor.getCol()] = true;
					neighbor.setdist(tile.getdist()+1);
					
					if(myAnts.contains(neighbor)){
						myAntCount++;
					}
					queue.add(neighbor);
				}
			}
			
		}
    	
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
				visited[i][j] = false;
			}
		}
		
		queue.clear();
		queue.add(mapTiles[enemyAnt.getRow()][enemyAnt.getCol()]);
		while(!queue.isEmpty()){
			Tile tile = queue.remove();
			visited[tile.getRow()][tile.getCol()] = true;
			
			if(tile.getdist() > 4){
				break;
			}
			
			for(Tile neighbor : tile.getNeighbors()){
				if(!visited[neighbor.getRow()][neighbor.getCol()]){
					visited[neighbor.getRow()][neighbor.getCol()] = true;
					neighbor.setdist(tile.getdist()+1);
					
					if(enemyAnts.contains(neighbor)){
						enemyAntCount++;
					}
					queue.add(neighbor);
				}
			}
			
		}
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
		
		if((myAntCount - enemyAntCount) > 0){
			return true;
		}
    	return false;
    }
    
    private void attackBFS(Set<Tile> allMyAnts){
    	HashMap<Tile, Tile> myEnemy = new HashMap<Tile, Tile>();
    	
    	for(Tile myAnt : allMyAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
    		myAnt.setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 10){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(enemyAnts.contains(neighbor)){
    						if(myEnemy.containsKey(myAnt)){
    							if((myEnemy.get(myAnt).getdist() > neighbor.getdist())){
    								myEnemy.put(myAnt, neighbor);
    							}
    						}else{
    							myEnemy.put(myAnt, neighbor);
    						}
    						
    					}
    					queue.add(neighbor);
    				}
    			}  			
    		}
    	}
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	
    	HashMap<Tile, Tile> finalEnemy = new HashMap<Tile, Tile>();
    	System.out.println(myEnemy.size()+"-->>>>>>>>>>>>>>>");
    	for(Map.Entry<Tile, Tile> entry : myEnemy.entrySet()){
    		if(safeMove(entry.getKey(), entry.getValue())){
    			finalEnemy.put(entry.getKey(), entry.getValue());
    		}
    	}
    	
    	for(Map.Entry<Tile, Tile> entry : finalEnemy.entrySet()){
    		Queue<Tile> queue = new LinkedList<Tile>();
    		boolean[][] visited = new boolean[ants.getRows()][ants.getCols()];
    		
    		queue.add(mapTiles[entry.getValue().getRow()][entry.getValue().getCol()]);
    		entry.getValue().setdist(0);
    		
    		while(!queue.isEmpty()){
    			Tile tile = queue.remove();
    			visited[tile.getRow()][tile.getCol()] = true;
    			
    			if(tile.getdist() > 9){
    				continue;
    			}
    			
    			for(Tile neighbor : tile.getNeighbors()){
    				if(!visited[neighbor.getRow()][neighbor.getCol()]){
    					visited[neighbor.getRow()][neighbor.getCol()] = true;
    					neighbor.setdist(tile.getdist()+1);
    					
    					if(neighbor.equals(entry.getKey())){
    						if(takePath(neighbor, tile)){
    							queue.clear();
    							break;
    						}
    					}
    					queue.add(neighbor);
    				}
    			}  			
    		}
    	}
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    }
    
    private void findGroups(){
    	    	
    	for(Tile myAnt : myAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
        	queue.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
        	boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
        	boolean flag = false;
        	
        	myAnt.setdist(0);
        	
        	while(!queue.isEmpty()){
        		Tile tile = queue.remove();
        		visited[tile.getRow()][tile.getCol()] = true;
        		Tile[] neighbors = tile.getNeighbors();
        		
        		if(tile.getdist() > 5){
        			continue;
        		}
        		
        		for(Tile neighbor : neighbors){
        			if( !visited[neighbor.getRow()][neighbor.getCol()]){ 
        				neighbor.setdist(tile.getdist()+1);
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				
        				if(enemyAnts.contains(neighbor)){
        					distMap.put(myAnt, neighbor);
        					flag = true;
        					break;
        				}else{
        					queue.add(neighbor);
        				}
        			}
        		}
        		if(flag == true){
        			myFightAnts.add(mapTiles[myAnt.getRow()][myAnt.getCol()]);
        			queue.clear();
        		}
        	}
    	}
    	
    	for(Tile myFightAnt : myFightAnts){
    		Queue<Tile> queue = new LinkedList<Tile>();
        	queue.add(mapTiles[myFightAnt.getRow()][myFightAnt.getCol()]);
        	boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
        	
        	myFightAnt.setdist(0);
        	
        	while(!queue.isEmpty()){
        		Tile tile = queue.remove();
        		visited[tile.getRow()][tile.getCol()] = true;
        		Tile[] neighbors = tile.getNeighbors();
        		
        		if(tile.getdist() > 5){
        			continue;
        		}
        		
        		for(Tile neighbor : neighbors){
        			if( !visited[neighbor.getRow()][neighbor.getCol()]){ 
        				neighbor.setdist(tile.getdist()+1);
        				visited[neighbor.getRow()][neighbor.getCol()] = true;
        				
        				if(enemyAnts.contains(neighbor)){
        					enemyFightAnts.add(neighbor);        					
        				}
        				queue.add(neighbor);
        			}
        		}
        	}
    	}
    	
    	for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
    	
    	//System.out.println("-------MYFIGHTANT--------------" +myFightAnts.size());
    	//System.out.println("-------ENEMYFIGHTANT--------------" +enemyFightAnts.size());
    	
    }
    
    public void multiMax(int i, int distValue){
    	if((System.currentTimeMillis() - startTime > 420)){
    		timeOut = true;
    	}
    	if(timeOut) return;
    	    	
    	if(i < myFightAnts.size()){
    		Tile myAnt = myFightAnts.get(i);
    		
    		Tile from = myAnt;
    		Tile currTile = from;
    		
    		for(Tile n : myAnt.getNeighbors()){
    			if((n!=null) && (path.contains(n) || !ants.getIlk(n).isUnoccupied())) continue;
    			if((n!=null) && myAnts.contains(n) && currTo.get(n) == from) continue;
    			
    			path.add(n);
    			//System.out.println("--------FightAnt------------"+myAnt.getRow() + "," + myAnt.getCol());
    			//System.out.println("--------FightAnt------------"+n.getRow() + "," + n.getCol());
    			currTo.put(myAnt, n);
    			currTile = n;
    			
    			int newDistValue = distValue + Euclidean(myAnt, distMap.get(myAnt));
    			multiMax(i+1, newDistValue);
    			path.remove(n);
    		}
    		if(currTile != from) currTo.remove(myAnt);
    		/*if(!path.contains(from)){
    			path.add(from);
    			currTo.put(myAnt, null);
    			int newDistValue = distValue + Euclidean(from, distMap.get(myAnt));
    			multiMax(i+1, newDistValue);
    			path.remove(from);
    		}*/
    	}else{
    		doCut = false;
    		
    		/*for(Map.Entry<Tile, Tile> entry : currTo.entrySet()){
    			System.out.println("--------FightAnt------------"+entry.getKey().getRow() + "," + entry.getKey().getCol());
    			System.out.println("--------FightAnt------------"+entry.getValue().getRow() + "," + entry.getValue().getCol());
    		}*/
    		
    		int value = multiMin(0, distValue);
    	
    		//System.out.println("<<<<<<<<BESTPRECVALUE<<<<<<<<<<<"+bestPrecValue);
    		//System.out.println("<<<<<<<<VALUE<<<<<<<<<<<"+value);
    		//System.out.println("<<<<<<<<BESTVALUE<<<<<<<<<<<"+bestPrecValue);
    		if(value > bestPrecValue){
    			bestPrecValue = value;
    		}
    	}
    }
    
	private int multiMin(int i, int distValue) {
		if((System.currentTimeMillis() - startTime > 420)){
    		timeOut = true;
    	}
    	if(timeOut) return bestPrecValue;
    	
    	if(i < enemyFightAnts.size()){
    		Tile enemyAnt = enemyFightAnts.get(i);
    		Tile from = enemyAnt;
    		Tile currTile = from;
    		
    		int bestValue = Integer.MAX_VALUE;
    		
    		for(Tile n : enemyAnt.getNeighbors()){
    			if((n!=null) && (enemyPath.contains(n) || !ants.getIlk(n).isUnoccupied())) continue;
    			
    			currTile = n;
    			enemyPath.add(n);
    			enemyCurrTo.put(enemyAnt, n);
    			
    			int value = multiMin(i+1, distValue);
    			
    			enemyPath.remove(n);
    			
    			if(doCut){
    				//System.out.println("--------PRUNE-------"+doCut);
    				if(currTile != from) enemyCurrTo.remove(enemyAnt);
    				return bestPrecValue;
    			}
    			if(value < bestValue) bestValue = value;
    		}
    		
    		if(currTile != from) enemyCurrTo.remove(enemyAnt);
    		/*if(!enemyPath.contains(from)){
    			enemyPath.add(from);
    			int value = multiMin(i+1, distValue);
    			enemyPath.remove(from);
    			if(doCut) return bestPrecValue;
    			if(value < bestValue) bestValue = value;
    		}*/
    		return bestValue;
    	}else{
    		int result = evaluate(distValue);
    		if(result < bestPrecValue) doCut = true;
    		return result;
    	}
		
	}

	private int evaluate(int distValue) {
		int myDeadCount = 0;
		int enemyDeadCount = 0;
		
		//System.out.println("<<<<<<<<MYANTCURRTO<<<<<<<<<<<"+currTo.size());
		//System.out.println("<<<<<<<<ENEMYANTCURRTO<<<<<<<<<<<"+enemyCurrTo.size());
		
		HashMap<Tile, Boolean> dead = new HashMap<Tile, Boolean>();
		HashMap<Tile, Integer> enemyWeakness = new HashMap<Tile, Integer>();
		HashMap<Tile, Integer> myAntWeakness = new HashMap<Tile, Integer>();
		
		for(Map.Entry<Tile, Tile> myAnt : currTo.entrySet()){
			dead.put(myAnt.getValue(), false);
		}
		
		for(Map.Entry<Tile, Tile> enemyAnt : enemyCurrTo.entrySet()){
			dead.put(enemyAnt.getValue(), false);
			enemyWeakness.put(enemyAnt.getValue(), 0);
			
			for(Map.Entry<Tile, Tile> myAnt : currTo.entrySet()){
				//System.out.println("<<<<<<<<MYANT<<<<<<<<<<<"+myAnt.getValue());
				//System.out.println("<<<<<<<<ENEMYANT<<<<<<<<<<<"+enemyAnt.getValue());
				if((myAnt.getValue() != null) && (enemyAnt.getValue() != null) && (isAlphaDist(enemyAnt.getValue(), myAnt.getValue()))){
					if(!enemyWeakness.containsKey(enemyAnt.getValue())){
						enemyWeakness.put(enemyAnt.getValue(), 1);
					}else{
						enemyWeakness.put(enemyAnt.getValue(), enemyWeakness.get(enemyAnt.getValue())+1);
					}
					if(!myAntWeakness.containsKey(myAnt.getValue())){
						myAntWeakness.put(myAnt.getValue(), 1);
					}else{
						myAntWeakness.put(myAnt.getValue(), myAntWeakness.get(myAnt.getValue())+1);
					}
				}
			}
		}
		
		//System.out.println("<<<<<<<<MYANTWEAKNESS<<<<<<<<<<<"+myAntWeakness.size());
		//System.out.println("<<<<<<<<ENEMYANTWEAKNESS<<<<<<<<"+enemyWeakness.size());
		
		for(Map.Entry<Tile, Tile> myAnt : currTo.entrySet()){
			if(myAntWeakness.containsKey(myAnt.getValue()) && myAntWeakness.get(myAnt.getValue()) != 0){
				for(Map.Entry<Tile, Tile> enemyAnt : enemyCurrTo.entrySet()){
					if((enemyWeakness.containsKey(enemyAnt.getValue())) && (enemyWeakness.get(enemyAnt.getValue()) == 0
							|| !isAlphaDist(myAnt.getValue(), enemyAnt.getValue())))
						continue;
					if((dead.containsKey(enemyAnt.getValue())) && !dead.get(enemyAnt.getValue()) && 
							(enemyWeakness.containsKey(enemyAnt.getValue())) &&
							(enemyWeakness.get(enemyAnt.getValue()) >= myAntWeakness.get(myAnt.getValue())))
					{
						dead.put(enemyAnt.getValue(), true);
						enemyDeadCount++;
					}
					if((dead.containsKey(myAnt.getValue())) && !dead.get(myAnt.getValue()) && 
							(myAntWeakness.containsKey(myAnt.getValue())) &&
							(myAntWeakness.get(myAnt.getValue()) >= enemyWeakness.get(enemyAnt.getValue())))
					{
						dead.put(myAnt.getValue(), true);
						myDeadCount++;
					}
					dead.put(myAnt.getValue(), false);
					myAntWeakness.put(myAnt.getValue(), 0);
				}
			}
		}
		
		//System.out.println("<<<<<<<<ENEMYDEAD<<<<<<<<<<<"+enemyDeadCount);
		//System.out.println("<<<<<<<<MYANTDEAD<<<<<<<<<<<"+myDeadCount);
		//System.out.println("<<<<<<<<DISTANCE<<<<<<<<<<<"+distValue);
		return enemyDeadCount * 300 - myDeadCount * 180 - distValue;
	}

	private boolean isAlphaDist(Tile tile1, Tile tile2) {
		int dx = distRow(tile1, tile2);
		int dy = distCol(tile1, tile2);
		//return (dy <= 1 && dx <= 2) || (dy == 2 && dx <= 1);
		return (dy <= 2 && dx <= 3) || (dy == 3 && dx <= 2);
	}
	
	private int distRow(Tile t1, Tile t2) {
		int dRow = Math.abs(t1.getRow() - t2.getRow());
		return Math.min(dRow, ants.getRows() - dRow);
	}
	
	private int distCol(Tile t1, Tile t2) {
		int dCol = Math.abs(t1.getCol() - t2.getCol());
		return Math.min(dCol, ants.getCols() - dCol);
	}
	
	private void calcDangered(){
		
		for(Tile myAnt : myAnts){
			for(Tile enemyAnt : enemyAnts){
				int dy = distRow(myAnt, enemyAnt);
				if(dy > 9) continue;
				int dx = distCol(myAnt, enemyAnt);
				if(dx > 9) continue;
				int dist = (dx * dx) + (dy * dy);
				if(dist <= 81){
					if((dx + dy <= 5) && !((dx==0 && dy==5) || (dy==0 && dx==5))){
						ArrayList<Tile> enemies = null;
						if(antGammaEnemies.containsKey(myAnt)){
							enemies = antGammaEnemies.get(myAnt);
						}else{
							enemies = new ArrayList<Tile>();
						}
						enemies.add(enemyAnt);
						antGammaEnemies.put(myAnt, enemies);
						
						ArrayList<Tile> ownEnemies = null;
						if(enemyAntGammaEnemies.containsKey(enemyAnt)){
							ownEnemies = enemyAntGammaEnemies.get(enemyAnt);
						}else{
							ownEnemies = new ArrayList<Tile>();
						}
						ownEnemies.add(myAnt);
						enemyAntGammaEnemies.put(enemyAnt, ownEnemies);
						
						if(!dangeredAnts.contains(myAnt) && dx+dy <=4 && 
								!((dx==0 && dy==4) || (dx==4 && dy==0)))
						{
							dangeredAnts.add(myAnt);
						}
					}
				}
			}
		}
		
		//System.out.println("--------DANGER-----------"+dangeredAnts.size());
	}
	
	private void escapeEnemies(){
		for(Tile myAnt : dangeredAnts){
			if(!orderAnts.contains(myAnt)){
				//System.out.println("--------DangeredAnt------------"+myAnt.getRow() + "," + myAnt.getCol());
				escapeAnt(myAnt);
			}
		}
	}
	
	private void escapeAnt(Tile myAnt){
		HashMap<Tile, Tile> prevFirsts = new HashMap<Tile, Tile>();
		final int DIST = 8;
		HashMap<Tile, Integer> values = new HashMap<Tile, Integer>();
		Queue<Tile> queue = new LinkedList<Tile>();
		Queue<Tile> changedTiles = new LinkedList<Tile>();
		boolean visited[][] = new boolean[ants.getRows()][ants.getCols()];
		Tile antTile = mapTiles[myAnt.getRow()][myAnt.getCol()];
		visited[antTile.getRow()][antTile.getCol()] = true;
		antTile.setdist(0);
		changedTiles.add(antTile);
		
		for(Tile neighbor : antTile.getNeighbors()){
			Tile n = mapTiles[neighbor.getRow()][neighbor.getCol()];
			values.put(n, 0);
			queue.add(n);
			n.setdist(1);
			visited[n.getRow()][n.getCol()] = true;
			prevFirsts.put(n, n);
			changedTiles.add(n);
		}
		
		//System.out.println("-----------PrevFirstsValuesBefore-------------"+prevFirsts.size());
		
		while(!queue.isEmpty()){
			Tile tile = queue.remove();
			if(tile.getdist() >= DIST){ 
				break;
			}
			for(Tile n : tile.getNeighbors()){
				if(visited[n.getRow()][n.getCol()]){
					if(n.getdist() == (tile.getdist() + 1)){
						//Tile prevFirst = prevFirsts.get(tile);
						prevFirsts.put(n, prevFirsts.get(tile));
						continue;
					}
				}
				visited[n.getRow()][n.getCol()] = true;
				n.setdist(tile.getdist()+1);
				
				prevFirsts.put(n, prevFirsts.get(tile));
				changedTiles.add(n);
				queue.add(n);
			}
		}
		
		/*Set<Tile> tempFirsts = new HashSet<Tile>();
		for(Map.Entry<Tile, Tile> entry : prevFirsts.entrySet()){
			if(tempFirsts.contains(entry.getValue())){
				continue;
			}
			tempFirsts.add(entry.getValue());
		}
		
		System.out.println("-----------PrevFirstsValuesAfter-------------"+tempFirsts.size());
		*/
		
		for(Tile tile : changedTiles){
			int addValue = DIST + 1 - tile.getdist();
			if(myAnts.contains(tile)) addValue *= 3;
			else if(enemyAnts.contains(tile)) addValue *= -3;
			
			//Tile prevFirst = prevFirsts.get(tile);
			
			if(values.get(prevFirsts.get(tile)) == null){
				//System.out.println(">>>>>>>>>>>>Crashed>>>>>>>>>>>>>>>>>");
				values.put(prevFirsts.get(tile), addValue);
			}else{
				values.put(prevFirsts.get(tile), values.get(prevFirsts.get(tile)) + addValue);
			}
			
			prevFirsts.remove(tile);
		}
		
		int bestValue = Integer.MIN_VALUE;
		Tile bestDest = null;
		for(Map.Entry<Tile, Integer> entry : values.entrySet()){
			if((entry.getKey()!= null) && entry.getValue() > bestValue){
				bestValue = entry.getValue();
				bestDest = entry.getKey();
			}
		}
		
		if(bestValue != 0 && bestDest != null){
			if(takePath(myAnt, bestDest)){
				//System.out.println("--------DangeredAnt------------"+myAnt.getRow() + "," + myAnt.getCol());
			}
		}
		
		for(int i=0; i<ants.getRows(); i++){
			for(int j=0; j<ants.getCols(); j++){
				mapTiles[i][j].setdist(0);
			}
		}
	}
	
	@Override
    public void doTurn() {
        ants = getAnts();
        orders.clear();
        orderAnts.clear();
        myFightAnts.clear();
        enemyFightAnts.clear();
        dangeredAnts.clear();
        antGammaEnemies.clear();
        enemyAntGammaEnemies.clear();
        
        mapTiles = ants.getMapTiles();
		myAnts = ants.getMyAnts();
		enemyAnts = ants.getEnemyAnts();
		myHills = ants.getMyHills();
		//enemyHills = ants.getEnemyHills();
		foodTiles = ants.getFoodTiles();
        
		doCut = false;
		startTime = System.currentTimeMillis();
		timeOut = false;
		bestPrecValue = Integer.MIN_VALUE;
		path.clear();
		currTo.clear();
		enemyPath.clear();
		enemyCurrTo.clear();
		
		// prevent stepping on own hill
        for (Tile myHill : myHills) {
            orders.put(myHill, null);
        }
		
        /** remember seen enemy ants */
        /*for(Tile enemyAnt : ants.getEnemyAnts()){
        	if(!enemyAnts.contains(enemyAnt)){
        		enemyAnts.add(enemyAnt);
        	}
        }*/
        
        /** remember seen enemy hills */
        for(Tile enemyHill : ants.getEnemyHills()){
        	if(!enemyHills.contains(enemyHill) && !enemyHillsRazed.contains(enemyHill)){
        		enemyHills.add(enemyHill);
        	}
        }
        
        
        //Init mapTiles explore values
		incExploreValue();
		
		//Explore Seen Area within 10 steps
        initExplore();
        
        //Calculate dangered Ants
        calcDangered();
        
		//Attack Enemy Hills
      	enemyHillsBFS(enemyHills, myAnts.size() < 10 ? 1 : 2, 15);
		
      	// find close food
        foodBFS(foodTiles, 15);
        
        //Attack Enemy Ants
        findGroups();
        multiMax(0, 0);
        //System.out.println("------------BESTVALUE-------------"+bestPrecValue);
        //attackBFS(myAnts);
        //enemyHillsBFS(enemyAnts, myAnts.size() < 30 ? 2 : 4, 10);
        if(bestPrecValue > 0){
        	//System.out.println("----------BESTVALUE--------"+bestPrecValue);
        	for(Map.Entry<Tile, Tile> entry : currTo.entrySet()){
    			if(takePath(entry.getKey(), entry.getValue())){
    				//System.out.println("--------DangeredAnt------------"+entry.getKey().getRow() + "," + entry.getKey().getCol());
    			}
    		}
        }
        
        //aprroach enemy ants
        approachEnemyBFS(enemyAnts);
        //approachEnemyBFS2();
        
        //Escape dangered ants from enemies
        escapeEnemies();
        
        //Explore unseen area
        for(Tile myAnt : myAnts){
        	if(!orderAnts.contains(myAnt)){
        		exploreBFS(myAnt);
        	}
        }
        
        //Move ants randomly on the screen
        exploreBorderTiles();
        
        //leftBot technique
        //leftyBotExplore();
    }
}
