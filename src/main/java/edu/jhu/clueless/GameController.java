package edu.jhu.clueless;

import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The client will subscribe to minimum of three channels.
 * 1) user channel where messages are sent only to them
 * 2) chat channel where messages can be sent to anyone
 * 3) game channel where messages are sent only to players in that game
 * 4) (Optional) error channel where any exceptions / errors will be sent
 * 
 * TODO - remove all @SendTo except for @SendTo("/queue/chat")
 * This can not be done until gameId has been implemented
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
	@RequestMapping(value = "home", method = RequestMethod.GET)
	public String getGamePage(HttpServletRequest request){
		// Generate the players id add add it to their session
		// This is needed for tracking users connected/disconnected
		String playerId = Calendar.getInstance().getTimeInMillis() + "";
		request.getSession().setAttribute(SESSION_CLIENT_ID, playerId);
		request.getSession().setAttribute(SESSION_VERSION_ID, version_);
		
		return "index";
		
	}
	
	/* (non-JavaDoc)
	 * URL will look like 
	 * http[s]://server[:port]/{app}/home/getGame?id={someId}
	 */
	@RequestMapping(value = "home/getGame")
	@ResponseBody
	public String getGame(@RequestParam(name="id") String id){
		// TODO
		return "TODO";
	}
	
	@RequestMapping(value = "home/getGames")
	@ResponseBody
	public String[] getAvailableGames(){
		// Should only return games that are not full and not started
		String[] myStrings = {"TODO1","TODO2"};
		// TODO
		return myStrings;
	}
	
	@RequestMapping(value = "createGame") 
	@ResponseBody
	public String createGame(@RequestParam(name="id") String id){
		// TODO
		return "TODO";
	}
	

//### Methods Called Via WebSocket ###//
	@MessageMapping("joinGame") // maps to /app/createGame
	public void joinGame(ClientAction client){
		// TODO
		
		String message = "User " + client.getPlayerId() + 
				" has joined the session.";
		sendGameMessageAllPlayers(client.getGameId(), message);
	}
	
	// If returning an object it then sends response
	// to /topic by default.
	// Below we override the default /topic and instead
	// send it to /queue/game so every client gets the response
	// Remember this should be removed so we only send to
	// users of the specified game
	@MessageMapping("move") 
	public void gameMove(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), "Action Move");
	}
	
	@MessageMapping("start")
	public void gameStart(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), "Game has started");
	}
	
	@MessageMapping("suggest")
	@SendTo("/queue/game")
	public void gameSuggest(ClientAction client){
		// TODO game logic
		// finally change playerId to the suggest users playerid
		sendMessageToUser(client.getPlayerId(), client);
	}
	
	@MessageMapping("respond-suggest")
	@SendTo("/queue/game")
	public void gameRespondSuggest(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	@MessageMapping("accuse")
	public void gameAccuse(ClientAction client){
		// TODO
		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	// This demonstrates how to send to a specific user
	@MessageMapping("testUser")
	public void testMessage(ClientAction client){
		// users all subscribe to /queue/user-### for their messages
		String user = GameController.CHANNEL_USER + client.getPlayerId();
		sendMessageToUser(user, "Hello to you only!");
	}
	
	@MessageMapping("chat")
	public void gameChat(ClientAction client){
		// only send to clients of the connected game
		// just reiterate the message
		// TODO add a field for chat to the DTO
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
