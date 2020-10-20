package ru.nsu.g.beryanov.model.sift.image;

import org.opencv.core.Mat;

public class KeyPointImage extends Image<KeyPoint> {
	private final int scale;
	private final double[][][] image;

	public KeyPointImage(int scale, Mat... iImages) {
		super(iImages[1]);
		this.scale = scale;
		image = new double[iImages[0].rows()][iImages[0].cols()][3];
		
		for (int scaleCount = 0; scaleCount < iImages.length; ++scaleCount) {
			Mat matImage = iImages[scaleCount];
			for (int row = 0; row < iImages[0].rows(); ++row) {
				for (int col = 0; col < iImages[0].cols(); ++col) {
					image[row][col][scaleCount] = matImage.get(row, col)[0];
				}
			}
		}
	}
	
	public KeyPointImage(double[][][] image, Mat matImage, int scale) {
		super(matImage);
		this.image = image;
		this.scale = scale;
	}
	
	@Override
	public KeyPointImage clone() {
		return new KeyPointImage(image, matImage, scale);
	}

	public double get(int row, int col, int scale) {
		if (scale == this.scale - 1)
			return image[row][col][0];

		if (scale == this.scale)
			return image[row][col][1];

		if (scale == this.scale + 1)
			return image[row][col][2];

		return -1.;
	}
	
	public double get(int row, int col) {
		return image[row][col][1];
	}
}
