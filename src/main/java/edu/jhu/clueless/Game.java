package edu.jhu.clueless;

import java.util.UUID;

import edu.jhu.clueless.Constants.EntityType;
import edu.jhu.clueless.Constants.Room;
import edu.jhu.clueless.Constants.PlayerAction;
import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.board.GameBoard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Game {

	private static Suspect[] SUSPECT_ORDER = { Suspect.MISS_SCARLET, Suspect.COLONEL_MUSTARD, Suspect.MRS_WHITE,
			Suspect.MR_GREEN, Suspect.MRS_PEACOCK, Suspect.PROFESSOR_PLUM };

	private String id;
	private String name;
	private GameBoard board;
	private Player winner;
	protected ConcurrentMap<Suspect, Player> players;
	protected int indexPlayerTurn;
	private int indexPlayerResponding;

	/**
	 * Whether the game is currently in an active state (started and still running)
	 */
	private AtomicBoolean active;

	/**
	 * Suggestion that is currently in progress, if any. This is persisted throughout the response cycle.
	 */
	private Collection<Card> currentSuggestion;

	/**
	 * Case file that contains the suspect, room, and weapon answer for this game
	 */
	private Map<EntityType, Card> caseFile;

	/**
	 * Actions (i.e. move, suggest, or accuse), that each player is currently allowed to take
	 */
	private Map<String, Set<PlayerAction>> allowedActions;

	public Game() {
		this("New Game");
	}

	public Game(String name) {
		this(UUID.randomUUID().toString(), name);
	}

	public Game(String id, String name) {
		this.id = id;
		this.name = name;
		board = new GameBoard();
		caseFile = new HashMap<>();
		players = new ConcurrentHashMap<>();
		indexPlayerTurn = -1;
		active = new AtomicBoolean();
		currentSuggestion = null;
		winner = null;
		allowedActions = new HashMap<>();
	}

	/**
	 * @throws CluelessException if there are not sufficient players to start the game
	 */
	public void start() throws CluelessException {
		if (players.size() < 3) {
			throw new CluelessException("Game cannot be started without at least 3 players");
		}
		// Only initialize game if it was not already active
		if (active.compareAndSet(false, true)) {
			distributeCards();
			indexPlayerTurn = rotatePlayerIndex(indexPlayerTurn, true);
			initializeAllowedActions();
		}
	}

	private void initializeAllowedActions() {
		for (Player player : players.values()) {
			allowedActions.put(player.getID(), new HashSet<>(Arrays.asList(PlayerAction.MOVE, PlayerAction.ACCUSE)));
		}
	}

	private void distributeCards() {
		// Get full set of cards and shuffle them
		List<Card> cards = new ArrayList<>(Card.getFullSet());
		Collections.shuffle(cards);

		// Set up case file with random suspect, room, and weapon
		for (EntityType type : EntityType.values()) {
			for (int i = 0; i < cards.size(); i++) {
				Card card = cards.get(i);
				if (card.getType() == type) {
					caseFile.put(type, card);
					cards.remove(i);
					break;
				}
			}
		}

		// Distribute remaining cards to players
		int indexPlayer = rotatePlayerIndex(-1, true);
		for (Iterator<Card> iterator = cards.iterator(); iterator.hasNext();) {
			Card card = iterator.next();
			Player player = players.get(SUSPECT_ORDER[indexPlayer]);
			player.addCard(card);
			iterator.remove();
			indexPlayer = rotatePlayerIndex(indexPlayer, true);
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Player> getPlayers() {
		return Collections.unmodifiableCollection(players.values());
	}

	public Map<EntityType, Card> getCaseFile() {
		return Collections.unmodifiableMap(caseFile);
	}

	public boolean isActive() {
		return active.get();
	}

	/**
	 * @return Winner of game, or null if no winner yet
	 */
	public Player getWinner() {
		return winner;
	}

	/**
	 * @return Player whose turn it is, or null if game is not active.
	 */
	public Player getPlayerTurn() {
		if (!active.get()) {
			return null;
		}
		return players.get(SUSPECT_ORDER[indexPlayerTurn]);
	}

	/**
	 * Add new player to the game, if the given suspect is available and game is not active.
	 * @param suspect Suspect to associate with the new player.
	 * @return Id of the new player, or null if the player could not be added.
	 */
	public String addPlayer(Suspect suspect) throws CluelessException {
		if (active.get()) {
			throw new CluelessException("Game already started");
		}

		Player newPlayer = new Player(suspect);
		Player existingPlayer = players.putIfAbsent(suspect, newPlayer);

		if (existingPlayer != null) {
			throw new CluelessException("The requested suspect is already taken");
		}

		return newPlayer.getID();
	}

	/**
	 * @param id Id of player to retrieve
	 * @return The player associated with the given ID, or null if no player is associated with the given ID.
	 */
	public Player getPlayer(String id) {
		for (Player player : players.values()) {
			if (player.getID().equals(id)) {
				return player;
			}
		}
		return null;
	}

	private boolean isPlayerTurn(Player player) {
		return player.getSuspect().equals(SUSPECT_ORDER[indexPlayerTurn]);
	}

	protected int rotatePlayerIndex(int currIndex, boolean mustBeActive) {
		int newIndex = incrementCounterModPlayers(currIndex);
		Player nextPlayer = players.get(SUSPECT_ORDER[newIndex]);
		while (nextPlayer == null || (mustBeActive && !nextPlayer.isActive())) {
			newIndex = incrementCounterModPlayers(newIndex);
			nextPlayer = players.get(SUSPECT_ORDER[newIndex]);
		}
		return newIndex;
	}

	private int incrementCounterModPlayers(int counter) {
		return (counter + 1) % SUSPECT_ORDER.length;
	}

	/**
	 * Move player to given destination
	 * @param player Player to move
	 * @param point Destination to move player to
	 * @throws CluelessException if the move request was invalid and game state was not modified.
	 */
	public void move(Player player, Point point) throws CluelessException {
		Set<PlayerAction> allowed = allowedActions.get(player.getID());

		// Validate request
		validateActionCommon(player, "move");
		if (!allowed.contains(PlayerAction.MOVE)) {
			throw new CluelessException(String.format("Not allowed to move. Valid actions=%s", allowed));
		}

		board.move(player.getSuspect(), point);
		allowed.remove(PlayerAction.MOVE);

		if (board.getRoom(point) != null) {
			allowed.add(PlayerAction.SUGGEST);
		}
	};

	/**
	 * @param player Player making suggestion
	 * @param suggestion Player's suggestion
	 * @throws CluelessException if the request was invalid and game state was not modified.
	 */
	public void suggest(Player player, Collection<Card> suggestion) throws CluelessException {
		Point playerLocation = board.getLocation(player.getSuspect());
		Room playerRoom = board.getRoom(playerLocation);
		Card suggestionRoom = getTypeFromProposal(suggestion, EntityType.ROOM);
		Set<PlayerAction> allowed = allowedActions.get(player.getID());

		// Validate request
		validateActionCommon(player, "suggest");
		if (!allowed.contains(PlayerAction.SUGGEST)) {
			throw new CluelessException(String.format("Not allowed to suggest. Valid actions=%s", allowed));
		}
		if (!isValidProposal(suggestion))  {
			throw new CluelessException(String.format("Invalid suggestion=%s. Must include exactly one suspect, weapon, "
					+ "and room", suggestion));
		}
		if (playerRoom != suggestionRoom) {
			throw new CluelessException(String.format("Player is in room=%s, but made a suggestion about room=%s",
					playerRoom, suggestionRoom));
		}

		// Move suggestion's suspect to suggestion's room, if necessary
		Suspect suggestedSuspect = (Suspect) getTypeFromProposal(suggestion, EntityType.SUSPECT);
		Point locationSuggestedSuspect = board.getLocation(suggestedSuspect);
		if (!locationSuggestedSuspect.equals(playerLocation)) {
			board.move(suggestedSuspect, playerLocation, true);
			Player suggestedPlayer = players.get(suggestedSuspect);
			if (suggestedPlayer != null) {
				Set<PlayerAction> allowedSuggestedPlayer = allowedActions.get(suggestedPlayer.getID());
				allowedSuggestedPlayer.add(PlayerAction.SUGGEST);
			}
		}

		allowed.remove(PlayerAction.SUGGEST);

		// Save this player's suggestion for comparison against responses
		currentSuggestion = suggestion;

		// Initialize rotation for players' responses
		indexPlayerResponding = rotatePlayerIndex(indexPlayerTurn, false);
	}

	/**
	 * @param player Player responding
	 * @param response Player's response, or null if player cannot disprove suggestion.
	 * @return True if there are more players to respond to suggestion, false if this was the last one.
	 * @throws CluelessException if the request was invalid and game state was not modified.
	 */
	public boolean respond(Player player, Card response) throws CluelessException {
		// Validate request
		if (currentSuggestion == null) {
			throw new CluelessException("Player responded to a suggestion, but no suggestion in progress");
		}
		if (!player.getSuspect().equals(SUSPECT_ORDER[indexPlayerResponding])) {
			throw new CluelessException("Player responded to suggestion out of turn");
		}
		if (response == null && player.hasAnyCard(currentSuggestion)) {
			throw new CluelessException(String.format("Player is able to disprove suggestion=%s", currentSuggestion));
		}
		if (response != null) {
			if (!currentSuggestion.contains(response)) {
				throw new CluelessException(String.format("Response card=%s was not included in original suggestion=%s",
						response, currentSuggestion));
			}
			if (!player.hasCard(response)) {
				throw new CluelessException(String.format("Player responded card=%s not in their hand", response));
			}
		}

		indexPlayerResponding = rotatePlayerIndex(indexPlayerResponding, false);

		// If we've wrapped back around to the original suggester, the response cycle is over, and the main turn
		// cycle continues
		if (indexPlayerResponding == indexPlayerTurn) {
			currentSuggestion = null;
			return false;
		}
		return true;
	}

	private void validateActionCommon(Player player, String action) throws CluelessException {
		if (!active.get() || winner != null) {
			throw new CluelessException("Game not started or has already ended");
		}
		if (!isPlayerTurn(player)) {
			throw new CluelessException("Player requested to " + action + " out of turn");
		}
		if (currentSuggestion != null) {
			throw new CluelessException("Player requested to " + action + ", but suggestion is in progress");
		}
	}

	/**
	 * @param player Player making accusation
	 * @param accusation Player's accusation
	 * @return True if the accusation was correct and the player has won the game, false otherwise.
	 * @throws CluelessException if the request was invalid and game state was not modified.
	 */
	public boolean accuse(Player player, Collection<Card> accusation) throws CluelessException {
		Set<PlayerAction> allowed = allowedActions.get(player.getID());

		// Validate request
		validateActionCommon(player, "accuse");
		if (!allowed.contains(PlayerAction.ACCUSE)) {
			throw new CluelessException(String.format("Not allowed to accuse. Valid actions=%s", allowed));
		}
		if (!isValidProposal(accusation)) {
			throw new CluelessException(String.format("Invalid accusation=%s. Must include exactly one suspect, one " +
					"weapon, and  one room", accusation));
		}

		allowed.remove(PlayerAction.ACCUSE);
		Player gameWinner = null;
		if (getTypeFromProposal(accusation, EntityType.SUSPECT) == caseFile.get(EntityType.SUSPECT) &&
			getTypeFromProposal(accusation, EntityType.WEAPON) == caseFile.get(EntityType.WEAPON) &&
			getTypeFromProposal(accusation, EntityType.ROOM) == caseFile.get(EntityType.ROOM)) {
			gameWinner = player;
		} else {
			// If player is in a hallway, move them into the billiard room so they won't block anyone
			Suspect suspect = player.getSuspect();
			Point playerLocation = board.getLocation(player.getSuspect());
			if (board.getRoom(playerLocation) == null) {
				board.move(suspect, board.getLocation(Room.BILLIARD_ROOM), true);
			}
		}

		player.setActive(false);
		endTurn(player);

		if (gameWinner != null) {
			active.compareAndSet(true, false);
			winner = gameWinner;
		}

		return winner != null;
	}

	public void endTurn(Player player) throws CluelessException {
		validateActionCommon(player, "end turn");

		if (player.isActive()) {
			Set<PlayerAction> allowed = allowedActions.get(player.getID());
			allowed.add(PlayerAction.MOVE);
			allowed.add(PlayerAction.ACCUSE);

		} else {
			allowedActions.remove(player.getID());
		}

		// Set the game to inactive if there are no more players left, otherwise rotate to the next player
		if (getNumActivePlayers() < 1) {
			active.compareAndSet(true, false);
		} else {
			indexPlayerTurn = rotatePlayerIndex(indexPlayerTurn, true);
		}
	}

	private int getNumActivePlayers() {
		int count = 0;
		for (Player player : players.values()) {
			if (player.isActive()) {
				count++;
			}
		}
		return count;
	}

	private boolean isValidProposal(Collection<Card> proposal) {
		if (proposal.size() != 3) {
			return false;
		}
		for (EntityType type : EntityType.values()) {
			boolean present = false;
			for (Card card : proposal) {
				if (card.getType() == type) {
					present = true;
					break;
				}
			}
			if (!present) {
				return false;
			}
		}
		return true;
	}

	private Card getTypeFromProposal(Collection<Card> cards, EntityType type) {
		for (Card card : cards) {
			if (card.getType() == type) {
				return card;
			}
		}
		return null;
	}

}
