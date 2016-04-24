package edu.jhu.clueless.board;

import edu.jhu.clueless.Constants.Room;

/**
 * Class representing a square on the {@link GameBoard}. A game square can be either a room or a hallway. Rooms have
 * a name and possibly a secret passage, hallways do not.
 */
public class GameSquare {

	private Room roomName;
	private Room secretPassageConnection;
	private boolean occupied;

	/**
	 * Constructs a new GameSquare
	 */
	public GameSquare() {
		this(null, null);
	}

	/**
	 * Constructs a new GameSquare
	 * @param roomName If this game square is a room, the room's name. Can be null if not a room.
	 */
	public GameSquare(Room roomName) {
		this(roomName, null);
	}

	/**
	 * Constructs a new GameSquare
	 * @param roomName If this game square is a room, the room's name. Can be null if not a room.
	 * @param secretPassage If this game square is a room and this room is connected to another room via a secret
	 *                      passage, the name of the connected room. Otherwise, can be null.
	 */
	public GameSquare(Room roomName, Room secretPassage) {
		this.roomName = roomName;
		this.secretPassageConnection = secretPassage;
		occupied = false;
	}

	/**
	 * @return The name of this room, or null if this is not a room.
	 */
	public Room getRoomName() {
		return roomName;
	}

	/**
	 * @return The room connected to this room via a secret passage, or null if this is not a room or doesn't have a
	 * secret passage.
	 */
	public Room getSecretPassageConnection() {
		return secretPassageConnection;
	}

	/**
	 * @return True if this game square is a room, false if it is a hallway.
	 */
	public boolean isRoom() {
		return this.roomName != null;
	}

	/**
	 * @return Returns true if this game square is a room or is an unoccupied hallway, false otherwise.
	 */
	public boolean isAvailable() {
		return isRoom() || !occupied;
	}

	/**
	 * Set whether this game square is occupied.
	 * @param occupied Whether this game square is occupied.
	 */
	public void setOccupied(boolean occupied) {
		if (!isRoom()) {
			this.occupied = occupied;
		}
	}

	/**
	 * Check whether this game square is connected via a secret passage to the given game square.
	 * @param other Game square to check connectivity to
	 * @return True if this game square and other game square are both rooms and are connected via a secret passage,
	 * false otherwise
	 */
	public boolean connectedTo(GameSquare other) {
		return this.isRoom() && other.isRoom() && this.getSecretPassageConnection() == other.getRoomName();
	}

}
