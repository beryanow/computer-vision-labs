package ru.nsu.g.beryanov.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.ImagePanel;
import ru.nsu.g.beryanov.model.ValueThreshold;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

@Service
public class FilterController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    @Qualifier("thresholdHSlider")
    private JSlider thresholdHSlider;

    @Autowired
    @Qualifier("thresholdNSlider")
    private JSlider thresholdNSlider;

    @Autowired
    @Qualifier("kernelSizeSlider")
    private JSlider kernelSizeSlider;

    @Autowired
    @Qualifier("weightSlider")
    private JSlider weightSlider;

    @Autowired
    private HistogramController histogramController;

    @Autowired
    private ImageController imageController;

    public boolean isForeground(double k, int pixel, long sum, long squareSum, int count) {
        double k2 = k * k / 16384;

        double mean = ((double) sum) / count;
        double variance = ((double) squareSum) / count - mean * mean;
        double tmp = (pixel + mean * (k - 1));

        if (k >= 0) {
            return tmp <= 0 || tmp * tmp <= mean * mean * k2 * variance;
        } else {
            return tmp <= 0 && tmp * tmp >= mean * mean * k2 * variance;
        }
    }

    public void binarize(byte[] from, byte[] to, int kernelSize, double weight) {
        int[] integral = new int[ComputerVisionUtility.imageSize];
        int[] integralSquare = new int[ComputerVisionUtility.imageSize];

        int l = (kernelSize + 1) / 2;
        int r = kernelSize / 2;
        int o = (kernelSize + 1) / 2;
        int u = kernelSize / 2;

        for (int i = 0, index = 0, imax = Math.min(ComputerVisionUtility.imageSize, u); i < imax; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++, index++) {
                int pixel = (from[index]) & 0xFF;
                integral[j] += pixel;
                integralSquare[j] += pixel * pixel;
            }
        }

        int dr0 = Math.min(kernelSize, ComputerVisionUtility.imageSize);
        int dr1 = Math.min(r, ComputerVisionUtility.imageSize);
        int dr2 = ComputerVisionUtility.imageSize - l;
        int dr3 = Math.min(dr2, 0);

        int indexTop = 0, indexBottom = u * ComputerVisionUtility.imageSize;
        for (int winTop = -o, winBottom = u, index = 0, bottomMax = ComputerVisionUtility.imageSize + u; winBottom < bottomMax; winTop++, winBottom++) {
            int top, bottom;

            if (winTop >= 0) {
                for (int j = 0; j < ComputerVisionUtility.imageSize; j++, indexTop++) {
                    int pixel = from[indexTop] & 0xFF;
                    integral[j] -= pixel;
                    integralSquare[j] -= pixel * pixel;
                }
                top = winTop;
            } else {
                top = -1;
            }

            if (winBottom < ComputerVisionUtility.imageSize) {
                for (int j = 0; j < ComputerVisionUtility.imageSize; j++, indexBottom++) {
                    int pixel = (from[indexBottom]) & 0xFF;
                    integral[j] += pixel;
                    integralSquare[j] += pixel * pixel;
                }
                bottom = winBottom;
            } else {
                bottom = ComputerVisionUtility.imageSize - 1;
            }

            int winHeight = bottom - top;
            int sum = 0;
            long squareSum = 0;

            for (int j = 0; j < dr1; j++) {
                sum += integral[j];
                squareSum += integralSquare[j];
            }

            int count = dr1 * winHeight;
            int winRight = r;

            for (; winRight < dr0; winRight++, index++) {
                count += winHeight;
                sum += integral[winRight];
                squareSum += integralSquare[winRight];
                to[index] = isForeground(weight, from[index] & 0xFF, sum, squareSum, count) ? (byte) 0x00 : (byte) 0xff;
            }

            int winLeft = winRight - kernelSize;
            if (winRight >= ComputerVisionUtility.imageSize) {
                for (; winLeft < dr3; winLeft++, index++) {
                    to[index] = isForeground(weight, from[index] & 0xFF, sum, squareSum, count) ? (byte) 0x00 : (byte) 0xff;
                }
            } else {
                for (; winRight < ComputerVisionUtility.imageSize; winLeft++, winRight++, index++) {
                    sum += integral[winRight] - integral[winLeft];
                    squareSum += integralSquare[winRight] - integralSquare[winLeft];
                    to[index] = isForeground(weight, from[index] & 0xFF, sum, squareSum, count) ? (byte) 0x00 : (byte) 0xff;
                }
            }

            for (; winLeft < dr2; winLeft++, index++) {
                count -= winHeight;
                sum -= integral[winLeft];
                squareSum -= integralSquare[winLeft];
                to[index] = isForeground(weight, from[index] & 0xFF, sum, squareSum, count) ? (byte) 0x00 : (byte) 0xff;
            }
        }
    }

    public void processNiblackSauvolaMethod() {
        int width = imagePanel.getImage().getWidth();
        int height = imagePanel.getImage().getHeight();

        BufferedImage gray = new BufferedImage(imagePanel.getImage().getWidth(), imagePanel.getImage().getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        for (int i = 0; i < imagePanel.getImage().getWidth(); i++) {
            for (int j = 0; j < imagePanel.getImage().getHeight(); j++) {
                Color pixelColor = new Color(imagePanel.getImage().getRGB(i, j));
                int color = (int) (0.2125 * pixelColor.getRed() + 0.7154 * pixelColor.getGreen() + 0.0721 * pixelColor.getBlue());
                gray.setRGB(i, j, new Color(color, color, color).getRGB());
            }
        }

        byte[] from = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        byte[] to = ((DataBufferByte) output.getRaster().getDataBuffer()).getData();

        binarize(from, to, kernelSizeSlider.getValue(), weightSlider.getValue() / 100.0);

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int pixelColorRGB = output.getRGB(i, j);
                imagePanel.getImage().setRGB(i, j, pixelColorRGB);
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processOtsuMethod() {
        Color pixelColor = new Color(imagePanel.getImage().getRGB(0, 0));
        int min = (int) (0.2125 * pixelColor.getRed() + 0.7154 * pixelColor.getGreen() + 0.0721 * pixelColor.getBlue());
        int max = min;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                pixelColor = new Color(imagePanel.getImage().getRGB(i, j));

                int color = (int) (0.2125 * pixelColor.getRed() + 0.7154 * pixelColor.getGreen() + 0.0721 * pixelColor.getBlue());
                if (color < min) min = color;
                if (color > max) max = color;
            }
        }

        int histSize = max - min + 1;
        int[] histogram = new int[histSize];

        for (int i = 0; i < histSize; i++) {
            histogram[i] = 0;
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                pixelColor = new Color(imagePanel.getImage().getRGB(i, j));
                int color = (int) (0.2125 * pixelColor.getRed() + 0.7154 * pixelColor.getGreen() + 0.0721 * pixelColor.getBlue());
                histogram[color - min]++;
            }
        }

        int m = 0;
        int n = 0;

        for (int i = 0; i <= max - min; i++) {
            m += i * histogram[i];
            n += histogram[i];
        }

        float maxSigma = -1;
        int threshold = 0;

        int alpha1 = 0;
        int beta1 = 0;

        for (int i = 0; i < max - min; i++) {
            alpha1 += i * histogram[i];
            beta1 += histogram[i];

            float w1 = (float) beta1 / n;
            float a = (float) alpha1 / beta1 - (float) (m - alpha1) / (n - beta1);
            float sigma = w1 * (1 - w1) * a * a;

            if (sigma > maxSigma) {
                maxSigma = sigma;
                threshold = i;
            }
        }

        threshold += min;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                pixelColor = new Color(imagePanel.getImage().getRGB(i, j));
                int color = (int) (0.2125 * pixelColor.getRed() + 0.7154 * pixelColor.getGreen() + 0.0721 * pixelColor.getBlue());

                if (color <= threshold) {
                    imagePanel.getImage().setRGB(i, j, Color.BLACK.getRGB());
                } else {
                    imagePanel.getImage().setRGB(i, j, Color.WHITE.getRGB());
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processGaborFilter() {
        int filterSize = 7;
        double[] thetas = new double[]{0, 45, 90, 135};

        double gamma = 0.1f;
        double lambda = 3.0f;
        double fi = 0;
        double sigma = 0.56f * lambda;
        double F = 1 / lambda;

        double[][] gaborMatrix0 = new double[filterSize][filterSize];
        double[][] gaborMatrix45 = new double[filterSize][filterSize];
        double[][] gaborMatrix90 = new double[filterSize][filterSize];
        double[][] gaborMatrix135 = new double[filterSize][filterSize];

        for (int m = 0; m < 4; m++) {
            for (int i = -filterSize / 2; i <= filterSize / 2; i++) {
                for (int j = -filterSize / 2; j <= filterSize / 2; j++) {
                    double rotatedX = i * Math.cos(thetas[m] / 180 * Math.PI) + j * Math.sin(thetas[m] / 180 * Math.PI);
                    double rotatedY = -i * Math.sin(thetas[m] / 180 * Math.PI) + j * Math.cos(thetas[m] / 180 * Math.PI);
                    switch (m) {
                        case 0:
                            gaborMatrix0[i + filterSize / 2][j + filterSize / 2] = Math.exp(-1.0 / 2 * (Math.pow(rotatedX, 2) + Math.pow(gamma, 2) * Math.pow(rotatedY, 2)) / Math.pow(sigma, 2)) * Math.cos(2 * Math.PI * F * rotatedX + fi);
                            break;
                        case 1:
                            gaborMatrix45[i + filterSize / 2][j + filterSize / 2] = Math.exp(-1.0 / 2 * (Math.pow(rotatedX, 2) + Math.pow(gamma, 2) * Math.pow(rotatedY, 2)) / Math.pow(sigma, 2)) * Math.cos(2 * Math.PI * F * rotatedX + fi);
                            break;
                        case 2:
                            gaborMatrix90[i + filterSize / 2][j + filterSize / 2] = Math.exp(-1.0 / 2 * (Math.pow(rotatedX, 2) + Math.pow(gamma, 2) * Math.pow(rotatedY, 2)) / Math.pow(sigma, 2)) * Math.cos(2 * Math.PI * F * rotatedX + fi);
                            break;
                        case 3:
                            gaborMatrix135[i + filterSize / 2][j + filterSize / 2] = Math.exp(-1.0 / 2 * (Math.pow(rotatedX, 2) + Math.pow(gamma, 2) * Math.pow(rotatedY, 2)) / Math.pow(sigma, 2)) * Math.cos(2 * Math.PI * F * rotatedX + fi);
                            break;
                    }
                }
            }
        }

        double[][] gaborValues = new double[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                double gaborValue = 0;
                for (int k = 0; k < filterSize; k++) {
                    for (int l = 0; l < filterSize; l++) {
                        try {
                            Color color = new Color(imagePanel.getImage().getRGB(i + k - filterSize / 2, j + l - filterSize / 2));

                            int red = color.getRed();
                            int green = color.getGreen();
                            int blue = color.getBlue();

                            double value = ComputerVisionUtility.evaluateHSV(red, green, blue)[2];

                            gaborValue += gaborMatrix0[k][l] * value;
                            gaborValue += gaborMatrix45[k][l] * value;
                            gaborValue += gaborMatrix90[k][l] * value;
                            gaborValue += gaborMatrix135[k][l] * value;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Color color = new Color(imagePanel.getImage().getRGB(i, j));

                            int red = color.getRed();
                            int green = color.getGreen();
                            int blue = color.getBlue();

                            double value = ComputerVisionUtility.evaluateHSV(red, green, blue)[2];

                            gaborValue += gaborMatrix0[k][l] * value;
                            gaborValue += gaborMatrix45[k][l] * value;
                            gaborValue += gaborMatrix90[k][l] * value;
                            gaborValue += gaborMatrix135[k][l] * value;
                        }
                    }
                }

                gaborValues[i][j] = gaborValue;
            }
        }

        double min = gaborValues[0][0];
        double max = gaborValues[0][0];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (gaborValues[i][j] < min) {
                    min = gaborValues[i][j];
                }
                if (gaborValues[i][j] > max) {
                    max = gaborValues[i][j];
                }
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (min < 0) {
                    if (max >= 0) {
                        gaborValues[i][j] -= min;
                        gaborValues[i][j] /= -min + max;
                        gaborValues[i][j] *= 100;
                    } else {
                        gaborValues[i][j] -= min;
                        gaborValues[i][j] /= -min - max;
                        gaborValues[i][j] *= 100;
                    }
                } else {
                    gaborValues[i][j] += min;
                    gaborValues[i][j] /= min + max;
                    gaborValues[i][j] *= 100;
                }
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int[] RGB = ComputerVisionUtility.evaluateRGB(new double[]{0, 0, gaborValues[i][j]});

                imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processThresholdImpact(double thresholdH, double thresholdN) {
        ValueThreshold[][] strongPixels = new ValueThreshold[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        ValueThreshold[][] weakPixels = new ValueThreshold[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                strongPixels[i][j] = new ValueThreshold();
                weakPixels[i][j] = new ValueThreshold();

                int pixelColorRGB = imagePanel.getImage().getRGB(i, j);
                double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, pixelColorRGB & 0xFF);

                if (HSV[2] >= thresholdH) {
                    strongPixels[i][j].setStrong(true);
                    strongPixels[i][j].setValue(HSV[2]);
                }

                if (HSV[2] >= thresholdN) {
                    weakPixels[i][j].setWeak(true);
                    weakPixels[i][j].setValue(HSV[2]);
                }

                imagePanel.getImage().setRGB(i, j, Color.BLACK.getRGB());
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (weakPixels[i][j].isWeak()) {
                    for (int k = i - 1; k <= i + 1; k++) {
                        for (int l = j - 1; l <= j + 1; l++) {
                            try {
                                if (strongPixels[k][l].isStrong()) {
                                    strongPixels[i][j].setStrong(true);
                                    weakPixels[i][j].setStrong(true);
                                }
                            } catch (ArrayIndexOutOfBoundsException ignored) {
                            }
                        }
                    }
                }

                if (strongPixels[i][j].isStrong()) {
                    int[] RGB = ComputerVisionUtility.evaluateRGB(new double[]{0, 0, strongPixels[i][j].getValue()});
                    imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
                }
            }
        }
        imagePanel.repaint();
    }

    public void nonPeaksSuppression() {
        for (int i = 1; i < ComputerVisionUtility.imageSize - 1; i++) {
            for (int j = 1; j < ComputerVisionUtility.imageSize - 1; j++) {
                double maxValue0 = -1;
                double maxValue45Pos = -1;
                double maxValue45Neg = -1;
                double maxValue90 = -1;
                for (int k = i - 1; k <= i + 1; k++) {
                    for (int l = j - 1; l <= j + 1; l++) {
                        int pixelColorRGB = imagePanel.getImage().getRGB(k, l);
                        double direction = imageController.getPixelButtons()[k][l].getSobelDirection();
                        double value = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, pixelColorRGB & 0xFF)[2];
                        if (direction == 0) {
                            if (value > maxValue0) maxValue0 = value;
                        } else if (direction == 45) {
                            if (value > maxValue45Pos) maxValue45Pos = value;
                        } else if (direction == -45) {
                            if (value > maxValue45Neg) maxValue45Neg = value;
                        } else if (direction == 90) {
                            if (value > maxValue90) maxValue90 = value;
                        }
                    }
                }

                for (int k = i - 1; k <= i + 1; k++) {
                    for (int l = j - 1; l <= j + 1; l++) {
                        int pixelColor = imagePanel.getImage().getRGB(k, l);
                        double direction = imageController.getPixelButtons()[k][l].getSobelDirection();
                        double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColor >> 16) & 0xFF, (pixelColor >> 8) & 0xFF, pixelColor & 0xFF);

                        if (direction == 0) {
                            if (HSV[2] < maxValue0) HSV[2] = 0;
                        } else if (direction == 45) {
                            if (HSV[2] < maxValue45Pos) HSV[2] = 0;
                        } else if (direction == -45) {
                            if (HSV[2] < maxValue45Neg) HSV[2] = 0;
                        } else if (direction == 90) {
                            if (HSV[2] < maxValue90) HSV[2] = 0;
                        }

                        int[] RGB = ComputerVisionUtility.evaluateRGB(HSV);
                        imagePanel.getImage().setRGB(k, l, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
                    }
                }
            }
        }
    }

    public void processCannyFilter() {
        processGaussFilter(1.4f, 3);
        processSobelFilter();
        nonPeaksSuppression();
        processThresholdImpact(thresholdHSlider.getValue(), thresholdNSlider.getValue());

        histogramController.updateHistogram();
    }

    public void processSobelFilter() {
        int[][] originalColor = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                originalColor[i][j] = imagePanel.getImage().getRGB(i, j);
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                double[][] sobelFilterX = ComputerVisionUtility.getSobelFilterX();
                double[][] sobelFilterY = ComputerVisionUtility.getSobelFilterY();

                double gX = 0;
                double gY = 0;
                for (int k = i - 1; k <= i + 1; k++) {
                    for (int l = j - 1; l <= j + 1; l++) {
                        try {
                            int pixelColorRGB = originalColor[k][l];
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            gX += HSV[2] * sobelFilterX[k - i + 1][l - j + 1];
                            gY += HSV[2] * sobelFilterY[k - i + 1][l - j + 1];
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                            int pixelColorRGB = originalColor[i][j];
                            double[] HSV = ComputerVisionUtility.evaluateHSV((pixelColorRGB >> 16) & 0xFF, (pixelColorRGB >> 8) & 0xFF, (pixelColorRGB) & 0xFF);
                            gX += HSV[2] * sobelFilterX[k - i + 1][l - j + 1];
                            gY += HSV[2] * sobelFilterY[k - i + 1][l - j + 1];
                        }
                    }
                }

                double direction = Math.atan(gY / gX) * 180;
                if (direction > 180) direction = 360 - direction;
                if (direction < -180) direction = 360 + direction;

                if (direction >= 157.5 && direction <= 180 || direction <= -157.5 && direction >= -180 || direction > -22.5 && direction <= 0 || direction < 22.5 && direction >= 0) {
                    direction = 0;
                } else if (direction >= 112.5 && direction < 157.5 || direction > -67.5 && direction <= -22.5) {
                    direction = 45;
                } else if (direction >= 67.5 && direction < 112.5 || direction > -112.5 && direction <= -67.5) {
                    direction = 90;
                } else if (direction >= 22.5 && direction < 67.5 || direction > -157.5 && direction <= -112.5) {
                    direction = -45;
                }

                double g = Math.sqrt(Math.pow(gX, 2) + Math.pow(gY, 2));
                if (g > 100) {
                    g = 100;
                }

                int[] RGB = ComputerVisionUtility.evaluateRGB(new double[]{0, 0, g});
                imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
                imageController.getPixelButtons()[i][j].setSobelDirection(direction);
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void processGaussFilter(double sigma, int filterSize) {
        double[][] gaussKernel = new double[filterSize][filterSize];
        double gaussSum = 0;
        for (int i = -filterSize / 2; i <= filterSize / 2; i++) {
            for (int j = -filterSize / 2; j <= filterSize / 2; j++) {
                gaussKernel[i + filterSize / 2][j + filterSize / 2] = (1.0 / (2 * Math.PI * Math.pow(sigma, 2))) * Math.exp(-(Math.pow(i, 2) + Math.pow(j, 2)) / (2 * Math.pow(sigma, 2)));
                gaussSum += gaussKernel[i + filterSize / 2][j + filterSize / 2];
            }
        }

        for (int i = 0; i < filterSize; i++) {
            for (int j = 0; j < filterSize; j++) {
                gaussKernel[i][j] /= gaussSum;
            }
        }

        int pixelPosX = 0;
        int pixelPosY = 0;
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                double rSum = 0, gSum = 0, bSum = 0;

                for (int k = 0; k < filterSize; k++) {
                    for (int l = 0; l < filterSize; l++) {
                        try {
                            pixelPosX = i + k - filterSize / 2;
                            pixelPosY = j + l - filterSize / 2;

                            int pixelColorRGB = imagePanel.getImage().getRGB(pixelPosX, pixelPosY);
                            double kernelVal = gaussKernel[k][l];

                            rSum += ((pixelColorRGB >> 16) & 0xFF) * kernelVal;
                            gSum += ((pixelColorRGB >> 8) & 0xFF) * kernelVal;
                            bSum += ((pixelColorRGB) & 0xFF) * kernelVal;
                        } catch (ArrayIndexOutOfBoundsException e) {
                            int pixelColorRGB = imagePanel.getImage().getRGB(i, j);
                            double kernelVal = gaussKernel[k][l];

                            rSum += ((pixelColorRGB >> 16) & 0xFF) * kernelVal;
                            gSum += ((pixelColorRGB >> 8) & 0xFF) * kernelVal;
                            bSum += ((pixelColorRGB) & 0xFF) * kernelVal;
                        }
                    }
                }

                if (rSum < 0) rSum = 0;
                if (rSum > 255) rSum = 255;

                if (gSum < 0) gSum = 0;
                if (gSum > 255) gSum = 255;

                if (bSum < 0) bSum = 0;
                if (bSum > 255) bSum = 255;

                imagePanel.getImage().setRGB(i, j, new Color((int) rSum, (int) gSum, (int) bSum).getRGB());
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }
}
