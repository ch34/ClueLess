package edu.jhu.clueless;

public class Point {

	private int x, y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}

		if (other instanceof Point) {
			return this.x == ((Point) other).getX() && this.y == ((Point) other).getY();
		}

		return false;
	}

	@Override
	public int hashCode() {
		return (x + "," + y).hashCode();
	}

}
