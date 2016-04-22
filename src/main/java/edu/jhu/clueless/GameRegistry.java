package edu.jhu.clueless;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO Change Registry<GameRegistry> to Registry<Game>
public class GameRegistry implements Registry<GameRegistry> {
	
	private static GameRegistry _myInstance = null;
	private Map<String, String> _games = 
			new ConcurrentHashMap<String, String>();
	
	private GameRegistry(){
		// empty
	}
	
	@Override
	public GameRegistry get(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameRegistry create(GameRegistry obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GameRegistry remove(GameRegistry obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCount() {
		return _games.size();
	}

	
	public static GameRegistry getInstance() {
		if( null == GameRegistry._myInstance){
			GameRegistry._myInstance = new GameRegistry();
		}
			
		return GameRegistry._myInstance;
	}

}
