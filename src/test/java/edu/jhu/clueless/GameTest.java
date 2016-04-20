package edu.jhu.clueless;

import org.junit.Test;

import edu.jhu.clueless.Constants.EntityType;
import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.Constants.Room;
import edu.jhu.clueless.Constants.Weapon;

import java.awt.*;
import java.util.*;

import static org.junit.Assert.*;

public class GameTest {

	@Test
	public void testInitialization() {
		Game game = new Game("Test Game");

		assertNotNull(game.getId());
		assertEquals("Test Game", game.getName());
		assertEquals(0, game.getPlayers().size());
		assertFalse(game.isActive());
	}

	@Test(expected = CluelessException.class)
	public void testAddPlayerSuspectTaken() throws CluelessException {
		Game game = new Game();

		assertNotNull(game.addPlayer(Suspect.COLONEL_MUSTARD));

		try {
			assertNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		} catch (CluelessException e) {
			assertEquals(1, game.getPlayers().size());
			throw e;
		}
	}

	@Test(expected = CluelessException.class)
	public void testAddPlayerGameStarted() throws CluelessException {
		Game game = new Game();

		assertNotNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		assertNotNull(game.addPlayer(Suspect.PROFESSOR_PLUM));
		assertNotNull(game.addPlayer(Suspect.MR_GREEN));
		game.start();

		try {
			assertNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		} catch (CluelessException e) {
			assertEquals(3, game.getPlayers().size());
			throw e;
		}
	}

	@Test
	public void testStartGameSuccess() throws CluelessException {
		Game game = new Game();
		game.addPlayer(Suspect.COLONEL_MUSTARD);
		game.addPlayer(Suspect.MR_GREEN);
		game.addPlayer(Suspect.MISS_SCARLET);
		game.addPlayer(Suspect.MRS_PEACOCK);

		game.start();

		// Verify case file has 3 cards
		Map<EntityType, Card> caseFile = game.getCaseFile();
		assertEquals(3, caseFile.size());

		// Verify remaining 18 unique cards have been distributed to players
		Set<Card> playerCards = new HashSet<>();
		for (Player player : game.getPlayers()) {
			playerCards.addAll(player.getCards());
		}
		assertEquals(18, playerCards.size());

		// Verify no overlap between players' cards and case file
		Set<Card> merged = new HashSet<>();
		merged.addAll(caseFile.values());
		merged.addAll(playerCards);
		assertEquals(21, merged.size());

		// Shouldn't hurt anything if game.start() is called again
		game.start();
	}

	@Test(expected = CluelessException.class)
	public void testStartGameFail() throws CluelessException {
		Game game = new Game();
		game.start();
	}

	private Game createAndStartGame() throws CluelessException {
		Game game = new Game();
		game.addPlayer(Suspect.MISS_SCARLET);
		game.addPlayer(Suspect.COLONEL_MUSTARD);
		game.addPlayer(Suspect.PROFESSOR_PLUM);
		game.start();
		return game;
	}

	@Test
	public void testRotatePlayerTurnActiveNotRequired() throws CluelessException {
		Game game = createAndStartGame();
		game.players.get(Suspect.COLONEL_MUSTARD).setActive(false);
		assertEquals(game.players.get(Suspect.MISS_SCARLET), game.getPlayerTurn());
		game.indexPlayerTurn = game.rotatePlayerIndex(0, false);
		assertEquals(game.players.get(Suspect.COLONEL_MUSTARD), game.getPlayerTurn());
		game.indexPlayerTurn = game.rotatePlayerIndex(1, false);
		assertEquals(game.players.get(Suspect.PROFESSOR_PLUM), game.getPlayerTurn());
		game.indexPlayerTurn = game.rotatePlayerIndex(5, false);
		assertEquals(game.players.get(Suspect.MISS_SCARLET), game.getPlayerTurn());
	}

	@Test
	public void testRotatePlayerTurnActiveRequired() throws CluelessException {
		Game game = createAndStartGame();
		game.players.get(Suspect.COLONEL_MUSTARD).setActive(false);
		assertEquals(game.players.get(Suspect.MISS_SCARLET), game.getPlayerTurn());
		game.indexPlayerTurn = game.rotatePlayerIndex(0, true);
		assertEquals(game.players.get(Suspect.PROFESSOR_PLUM), game.getPlayerTurn());
		game.indexPlayerTurn = game.rotatePlayerIndex(5, true);
		assertEquals(game.players.get(Suspect.MISS_SCARLET), game.getPlayerTurn());
	}

	@Test
	public void testValidMove() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
	}

	@Test(expected = CluelessException.class)
	public void testMoveGameNotStarted() throws CluelessException {
		Game game = new Game();
		UUID scarlet = game.addPlayer(Suspect.MISS_SCARLET);
		game.move(game.getPlayer(scarlet), new Point(4, 4));
	}

	@Test(expected = CluelessException.class)
	public void testMoveGameWon() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.accuse(scarlet, game.getCaseFile().values());

		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		game.move(mustard, new Point(4, 4));
	}

	@Test(expected = CluelessException.class)
	public void testMoveOutOfTurn() throws CluelessException {
		Game game = createAndStartGame();
		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		game.move(mustard, new Point(4, 4));
	}

	@Test(expected = CluelessException.class)
	public void testInvalidDoubleMove() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		game.move(scarlet, new Point(3, 4));
	}

	@Test
	public void testValidSuggestion() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		game.suggest(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK)));
	}

	@Test(expected = CluelessException.class)
	public void testSuggestHallway() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.suggest(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.BALLROOM,
				Weapon.CANDLESTICK)));
	}

	@Test(expected = CluelessException.class)
	public void testInvalidSuggestion() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		game.suggest(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Suspect.PROFESSOR_PLUM,
				Weapon.CANDLESTICK)));
	}

	@Test(expected = CluelessException.class)
	public void testSuggestionWrongRoom() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		game.suggest(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.KITCHEN,
				Weapon.CANDLESTICK)));
	}

	@Test
	public void testValidAccusationIncorrect() throws CluelessException {
		Game game = createAndStartGame();

		// Assemble incorrect suggestion
		Card roomCorrect = game.getCaseFile().get(EntityType.ROOM);
		Set<Card> suggestion = new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Weapon.CANDLESTICK));
		for (Room room : Room.values()) {
			if (room != roomCorrect) {
				suggestion.add(room);
				break;
			}
		}

		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.accuse(scarlet, suggestion);
		assertNull(game.getWinner());
		assertTrue(game.isActive());
		assertFalse(scarlet.isActive());
	}

	@Test
	public void testValidAccusationCorrect() throws CluelessException {
		Game game = createAndStartGame();

		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.accuse(scarlet, game.getCaseFile().values());
		assertEquals(scarlet, game.getWinner());
		assertFalse(game.isActive());
		assertFalse(scarlet.isActive());
	}

	@Test(expected = CluelessException.class)
	public void testInvalidAccusation() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.accuse(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Suspect.PROFESSOR_PLUM,
				Weapon.CANDLESTICK)));
	}

	@Test
	public void testValidResponse() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion =  new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);

		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		Card response = null;
		for (Card card : mustard.getCards()) {
			if (suggestion.contains(card)) {
				response = card;
				break;
			}
		}

		game.respond(mustard, response);
	}

	@Test(expected = CluelessException.class)
	public void testRespondNoSuggestion() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.respond(scarlet, Suspect.COLONEL_MUSTARD);
	}

	@Test(expected = CluelessException.class)
	public void testRespondOutOfTurn() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion =  new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);

		Player plum = game.players.get(Suspect.PROFESSOR_PLUM);
		Card response = null;
		for (Card card : plum.getCards()) {
			if (suggestion.contains(card)) {
				response = card;
				break;
			}
		}

		game.respond(plum, response);
	}

	@Test(expected = CluelessException.class)
	public void testInvalidResponse() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion =  new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);

		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);

		// Assemble invalid response
		Card response = null;
		for (Card card : mustard.getCards()) {
			if (!suggestion.contains(card)) {
				response = card;
			}
		}

		game.respond(mustard, response);
	}

	@Test(expected = CluelessException.class)
	public void testRespondCycleOver() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		Player plum = game.players.get(Suspect.PROFESSOR_PLUM);

		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion = new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);
		game.respond(mustard, getValidResponse(mustard, suggestion));
		game.respond(plum, getValidResponse(plum, suggestion));
		game.respond(mustard, getValidResponse(mustard, suggestion));
	}

	@Test
	public void testEndTurnValid() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		game.move(scarlet, new Point(4, 4));
		game.endTurn(scarlet);
	}

	@Test(expected = CluelessException.class)
	public void testSuggestTwiceSameRoomInvalid() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		Player plum = game.players.get(Suspect.PROFESSOR_PLUM);

		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion = new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);
		game.respond(mustard, getValidResponse(mustard, suggestion));
		game.respond(plum, getValidResponse(plum, suggestion));
		game.endTurn(scarlet);
		game.endTurn(mustard);
		game.endTurn(plum);

		game.suggest(scarlet, new HashSet<>(Arrays.asList(Suspect.MRS_PEACOCK, Room.LOUNGE,
				Weapon.CANDLESTICK)));
	}

	@Test
	public void testSuggestAfterPassiveMoveValid() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		Player plum = game.players.get(Suspect.PROFESSOR_PLUM);

		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion = new HashSet<>(Arrays.asList(Suspect.COLONEL_MUSTARD, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);
		game.respond(mustard, getValidResponse(mustard, suggestion));
		game.respond(plum, getValidResponse(plum, suggestion));
		game.endTurn(scarlet);
		game.suggest(mustard, suggestion);
	}

	private Card getValidResponse(Player player, Set<Card> suggestion) {
		for (Card card : player.getCards()) {
			if (suggestion.contains(card)) {
				return card;
			}
		}
		return null;
	}

	@Test(expected = CluelessException.class)
	public void testInvalidDoubleSuggestion() throws CluelessException {
		Game game = createAndStartGame();
		Player scarlet = game.players.get(Suspect.MISS_SCARLET);
		Player mustard = game.players.get(Suspect.COLONEL_MUSTARD);
		Player plum = game.players.get(Suspect.PROFESSOR_PLUM);

		game.move(scarlet, new Point(4, 4));
		Set<Card> suggestion = new HashSet<>(Arrays.asList(Suspect.COLONEL_MUSTARD, Room.LOUNGE,
				Weapon.CANDLESTICK));
		game.suggest(scarlet, suggestion);
		game.respond(mustard, getValidResponse(mustard, suggestion));
		game.respond(plum, getValidResponse(plum, suggestion));
		game.suggest(scarlet, suggestion);
	}

}
