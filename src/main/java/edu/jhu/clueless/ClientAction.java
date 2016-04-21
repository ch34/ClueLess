package edu.jhu.clueless;


/**
 * POJO class to map client requests to application
 * requests
 * 
 * @author Chris
 * @date Apr 7, 2016
 */
public class ClientAction {
	private String _myAction;
	private String _myPlayerId;
	private String _myGameId;
	
	public ClientAction(){
		// empty This is necessary for Jackson
	}

	public String getGameId() {
		return _myGameId;
	}

	public void setGameId(String gameId) {
		this._myGameId = gameId;
	}

	public String getAction() {
		return _myAction;
	}

	public void setAction(String action) {
		this._myAction = action;
	}

	public String getPlayerId() {
		return _myPlayerId;
	}

	public void setPlayerId(String playerId) {
		this._myPlayerId = playerId;
	}
	
}
