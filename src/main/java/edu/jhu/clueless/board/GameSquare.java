package edu.jhu.clueless.board;

import java.awt.*;

public class GameSquare {

	private Point location;
	private boolean occupied = false;

	public GameSquare(Point location) {
		this.location = location;
	}

	public void setOccupied(boolean occupied) {
		this.occupied = occupied;
	}

	public boolean isOccupied() {
		return occupied;
	}

	public Point getLocation() {
		return location;
	}

}
