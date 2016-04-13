package edu.jhu.clueless;

import edu.jhu.clueless.board.GameSquare;

import java.awt.*;

public class Room extends GameSquare {

	private Constants.Room name;
	private Constants.Room secretPassage;

	public Room(Point location, Constants.Room name) {
		this(location, name, null);
	}

	public Room(Point location, Constants.Room name, Constants.Room secretPassage) {
		super(location);
		this.name = name;
		this.secretPassage = secretPassage;
	}

	public Constants.Room getName() {
		return name;
	}

	public Constants.Room getSecretPassage() {
		return secretPassage;
	}

}
