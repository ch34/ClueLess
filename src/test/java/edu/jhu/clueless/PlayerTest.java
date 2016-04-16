package edu.jhu.clueless;

import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.Constants.Weapon;
import edu.jhu.clueless.Constants.Room;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class PlayerTest {

	@Test
	public void testInitialization() {
		Player player = new Player(Suspect.COLONEL_MUSTARD);
		assertNotNull(player.getCards());
		assertEquals(0, player.getCards().size());
		assertNotNull(player.getID());
		assertTrue(player.isActive());
	}

	@Test
	public void testCards() {
		Player player = new Player(Suspect.COLONEL_MUSTARD);
		player.addCard(Suspect.MISS_SCARLET);
		player.addCard(Weapon.CANDLESTICK);
		player.addCard(Room.LIBRARY);
		player.addCard(Weapon.CANDLESTICK);
		assertEquals(3, player.getCards().size());
		assertTrue(player.hasCard(Suspect.MISS_SCARLET));
		assertTrue(player.hasCard(Weapon.CANDLESTICK));
		assertTrue(player.hasCard(Room.LIBRARY));
		assertFalse(player.hasCard(Room.BALLROOM));
		assertTrue(player.hasAnyCard(new HashSet<Card>(Arrays.asList(Weapon.CANDLESTICK, Weapon.KNIFE))));
		assertFalse(player.hasAnyCard(new HashSet<Card>(Arrays.asList(Room.BALLROOM, Weapon.KNIFE))));
	}

}
