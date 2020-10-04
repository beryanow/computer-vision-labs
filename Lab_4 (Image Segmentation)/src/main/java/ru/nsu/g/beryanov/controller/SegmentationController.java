package ru.nsu.g.beryanov.controller;

import lombok.SneakyThrows;
import org.opencv.core.Scalar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.model.ImageTreeNode;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.ImagePanel;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

@Service
public class SegmentationController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    private HistogramController histogramController;

    public void processKMeans(int k) {
        int[] rgb = new int[(int) Math.pow(ComputerVisionUtility.imageSize, 2)];
        int count = 0;

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int rgbVal = imagePanel.getImage().getRGB(i, j);
                rgb[count++] = rgbVal;
            }
        }

        proceedKMeansAlgorithm(rgb, k);

        count = 0;
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                imagePanel.getImage().setRGB(i, j, rgb[count++]);
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void proceedKMeansAlgorithm(int[] rgb, int k) {
        double[][] means = new double[k + 1][4];
        long[] count = new long[k + 1];

        for (int i = 1; i <= k; i++) {
            Random rand = new Random();

            means[i][0] = rand.nextDouble() * 255;
            means[i][1] = rand.nextDouble() * 255;
            means[i][2] = rand.nextDouble() * 255;
            means[i][3] = rand.nextDouble() * 255;

            count[i] = 0;
        }

        int[] red = new int[rgb.length];
        int[] blue = new int[rgb.length];
        int[] green = new int[rgb.length];
        int[] alpha = new int[rgb.length];

        for (int i = 0; i < rgb.length; i++) {
            Color c = new Color(rgb[i]);

            red[i] = c.getRed();
            blue[i] = c.getBlue();
            green[i] = c.getGreen();
            alpha[i] = c.getAlpha();
        }

        int[] curAssignments = new int[rgb.length];
        boolean change = true;

        while (change) {
            change = false;
            int[] tempAssignments = new int[rgb.length];

            for (int x = 0; x < rgb.length; x++) {
                double closest = Double.MAX_VALUE;
                for (int m = 1; m <= k; m++) {
                    double distance = (Math.pow(red[x] - means[m][0], 2) + Math.pow(blue[x] - means[m][1], 2) + Math.pow(green[x] - means[m][2], 2) + Math.pow(alpha[x] - means[m][3], 2));
                    if (distance < closest) {
                        tempAssignments[x] = m;
                        closest = distance;
                    }
                }
            }

            double changeVal = 0;
            for (int x = 0; x < rgb.length; x++) {
                if (curAssignments[x] != tempAssignments[x]) {
                    changeVal += 1;
                }
            }

            if (changeVal > 0) {
                change = true;

                System.arraycopy(tempAssignments, 0, curAssignments, 0, rgb.length);
                for (int m = 1; m <= k; m++) {
                    count[m] = 0;
                }

                for (int i = 0; i < rgb.length; i++) {
                    int meanInd = tempAssignments[i];
                    count[meanInd] += 1;

                    if (count[meanInd] == 1) {
                        means[meanInd][0] = red[i];
                        means[meanInd][1] = blue[i];
                        means[meanInd][2] = green[i];
                        means[meanInd][3] = alpha[i];
                    } else {
                        double prevC = (double) count[meanInd] - 1.0;

                        means[meanInd][0] = (prevC / (prevC + 1.0)) * (means[meanInd][0] + (red[i] / prevC));
                        means[meanInd][1] = (prevC / (prevC + 1.0)) * (means[meanInd][1] + (blue[i] / prevC));
                        means[meanInd][2] = (prevC / (prevC + 1.0)) * (means[meanInd][2] + (green[i] / prevC));
                        means[meanInd][3] = (prevC / (prevC + 1.0)) * (means[meanInd][3] + (alpha[i] / prevC));
                    }
                }
            }
        }

        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = (int) means[curAssignments[i]][3] << 24 | (int) means[curAssignments[i]][0] << 16 | (int) means[curAssignments[i]][2] << 8 | (int) means[curAssignments[i]][1];
        }
    }

    public void processDfs(ArrayList<ImageTreeNode> nodes, ImageTreeNode imageTreeNode) {
        if (Optional.ofNullable(imageTreeNode.getChildrenNodes()).isPresent()) {
            for (int i = 0; i < imageTreeNode.getChildrenNodes().length; i++) {
                processDfs(nodes, imageTreeNode.getChildrenNodes()[i]);
            }
        }
        nodes.add(imageTreeNode);
    }

    public void processSplitAndMerge(int splitThresholdValue, int mergeThresholdValue) {
        ArrayList<ImageTreeNode> processList = new ArrayList<>();

        int index = 0;

        Integer[][] image = new Integer[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                image[i][j] = imagePanel.getImage().getRGB(i, j);
            }
        }

        ImageTreeNode topNode = new ImageTreeNode(index, null, null, 0, new Integer[]{0, 0}, image, null);
        processList.add(topNode);

        while (!processList.isEmpty()) {
            ImageTreeNode nearestNode = processList.get(0);

            double meanRed = 0;
            double meanGreen = 0;
            double meanBlue = 0;

            Integer[][] nearestNodeImage = nearestNode.getImagePart();

            for (int i = 0; i < nearestNodeImage.length; i++) {
                for (int j = 0; j < nearestNodeImage[0].length; j++) {
                    meanRed += (nearestNodeImage[i][j] >> 16) & 0xFF;
                    meanGreen += (nearestNodeImage[i][j] >> 8) & 0xFF;
                    meanBlue += nearestNodeImage[i][j] & 0xFF;
                }
            }

            meanRed /= Math.pow(nearestNodeImage.length, 2);
            meanGreen /= Math.pow(nearestNodeImage.length, 2);
            meanBlue /= Math.pow(nearestNodeImage.length, 2);

            double varianceRed = 0;
            double varianceGreen = 0;
            double varianceBlue = 0;

            for (int i = 0; i < nearestNodeImage.length; i++) {
                for (int j = 0; j < nearestNodeImage[0].length; j++) {
                    varianceRed += Math.pow(((nearestNodeImage[i][j] >> 16) & 0xFF) - meanRed, 2);
                    varianceGreen += Math.pow(((nearestNodeImage[i][j] >> 8) & 0xFF) - meanGreen, 2);
                    varianceBlue += Math.pow((nearestNodeImage[i][j] & 0xFF) - meanBlue, 2);
                }
            }

            varianceRed /= Math.pow(nearestNodeImage.length, 2);
            varianceGreen /= Math.pow(nearestNodeImage.length, 2);
            varianceBlue /= Math.pow(nearestNodeImage.length, 2);

            if (varianceRed + varianceBlue + varianceGreen >= splitThresholdValue) {
                Integer[][] splitOne = new Integer[nearestNodeImage.length / 2][nearestNodeImage[0].length / 2];
                Integer[][] splitTwo = new Integer[nearestNodeImage.length - nearestNodeImage.length / 2][nearestNodeImage[0].length / 2];
                Integer[][] splitThree = new Integer[nearestNodeImage.length / 2][nearestNodeImage[0].length - nearestNodeImage[0].length / 2];
                Integer[][] splitFour = new Integer[nearestNodeImage.length - nearestNodeImage.length / 2][nearestNodeImage[0].length - nearestNodeImage[0].length / 2];

                for (int i = 0; i < nearestNodeImage.length / 2; i++) {
                    System.arraycopy(nearestNodeImage[i], 0, splitOne[i], 0, nearestNodeImage[0].length / 2);
                }

                for (int i = nearestNodeImage.length / 2; i < nearestNodeImage.length; i++) {
                    System.arraycopy(nearestNodeImage[i], 0, splitTwo[i - nearestNodeImage.length / 2], 0, nearestNodeImage[0].length / 2);
                }

                for (int i = 0; i < nearestNodeImage.length / 2; i++) {
                    if (nearestNodeImage[0].length - nearestNodeImage[0].length / 2 >= 0)
                        System.arraycopy(nearestNodeImage[i], nearestNodeImage[0].length / 2, splitThree[i], 0, nearestNodeImage[0].length - nearestNodeImage[0].length / 2);
                }

                for (int i = nearestNodeImage.length / 2; i < nearestNodeImage.length; i++) {
                    if (nearestNodeImage[0].length - nearestNodeImage[0].length / 2 >= 0)
                        System.arraycopy(nearestNodeImage[i], nearestNodeImage[0].length / 2, splitFour[i - nearestNodeImage.length / 2], 0, nearestNodeImage[0].length - nearestNodeImage[0].length / 2);
                }

                int parentIndex = nearestNode.getIndex();
                int parentLevel = nearestNode.getIndex();

                Integer[] parentCoordinates = nearestNode.getCoordinates();
                ImageTreeNode nodeFirst = new ImageTreeNode(++index, parentIndex, null, parentLevel + 1, new Integer[]{parentCoordinates[0], parentCoordinates[1]}, splitOne, null);
                ImageTreeNode nodeSecond = new ImageTreeNode(++index, parentIndex, null, parentLevel + 1, new Integer[]{parentCoordinates[0] + nearestNodeImage.length / 2, parentCoordinates[1]}, splitTwo, null);
                ImageTreeNode nodeThird = new ImageTreeNode(++index, parentIndex, null, parentLevel + 1, new Integer[]{parentCoordinates[0], parentCoordinates[1] + nearestNodeImage.length / 2}, splitThree, null);
                ImageTreeNode nodeFourth = new ImageTreeNode(++index, parentIndex, null, parentLevel + 1, new Integer[]{parentCoordinates[0] + nearestNodeImage.length / 2, parentCoordinates[1] + nearestNodeImage.length / 2}, splitFour, null);

                if (Optional.ofNullable(topNode.getChildrenNodes()).isEmpty()) {
                    topNode.setChildrenNodes(new ImageTreeNode[]{nodeFirst, nodeSecond, nodeThird, nodeFourth});
                } else {
                    nearestNode.setChildrenNodes(new ImageTreeNode[]{nodeFirst, nodeSecond, nodeThird, nodeFourth});
                    processList.remove(0);
                    processList.add(nodeFourth);
                    processList.add(nodeThird);
                    processList.add(nodeSecond);
                    processList.add(nodeFirst);
                }
            } else {
                processList.remove(0);
            }
        }

        ArrayList<ImageTreeNode> imageTreeNodeArray = new ArrayList<>();
        processDfs(imageTreeNodeArray, topNode);

        int segmentNumber = 0;
        while (!imageTreeNodeArray.isEmpty()) {
            ImageTreeNode node = imageTreeNodeArray.get(0);

            double meanRed = 0;
            double meanGreen = 0;
            double meanBlue = 0;

            for (int j = 0; j < node.getImagePart().length; j++) {
                for (int k = 0; k < node.getImagePart()[0].length; k++) {
                    meanRed += (node.getImagePart()[j][k] >> 16) & 0xFF;
                    meanGreen += (node.getImagePart()[j][k] >> 8) & 0xFF;
                    meanBlue += node.getImagePart()[j][k] & 0xFF;
                }
            }

            meanRed /= Math.pow(node.getImagePart().length, 2);
            meanGreen /= Math.pow(node.getImagePart().length, 2);
            meanBlue /= Math.pow(node.getImagePart().length, 2);

            for (int i = 1; i < imageTreeNodeArray.size(); i++) {
                ImageTreeNode anotherNode = imageTreeNodeArray.get(i);

                double anotherMeanRed = 0;
                double anotherMeanGreen = 0;
                double anotherMeanBlue = 0;

                for (int j = 0; j < anotherNode.getImagePart().length; j++) {
                    for (int k = 0; k < anotherNode.getImagePart()[0].length; k++) {
                        anotherMeanRed += (anotherNode.getImagePart()[j][k] >> 16) & 0xFF;
                        anotherMeanGreen += (anotherNode.getImagePart()[j][k] >> 8) & 0xFF;
                        anotherMeanBlue += anotherNode.getImagePart()[j][k] & 0xFF;
                    }
                }

                anotherMeanRed /= Math.pow(anotherNode.getImagePart().length, 2);
                anotherMeanGreen /= Math.pow(anotherNode.getImagePart().length, 2);
                anotherMeanBlue /= Math.pow(anotherNode.getImagePart().length, 2);

                if (Math.abs(meanRed - anotherMeanRed) + Math.abs(meanGreen - anotherMeanGreen) + Math.abs(meanBlue - anotherMeanBlue) <= mergeThresholdValue) {
                    if (node.getParentIndex().equals(anotherNode.getParentIndex())) {
                        if (Optional.ofNullable(node.getSegmentNumber()).isEmpty()) {
                            node.setSegmentNumber(segmentNumber);
                            anotherNode.setSegmentNumber(segmentNumber);
                        } else {
                            anotherNode.setSegmentNumber(node.getSegmentNumber());
                        }
                    }
                }
            }

            if (Optional.ofNullable(node.getSegmentNumber()).isEmpty()) {
                node.setSegmentNumber(++segmentNumber);
            }

            imageTreeNodeArray.remove(0);
        }
        
        LinkedList<ImageTreeNode> imageTreeNodeLinkedList = new LinkedList<>();
        imageTreeNodeLinkedList.add(topNode);

        while (!imageTreeNodeLinkedList.isEmpty()) {
            ImageTreeNode imageTreeNode = imageTreeNodeLinkedList.get(0);
            imageTreeNodeLinkedList.remove();

            if (Optional.ofNullable(imageTreeNode.getChildrenNodes()).isPresent()) {
                imageTreeNodeLinkedList.addAll(Arrays.asList(imageTreeNode.getChildrenNodes()));

                HashSet<Integer> segments = new HashSet<>();
                for (int i = 0; i < imageTreeNode.getChildrenNodes().length; i++) {
                    segments.add(imageTreeNode.getChildrenNodes()[i].getSegmentNumber());
                }

                Integer[] segmentsArray = new Integer[segments.size()];
                segments.toArray(segmentsArray);
                HashMap<Integer, Integer> segmentColors = new HashMap<>();
                for (int i = 0; i < segmentsArray.length; i++) {
                    for (int j = 0; j < imageTreeNode.getChildrenNodes().length; j++) {
                        if (segmentsArray[i].equals(imageTreeNode.getChildrenNodes()[j].getSegmentNumber())) {
                            if (imageTreeNode.getChildrenNodes()[j].getImagePart().length > 0 && imageTreeNode.getChildrenNodes()[j].getImagePart()[0].length > 0) {
                                segmentColors.put(segmentsArray[i], imageTreeNode.getChildrenNodes()[j].getImagePart()[0][0]);
                            }
                        }
                    }
                }

                for (int i = 0; i < imageTreeNode.getChildrenNodes().length; i++) {
                    int segment = imageTreeNode.getChildrenNodes()[i].getSegmentNumber();

                    int x = imageTreeNode.getChildrenNodes()[i].getCoordinates()[0];
                    int y = imageTreeNode.getChildrenNodes()[i].getCoordinates()[1];

                    for (int j = x; j < x + imageTreeNode.getChildrenNodes()[i].getImagePart().length; j++) {
                        for (int k = y; k < y + imageTreeNode.getChildrenNodes()[i].getImagePart()[0].length; k++) {
                            imagePanel.getImage().setRGB(j, k, segmentColors.get(segment));
                        }
                    }
                }

            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    @SneakyThrows
    public void processNormalizedCut(int threshold) {
        Process process = Runtime.getRuntime().exec("python3 src/main/resources/script/normalized_cut.py src/main/resources/" + ComputerVisionUtility.imageName + ".jpg " + ComputerVisionUtility.imageName + " " + (double) threshold / 1000);
        while (true) {
            if (!process.isAlive()) {
                break;
            }
        }

        Scanner scanner = new Scanner(new File("src/main/resources/script/" + "segmented_" + ComputerVisionUtility.imageName + ".txt"));
        int i = 0;
        while (scanner.hasNextInt()) {
            int red = scanner.nextInt();
            int green = scanner.nextInt();
            int blue = scanner.nextInt();

            int x = i % ComputerVisionUtility.imageSize;
            int y = i / ComputerVisionUtility.imageSize;

            imagePanel.getImage().setRGB(x, y, new Color(red, green, blue).getRGB());
            i++;
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }
}
