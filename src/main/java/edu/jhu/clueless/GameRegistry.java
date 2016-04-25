package edu.jhu.clueless;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRegistry implements Registry<Game> {
	
	private static GameRegistry _myInstance = null;
	private Map<String, Game> _games = new ConcurrentHashMap<>();
	
	private GameRegistry(){
		// empty
	}
	
	@Override
	public Game get(String id) {
		return _games.get(id);
	}

	@Override
	public void add(Game game) {
		_games.put(game.getId(), game);
	}

	@Override
	public Game remove(String id) {
		return _games.remove(id);
	}

	@Override
	public int getCount() {
		return _games.size();
	}

	public Collection<Game> getGames() {
		return _games.values();
	}

	
	public static GameRegistry getInstance() {
		if( null == GameRegistry._myInstance){
			GameRegistry._myInstance = new GameRegistry();
		}
			
		return GameRegistry._myInstance;
	}

}
