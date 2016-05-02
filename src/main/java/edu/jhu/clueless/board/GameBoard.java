package edu.jhu.clueless.board;

import edu.jhu.clueless.CluelessException;
import edu.jhu.clueless.Constants.Room;
import edu.jhu.clueless.Constants.Suspect;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GameBoard {

	protected Map<Suspect, Point> suspectPawns;
	protected Map<Point, GameSquare> gameSquares;

	public GameBoard() {
		gameSquares = new HashMap<>();
		suspectPawns = new HashMap<>();
		initializeGameSquares();
		initializeSuspectPawns();
	}

	// Point (0,0) corresponds to bottom-left square in Clue board (i.e. the Conservatory)
	public void initializeGameSquares() {
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				// If x and y are both even, this is a room. Determine which specific room based on coordinates
				if (x % 2 == 0 && y % 2 == 0) {
					String xy = x + "," + y;
					GameSquare room = null;
					switch (xy) {
						case "0,0": room = new GameSquare(Room.CONSERVATORY, Room.LOUNGE); break;
						case "2,0": room = new GameSquare(Room.BALLROOM); break;
						case "4,0": room = new GameSquare(Room.KITCHEN, Room.STUDY);
						case "0,2": room = new GameSquare(Room.LIBRARY); break;
						case "2,2": room = new GameSquare(Room.BILLIARD_ROOM); break;
						case "4,2": room = new GameSquare(Room.DINING_ROOM); break;
						case "0,4": room = new GameSquare(Room.STUDY, Room.KITCHEN); break;
						case "2,4": room = new GameSquare(Room.HALL); break;
						case "4,4": room = new GameSquare(Room.LOUNGE, Room.CONSERVATORY); break;
					}
					gameSquares.put(new Point(x, y), room);
				// If x is even and y is odd OR x is odd and y is even, this is a hallway
				} else if ((x % 2 == 0 && y % 2 == 1) || (x % 2 == 1 && y % 2 == 0)) {
					gameSquares.put(new Point(x, y), new GameSquare());
				}
				// Otherwise, no game square exists at these coordinates
			}
		}
	}

	/**
	 * @param point Point being queried
	 * @return Room located at given point, or null if not a room.
	 */
	public Room getRoom(Point point) {
		return gameSquares.get(point).getRoomName();
	}

	/**
	 * @param suspect Suspect being queried
	 * @return Point location of given suspect
	 */
	public Point getLocation(Suspect suspect) {
		return suspectPawns.get(suspect);
	}

	/**
	 * @param room Suspect being queried
	 * @return Point location of given room
	 */
	public Point getLocation(Room room) {
		for (Map.Entry<Point, GameSquare> square : gameSquares.entrySet()) {
			if (square.getValue().getRoomName() == room) {
				return square.getKey();
			}
		}
		return null;
	}

	public void initializeSuspectPawns() {
		for (Suspect suspect : Suspect.values()) {
			Point home = null;
			switch (suspect) {
				case COLONEL_MUSTARD: home = new Point(4, 3); break;
				case MISS_SCARLET: home = new Point(3, 4); break;
				case PROFESSOR_PLUM: home = new Point(0, 3); break;
				case MR_GREEN: home = new Point(1, 0); break;
				case MRS_WHITE: home = new Point(3, 0); break;
				case MRS_PEACOCK: home = new Point(0, 1);
			}
			suspectPawns.put(suspect, home);
			gameSquares.get(home).setOccupied(true);
		}
	}

	/**
	 * Move given suspect pawn to given destination point, if allowed.
	 * @param suspect Suspect to move
	 * @param destination Desired destination
	 * @throws CluelessException if the move request was invalid and game state was not modified.
	 */
	public void move(Suspect suspect, Point destination) throws CluelessException {
		move(suspect, destination, false);
	}

	/**
	 * Move given suspect pawn to given destination point, if allowed.
	 * @param suspect Suspect to move
	 * @param destination Desired destination
	 * @param triggeredBySuggestion Whether this move was triggered by a suggestion
	 * @throws CluelessException if the move request was invalid and game state was not modified.
	 */
	public void move(Suspect suspect, Point destination, boolean triggeredBySuggestion) throws CluelessException{
		Point start = suspectPawns.get(suspect);
		GameSquare startSquare = gameSquares.get(start);
		GameSquare destinationSquare = gameSquares.get(destination);
		int diffX = Math.abs(start.x - destination.x);
		int diffY = Math.abs(start.y - destination.y);

		// Validate request
		if (destinationSquare == null) {
			throw new CluelessException(String.format("Location=%s is not a valid square on game board", destination));
		}
		if (triggeredBySuggestion && !destinationSquare.isRoom()) {
			throw new CluelessException(String.format("Requested to move suspect=%s to location=%s, but this location is " +
					"not a room", suspect, destination));
		}
		if (!triggeredBySuggestion) {
			if (diffX == 0 && diffY == 0) {
				throw new CluelessException(String.format("Current location=%s and proposed destination=%s are the same",
						start, destination));
			}
			if (!destinationSquare.isAvailable()) {
				throw new CluelessException(String.format("Requested destination=%s is not available", destination));
			}
			if (diffX + diffY > 1 && !startSquare.connectedTo(destinationSquare)) {
				throw new CluelessException(String.format("Invalid request to move from %s to %s. Must either move to an " +
						"adjacent square or through a secret passage", start, destination));
			}
		}

		// Update location of suspect pawn
		suspectPawns.put(suspect, destination);
		startSquare.setOccupied(false);
		destinationSquare.setOccupied(true);
	};

}
