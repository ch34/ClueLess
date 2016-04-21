package edu.jhu.clueless;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The client will subscribe to minimum of three channels.
 * 1) user channel where messages are sent only to them
 * 2) game channel where messages are sent only to players in that game
 * 3) (Optional) error channel where any exceptions / errors will be sent
 *  
 * @author Chris
 * @date Feb 18, 2016
 */
@Controller
@RequestMapping("/")
public class GameController {
	/* 
	 * Static vars session data. Maybe put these into 
	 * the Constants class 
	 */
	public static final String SESSION_CLIENT_ID = "clientId";
	public static final String SESSION_GAME_ID = "gameId";
	public static final String SESSION_VERSION_ID = "version";
	
	/* 
	 * Channels 
	 */
	public static final String CHANNEL_CHAT = "/queue/chat";
	/* User subscribes to /queue/game-{gameId} */
	public static final String CHANNEL_GAME = "/queue/game-";
	/* User subscribes to /queue/user-{userId} */
	public static final String CHANNEL_USER = "/queue/user-";
	
	/* Game Version */
	private static final String version_ = "0.1.0";
	
	/* Registry */
	private GameRegistry reg = GameRegistry.getInstance();

	
	/*
	 * Helps with sending messages to specifc queues, users, etc
	 */
	@Autowired
	private SimpMessagingTemplate msgTemplate;
	
	
//### Methods Called Via Http / Ajax calls ###//
	/**
	 * Returns the Game page
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = "home", method = RequestMethod.GET)
	public String getGamePage(HttpServletRequest request){
		// Generate the players id add add it to their session
		// This is needed for tracking users connected/disconnected
		String playerId = Calendar.getInstance().getTimeInMillis() + "";
		request.getSession().setAttribute(SESSION_CLIENT_ID, playerId);
		request.getSession().setAttribute(SESSION_VERSION_ID, version_);
		
		return "index";
		
	}
	
	/* Note: Not currently being used by the client
	 * (non-JavaDoc)
	 * URL will look like 
	 * http[s]://server[:port]/{app}/home/getGame?id={someId}
	 */
	@RequestMapping(value = "home/getGame")
	@ResponseBody
	public String getGame(@RequestParam(name="id") String id){
		// TODO
		return "TODO";
	}
	
	/**
	 * Returns an array of available games that have not started
	 * and are not full
	 * 
	 * @return Array of available Game ID's
	 */
	@RequestMapping(value = "home/getGames")
	@ResponseBody
	public String[] getAvailableGames(){
		// Should only return games that are not full and not started
		String[] myStrings = {"game1","game2"};
		// TODO
		return myStrings;
	}
	
	/**
	 * Create a new game
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "home/createGame") 
	@ResponseBody
	public String createGame(@RequestParam(name="id") String id){
		// TODO - return the games ID
		// We do not join the player here. They make a separate request
		// to join to the game as they will need to select a character first
		
		return "TODO";
	}
	
	/**
	 * Adds a player to an existing game
	 * 
	 * @param gameId Id of game player wants to join
	 * @param character Suspect player is controlling
	 * @param request HttpServletRequest for setting session data
	 * @return
	 */
	@RequestMapping("home/joinGame") 
	@ResponseBody
	public String joinGame(@RequestParam(name="id") String gameId,
			@RequestParam(name="suspect") String character,
			HttpServletRequest request){
		// TODO - join the game
		
		String playerId = (String)request.getSession()
				.getAttribute(SESSION_CLIENT_ID);
		String message = "User " + playerId + " has joined the session.";
		sendGameMessageAllPlayers(GameController.CHANNEL_GAME + gameId, message);
		
		// lastly - add the gameid to the players session only
		// if joining game succeeded
		request.getSession().setAttribute("gameId", gameId);
		return "success";
	}

//### Methods Called Via WebSocket ###//	

	/**
	 * Processes Player Movement requests
	 * @param client
	 */
	@MessageMapping("move") 
	public void gameMove(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), "Action Move");
	}
	
	/**
	 * Processes Game Start requests
	 * @param client
	 */
	@MessageMapping("start")
	public void gameStart(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), "Game has started");
	}
	
	/**
	 * Processes Game Suggestions
	 * @param client
	 */
	@MessageMapping("suggest")
	public void gameSuggest(ClientAction client){
		// TODO game logic
		// finally change playerId to the suggest users playerid
		sendMessageToUser(client.getPlayerId(), client);
	}
	
	/**
	 * Processes Game Responses to Suggestions
	 * @param client
	 */
	@MessageMapping("respondsuggest")
	public void gameRespondSuggest(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	/**
	 * Processes Game Accusations
	 * @param client
	 */
	@MessageMapping("accuse")
	public void gameAccuse(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	/**
	 * Delivers a game chat message to all players of the game
	 * @param client
	 */
	@MessageMapping("chat")
	public void gameChat(ClientAction client){
		// only send to clients of the connected game
		// just reiterate the message while updating who sent it
		String msg = client.getMessage();
		client.setMessage("Player " + client.getPlayerId() + ": " + msg);
		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	/**
	 * Sends a message to all connected players with the specified gameId
	 * 
	 * @param gameId - game in which to notify players
	 * @param payload - object to send to players
	 */
	public void sendGameMessageAllPlayers(String gameId, Object payload){
		msgTemplate.convertAndSend(GameController.CHANNEL_GAME + gameId, payload);
	}
	
	/**
	 * Sends a message to the user with the specified userId
	 * 
	 * @param userId - user to send message to
	 * @param payload - object to send to players
	 */
	public void sendMessageToUser(String userId, Object payload){
		msgTemplate.convertAndSend(GameController.CHANNEL_USER + userId, payload);
	}
}
