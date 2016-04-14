package edu.jhu.clueless.board;

import edu.jhu.clueless.Constants.Suspect;
import edu.jhu.clueless.Point;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class GameBoardTest {

	@Test
	public void testGameBoardInitialization() {
		GameBoard board = new GameBoard();

		// Check that game squares and suspect maps are of expected size
		assertEquals(21, board.gameSquares.size());
		assertEquals(6, board.suspectPawns.size());

		// Check that all suspects' homes are occupied
		Set<Point> homes = new HashSet<>();
		for (Suspect suspect : Suspect.values()) {
			Point home = board.suspectPawns.get(suspect);
			homes.add(home);
			GameSquare square = board.gameSquares.get(home);
			assertFalse(square.isAvailable());
		}

		// Check that all remaining squares are available
		for (Map.Entry<Point, GameSquare> square : board.gameSquares.entrySet()) {
			if (!homes.contains(square.getKey())) {
				assertTrue(square.getValue().isAvailable());
			}
		}
	}

	@Test
	public void testMoveToNonexistentSquare() {
		GameBoard board = new GameBoard();
		Point start = board.suspectPawns.get(Suspect.COLONEL_MUSTARD);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, new Point(0, 5)));
		assertEquals(start, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertFalse(board.gameSquares.get(start).isAvailable());
	}

	@Test
	public void testNoMoveHallway() {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, home));
		assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertFalse(board.gameSquares.get(home).isAvailable());
	}

	@Test
	public void testNoMoveRoom() {
		GameBoard board = new GameBoard();
		Point lounge = new Point(4, 4);

		board.move(Suspect.COLONEL_MUSTARD, lounge);

		assertTrue(board.move(Suspect.COLONEL_MUSTARD, lounge));
		assertEquals(lounge, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testMoveOccupiedHallway() {
		GameBoard board = new GameBoard();
		Point lounge = new Point(4, 4);
		Point occupiedHallway = new Point(3, 4);

		board.move(Suspect.COLONEL_MUSTARD, lounge);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, occupiedHallway));
		assertEquals(lounge, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertFalse(board.gameSquares.get(occupiedHallway).isAvailable());
	}

	@Test
	public void testValidHallToRoom() {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point lounge = new Point(4, 4);

		assertTrue(board.move(Suspect.COLONEL_MUSTARD, lounge));
		assertTrue(board.gameSquares.get(home).isAvailable());
		assertEquals(lounge, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testValidRoomToHall() {
		GameBoard board = new GameBoard();
		Point diningRoom = new Point(4, 2);
		Point hallway = new Point(4, 1);

		board.move(Suspect.COLONEL_MUSTARD, diningRoom);

		assertTrue(board.move(Suspect.COLONEL_MUSTARD, hallway));
		assertFalse(board.gameSquares.get(hallway).isAvailable());
		assertEquals(hallway, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testValidRoomToRoom() {
		GameBoard board = new GameBoard();
		Point lounge = new Point(4, 4);
		Point conservatory = new Point(0, 0);

		board.move(Suspect.COLONEL_MUSTARD, lounge);

		assertTrue(board.move(Suspect.COLONEL_MUSTARD, conservatory));
		assertEquals(conservatory, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testInvalidRoomToRoom() {
		GameBoard board = new GameBoard();
		Point diningRoom = new Point(4, 2);
		Point conservatory = new Point(0, 0);

		board.move(Suspect.COLONEL_MUSTARD, diningRoom);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, conservatory));
		assertEquals(diningRoom, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testInvalidHallToHall() {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point hallway = new Point(3, 2);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, hallway));
		assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertFalse(board.gameSquares.get(home).isAvailable());
		assertTrue(board.gameSquares.get(hallway).isAvailable());
	}

	@Test
	public void testInvalidHallToRoom() {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point kitchen = new Point(4, 0);

		assertFalse(board.move(Suspect.COLONEL_MUSTARD, kitchen));
		assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertFalse(board.gameSquares.get(home).isAvailable());
	}

}
