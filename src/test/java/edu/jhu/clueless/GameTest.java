package edu.jhu.clueless;

import org.junit.Test;

import edu.jhu.clueless.Constants.Suspect;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GameTest {

	@Test
	public void testInitialization() {
		Game game = new Game("Test Game");

		assertNotNull(game.getId());
		assertEquals("Test Game", game.getName());
		assertEquals(0, game.getPlayers().size());
		assertFalse(game.isStarted());
	}

	@Test
	public void testAddPlayerSuspectTaken() {
		Game game = new Game();

		assertNotNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		assertNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		assertEquals(1, game.getPlayers().size());
	}

	@Test
	public void testAddPlayerGameStarted() {
		Game game = new Game();

		assertNotNull(game.addPlayer(Suspect.COLONEL_MUSTARD));
		assertNotNull(game.addPlayer(Suspect.PROFESSOR_PLUM));
		assertNotNull(game.addPlayer(Suspect.MR_GREEN));
		game.start();
		assertNull(game.addPlayer(Suspect.MISS_SCARLET));
		assertEquals(3, game.getPlayers().size());
	}

	@Test
	public void testStartGameSuccess() {
		Game game = new Game();
		game.addPlayer(Suspect.COLONEL_MUSTARD);
		game.addPlayer(Suspect.MR_GREEN);
		game.addPlayer(Suspect.MISS_SCARLET);
		game.addPlayer(Suspect.MRS_PEACOCK);

		assertTrue(game.start());

		// Verify case file has 3 cards
		Set<Card> caseFile = game.getCaseFile();
		assertEquals(3, caseFile.size());

		// Verify remaining 18 unique cards have been distributed to players
		Set<Card> playerCards = new HashSet<>();
		for (Player player : game.getPlayers()) {
			playerCards.addAll(player.getCards());
		}
		assertEquals(18, playerCards.size());

		// Verify no overlap between players' cards and case file
		Set<Card> merged = new HashSet<>();
		merged.addAll(caseFile);
		merged.addAll(playerCards);
		assertEquals(21, merged.size());

		// Shouldn't hurt anything if game.start() is called again
		assertTrue(game.start());
	}

	@Test
	public void testStartGameFail() {
		Game game = new Game();
		assertFalse(game.start());
	}

}
