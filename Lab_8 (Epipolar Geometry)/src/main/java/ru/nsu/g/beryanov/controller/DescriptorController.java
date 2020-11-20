package ru.nsu.g.beryanov.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.exception.NotRelevantTemplateException;
import ru.nsu.g.beryanov.model.CornerInfo;
import ru.nsu.g.beryanov.model.sift.image.KeyPoint;
import ru.nsu.g.beryanov.model.sift.image.KeyPointImage;
import ru.nsu.g.beryanov.model.sift.tools.Filter;
import ru.nsu.g.beryanov.model.sift.tools.Tools;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Matrix {
    static float[][] inverseMatrix2(float[][] Matrix) {
        float[][] resultMatrix = new float[2][2];
        float determinant = Matrix[1][1] * Matrix[0][0] - Matrix[0][1] * Matrix[1][0];

        resultMatrix[0][0] = Matrix[1][1] / determinant;
        resultMatrix[1][1] = Matrix[0][0] / determinant;
        resultMatrix[1][0] = -Matrix[1][0] / determinant;
        resultMatrix[0][1] = -Matrix[0][1] / determinant;

        return resultMatrix;
    }
}

@Getter
@AllArgsConstructor
class ImageMatrix {
    float[][] color;
    private int width;
    private int height;
    int totalColors;

    float[][] spaceDerivative() {
        float[][] derivative = new float[this.getWidth() * this.getHeight()][2];

        int n = 0;
        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                if (x == 0) {
                    derivative[n][0] = (this.color[x + 1][y] - this.color[x][y]) / totalColors;
                } else if (x == this.getWidth() - 1) {
                    derivative[n][0] = (this.color[x][y] - this.color[x - 1][y]) / totalColors;
                } else {
                    derivative[n][0] = (this.color[x + 1][y] - this.color[x - 1][y]) / 2 / totalColors;
                }

                if (y == 0) {
                    derivative[n][1] = (this.color[x][y + 1] - this.color[x][y]) / totalColors;
                } else if (y == this.getHeight() - 1) {
                    derivative[n][1] = (this.color[x][y] - this.color[x][y - 1]) / totalColors;
                } else {
                    derivative[n][1] = (this.color[x][y + 1] - this.color[x][y - 1]) / 2 / totalColors;
                }

                n++;
            }
        }

        return derivative;
    }

    float[] timeDerivative(ImageMatrix image) {
        float[] derivative = new float[this.getWidth() * this.getHeight()];
        int n = 0;

        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                derivative[n] = -(image.color[x][y] - this.color[x][y]) / totalColors;
                n++;
            }
        }

        return derivative;
    }
}

class Image {
    static BufferedImage readImage(BufferedImage image, String Input_Path) {
        try {
            File input_file = new File(Input_Path);
            image = ImageIO.read(input_file);
            return image;
        } catch (IOException ignored) {}

        return image;
    }

    static BufferedImage convertToBlackWhite(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int p = image.getRGB(x, y);

                int a = (p >> 24) & 0xff;

                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int BW = (Max3(r, g, b) + Min3(r, g, b)) / 2;
                p = (a << 24) | (BW << 16) | (BW << 8) | BW;

                image.setRGB(x, y, p);
            }
        }

        return image;
    }

    public static int Max3(int a, int b, int c) {
        if (a >= b && a >= c)
            return a;
        else return Math.max(b, c);
    }

    public static int Min3(int a, int b, int c) {
        if (a <= b && a <= c)
            return a;
        else return Math.min(b, c);
    }
}

class Extractor {
    private final Mat image;
    private final int octaveNumber;
    private final int scaleNumber;
    private final double blurScale;
    private final Mat[][] loG;
    private final Mat[][] doGApprox;
    private final KeyPointImage[][] allKeyPointImages;
    private final KeyPointImage[][] optimaKeyPointImages;

    public Extractor(final Mat iImage) {
        octaveNumber = 1;
        scaleNumber = 5;
        blurScale = 1.6;

        image = Filter.grayscale(iImage);
        loG = new Mat[octaveNumber][scaleNumber];
        doGApprox = new Mat[octaveNumber][scaleNumber - 1];
        allKeyPointImages = new KeyPointImage[octaveNumber][scaleNumber - 3];
        optimaKeyPointImages = new KeyPointImage[octaveNumber][scaleNumber - 3];
    }

    public List<KeyPoint> processSIFTExtraction() {
        scaleSpace();
        doGApproximation();
        findKeyPoints();
        maximaKeyPoints();
        orientationAssigment();
        generateFeatures();

        List<KeyPoint> keyPoints = new ArrayList<>();
        for (int lOctave = 0; lOctave < octaveNumber; ++lOctave) {
            for (int lScale = 0; lScale < scaleNumber - 3; ++lScale) {
                KeyPointImage lKeyPointImage = optimaKeyPointImages[lOctave][lScale];
                keyPoints.addAll(lKeyPointImage.getRegionOfInterest());
            }
        }

        return keyPoints;
    }

    private void scaleSpace() {
        double lK = Math.pow(2, 1. / scaleNumber);
        int lPow = 0;

        for (int octave = 0; octave < octaveNumber; octave++) {
            Mat octaveImage = image;

            for (int octaveIterator = 0; octaveIterator < octave; ++octaveIterator) {
                octaveImage = Tools.changeSizeMat(octaveImage, octaveImage.cols() / 2, octaveImage.rows() / 2);
            }

            for (int lScale = 0; lScale < scaleNumber; ++lScale) {
                double lValue = Math.pow(lK, lPow++) * blurScale;
                Mat lBlurImage = Filter.gaussian(octaveImage, lValue);
                loG[octave][lScale] = lBlurImage;
            }
        }
    }

    private void doGApproximation() {
        for (int octave = 0; octave < octaveNumber; ++octave) {
            for (int scale = 0; scale < scaleNumber - 1; ++scale) {
                doGApprox[octave][scale] = Filter.diffGaussian(loG[octave][scale + 1], loG[octave][scale]);
            }
        }
    }

    private void findKeyPoints() {
        for (int octave = 0; octave < octaveNumber; ++octave) {
            for (int scale = 1; scale < scaleNumber - 2; ++scale) {
                Mat[] scales = new Mat[3];

                scales[0] = doGApprox[octave][scale - 1];
                scales[1] = doGApprox[octave][scale];
                scales[2] = doGApprox[octave][scale + 1];

                KeyPointImage keyPointImage = new KeyPointImage(scale, doGApprox[octave][scale - 1], doGApprox[octave][scale], doGApprox[octave][scale + 1]);
                allKeyPointImages[octave][scale - 1] = keyPointImage;

                for (int row = 1; row < scales[0].rows() - 1; ++row) {
                    for (int col = 1; col < scales[0].cols() - 1; ++col) {
                        int fewer = 0;
                        int bigger = 0;
                        double lRefValue = scales[1].get(row, col)[0];
                        boolean optima = false;
                        boolean breaking = false;
                        for (int scaleLevel = 0; scaleLevel < 3; ++scaleLevel) {
                            for (int convolRow = row - 1; convolRow <= row + 1; ++convolRow) {
                                for (int convolCol = col - 1; convolCol <= col + 1; ++convolCol) {
                                    if (!(convolRow == row && convolCol == col && scaleLevel == 1)) {
                                        double value = scales[scaleLevel].get(convolRow, convolCol)[0];

                                        fewer = value < lRefValue ? fewer + 1 : fewer;
                                        bigger = value > lRefValue ? bigger + 1 : bigger;

                                        if (fewer != 0 && bigger != 0) {
                                            breaking = true;
                                        }

                                        if (fewer == 26 || bigger == 26) {
                                            optima = true;
                                            breaking = true;
                                        }
                                    }
                                    if (breaking) break;
                                }
                                if (breaking) break;
                            }
                            if (breaking) break;
                        }

                        if (optima) {
                            keyPointImage.getRegionOfInterest().add(new KeyPoint(row, col, blurScale * scale, scale));
                        }
                    }
                }
            }
        }
    }

    private void maximaKeyPoints() {
        for (int octave = 0; octave < octaveNumber; ++octave) {
            for (int scale = 0; scale < scaleNumber - 3; ++scale) {
                KeyPointImage keyPointImage = allKeyPointImages[octave][scale];
                KeyPointImage optimaKeyPointImage = keyPointImage.clone();
                optimaKeyPointImages[octave][scale] = optimaKeyPointImage;

                for (KeyPoint keyPoint : keyPointImage.getRegionOfInterest()) {
                    int iterator = 0;
                    int refRow = keyPoint.row();
                    int refCol = keyPoint.col();
                    int refLevel = keyPoint.level();
                    double refScale = keyPoint.scale();

                    while (iterator < 25) {
                        if (refCol >= optimaKeyPointImage.getMatImage().cols() - 1 || refCol <= 0 || refRow >= optimaKeyPointImage.getMatImage().rows() - 1 || refRow <= 0) {
                            break;
                        }

                        double[][] lHessianMatrix3D = Tools.hessian(keyPointImage, refRow, refCol, refLevel);

                        if (Tools.isInvertibleMatrix(lHessianMatrix3D)) {
                            double[][] hessianInverseMatrix = Tools.invertMatrix(lHessianMatrix3D);
                            double[][] negativeIdentityMatrix = new double[][]{{-1, 0, 0}, {0, -1, 0}, {0, 0, -1}};
                            double[][] hessianNegativeMatrix = Tools.multiplyMatrix(hessianInverseMatrix, negativeIdentityMatrix);
                            double[] derivative = Tools.derivative(keyPointImage, refRow, refCol, refLevel);
                            double[] delta = Tools.multiplyMatrix(hessianNegativeMatrix, derivative);

                            double deltaRow = delta[0];
                            double deltaCol = delta[1];
                            double deltaScale = delta[2];

                            if (refRow + deltaRow < 0 || refCol + deltaCol < 0 || refScale + deltaScale < 0)
                                break;

                            refRow += deltaRow;
                            refCol += deltaCol;
                            refScale += deltaScale;

                            if (deltaRow < 0.5 && deltaCol < 0.5 && deltaScale < 0.5) {
                                break;
                            }
                        }
                        iterator++;
                    }

                    if (refCol >= optimaKeyPointImage.getMatImage().cols() - 1 || refCol <= 0 || refRow >= optimaKeyPointImage.getMatImage().rows() - 1 || refRow <= 0)
                        continue;

                    int threshold = 10;
                    double contrastThreshold = 0.03;
                    double[][] hessianMatrix2D = Tools.hessian(keyPointImage, refRow, refCol);

                    double trace = Tools.traceMatrix(hessianMatrix2D);
                    double det = Tools.detMatrix(hessianMatrix2D);
                    double curvatureRatio = Math.pow(trace, 2) / det;
                    double curvatureThreshold = Math.pow(threshold + 1, 2) / threshold;
                    double contrast = Tools.magnitude(optimaKeyPointImage.getMatImage(), refRow, refCol);

                    if (curvatureRatio >= curvatureThreshold)
                        continue;

                    if (contrast < contrastThreshold)
                        continue;

                    optimaKeyPointImage.getRegionOfInterest().add(new KeyPoint(refRow, refCol, refScale, refLevel));
                }
            }
        }
    }

    private void orientationAssigment() {
        for (int octave = 0; octave < octaveNumber; ++octave) {
            for (int scale = 0; scale < scaleNumber - 3; ++scale) {
                KeyPointImage optimaKeyPointImage = optimaKeyPointImages[octave][scale];
                Mat loGImage = loG[octave][scale];
                List<KeyPoint> newKeyPoints = new ArrayList<>();

                for (KeyPoint keyPoint : optimaKeyPointImage.getRegionOfInterest()) {
                    int sigma = (int) (1.5 * keyPoint.scale());
                    int highestBin = 0;
                    int secondHighestBin = 0;

                    double maxMagnitude = 0;
                    double secondMaxMagnitude = 0;

                    List<List<Double>> gradientHist = new ArrayList<>();
                    for (int bin = 0; bin < 36; ++bin) {
                        gradientHist.add(new ArrayList<>());
                    }

                    for (int row = (Math.max(keyPoint.row() - sigma, 1));
                         row <= (keyPoint.row() + sigma < optimaKeyPointImage.getMatImage().rows() - 1 ? keyPoint.row() + sigma : optimaKeyPointImage.getMatImage().rows() - 2); row++) {
                        for (int col = (Math.max(keyPoint.col() - sigma, 1)); col <= (keyPoint.col() + sigma < optimaKeyPointImage.getMatImage().cols() - 1 ? keyPoint.col() + sigma : optimaKeyPointImage.getMatImage().cols() - 2); col++) {
                            double magnitude = Tools.magnitude(loGImage, row, col);
                            double orientation = Tools.orientation(loGImage, row, col);

                            int histIndex = (int) (Math.round((orientation / 10.) - 0.5));
                            gradientHist.get(histIndex < 36 ? histIndex : 35).add(magnitude * Tools.gaussian(0, sigma, Tools.distance(keyPoint.row(), keyPoint.col(), row, col)));
                        }
                    }

                    for (int bin = 0; bin < 36; ++bin) {
                        List<Double> magnitudes = gradientHist.get(bin);
                        double amountMagnitude = 0;
                        for (Double magnitude : magnitudes) {
                            amountMagnitude += magnitude;
                        }

                        if (amountMagnitude > secondMaxMagnitude && amountMagnitude < maxMagnitude) {
                            secondMaxMagnitude = amountMagnitude;
                            secondHighestBin = bin;
                        }

                        if (amountMagnitude > maxMagnitude) {
                            if (amountMagnitude > secondMaxMagnitude) {
                                secondMaxMagnitude = maxMagnitude;
                                secondHighestBin = highestBin;
                            }

                            maxMagnitude = amountMagnitude;
                            highestBin = bin;
                        }
                    }

                    keyPoint.setOrientation(highestBin * 10);
                    keyPoint.setMagnitude(maxMagnitude);

                    if (secondMaxMagnitude / maxMagnitude >= 0.8)
                        newKeyPoints.add(new KeyPoint(keyPoint.row(), keyPoint.col(), keyPoint.scale(), secondHighestBin * 10, secondMaxMagnitude));
                }

                optimaKeyPointImage.getRegionOfInterest().addAll(newKeyPoints);
            }
        }
    }

    private void generateFeatures() {
        int keyPointMarge = 8;
        int subMarge = 4;
        for (int octave = 0; octave < octaveNumber; ++octave) {
            for (int scale = 0; scale < scaleNumber - 3; ++scale) {
                KeyPointImage optimaKeyPointImage = optimaKeyPointImages[octave][scale];
                Mat loGImage = loG[octave][scale];

                for (KeyPoint keyPoint : optimaKeyPointImage.getRegionOfInterest()) {
                    int sigma = (int) (1.5 * keyPoint.scale());

                    for (int row = keyPoint.row() - keyPointMarge; row < keyPoint.row() + keyPointMarge; row += 4) {
                        for (int col = keyPoint.col() - keyPointMarge; col < keyPoint.col() + keyPointMarge; col += 4) {
                            Double[] gradientHist = new Double[]{0., 0., 0., 0., 0., 0., 0., 0.};

                            for (int subRow = (row > 0 ? row : 1); subRow < (row + subMarge < optimaKeyPointImage.getMatImage().rows() - 1 ? row + subMarge : optimaKeyPointImage.getMatImage().rows() - 2); subRow++) {
                                for (int subCol = (col > 0 ? col : 1); subCol < (col + subMarge < optimaKeyPointImage.getMatImage().cols() - 1 ? col + subMarge : optimaKeyPointImage.getMatImage().cols() - 2); subCol++) {
                                    double magnitude = Tools.magnitude(loGImage, subRow, subCol);
                                    double orientation = Tools.positiveAngle(Tools.orientation(loGImage, subRow, subCol) - keyPoint.orientation());

                                    int histIndex = (int) (Math.round((orientation / 45.) - 0.5));
                                    gradientHist[histIndex < 8 ? histIndex : 7] += magnitude * Tools.gaussian(0, sigma, Tools.distance(keyPoint.row(), keyPoint.col(), subRow, subCol));
                                }
                            }

                            for (int bin = 0; bin < 8; ++bin) {
                                keyPoint.vectorFeatures().add(gradientHist[bin]);
                            }
                        }
                    }

                    Tools.normalizedVector(keyPoint.vectorFeatures());
                    Tools.threshold(keyPoint.vectorFeatures(), 0, 0.2);
                    Tools.normalizedVector(keyPoint.vectorFeatures());
                }
            }
        }
    }
}

@Service
public class DescriptorController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    private FilterController filterController;

    @Autowired
    private HistogramController histogramController;

    private void highlightPixel(int x, int y) {
        int size = 7;
        for (int i = x - size / 2; i < x + size / 2; i++) {
            for (int j = y - size / 2; j < y + size / 2; j++) {
                try {
                    imagePanel.getImage().setRGB(i, j, Color.RED.getRGB());
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }
        }
    }

    private void outlinePixelSquare(int x, int y, int diameter) {
        for (int i = x - diameter / 2; i <= x + diameter / 2; i++) {
            for (int j = y - diameter / 2; j <= y + diameter / 2; j++) {
                if (i == x - diameter / 2 || j == y - diameter / 2 || i == x + diameter / 2 || j == y + diameter / 2) {
                    try {
                        imagePanel.getImage().setRGB(i, j, Color.RED.getRGB());
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                }
            }
        }
    }

    private void outlinePixelTriangle(int x, int y, int size) {
        try {
            for (int i = 0; i < size / 2; i++) {
                if (i == 0) {
                    for (int j = (int) (size * 0.4); j < size * 1.4; j += 2) {
                        imagePanel.getImage().setRGB(j + y, (int) (i + x - size * 1.4), Color.RED.getRGB());
                    }
                }
                imagePanel.getImage().setRGB((int) (i + size * 0.4 + y), (int) ((int) (2.1 * i) + x - size * 1.4), Color.RED.getRGB());
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        try {
            for (int i = size - 1; i >= size / 2; i--) {
                imagePanel.getImage().setRGB((int) (i + size * 0.4 + y), (int) ((int) (2.1 * (size - 1 - i)) + x - size * 1.4), Color.RED.getRGB());
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    private void outlinePixelCircle(int x, int y, int diameter) {
        int r = diameter / 2;
        for (double i = 0; i <= 2 * Math.PI; i += Math.PI / 16) {
            double xCircle = Math.sin(i) * r;
            double yCircle = Math.cos(i) * r;
            try {
                imagePanel.getImage().setRGB((int) (xCircle + x), (int) (yCircle + y), Color.RED.getRGB());
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public double[][] getGaussianMatrix(int size, double sigma) {
        double[][] gaussianMatrix = new double[size][size];
        for (int i = -size / 2; i <= size / 2; i++) {
            for (int j = -size / 2; j <= size / 2; j++) {
                gaussianMatrix[i + size / 2][j + size / 2] = 1.0 / (2 * Math.PI * Math.pow(sigma, 2)) * Math.exp(-((Math.pow(i, 2) + Math.pow(j, 2)) / (2 * Math.pow(sigma, 2))));
            }
        }
        return gaussianMatrix;
    }

    public double[][] getSingularMatrix(int size) {
        double[][] singularMatrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                singularMatrix[i][j] = 1.0;
            }
        }
        return singularMatrix;
    }

    public void processShift(int shiftX, int shiftY, int matrixSize, ArrayList<Double> intensityChange) {
        double intensity = 0;
        for (int i = shiftX - matrixSize / 2; i < shiftX + matrixSize / 2; i++) {
            for (int j = shiftY - matrixSize / 2; j < shiftY + matrixSize / 2; j++) {
                Color colorShifted;
                Color colorOriginal;

                try {
                    colorShifted = new Color(imagePanel.getImage().getRGB(i, j));
                    colorOriginal = new Color(imagePanel.getImage().getRGB(i - shiftX, j - shiftY));
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }

                double grayscaleValueShifted = ComputerVisionUtility.evaluateHSV(colorShifted.getRed(), colorShifted.getGreen(), colorShifted.getBlue())[2];
                double grayscaleValueOriginal = ComputerVisionUtility.evaluateHSV(colorOriginal.getRed(), colorOriginal.getGreen(), colorOriginal.getBlue())[2];

                intensity += Math.pow(grayscaleValueShifted - grayscaleValueOriginal, 2);
            }
        }
        intensityChange.add(intensity);
    }

    private CornerInfo[][] nonMaximumSuppression(CornerInfo[][] corners, int matrixSize) {
        CornerInfo[][] suppressedCorners = new CornerInfo[corners.length][corners[0].length];

        for (int i = 0; i < corners.length; i++) {
            for (int j = 0; j < corners[0].length; j++) {
                suppressedCorners[i][j] = new CornerInfo();
                suppressedCorners[i][j].setIntensity(corners[i][j].getIntensity());

                int direction = corners[i][j].getDirection();
                double intensity = corners[i][j].getIntensity();

                for (int k = -matrixSize / 2; k <= matrixSize / 2; k++) {
                    for (int m = -matrixSize / 2; m <= matrixSize / 2; m++) {
                        try {
                            if (corners[i + k][j + m].getDirection() == direction) {
                                if (intensity < corners[i + k][j + m].getIntensity()) {
                                    suppressedCorners[i][j].setIntensity(0);
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                        }
                    }
                }
            }
        }

        return suppressedCorners;
    }

    public void processMoravecDetector(int threshold) {
        CornerInfo[][] corners = new CornerInfo[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        int matrixSize = 3;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                ArrayList<Double> intensityChange = new ArrayList<>();
                for (int shiftX = -1; shiftX <= 1; shiftX++) {
                    for (int shiftY = -1; shiftY <= 1; shiftY++) {
                        if (!(shiftX == shiftY && shiftX == 0)) {
                            processShift(i + shiftX, j + shiftY, matrixSize, intensityChange);
                        }
                    }
                }

                double minIntensity = Integer.MAX_VALUE;
                int direction = 0;
                for (int k = 0; k < 8; k++) {
                    if (intensityChange.get(k) < minIntensity) {
                        minIntensity = intensityChange.get(k);
                        direction = k;
                    }
                }

                corners[i][j] = new CornerInfo();
                corners[i][j].setIntensity(minIntensity);
                corners[i][j].setDirection(direction);

                if (minIntensity < threshold) {
                    corners[i][j].setIntensity(0);
                }
            }
        }

        CornerInfo[][] suppressedCorners = nonMaximumSuppression(corners, matrixSize);

        for (int i = 0; i < corners.length; i++) {
            for (int j = 0; j < corners[0].length; j++) {
                if (suppressedCorners[i][j].getIntensity() > 0) {
                    highlightPixel(i, j);
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processHarrisDetector(int top) {
        int matrixSize = 7;
        double[][] gaussianMatrix = getGaussianMatrix(matrixSize, 1);
//        double[][] singularMatrix = getSingularMatrix(matrixSize);
        double[][] sobelFilterXMatrix = ComputerVisionUtility.getSobelFilterX7();
        double[][] sobelFilterYMatrix = ComputerVisionUtility.getSobelFilterY7();

        double[][] RMatrix = new double[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        ArrayList<Double> RValues = new ArrayList<>();
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                double[][] MMatrix = new double[2][2];
                double Ix = 0;
                double Iy = 0;
                for (int k = i - matrixSize / 2; k <= i + matrixSize / 2; k++) {
                    for (int l = j - matrixSize / 2; l <= j + matrixSize / 2; l++) {
                        try {
                            int pixelColorRGB = imagePanel.getImage().getRGB(k, l);
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            Ix += (100.0 - HSV[2]) / 100 * sobelFilterXMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                            Iy += (100.0 - HSV[2]) / 100 * sobelFilterYMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                            int pixelColorRGB = imagePanel.getImage().getRGB(i, j);
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            Ix += (100.0 - HSV[2]) / 100 * sobelFilterXMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                            Iy += (100.0 - HSV[2]) / 100 * sobelFilterYMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                        }
                    }
                }

                for (int k = 0; k < matrixSize; k++) {
                    for (int l = 0; l < matrixSize; l++) {
                        MMatrix[0][0] += gaussianMatrix[k][l] * Math.pow(Ix, 2);
                        MMatrix[1][0] += gaussianMatrix[k][l] * Ix * Iy;
                        MMatrix[0][1] += gaussianMatrix[k][l] * Ix * Iy;
                        MMatrix[1][1] += gaussianMatrix[k][l] * Math.pow(Iy, 2);
                    }
                }

                double determinantM = MMatrix[0][0] * MMatrix[1][1] - MMatrix[0][1] * MMatrix[1][0];
                double k = 4.0E-17;
                double traceM = MMatrix[0][0] + MMatrix[1][1];

                double R = determinantM - k * Math.pow(traceM, 2);
                RMatrix[i][j] = R;
                RValues.add(R);
            }
        }

        RValues.sort(Collections.reverseOrder());
        RValues = new ArrayList<>(RValues.subList(0, top));

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (RMatrix[i][j] > 0 && RValues.contains(RMatrix[i][j])) {
                    highlightPixel(i, j);
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processForstnerDetector(int top) {
        int matrixSize = 7;
        double[][] gaussianMatrix = getGaussianMatrix(matrixSize, 1);
//        double[][] singularMatrix = getSingularMatrix(matrixSize);
        double[][] sobelFilterXMatrix = ComputerVisionUtility.getSobelFilterX7();
        double[][] sobelFilterYMatrix = ComputerVisionUtility.getSobelFilterY7();

        double[][] RMatrix = new double[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        ArrayList<Double> RValues = new ArrayList<>();
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                double[][] MMatrix = new double[2][2];
                double Ix = 0;
                double Iy = 0;
                for (int k = i - matrixSize / 2; k <= i + matrixSize / 2; k++) {
                    for (int l = j - matrixSize / 2; l <= j + matrixSize / 2; l++) {
                        try {
                            int pixelColorRGB = imagePanel.getImage().getRGB(k, l);
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            Ix += (100.0 - HSV[2]) / 100 * sobelFilterXMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                            Iy += (100.0 - HSV[2]) / 100 * sobelFilterYMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                            int pixelColorRGB = imagePanel.getImage().getRGB(i, j);
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            Ix += (100.0 - HSV[2]) / 100 * sobelFilterXMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                            Iy += (100.0 - HSV[2]) / 100 * sobelFilterYMatrix[k - i + matrixSize / 2][l - j + matrixSize / 2];
                        }
                    }
                }

                for (int k = 0; k < matrixSize; k++) {
                    for (int l = 0; l < matrixSize; l++) {
                        MMatrix[0][0] += gaussianMatrix[k][l] * Math.pow(Ix, 2);
                        MMatrix[1][0] += gaussianMatrix[k][l] * Ix * Iy;
                        MMatrix[0][1] += gaussianMatrix[k][l] * Ix * Iy;
                        MMatrix[1][1] += gaussianMatrix[k][l] * Math.pow(Iy, 2);
                    }
                }

                double determinantM = MMatrix[0][0] * MMatrix[1][1] - MMatrix[0][1] * MMatrix[1][0];
                double traceM = MMatrix[0][0] + MMatrix[1][1];

                double R = determinantM / traceM;
                double cornerRoundness = 4 * determinantM / Math.pow(traceM, 2);

                if (!Double.isNaN(R) && R > 0 && cornerRoundness > 2E-16) {
                    RMatrix[i][j] = R;
                    RValues.add(R);
                }
            }
        }

        RValues.sort(Collections.reverseOrder());
        RValues = new ArrayList<>(RValues.subList(0, top));

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (RMatrix[i][j] > 0 && RValues.contains(RMatrix[i][j])) {
                    highlightPixel(i, j);
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processSift() {
        OpenCV.loadLocally();
        Mat lFirstImage = Tools.fileToMat("src/main/resources/" + ComputerVisionUtility.imageName + ".jpg");

        Extractor lFirstExtractor = new Extractor(lFirstImage);
        List<KeyPoint> lFirstImageFeatures = lFirstExtractor.processSIFTExtraction();

        for (KeyPoint keyPoint : lFirstImageFeatures) {
            int width = keyPoint.col();
            int height = keyPoint.row();
            highlightPixel(width, height);
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public int[][] getSquare(int diameter) {
        int[][] square = new int[diameter][diameter];

        for (int i = 0; i < diameter; i++) {
            for (int j = 0; j < diameter; j++) {
                if (i == 0 || j == 0 || i == diameter - 1 || j == diameter - 1) {
                    square[i][j] = 1;
                } else {
                    square[i][j] = 0;
                }
            }
        }

        return square;
    }

    public int[][] getTriangle(int size) {
        int[][] triangle = new int[size][size];

        try {
            for (int i = 0; i < size; i++) {
                if (i == 0) {
                    for (int j = 0; j < size; j += 2) {
                        triangle[i][j] = 1;
                    }
                }
                triangle[(int) (2.1 * i)][i] = 1;
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        try {
            for (int i = size - 1; i >= 0; i--) {
                triangle[(int) (2.1 * (size - 1 - i))][i] = 1;
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        return triangle;
    }

    public int[][] getCircle(int diameter) {
        int[][] circle = new int[diameter][diameter];

        int r = diameter / 2;
        for (double i = 0; i <= 2 * Math.PI; i += Math.PI / 16) {
            double x = Math.sin(i) * r + r;
            double y = Math.cos(i) * r + r;
            circle[(int) x][(int) y] = 1;
        }

        return circle;
    }

    public int countRelevant(int i, int j, int radius, int[][] image) {
        int count = 0;

        for (int k = -radius / 2; k < radius / 2; k++) {
            for (int m = -radius / 2; m < radius / 2; m++) {
                try {
                    if (image[k + i][m + j] == 1) {
                        count++;
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            }
        }

        if (count == 0) {
            throw new NotRelevantTemplateException();
        }

        return count;
    }

    public void processRANSAC() {
        int[][] rgb = filterController.processSobelFilter();

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color color = new Color(rgb[i][j]);
                if (ComputerVisionUtility.evaluateHSV(color.getRed(), color.getGreen(), color.getBlue())[2] >= 20) {
                    rgb[i][j] = 1;
                } else {
                    rgb[i][j] = 0;
                }
            }
        }

//        int diameters[] = new int[]{35}; // for square
//        int diameters[] = new int[]{29, 35}; // for circle
        int diameters[] = new int[]{23}; // for triangle
        int[][] bestRelevant = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int diameter : diameters) {
            int[][] template = getTriangle(diameter);
//            int[][] template = getCircle(diameter);
//            int[][] template = getSquare(diameter);

            for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
                for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                    int relevantAmount = 0;

                    try {
                        for (int k = -diameter / 2; k <= diameter / 2; k++) {
                            for (int m = -diameter / 2; m <= diameter / 2; m++) {
                                if (template[k + diameter / 2][m + diameter / 2] == 1) {
                                    relevantAmount += countRelevant(i + k, j + m, 7, rgb);
                                }
                            }
                        }

                        if (bestRelevant[i][j] < relevantAmount) {
                            bestRelevant[i][j] = relevantAmount;
                        }
                    } catch (NotRelevantTemplateException ignored) {
                    }
                }
            }

            ArrayList<Integer> relevants = new ArrayList<>();
            for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
                for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                    relevants.add(bestRelevant[i][j]);
                }
            }

            relevants.sort(Collections.reverseOrder());

            for (int k = 0; k < 1; k++) {
                int relevant = relevants.get(k);
                for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
                    for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                        if (bestRelevant[i][j] == relevant) {
//                            outlinePixelCircle(i, j, diameter);
                            outlinePixelTriangle(i, j, diameter);
//                            outlinePixelSquare(i, j, diameter);
                        }
                    }
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processHoughTransform() {
        int[][] rgb = filterController.processSobelFilter();

        int maxRadius = ComputerVisionUtility.imageSize;
        int voting[][][] = new int[ComputerVisionUtility.imageSize - 1][ComputerVisionUtility.imageSize - 1][maxRadius];

        int minimumRadius = 10;
        int thetaIncrement = 2;

        for (int i = 1; i < ComputerVisionUtility.imageSize - 1; i++) {
            for (int j = 1; j < ComputerVisionUtility.imageSize - 1; j++) {
                Color color = new Color(rgb[j][i]);
                if (ComputerVisionUtility.evaluateHSV(color.getRed(), color.getGreen(), color.getBlue())[2] >= 20) {
                    for (int radius = minimumRadius; radius < maxRadius; radius++) {
                        for (int theta = 0; theta < 360; theta += thetaIncrement) {
                            int a = (int) (j - radius * Math.cos(theta * Math.PI / 180));
                            int b = (int) (i - radius * Math.sin(theta * Math.PI / 180));

                            if (b < ComputerVisionUtility.imageSize - 1 && a < ComputerVisionUtility.imageSize - 1 && b >= 0 && a >= 0) {
                                voting[a][b][radius] += 1;
                            }
                        }
                    }
                }
            }
        }

        int maxVote = 0;
        int votingThreshold = 171;
        for (int i = 1; i < ComputerVisionUtility.imageSize - 1; i++) {
            for (int j = 1; j < ComputerVisionUtility.imageSize - 1; j++) {
                for (int radius = minimumRadius; radius < maxRadius; radius++) {
                    if (voting[j][i][radius] > votingThreshold) {
                        if (voting[j][i][radius] > maxVote) {
                            maxVote = voting[j][i][radius];
                        }

                        for (int theta = 0; theta < 360; theta += 1) {
                            int a = (int) (j - radius * Math.cos(theta * Math.PI / 180));
                            int b = (int) (i - radius * Math.sin(theta * Math.PI / 180));

                            if (b < ComputerVisionUtility.imageSize - 1 && a < ComputerVisionUtility.imageSize - 1 && b >= 0 && a >= 0) {
                                imagePanel.getImage().setRGB(a, b, Color.RED.getRGB());
                            }
                        }
                    }
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processLucasKanadeMethod() {
        BufferedImage image1 = null;
        BufferedImage image2 = null;

        image1 = Image.readImage(image1, "src/main/resources/motion/porshe1.png");
        image2 = Image.readImage(image2, "src/main/resources/motion/porshe2.png");

        int width = image1.getWidth();
        int height = image1.getHeight();

        Image.convertToBlackWhite(image1);
        float[][] color = getColorMatrix(image1);
        ImageMatrix img1 = new ImageMatrix(color, width, height, 255);

        Image.convertToBlackWhite(image2);
        color = getColorMatrix(image2);
        ImageMatrix img2 = new ImageMatrix(color, width, height, 255);

        int pixelsAmount = width * height;
        float[][] Ixy = img1.spaceDerivative();
        float[] v = new float[2];
        float[] It = img1.timeDerivative(img2);

        float[][] AtA = {{0, 0}, {0, 0}};
        float[] Atb = {0, 0};

        int streamWidth = 20;
        int streamHeight = 20;
        for (int streamX = width / streamWidth; streamX < width - width / streamWidth; streamX += width / streamWidth)
            for (int streamY = height / streamHeight; streamY < height - height / streamHeight; streamY += height / streamHeight) {
                for (int i = 0; i < pixelsAmount; i++) {
                    int y = i % height;
                    int x = i / height;

                    float weight = gaussianFunction(Math.sqrt((streamX - x) * (streamX - x) + (streamY - y) * (streamY - y)));

                    AtA[0][0] += weight * Ixy[i][0] * Ixy[i][0];
                    AtA[0][1] += weight * Ixy[i][0] * Ixy[i][1];
                    AtA[1][1] += weight * Ixy[i][1] * Ixy[i][1];

                    Atb[0] += weight * It[i] * Ixy[i][0];
                    Atb[1] += weight * It[i] * Ixy[i][1];
                }

                AtA[1][0] = AtA[0][1];

                float[][] AtAi = Matrix.inverseMatrix2(AtA);
                v[0] = AtAi[0][0] * Atb[0] + AtAi[0][1] * Atb[1];
                v[1] = AtAi[1][0] * Atb[0] + AtAi[1][1] * Atb[1];

                int multiplierForArrows = 100;
                if (v[0] > v[1]) {
                    for (int i = 0; i < v[0] * multiplierForArrows; i++) {
                        try {
                            image1.setRGB(streamX + i, (int) (streamY + i * v[1] / v[0]), Color.RED.getRGB());
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                        }
                    }
                } else {
                    for (int i = 0; i < v[1] * multiplierForArrows; i++) {
                        try {
                            image1.setRGB((int) (streamX + i * v[0] / v[1]), streamY + i, Color.RED.getRGB());
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                        }
                    }
                }
                image1.setRGB(streamX, streamY, Color.GREEN.getRGB());
            }

        for (int i = 110; i < 790; i++) {
            for (int j = 110; j < 790; j++) {
                imagePanel.getImage().setRGB(i - 110, j - 110, image1.getRGB(i, j));
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public float[][] getColorMatrix(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        float[][] color = new float[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int p = image.getRGB(x, y);
                int BW = (p >> 16) & 0xff;
                color[x][y] = BW;
            }
        }

        return color;
    }

    public float gaussianFunction(double d) {
        double sigma = 2;

        return (float) ((float) Math.exp(-d / (2 * sigma * sigma)) / Math.sqrt(2 * Math.PI * sigma * sigma));
    }
}
