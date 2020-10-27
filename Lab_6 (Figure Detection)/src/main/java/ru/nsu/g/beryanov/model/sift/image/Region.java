package ru.nsu.g.beryanov.model.sift.image;

import java.util.ArrayList;
import java.util.List;

public abstract class Region {
	
	protected int mRow;
	protected int mCol;
	protected int mLevel;
	protected double mScale;
	protected double mOrientation;
	protected double mMagnitude;
	protected final List<Double> mVectorFeatures;

	public Region(int iRow, int iCol, double iScale, int iLevel) {
		mRow = iRow;
		mCol = iCol;
		mScale = iScale;
		mLevel = iLevel;
		mVectorFeatures = new ArrayList<Double>();
	}
	
	public Region(int iRow, int iCol, double iScale, double iOrientation, double iMagnitude) {
		mRow = iRow;
		mCol = iCol;
		mScale = iScale;
		mOrientation = iOrientation;
		mMagnitude = iMagnitude;
		mVectorFeatures = new ArrayList<Double>();
	}
	
	public int level() {
		return mLevel;
	}
	
	public int row() {
		return mRow;
	}

	public int col() {
		return mCol;
	}

	public double scale() {
		return mScale;
	}

	public double orientation() {
		return mOrientation;
	}

	public void setOrientation(double iOrientation) {
		this.mOrientation = iOrientation;
	}

	public void setMagnitude(double iMagnitude) {
		this.mMagnitude = iMagnitude;
	}
	
	public List<Double> vectorFeatures() {
		return mVectorFeatures;
	}
}
