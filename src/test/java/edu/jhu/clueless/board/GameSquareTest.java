package edu.jhu.clueless.board;

import edu.jhu.clueless.Constants.Room;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameSquareTest {

	@Test
	public void testInitializeHallway() {
		GameSquare square = new GameSquare();
		assertNull(square.getRoomName());
		assertNull(square.getSecretPassageConnection());
		assertTrue(square.isAvailable());
		assertFalse(square.isRoom());
	}

	@Test
	public void testInitializeRoomWithoutPassage() {
		GameSquare square = new GameSquare(Room.BILLIARD_ROOM);
		assertEquals(Room.BILLIARD_ROOM, square.getRoomName());
		assertNull(square.getSecretPassageConnection());
		assertTrue(square.isAvailable());
	}

	@Test
	public void testInitializeRoomWithPassage() {
		GameSquare conservatory = new GameSquare(Room.CONSERVATORY, Room.LOUNGE);
		GameSquare lounge = new GameSquare(Room.LOUNGE, Room.CONSERVATORY);
		assertEquals(Room.CONSERVATORY, conservatory.getRoomName());
		assertEquals(Room.LOUNGE, conservatory.getSecretPassageConnection());
		assertTrue(conservatory.connectedTo(lounge));
		assertTrue(conservatory.isAvailable());
	}

	@Test
	public void testAvailableCheckHallway() {
		GameSquare hallway = new GameSquare();
		hallway.setOccupied(true);
		assertFalse(hallway.isAvailable());
		hallway.setOccupied(false);
		assertTrue(hallway.isAvailable());
	}

	@Test
	public void testAvailableCheckRoom() {
		GameSquare room = new GameSquare(Room.LOUNGE);
		room.setOccupied(true);
		assertTrue(room.isAvailable());
		room.setOccupied(false);
		assertTrue(room.isAvailable());
	}

}
