package edu.jhu.clueless.board;

import edu.jhu.clueless.Constants;

import java.awt.*;
import java.util.Map;

public class GameBoard {

	private Map<Constants.Suspect, Point> suspectPawns;
	private Map<Point, GameSquare> gameSquares;

	// TODO: intialize game board

	// TODO: Does this functionality need to be exposed?
	public Point getLocation(Constants.Suspect suspect) {
		return suspectPawns.get(suspect);
	}

	// TODO: Does this functionality need to be exposed?
	public GameSquare getGameSquare(Point location) {
		return gameSquares.get(location);
	}

	public void move(Constants.Suspect suspect, Point destination) {
		// TODO: handle move, throw exception or return false if invalid
	};

}
