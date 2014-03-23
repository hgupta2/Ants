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
	
	private boolean takePath(Tile antLoc , Tile newLoc){
		if(enemyHills.contains(newLoc)){
			enemyHillsRazed.add(newLoc);
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
	
    private void foodBFS(Set<Tile> sortedTarget, int distance){
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
                            && !orders.containsValue(neighbor)
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
    					
    					if(myAnts.contains(neighbor) && !orders.containsValue(neighbor) && takePath(neighbor, tile)){
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
    
    private void enemyAntsBFS(Set<Tile> targets){
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
    			
    			if(tile.getdist() > 5){
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
    	
    	System.out.println(targetTiles.size() + ">>>>>>>>>>>>>>>>>");
    	foodBFS(targetTiles, 10);
    }
    
	@Override
    public void doTurn() {
        ants = getAnts();
        mapTiles = ants.getMapTiles();
		orders.clear();
		myAnts = ants.getMyAnts();
		//enemyAnts = ants.getEnemyHills();
		myHills = ants.getMyHills();
		//enemyHills = ants.getEnemyHills();
		foodTiles = ants.getFoodTiles();
        
		// prevent stepping on own hill
        for (Tile myHill : myHills) {
            orders.put(myHill, null);
        }
		
        /** remember seen enemy ants */
        for(Tile enemyAnt : ants.getEnemyAnts()){
        	if(!enemyAnts.contains(enemyAnt)){
        		enemyAnts.add(enemyAnt);
        	}
        }
        
        /** remember seen enemy hills */
        for(Tile enemyHill : ants.getEnemyHills()){
        	if(!enemyHills.contains(enemyHill) && !enemyHillsRazed.contains(enemyHill)){
        		enemyHills.add(enemyHill);
        	}
        }
        
        
        // unblock hills
        /*for (Tile myHill : ants.getMyHills()) {
            if (ants.getMyAnts().contains(myHill) && !orders.containsValue(myHill)) {
                //for (Aim direction : Aim.values()) {
                    if (takeRandomPath(ants)) {
                        break;
                    }
                //}
            }
        }*/
		
		//Init mapTiles explore values
		incExploreValue();
		
		//Explore Seen Area within 10 steps
        initExplore();
        
		//Attack Enemy Hills
      	enemyHillsBFS(enemyHills, myAnts.size() < 10 ? 1 : 2, 20);
		
      	// find close food
        foodBFS(foodTiles, 15);
        
        //attack enemy ants
        enemyAntsBFS(enemyAnts);
        
        //Explore unseen area
        for(Tile myAnt : myAnts){
        	if(!orders.containsValue(myAnt)){
        		exploreBFS(myAnt);
        	}
        }
        
        //Move ants randomly on the screen
        exploreBorderTiles();
        
        //leftBot technique
        //leftyBotExplore();
    }
}
