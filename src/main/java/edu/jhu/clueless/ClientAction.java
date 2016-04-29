package edu.jhu.clueless;

import java.util.HashSet;
import java.util.Set;


/**
 * POJO class to map client requests to application
 * requests
 * 
 * @author Chris
 * @date Apr 7, 2016
 */
public class ClientAction {

	public static final String ACTION_SUGGEST = "suggest";
	public static final String ACTION_RESPOND = "respondsuggest";
	public static final String ACTION_MOVE = "move";
	public static final String ACTION_ACCUSE = "accuse";
	public static final String ACTION_CHAT = "chat";
	public static final String HAND_UPDATE = "set_hand";

	private String _mySuspect;
	private String _myAction;
	private String _myPlayerId;
	private String _myGameId;
	// used for chat, errors, etc.
	private String message;
	private int location_x = 0;
	private int location_y = 0;
	// holds card names
	private Set<String> cards = new HashSet<String>();
	
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
	
	public Set<String> getCards() {
		return cards;
	}

	public void setCards(Set<String> cards) {
		this.cards = cards;
	}

	public void addCard(String card){
		this.cards.add(card);
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getLocationX() {
		return location_x;
	}

	public void setLocationX(int locationX) {
		this.location_x = locationX;
	}

	public int getLocationY() {
		return location_y;
	}

	public void setLocationY(int locationY) {
		this.location_y = locationY;
	}

	public void setSuspect(String suspect) {
		this._mySuspect = suspect;
	}

	public String getSuspect() {
		return _mySuspect;
	}
}
