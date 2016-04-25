package edu.jhu.clueless;

import java.awt.*;
import java.util.*;
import java.util.List;

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
		List<String> availableGames = new ArrayList<>();
		for (Game game : reg.getGames()) {
			if (!game.isActive() && game.getWinner() == null && game.getPlayers().size() < 6) {
				availableGames.add(game.getId().toString());
			}
		}
		return availableGames.toArray(new String[availableGames.size()]);
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
		Game game = new Game(id, "New game");
		this.reg.add(game);
		return game.getId();
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
		// TODO: handle case that registry returns null

		try {
			String charNorm = normalizeConstant(normalizeConstant(character));
			reg.get(gameId).addPlayer(Constants.Suspect.valueOf(charNorm));
		} catch (CluelessException e) {
			// TODO: handle exception
		}

		// TODO: should this player ID be the same as the one in the player object?
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
		// TODO: handle case that registry returns null
		Game game = reg.get(client.getGameId());
		Player player = game.getPlayer(client.getPlayerId());
		Point destination = new Point(client.getLocationX(), client.getLocationY());

		try {
			game.move(player, destination);
		} catch (CluelessException e) {
			// TODO: handle exception
		}

		sendGameMessageAllPlayers(client.getGameId(), "Action Move");
	}
	
	/**
	 * Processes Game Start requests
	 * @param client
	 */
	@MessageMapping("start")
	public void gameStart(ClientAction client){
		try {
			// TODO: handle case that registry returns null
			reg.get(client.getGameId()).start();
		} catch (CluelessException e) {
			// TODO: handle exception
		}
		sendGameMessageAllPlayers(client.getGameId(), "Game has started");
	}
	
	/**
	 * Processes Game Suggestions
	 * @param client
	 */
	@MessageMapping("suggest")
	public void gameSuggest(ClientAction client){
		// TODO: handle case that registry returns null
		Game game = reg.get(client.getGameId());
		Player player = game.getPlayer(client.getPlayerId());
		try {
			Set<Card> suggestion = buildProposal(client.getCards());
			game.suggest(player, suggestion);
		} catch (CluelessException e) {
			// TODO: Handle exception
		}

		sendMessageToUser(client.getPlayerId(), client);
	}
	
	/**
	 * Processes Game Responses to Suggestions
	 * @param client
	 */
	@MessageMapping("respondsuggest")
	public void gameRespondSuggest(ClientAction client){
		// TODO: handle case that registry returns null
		Game game = reg.get(client.getGameId());
		Player player = game.getPlayer(client.getPlayerId());

		Set<String> cards = client.getCards();
		String responseName = null;
		if (cards.size() != 1) {
			// TODO: handle invalid input
		} else {
			responseName = cards.iterator().next();
		}

		try {
			Card response = parseCard(normalizeConstant(responseName));
			game.respond(player, response);
		} catch (CluelessException e) {
			// TODO: Handle exception
		}

		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	/**
	 * Processes Game Accusations
	 * @param client
	 */
	@MessageMapping("accuse")
	public void gameAccuse(ClientAction client){
		// TODO: handle case that registry returns null
		Game game = reg.get(client.getGameId());
		Player player = game.getPlayer(client.getPlayerId());
		try {
			Set<Card> accusation = buildProposal(client.getCards());
			game.accuse(player, accusation);
		} catch (CluelessException e) {
			// TODO: Handle exception
		}

		// TODO: Handle end of game

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

	private static String normalizeConstant(String s) {
		return s.trim().toUpperCase();
	}

	private static boolean isOfCardType(Constants.EntityType type, String cardName) {
		if (type == Constants.EntityType.ROOM) {
			for (Constants.Room room : Constants.Room.values()) {
				if (cardName.equals(room.toString())) {
					return true;
				}
			}
			return false;
		}

		else if (type == Constants.EntityType.WEAPON) {
			for (Constants.Weapon weapon : Constants.Weapon.values()) {
				if (cardName.equals(weapon.toString())) {
					return true;
				}
			}
			return false;
		}

		else {
			for (Constants.Suspect suspect : Constants.Suspect.values()) {
				if (cardName.equals(suspect.toString())) {
					return true;
				}
			}
			return false;
		}
	}

	private static Card parseCard(String cardName) throws CluelessException {
		Card card;
		if (isOfCardType(Constants.EntityType.ROOM, cardName)) {
			card = Constants.Room.valueOf(cardName);
		} else if (isOfCardType(Constants.EntityType.WEAPON, cardName)) {
			card = Constants.Weapon.valueOf(cardName);
		} else if (isOfCardType(Constants.EntityType.SUSPECT, cardName)) {
			card = Constants.Suspect.valueOf(cardName);
		} else {
			throw new CluelessException(String.format("Unable to parse proposal=%s into Clue card", cardName));
		}
		return card;
	}

	private static Set<Card> buildProposal(Collection<String> input) throws CluelessException {
		Set<Card> proposal = new HashSet<>();
		for (String cardName : input) {
			String cardNorm = normalizeConstant(cardName);
			proposal.add(parseCard(cardNorm));
		}
		return proposal;
	}

}
