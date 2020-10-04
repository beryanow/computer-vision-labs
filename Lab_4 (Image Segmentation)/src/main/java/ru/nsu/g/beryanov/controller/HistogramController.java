package ru.nsu.g.beryanov.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.HistogramPanel;
import ru.nsu.g.beryanov.view.ImagePanel;

import java.awt.*;
import java.util.Arrays;

@Service
public class HistogramController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    @Qualifier("histogramPanel")
    private HistogramPanel histogramPanel;

    public void updateHistogram() {
        double[] statistics = new double[101];
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color pixelColor = new Color(imagePanel.getImage().getRGB(i, j));

                statistics[(int) ComputerVisionUtility.evaluateLAB(ComputerVisionUtility.evaluateXYZ(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue()))[0]]++;
            }
        }

        double max = Arrays.stream(statistics).max().getAsDouble();

        for (int i = 0; i < 101; i++) {
            statistics[i] /= max;
            statistics[i] *= 101;
        }

        for (int i = 0; i < 101; i++) {
            for (int j = 100; j > 100 - (int) statistics[i]; j--) {
                histogramPanel.getImage().setRGB(i, j, Color.BLACK.getRGB());
            }
            for (int j = 0; j < 101 - statistics[i]; j++) {
                histogramPanel.getImage().setRGB(i, j, Color.WHITE.getRGB());
            }
        }

        histogramPanel.repaint();
    }
}
