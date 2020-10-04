package ru.nsu.g.beryanov.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Service
public class ToolController {
    @Autowired
    @Qualifier(value = "computerVisionFrame")
    private JFrame computerVisionFrame;

    @Autowired
    @Qualifier(value = "filterPanel")
    private JPanel filterPanel;

    @Autowired
    @Qualifier(value = "morphologyPanel")
    private JPanel morphologyPanel;

    @Autowired
    @Qualifier(value = "segmentationPanel")
    private JPanel segmentationPanel;

    @Autowired
    @Qualifier(value = "toolPanel")
    private JPanel toolPanel;

    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    private ImageController imageController;

    @Autowired
    private HistogramController histogramController;

    boolean isExtended = true;

    public void switchPanel() {
        if (isExtended) {
            computerVisionFrame.remove(filterPanel);
            computerVisionFrame.remove(morphologyPanel);

            computerVisionFrame.add(segmentationPanel);
            computerVisionFrame.add(toolPanel);

            segmentationPanel.repaint();
            toolPanel.repaint();

            computerVisionFrame.repaint();
            computerVisionFrame.revalidate();

            isExtended = false;
        } else {
            computerVisionFrame.remove(segmentationPanel);
            computerVisionFrame.remove(toolPanel);

            computerVisionFrame.add(filterPanel);
            computerVisionFrame.add(morphologyPanel);

            computerVisionFrame.repaint();

            computerVisionFrame.setSize(1425, 800);
            computerVisionFrame.revalidate();

            isExtended = true;
        }
    }

    public void cleanBackground() {
        int[][] blackImage = imageController.imageToArray(imagePanel.getImage());

        for (int i = 0; i < blackImage.length; i++) {
            for (int j = 0; j < blackImage.length; j++) {
                if (blackImage[i][j] == 1) {
                    imagePanel.getImage().setRGB(i, j, imageController.getPixelButtons()[i][j].getDefaultColor().getRGB());
                } else {
                    imagePanel.getImage().setRGB(i, j, Color.WHITE.getRGB());
                }
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }
}
