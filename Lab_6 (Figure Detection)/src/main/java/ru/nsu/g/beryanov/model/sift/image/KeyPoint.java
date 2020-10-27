package ru.nsu.g.beryanov.model.sift.image;

public class KeyPoint extends Region {
	public KeyPoint(int row, int col, double scale, int level) {
		super(row, col, scale, level);
	}
	
	public KeyPoint(int row, int col, double scale, double orientation, double magnitude) {
		super(row, col, scale, orientation, magnitude);
	}
}
