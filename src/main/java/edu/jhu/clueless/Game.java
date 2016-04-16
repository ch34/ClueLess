package edu.jhu.clueless;

import com.sun.istack.internal.Nullable;
import edu.jhu.clueless.Constants.EntityType;
import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.board.GameBoard;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class Game {

	private static Suspect[] TURN_ORDER = { Suspect.MISS_SCARLET, Suspect.COLONEL_MUSTARD, Suspect.MRS_WHITE,
			Suspect.MR_GREEN, Suspect.MRS_PEACOCK, Suspect.PROFESSOR_PLUM };

	private UUID id;
	private String name;
	private GameBoard board;
	private Map<EntityType, Card> caseFile;
	private int indexPlayerTurn;
	private int indexPlayerResponding;
	private AtomicBoolean started;
	private Player winner;
	private Map<EntityType, Card> currentSuggestion;
	private ConcurrentMap<Suspect, Player> players;

	public Game() {
		this("New Game");
	}

	public Game(String name) {
		id = UUID.randomUUID();
		this.name = name;
		board = new GameBoard();
		caseFile = new HashMap<>();
		players = new ConcurrentHashMap<>();
		indexPlayerTurn = -1;
		started = new AtomicBoolean();
		currentSuggestion = null;
		winner = null;
	}

	/**
	 * @return True if the game was started successfully (or was already started), false if the game could not be
	 * started because there are not at least three players
	 */
	public boolean start() {
		if (players.size() < 3) {
			return false;
		}
		// Only initialize game if it was not already started
		if (started.compareAndSet(false, true)) {
			distributeCards();
			indexPlayerTurn = rotatePlayerIndex(indexPlayerTurn, true);
		}
		return true;
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
			Player player = players.get(TURN_ORDER[indexPlayer]);
			player.addCard(card);
			iterator.remove();
			indexPlayer = rotatePlayerIndex(indexPlayer, true);
		}
	}

	public UUID getId() {
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

	public boolean isStarted() {
		return started.get();
	}

	/**
	 * @return Winner of game, or null if no winner yet
	 */
	public Player getWinner() {
		return winner;
	}

	/**
	 * @return Player whose turn it is, or null if game is not yet started or has already been won.
	 */
	public Player getPlayerTurn() {
		if (!started.get()|| winner != null) {
			return null;
		}
		return players.get(TURN_ORDER[indexPlayerTurn]);
	}

	/**
	 * Add new player to the game, if the given suspect is available and game is not started.
	 * @param suspect Suspect to associate with the new player.
	 * @return Id of the new player, or null if the player could not be added.
	 */
	public UUID addPlayer(Suspect suspect) {
		if (started.get()) {
			return null;
		}
		Player newPlayer = new Player(suspect);
		Player existingPlayer = players.putIfAbsent(suspect, newPlayer);
		return existingPlayer == null ? newPlayer.getID() : null;
	}

	/**
	 * @param id Id of player to retrieve
	 * @return The player associated with the given ID, or null if no player is associated with the given ID.
	 */
	public Player getPlayer(UUID id) {
		for (Player player : players.values()) {
			if (player.getID().equals(id)) {
				return player;
			}
		}
		return null;
	}

	private boolean isPlayerTurn(Player player) {
		return isStarted() &&
			winner == null &&
			player.getSuspect().equals(TURN_ORDER[indexPlayerTurn]);
	}

	private int rotatePlayerIndex(int currIndex, boolean mustBeActive) {
		int newIndex = incrementCounterModPlayers(currIndex);
		Player nextPlayer = players.get(TURN_ORDER[newIndex]);
		while (nextPlayer == null || (mustBeActive && !nextPlayer.isActive())) {
			newIndex = incrementCounterModPlayers(newIndex);
			nextPlayer = players.get(TURN_ORDER[newIndex]);
		}
		return newIndex;
	}

	private int incrementCounterModPlayers(int counter) {
		return (counter + 1) % TURN_ORDER.length;
	}

	/**
	 * Move player to given destination
	 * @param player Player to move
	 * @param point Destination to move player to
	 * @return True if move request was valid and game state was updated as a result, false otherwise.
	 */
	public boolean move(Player player, Point point) {
		boolean valid = isPlayerTurn(player) &&
				board.move(player.getSuspect(), point);

		// If player moved into a hallway, their turn is over. If they moved into a room, they can make a suggestion.
		if (valid && board.getRoom(point) == null) {
			indexPlayerTurn = rotatePlayerIndex(indexPlayerTurn, true);
		}

		return valid;
	};

	/**
	 * @param player Player making suggestion
	 * @param suggestion Player's suggestion
	 * @return True if suggestion is valid and game state is updated as a result, false otherwise.
	 */
	public boolean suggest(Player player, Map<EntityType, Card> suggestion) {
		Point playerLocation = board.getLocation(player.getSuspect());

		boolean valid = isPlayerTurn(player) &&
				suggestion.size() == 3 &&
				board.getRoom(playerLocation) == suggestion.get(EntityType.ROOM);

		if (valid) {
			// Move suggestion's suspect to suggestion's room
			board.move((Suspect) suggestion.get(EntityType.SUSPECT), playerLocation, true);

			// Save this player's suggestion for comparison against responses
			currentSuggestion = suggestion;

			// Initialize rotation for players' responses
			indexPlayerResponding = rotatePlayerIndex(indexPlayerTurn, false);
		}

		return valid;
	}

	/**
	 * Check if this player's response to the current suggestion is valid.
	 * @param player Player responding
	 * @param response Player's response, or null if player cannot disprove suggestion.
	 * @return True if this response is valid and game state is updated as a result, false otherwise.
	 */
	public boolean isResponseValid(Player player, @Nullable Card response) {
		boolean valid;
		if (currentSuggestion == null) {
			valid = false;
		} else if (!player.getSuspect().equals(TURN_ORDER[indexPlayerResponding])) {
			valid = false;
		} else if (response == null) {
			valid = !player.hasAnyCard(currentSuggestion.values());
		} else {
			valid = currentSuggestion.containsValue(response) && player.hasCard(response);
		}

		if (valid) {
			indexPlayerResponding = rotatePlayerIndex(indexPlayerResponding, false);

			// If we've wrapped back around to the original suggester, the response cycle is over, and the main turn
			// cycle continues
			if (indexPlayerResponding == indexPlayerTurn) {
				currentSuggestion = null;
				indexPlayerTurn = rotatePlayerIndex(indexPlayerTurn, true);
			}
		}

		return valid;
	}

	/**
	 * @param player Player making accusation
	 * @param accusation Player's accusation
	 * @return True if accusation is valid (not necessarily correct) and game state is updated as a result, false
	 * otherwise.
	 */
	public boolean accuse(Player player, Map<EntityType, Card> accusation) {
		boolean valid = isPlayerTurn(player) &&
				accusation.size() == 3;

		if (valid) {
			if (accusation.get(EntityType.SUSPECT) == caseFile.get(EntityType.SUSPECT) &&
					accusation.get(EntityType.WEAPON) == caseFile.get(EntityType.WEAPON) &&
					accusation.get(EntityType.ROOM) == caseFile.get(EntityType.ROOM)) {
				winner = player;
			} else {
				player.setActive(false);
			}
		}

		return valid;
	}

}
