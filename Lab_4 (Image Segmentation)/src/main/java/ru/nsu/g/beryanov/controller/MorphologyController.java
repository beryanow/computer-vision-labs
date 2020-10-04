package ru.nsu.g.beryanov.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.exception.ErosionImpossibleException;
import ru.nsu.g.beryanov.model.PixelMarking;
import ru.nsu.g.beryanov.model.StructuringElement;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;

@Service
public class MorphologyController {
    public int[][] processDilation(int[][] image, StructuringElement structuringElement) {
        int[][] dilationImage = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                dilationImage[i][j] = 0;
            }
        }

        int structuringElementCenterWidth = structuringElement.getData().length / 2;
        int structuringElementCenterHeight = structuringElement.getData()[0].length / 2;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (image[i][j] == 1) {
                    for (int k = -structuringElementCenterWidth; k <= structuringElementCenterWidth; k++) {
                        for (int l = -structuringElementCenterHeight; l <= structuringElementCenterHeight; l++) {
                            try {
                                if (structuringElement.getData()[k + structuringElementCenterWidth][l + structuringElementCenterHeight] == 1) {
                                    dilationImage[i + k][j + l] = 1;
                                }
                            } catch (ArrayIndexOutOfBoundsException ignored) {}
                        }
                    }
                }
            }
        }

        return dilationImage;
    }

    public int[][] processErosion(int[][] image, StructuringElement structuringElement) {
        int[][] erosionImage = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                erosionImage[i][j] = 0;
            }
        }

        int structuringElementCenterWidth = structuringElement.getData().length / 2;
        int structuringElementCenterHeight = structuringElement.getData()[0].length / 2;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (image[i][j] == 1) {
                    try {
                        for (int k = -structuringElementCenterWidth; k <= structuringElementCenterWidth; k++) {
                            for (int l = -structuringElementCenterHeight; l <= structuringElementCenterHeight; l++) {
                                int pixelColor = image[i + k][j + l];

                                if (structuringElement.getData()[k + structuringElementCenterWidth][l + structuringElementCenterHeight] == 1 && pixelColor == 0) {
                                    throw new ErosionImpossibleException();
                                }
                            }
                        }

                        erosionImage[i][j] = 1;
                    } catch (ErosionImpossibleException | ArrayIndexOutOfBoundsException ignored) {
                    }
                }
            }
        }

        return erosionImage;
    }

    public int[][] processClosing(int[][] image, StructuringElement structuringElement) {
        int[][] dilationImage = processDilation(image, structuringElement);

        return processErosion(dilationImage, structuringElement);
    }

    public int[][] processOpening(int[][] image, StructuringElement structuringElement) {
        int[][] erosionImage = processErosion(image, structuringElement);

        return processDilation(erosionImage, structuringElement);
    }

    public int[][] processBorderDefinition(int[][] image, StructuringElement structuringElement) {
        int[][] erosionImage = processErosion(image, structuringElement);

        return processDifference(image, erosionImage);
    }

    public int[][] processDifference(int[][] image, int[][] imageToDiffer) {
        int[][] differenceImage = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int pixelColor = imageToDiffer[i][j];

                if (pixelColor == 1) {
                    differenceImage[i][j] = 0;
                } else {
                    differenceImage[i][j] = image[i][j];
                }
            }
        }

        return differenceImage;
    }

    public int[][] processImageMarker(int[][] image) {
        int[][] imageMarker = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (i == 0 || i == ComputerVisionUtility.imageSize - 1 || j == 0 || j == ComputerVisionUtility.imageSize - 1) {
                    int pixelColor = (image[i][j] + 1) % 2;
                    imageMarker[i][j] = pixelColor;
                } else {
                    imageMarker[i][j] = 0;
                }
            }
        }

        return imageMarker;
    }

    public int[][] invertImage(int[][] image) {
        int[][] invertImage = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int pixelColor = (image[i][j] + 1) % 2;
                invertImage[i][j] = pixelColor;
            }
        }

        return invertImage;
    }

    public int[][] intersectImages(int[][] image, int[][] imageToIntersectWith) {
        int[][] intersectionImage = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (image[i][j] == imageToIntersectWith[i][j]) {
                    intersectionImage[i][j] = image[i][j];
                } else {
                    intersectionImage[i][j] = 0;
                }
            }
        }

        return intersectionImage;
    }

    public int[][] processMorphologicalReconstructionThroughDilation(StructuringElement structuringElement, int[][] marker, int[][] mask) {
        int[][] dilationImage = processDilation(marker, structuringElement);

        return intersectImages(dilationImage, mask);
    }

    public int[][] getMorphologicalReconstructionThroughDilationImage(StructuringElement structuringElement, int[][] marker, int[][] mask) {
        int[][] image1 = processMorphologicalReconstructionThroughDilation(structuringElement, marker, mask);
        int[][] image2 = processMorphologicalReconstructionThroughDilation(structuringElement, image1, mask);

        boolean isEqual = false;

        while (!isEqual) {
            isEqual = true;
            for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
                for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                    if (image1[i][j] != image2[i][j]) {
                        isEqual = false;
                        break;
                    }
                }
            }

            if (!isEqual) {
                image1 = image2;
                image2 = processMorphologicalReconstructionThroughDilation(structuringElement, image1, mask);
            }
        }

        return image2;
    }

    public int[] updateNeighbours(int[][] image, int i, int j) {
        int[] neighbours = new int[8];

        try {
            neighbours[0] = (int) (image[i - 1][j - 1] + Math.sqrt(2));
        } catch (ArrayIndexOutOfBoundsException ignored) {
            neighbours[0] = (int) Math.sqrt(2);
        }

        try {
            neighbours[1] = image[i - 1][j] + 1;
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[1] = 1;
        }

        try {
            neighbours[2] = (int) (image[i - 1][j + 1] + Math.sqrt(2));
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[2] = (int) Math.sqrt(2);
        }

        try {
            neighbours[3] = image[i][j - 1] + 1;
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[3] = 1;
        }

        try {
            neighbours[4] = image[i][j + 1] + 1;
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[4] = 1;
        }

        try {
            neighbours[5] = (int) (image[i + 1][j - 1] + Math.sqrt(2));
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[5] = (int) Math.sqrt(2);
        }

        try {
            neighbours[6] = image[i + 1][j] + 1;
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[6] = 1;
        }

        try {
            neighbours[7] = (int) (image[i + 1][j + 1] + Math.sqrt(2));
        } catch (ArrayIndexOutOfBoundsException exception) {
            neighbours[6] = (int) Math.sqrt(2);
        }

        return neighbours;
    }

    public static int findMinNeighbor(int[] neighbours, int[][] image, int row, int col, int pass) {
        int temp = 0;
        if (pass == 1) {
            temp = neighbours[0];
            for (int i = 1; i < 4; i++)
                temp = Math.min(neighbours[i], temp);
        } else if (pass == 2) {
            temp = image[row][col];
            for (int i = 4; i < 8; i++)
                temp = (Math.min(neighbours[i], temp));
        }
        return temp;
    }

    public double[][] processEuclideanDistanceTransform(int[][] image) {
        double[][] distanceTransformPixels = new double[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        int[] neighbours;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                neighbours = updateNeighbours(image, i, j);
                if (image[i][j] > 0)
                    image[i][j] = findMinNeighbor(neighbours, image, i, j, 1);
            }
        }

        for (int i = ComputerVisionUtility.imageSize - 1; i >= 0; i--) {
            for (int j = ComputerVisionUtility.imageSize - 1; j >= 0; j--) {
                neighbours = updateNeighbours(image, i, j);
                if (image[i][j] > 0)
                    image[i][j] = findMinNeighbor(neighbours, image, i, j, 2);
            }
        }

        int max = -1;
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                if (image[i][j] > max) {
                    max = image[i][j];
                }
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                distanceTransformPixels[i][j] = (double) image[i][j] / max * 100;
            }
        }

        return distanceTransformPixels;
    }

    public int[][] processThresholdConditioning(BufferedImage image, int threshold) {
        int[][] thresholdConditioned = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color pixelColor = new Color(image.getRGB(i, j));
                if (ComputerVisionUtility.evaluateHSV(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue())[2] >= threshold) {
                    thresholdConditioned[i][j] = 1;
                } else {
                    thresholdConditioned[i][j] = 0;
                }
            }
        }

        return thresholdConditioned;
    }

    public PixelMarking[][] processConnectedAreasMarking(int[][] image) {
        PixelMarking[][] pixelMarkings = new PixelMarking[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                pixelMarkings[i][j] = new PixelMarking();
            }
        }

        int label = 0;
        HashMap<Integer, Integer> equivalentLabels = new HashMap<>();
        for (int i = -1; i < ComputerVisionUtility.imageSize - 1; i++) {
            for (int j = -1; j < ComputerVisionUtility.imageSize - 1; j++) {
                if (image[i + 1][j + 1] != 0) {
                    if (i == -1 && j == -1) {
                        label++;
                        pixelMarkings[i + 1][j + 1].setLabeled(true);
                        pixelMarkings[i + 1][j + 1].setLabel(label);
                    } else if (i == -1) {
                        if (!pixelMarkings[i + 1][j].isLabeled()) {
                            label++;
                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            pixelMarkings[i + 1][j + 1].setLabel(label);
                        } else {
                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i + 1][j].getLabel());
                        }
                    } else if (j == -1) {
                        if (!pixelMarkings[i][j + 1].isLabeled()) {
                            label++;
                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            pixelMarkings[i + 1][j + 1].setLabel(label);
                        } else {
                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i][j + 1].getLabel());
                        }
                    } else {
                        if (!pixelMarkings[i][j + 1].isLabeled() && !pixelMarkings[i + 1][j].isLabeled()) {
                            label++;

                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            pixelMarkings[i + 1][j + 1].setLabel(label);
                        } else if (pixelMarkings[i][j + 1].isLabeled() ^ pixelMarkings[i + 1][j].isLabeled()) {
                            pixelMarkings[i + 1][j + 1].setLabeled(true);
                            if (pixelMarkings[i][j + 1].isLabeled()) {
                                pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i][j + 1].getLabel());
                            } else if (pixelMarkings[i + 1][j].isLabeled()) {
                                pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i + 1][j].getLabel());
                            }
                        } else if (pixelMarkings[i][j + 1].isLabeled() && pixelMarkings[i + 1][j].isLabeled()) {
                            if (pixelMarkings[i][j + 1].getLabel() == pixelMarkings[i + 1][j].getLabel()) {
                                pixelMarkings[i + 1][j + 1].setLabeled(true);
                                pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i + 1][j].getLabel());
                            } else {
                                pixelMarkings[i + 1][j + 1].setLabeled(true);
                                pixelMarkings[i + 1][j + 1].setLabel(pixelMarkings[i][j + 1].getLabel());
                                equivalentLabels.put(pixelMarkings[i + 1][j].getLabel(), pixelMarkings[i][j + 1].getLabel());
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int equivalentLabel;
                int previousLabel = pixelMarkings[i][j].getLabel();
                if ((equivalentLabel = equivalentLabels.getOrDefault(previousLabel, 0)) != 0) {
                    equivalentLabels.entrySet().stream().filter(entry -> entry.getValue() == previousLabel).forEach(entry -> equivalentLabels.put(entry.getKey(), equivalentLabel));
                    pixelMarkings[i][j].setLabel(equivalentLabel);
                }
            }
        }

        return pixelMarkings;
    }

    public int processAreasCounting(int[][] image) {
        PixelMarking[][] markedPixels = processConnectedAreasMarking(image);

        int[][] marked = new int[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];

        HashSet<Integer> markings = new HashSet<>();
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                markings.add(markedPixels[i][j].getLabel());
            }
        }

        markings.remove(0);
        return markings.size();
    }
}

