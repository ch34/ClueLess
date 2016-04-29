package edu.jhu.clueless;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	private static final Logger logger = Logger.getLogger(GameController.class);
	
	/* 
	 * Channels 
	 */
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
		String playerId = UUID.randomUUID().toString();
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
				availableGames.add(game.getId());
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
	public ResponseEntity<String> createGame(@RequestParam(name="id", required = false) String id){
		if (id != null && reg.get(id) != null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					String.format("Game with ID=%s already exists", id));
		}
		Game game = id == null ? new Game("New game") : new Game(id, "New game");
		this.reg.add(game);
		return ResponseEntity.ok(game.getId());
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
	public ResponseEntity<String> joinGame(@RequestParam(name="id") String gameId,
										   @RequestParam(name="suspect") String character,
										   HttpServletRequest request){

		String playerId = (String) request.getSession().getAttribute(SESSION_CLIENT_ID);

		logger.debug(String.format("PlayerId=%s requesting to join gameId=%s with character=%s",
				playerId, gameId, character));

		Game game = reg.get(gameId);
		if (game == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid game ID");
		}

		Player addedPlayer;
		try {
			String charNorm = normalizeConstant(character);
			addedPlayer = game.addPlayer(playerId, Constants.Suspect.valueOf(charNorm));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unknown character=" + character);
		} catch (CluelessException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}

		String message = getDisplayCharFromPlayer(game, playerId) + " has joined the session.";
		sendGameMessageAllPlayers(gameId, message);
		
		// lastly - add the gameid to the players session only
		// if joining game succeeded
		request.getSession().setAttribute("gameId", gameId);

		logger.debug(String.format("Added player=%s to game=%s", addedPlayer, gameId));

		return ResponseEntity.ok(gameId);
	}

	private String getDisplayCharFromPlayer(Game game, String playerId) {
		return game.getPlayer(playerId).getSuspect().toString().toLowerCase();
	}

//### Methods Called Via WebSocket ###//	

	/**
	 * Processes Player Movement requests
	 * @param client
	 */
	@MessageMapping("move") 
	public void gameMove(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		Player player = game.getPlayer(playerId);
		Point destination = new Point(client.getLocationX(), client.getLocationY());

		try {
			game.move(player, destination);
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		client.setSuspect(player.getSuspect().toString());
		sendGameMessageAllPlayers(gameId, client);
	}
	
	/**
	 * Processes Game Start requests
	 * @param client
	 */
	@MessageMapping("start")
	public void gameStart(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		try {
			game.start();
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		sendGameMessageAllPlayers(gameId, "Game has started");
		String playerTurn = getDisplayCharFromPlayer(game, game.getPlayerTurn().getID());
		sendGameMessageAllPlayers(gameId, "It is " + playerTurn + "'s turn");

		// Notify all users of their hand
		for (Player player : game.getPlayers()) {
			String thisPlayerId = player.getID();

			ClientAction update = new ClientAction();
			update.setCards(player.getCardNames());
			update.setAction(ClientAction.HAND_UPDATE);
			update.setPlayerId(thisPlayerId);

			sendMessageToUser(thisPlayerId, update);
		}
	}
	
	/**
	 * Processes Game Suggestions
	 * @param client
	 */
	@MessageMapping("suggest")
	public void gameSuggest(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		Player player = game.getPlayer(client.getPlayerId());
		try {
			Set<Card> suggestion = buildProposal(client.getCards());
			game.suggest(player, suggestion);
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		sendMessageToUser(client.getPlayerId(), client);
	}

	/**
	 * Processes Game Responses to Suggestions
	 * @param client
	 */
	@MessageMapping("respondsuggest")
	public void gameRespondSuggest(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		Player player = game.getPlayer(client.getPlayerId());

		Set<String> cards = client.getCards();
		String responseName;
		if (cards.size() != 1) {
			sendMessageToUser(playerId, "Invalid response: must include only a single card");
			return;
		} else {
			responseName = cards.iterator().next();
		}

		try {
			Card response = parseCard(normalizeConstant(responseName));
			game.respond(player, response);
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		sendGameMessageAllPlayers(client.getGameId(), client);
	}
	
	/**
	 * Processes Game Accusations
	 * @param client
	 */
	@MessageMapping("accuse")
	public void gameAccuse(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		Player player = game.getPlayer(client.getPlayerId());
		boolean gameWon;
		try {
			Set<Card> accusation = buildProposal(client.getCards());
			gameWon = game.accuse(player, accusation);
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		if (gameWon) {
			sendGameMessageAllPlayers(client.getGameId(), client);
			reg.remove(gameId);
		} else {
			String playerChar = getDisplayCharFromPlayer(game, playerId);
			sendGameMessageAllPlayers(client.getGameId(), playerChar + " has made an incorrect accusation and has lost the game.");
			if (!game.isActive()) {
				sendGameMessageAllPlayers(client.getGameId(), "No players left in the game. Game over");
				reg.remove(gameId);
			}
 		}
	}

	/**
	 * Processes Game Suggestions
	 * @param client
	 */
	@MessageMapping("end_turn")
	public void endTurn(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		Player player = game.getPlayer(client.getPlayerId());
		try {
			game.endTurn(player);
		} catch (CluelessException e) {
			sendMessageToUser(playerId, e.getMessage());
			return;
		}

		String playerTurn = getDisplayCharFromPlayer(game, game.getPlayerTurn().getID());
		sendGameMessageAllPlayers(gameId, "It is " + playerTurn + "'s turn");
	}
	
	/**
	 * Delivers a game chat message to all players of the game
	 * @param client
	 */
	@MessageMapping("chat")
	public void gameChat(ClientAction client){
		String playerId = client.getPlayerId();
		String gameId = client.getGameId();

		logger.debug(String.format("PlayerId=%s requesting to chat in gameId=%s", playerId, gameId));

		Game game = reg.get(gameId);
		if (game == null) {
			sendMessageToUser(playerId, "Invalid game ID");
			return;
		}

		// only send to clients of the connected game
		// just reiterate the message while updating who sent it
		String msg = client.getMessage();
		client.setMessage(getDisplayCharFromPlayer(game, playerId) + ": " + msg);
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
			throw new CluelessException(String.format("Unable to parse card=%s into Clue card", cardName));
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
