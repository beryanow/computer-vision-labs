package ru.nsu.g.beryanov.model.sift.tools;

import java.util.List;

import Jama.Matrix;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import ru.nsu.g.beryanov.model.sift.image.KeyPointImage;

public class Tools {
    public static Mat changeSizeMat(final Mat matImage, int rowNumber, int colNumber) {
        Mat newImage = new Mat(rowNumber, colNumber, CvType.CV_8UC1);

        try {
            Imgproc.resize(matImage, newImage, new Size(rowNumber, colNumber));
        } catch (RuntimeException e) {
            newImage = null;
        }

        return newImage;
    }

    public static Mat fileToMat(String path) {
        return Imgcodecs.imread(path);
    }

    public static double[][] hessian(KeyPointImage keyPointImage, int row, int col) {
        double[][] hessianMatrix = new double[2][2];
        double[] derivative = derivative(keyPointImage, row, col);

        hessianMatrix[0][0] = derivative[0];
        hessianMatrix[0][1] = (keyPointImage.get(row + 1, col + 1) - keyPointImage.get(row - 1, col + 1) - keyPointImage.get(row + 1, col - 1) + keyPointImage.get(row - 1, col - 1)) / 2.;
        hessianMatrix[1][0] = (keyPointImage.get(row + 1, col + 1) - keyPointImage.get(row + 1, col - 1) - keyPointImage.get(row - 1, col + 1) + keyPointImage.get(row - 1, col - 1)) / 2.;
        hessianMatrix[1][1] = derivative[1];

        return hessianMatrix;
    }

    public static double[][] hessian(KeyPointImage keyPointImage, int row, int col, int scale) {
        double[][] hessianMatrix = new double[3][3];
        double[] derivative = derivative(keyPointImage, row, col, scale);

        hessianMatrix[0][0] = derivative[0];
        hessianMatrix[0][1] = (keyPointImage.get(row + 1, col + 1, scale) - keyPointImage.get(row - 1, col + 1, scale) - keyPointImage.get(row + 1, col - 1, scale) + keyPointImage.get(row - 1, col - 1, scale)) / 2.;
        hessianMatrix[0][2] = (keyPointImage.get(row + 1, col, scale + 1) - keyPointImage.get(row - 1, col, scale + 1) - keyPointImage.get(row + 1, col, scale - 1) + keyPointImage.get(row - 1, col, scale - 1)) / 2.;

        hessianMatrix[1][0] = (keyPointImage.get(row + 1, col + 1, scale) - keyPointImage.get(row + 1, col - 1, scale) - keyPointImage.get(row - 1, col + 1, scale) + keyPointImage.get(row - 1, col - 1, scale)) / 2.;
        hessianMatrix[1][1] = derivative[1];
        hessianMatrix[1][2] = (keyPointImage.get(row, col + 1, scale + 1) - keyPointImage.get(row, col - 1, scale + 1) - keyPointImage.get(row, col + 1, scale - 1) + keyPointImage.get(row, col - 1, scale - 1)) / 2.;

        hessianMatrix[2][0] = (keyPointImage.get(row + 1, col, scale + 1) - keyPointImage.get(row + 1, col, scale - 1) - keyPointImage.get(row - 1, col, scale + 1) + keyPointImage.get(row - 1, col, scale - 1)) / 2.;
        hessianMatrix[2][1] = (keyPointImage.get(row, col + 1, scale + 1) - keyPointImage.get(row, col + 1, scale - 1) - keyPointImage.get(row, col - 1, scale + 1) + keyPointImage.get(row, col - 1, scale - 1)) / 2.;
        hessianMatrix[2][2] = derivative[2];

        return hessianMatrix;
    }

    public static double[] derivative(KeyPointImage keyPointImage, int row, int col, int scale) {
        double[] lDerivative = new double[3];

        lDerivative[0] = (keyPointImage.get(row + 1, col, scale) + keyPointImage.get(row - 1, col, scale)) / 2.;
        lDerivative[1] = (keyPointImage.get(row, col + 1, scale) + keyPointImage.get(row, col - 1, scale)) / 2.;
        lDerivative[2] = (keyPointImage.get(row, col, scale + 1) + keyPointImage.get(row, col, scale - 1)) / 2.;

        return lDerivative;
    }

    public static double[] derivative(KeyPointImage keyPointImage, int row, int col) {
        double[] derivative = new double[2];

        derivative[0] = (keyPointImage.get(row + 1, col) + keyPointImage.get(row - 1, col)) / 2.;
        derivative[1] = (keyPointImage.get(row, col + 1) + keyPointImage.get(row, col - 1)) / 2.;

        return derivative;
    }

    public static double[][] multiplyMatrix(final double[][] a, final double[][] b) {
        int aRows = a.length;
        int aColumns = a[0].length;
        int bColumns = b[0].length;

        double[][] result = new double[aRows][bColumns];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                result[i][j] = 0.00000;
            }
        }

        for (int i = 0; i < aRows; ++i) {
            for (int j = 0; j < bColumns; ++j) {
                for (int k = 0; k < aColumns; ++k) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        return result;
    }

    public static double[] multiplyMatrix(final double[][] a, final double[] b) {
        int aRows = a.length;
        int aColumns = a[0].length;

        double[] result = new double[aRows];
        for (int i = 0; i < 2; i++) {
            result[i] = 0.00000;
        }

        for (int i = 0; i < aRows; ++i) {
            for (int k = 0; k < aColumns; ++k) {
                result[i] += a[i][k] * b[k];
            }
        }

        return result;
    }

    public static double[][] invertMatrix(final double[][] a) {
        Matrix matrix = new Matrix(a);
        matrix.inverse();

        return matrix.getArray();
    }

    public static double traceMatrix(final double[][] a) {
        double trace = 0;

        for (int i = 0; i < a.length; ++i) {
            trace += a[i][i];
        }

        return trace;
    }

    public static double detMatrix(final double[][] a) {
        Matrix matrix = new Matrix(a);

        return matrix.det();
    }

    public static boolean isInvertibleMatrix(final double[][] a) {
        Matrix lMat = new Matrix(a);

        return lMat.det() != 0;
    }

    public static double magnitude(final Mat image, int row, int col) {
        return Math.sqrt(Math.pow(image.get(row + 1, col)[0] - image.get(row - 1, col)[0], 2) + Math.pow(image.get(row, col + 1)[0] - image.get(row, col - 1)[0], 2));
    }

    public static double orientation(final Mat image, int row, int col) {
        return image.get(row, col + 1)[0] != image.get(row, col - 1)[0] ? positiveAngle(Math.toDegrees(Math.atan((image.get(row + 1, col)[0] - image.get(row - 1, col)[0]) / (image.get(row, col + 1)[0] - image.get(row, col - 1)[0])))) : 0;
    }

    public static double gaussian(double mean, double std, double value) {
        return (1 / (std * Math.sqrt(2 * Math.PI))) * Math.exp(-Math.pow((value - mean), 2) / (2 * Math.pow(std, 2)));
    }

    public static double distance(int xA, int yA, int xB, int yB) {
        return Math.sqrt(Math.pow(xA - xB, 2) + Math.pow(yA - yB, 2));
    }

    public static double norm(List<Double> vector) {
        double sum = 0;

        for (Double value : vector) {
            sum += Math.pow(value, 2);
        }

        return Math.sqrt(sum);
    }

    public static void normalizedVector(List<Double> vector) {
        double norm = norm(vector);
        for (int i = 0; i < vector.size(); ++i) {
            vector.set(i, vector.get(i) / norm);
        }
    }

    public static double upThreshold(double value, double threshold) {
        return Math.min(value, threshold);
    }

    public static double lowThreshold(double value, double threshold) {
        return Math.max(value, threshold);
    }

    public static double threshold(double value, double lowThreshold, double upThreshold) {
        return lowThreshold(upThreshold(value, upThreshold), lowThreshold);
    }

    public static void threshold(List<Double> vector, double lowTreshold, double upTreshold) {
        for (int i = 0; i < vector.size(); ++i)
            vector.set(i, threshold(vector.get(i), lowTreshold, upTreshold));
    }

    public static double positiveAngle(double angle) {
        return (360 + angle) % 360;
    }
}
