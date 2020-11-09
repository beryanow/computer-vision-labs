package ru.nsu.g.beryanov.model.sift.tools;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Filter {
	public static Mat grayscale(final Mat refImage) {
		Mat result = new Mat(refImage.rows(), refImage.cols(), CvType.CV_8UC1);

		for (int i = 0; i < refImage.rows(); ++i) {
			for (int j = 0; j < refImage.cols(); ++j) {
				double blue = refImage.get(i, j)[0];
				double green = refImage.get(i, j)[1];
				double red  = refImage.get(i, j)[2];

				double gray = (red * 299 + green * 587 + blue * 114) / 1000;
				result.put(i, j, gray);
			}
		}

		return result;
	}
	
	public static Mat gaussian(final Mat refImage, final double sigma) {
		Mat result = new Mat(refImage.rows(), refImage.cols(), CvType.CV_8UC1);
		Imgproc.GaussianBlur(refImage, result, new Size(5, 5), sigma);

		return result;
	}
	
	public static Mat diffGaussian(final Mat firstImage, final Mat secondImage) {
		Mat result = new Mat(firstImage.rows(), firstImage.cols(), CvType.CV_8UC1);

		for (int i = 0; i < firstImage.rows(); ++i) {
			for (int j = 0; j < firstImage.cols(); ++j) {
				result.put(i, j, Math.abs(firstImage.get(i, j)[0] - secondImage.get(i, j)[0]));
			}
		}

		return result;
	}
}
