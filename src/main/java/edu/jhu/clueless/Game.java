package edu.jhu.clueless;

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
	private Set<Card> caseFile;
	private int indexPlayerTurn;
	private AtomicBoolean isStarted;
	private Set<Card> currentSuggestion;
	private ConcurrentMap<Suspect, Player> players;

	public Game() {
		this("New Game");
	}

	public Game(String name) {
		id = UUID.randomUUID();
		this.name = name;
		board = new GameBoard();
		caseFile = new HashSet<>();
		players = new ConcurrentHashMap<>();
		indexPlayerTurn = -1;
		isStarted = new AtomicBoolean();
		currentSuggestion = new HashSet<>();
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
		if (isStarted.compareAndSet(false, true)) {
			distributeCards();
			incrementPlayerTurn();
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
					caseFile.add(card);
					cards.remove(i);
					break;
				}
			}
		}

		// Distribute remaining cards to players
		int indexPlayer = 0;
		for (Iterator<Card> iterator = cards.iterator(); iterator.hasNext();) {
			Card card = iterator.next();
			Player player;
			while ((player = players.get(TURN_ORDER[indexPlayer])) == null) {
				indexPlayer = incrementPlayerCounter(indexPlayer);
			}
			player.addCard(card);
			iterator.remove();
			indexPlayer = incrementPlayerCounter(indexPlayer);
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

	public Set<Card> getCaseFile() {
		return Collections.unmodifiableSet(caseFile);
	}

	public boolean isStarted() {
		return isStarted.get();
	}

	/**
	 * Add new player to the game, if the given suspect is available and game is not started.
	 * @param suspect Suspect to associate with the new player.
	 * @return Id of the new player, or null if the player could not be added.
	 */
	public UUID addPlayer(Suspect suspect) {
		if (isStarted.get()) {
			return null;
		}
		Player newPlayer = new Player(suspect);
		Player existingPlayer = players.putIfAbsent(suspect, newPlayer);
		return existingPlayer == null ? newPlayer.getID() : null;
	}

	private Player getPlayer(UUID id) {
		for (Player player : players.values()) {
			if (player.getID().equals(id)) {
				return player;
			}
		}
		return null;
	}

	private boolean isPlayerTurn(Player player) {
		return player.getSuspect().equals(TURN_ORDER[indexPlayerTurn]);
	}

	private void incrementPlayerTurn() {
		indexPlayerTurn = incrementPlayerCounter(indexPlayerTurn);
		while (players.get(TURN_ORDER[indexPlayerTurn]) == null) {
			indexPlayerTurn = incrementPlayerCounter(indexPlayerTurn);
		}
	}

	private int incrementPlayerCounter(int counter) {
		return (counter + 1) % TURN_ORDER.length;
	}

	/**
	 * Move player to given destination
	 * @param playerId Id of player to move
	 * @param point Destination to move player to
	 * @return True if move request was valid and game state was updated as a result, false otherwise.
	 */
	public boolean move(UUID playerId, Point point) {
		Player player = getPlayer(playerId);
		return player != null &&
				isPlayerTurn(player) &&
				board.move(player.getSuspect(), point);
	};

	/**
	 *
	 * @param playerId Id of player making suggestion
	 * @param suggestion Player's suggestion
	 * @return True if suggestion is valid and game state is updated as a result, false otherwise.
	 */
	public boolean suggest(UUID playerId, List<Card> suggestion) {
		Player player = getPlayer(playerId);
		// TODO: implement
		return true;
	}

}
