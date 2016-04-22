package edu.jhu.clueless.board;

import edu.jhu.clueless.CluelessException;
import edu.jhu.clueless.Constants.Suspect;
import org.junit.Test;

import java.awt.*;
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

	@Test(expected = CluelessException.class)
	public void testMoveToNonexistentSquare() throws CluelessException {
		GameBoard board = new GameBoard();
		Point start = board.suspectPawns.get(Suspect.COLONEL_MUSTARD);

		try {
			board.move(Suspect.COLONEL_MUSTARD, new Point(0, 5));
		} catch (CluelessException e) {
			assertEquals(start, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(start).isAvailable());
			throw e;
		}
	}

	@Test(expected = CluelessException.class)
	public void testNoMoveHallway() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);

		try {
			board.move(Suspect.COLONEL_MUSTARD, home);
		} catch (CluelessException e) {
			assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(home).isAvailable());
			throw e;
		}
	}

	@Test(expected = CluelessException.class)
	public void testMoveOccupiedHallway() throws CluelessException {
		GameBoard board = new GameBoard();
		Point lounge = new Point(4, 4);
		Point occupiedHallway = new Point(3, 4);

		board.move(Suspect.COLONEL_MUSTARD, lounge);

		try {
			board.move(Suspect.COLONEL_MUSTARD, occupiedHallway);
		} catch (CluelessException e) {
			assertEquals(lounge, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(occupiedHallway).isAvailable());
			throw e;
		}
	}

	@Test
	public void testValidHallToRoom() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point lounge = new Point(4, 4);

		board.move(Suspect.COLONEL_MUSTARD, lounge);
		assertTrue(board.gameSquares.get(home).isAvailable());
		assertEquals(lounge, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testValidRoomToHall() throws CluelessException{
		GameBoard board = new GameBoard();
		Point diningRoom = new Point(4, 2);
		Point hallway = new Point(4, 1);

		board.move(Suspect.COLONEL_MUSTARD, diningRoom);

		board.move(Suspect.COLONEL_MUSTARD, hallway);
		assertFalse(board.gameSquares.get(hallway).isAvailable());
		assertEquals(hallway, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test
	public void testValidRoomToRoom() throws CluelessException {
		GameBoard board = new GameBoard();
		Point lounge = new Point(4, 4);
		Point conservatory = new Point(0, 0);

		board.move(Suspect.COLONEL_MUSTARD, lounge);

		board.move(Suspect.COLONEL_MUSTARD, conservatory);
		assertEquals(conservatory, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
	}

	@Test(expected = CluelessException.class)
	public void testInvalidRoomToRoom() throws CluelessException {
		GameBoard board = new GameBoard();
		Point diningRoom = new Point(4, 2);
		Point conservatory = new Point(0, 0);

		board.move(Suspect.COLONEL_MUSTARD, diningRoom);

		try {
			board.move(Suspect.COLONEL_MUSTARD, conservatory);
		} catch (CluelessException e) {
			assertEquals(diningRoom, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			throw e;
		}
	}

	@Test(expected = CluelessException.class)
	public void testInvalidHallToHall() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point hallway = new Point(3, 2);

		try {
			board.move(Suspect.COLONEL_MUSTARD, hallway);
		} catch (CluelessException e) {
			assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(home).isAvailable());
			assertTrue(board.gameSquares.get(hallway).isAvailable());
			throw e;
		}
	}

	@Test(expected = CluelessException.class)
	public void testInvalidHallToRoom() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point kitchen = new Point(4, 0);

		try {
			board.move(Suspect.COLONEL_MUSTARD, kitchen);
		} catch (CluelessException e) {
			assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(home).isAvailable());
			throw e;
		}
	}

	@Test
	public void testValidMoveTriggeredBySuggestion() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point library = new Point(0, 2);

		board.move(Suspect.COLONEL_MUSTARD, library, true);
		assertEquals(library, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
		assertTrue(board.gameSquares.get(home).isAvailable());
	}

	@Test(expected = CluelessException.class)
	public void testInvalidMoveTriggeredBySuggestion() throws CluelessException {
		GameBoard board = new GameBoard();
		Point home = new Point(4, 3);
		Point hallway = new Point(3, 2);

		try {
			board.move(Suspect.COLONEL_MUSTARD, hallway, true);
		} catch (CluelessException e) {
			assertEquals(home, board.suspectPawns.get(Suspect.COLONEL_MUSTARD));
			assertFalse(board.gameSquares.get(home).isAvailable());
			throw e;
		}
	}

}
