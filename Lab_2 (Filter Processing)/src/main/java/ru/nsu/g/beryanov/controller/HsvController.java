package ru.nsu.g.beryanov.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.swing.*;
import java.awt.*;

@Component
public class HsvController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    @Qualifier("hueSlider")
    private JSlider hueSlider;

    @Autowired
    @Qualifier("saturationSlider")
    private JSlider saturationSlider;

    @Autowired
    @Qualifier("valueSlider")
    private JSlider valueSlider;

    @Autowired
    private HistogramController histogramController;

    @Autowired
    private ImageController imageController;

    public void changeHue(int delta) {
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color color = new Color(imagePanel.getImage().getRGB(i, j));
                double[] HSV = ComputerVisionUtility.evaluateHSV(color.getRed(), color.getGreen(), color.getBlue());

                Color defaultColor = imageController.getPixelButtons()[i][j].getDefaultColor();
                double[] originalHSV = ComputerVisionUtility.evaluateHSV(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue());

                HSV[0] = originalHSV[0];
                HSV[1] = originalHSV[1] + saturationSlider.getValue();
                HSV[2] = originalHSV[2] + valueSlider.getValue();

                HSV[1] = HSV[1] < 0 ? 0 : HSV[1] > 100 ? 100 : HSV[1];
                HSV[2] = HSV[2] < 0 ? 0 : HSV[2] > 100 ? 100 : HSV[2];

                double hueValue;

                if (delta > 0) {
                    HSV[0] = ((hueValue = HSV[0] + delta) > 360) ? hueValue - 360 : hueValue;
                } else {
                    HSV[0] = ((hueValue = HSV[0] + delta) < 0) ? hueValue + 360 : hueValue;
                }

                int[] RGB = ComputerVisionUtility.evaluateRGB(HSV);
                imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void changeSaturation(int delta) {
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color color = new Color(imagePanel.getImage().getRGB(i, j));
                double[] HSV = ComputerVisionUtility.evaluateHSV(color.getRed(), color.getGreen(), color.getBlue());

                Color defaultColor = imageController.getPixelButtons()[i][j].getDefaultColor();
                double[] originalHSV = ComputerVisionUtility.evaluateHSV(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue());

                HSV[0] = originalHSV[0] + hueSlider.getValue();
                HSV[1] = originalHSV[1];
                HSV[2] = originalHSV[2] + valueSlider.getValue();

                HSV[0] = HSV[0] < 0 ? 360 - HSV[0] : HSV[0] > 360 ? HSV[0] - 360 : HSV[0];
                HSV[2] = HSV[2] < 0 ? 0 : HSV[2] > 100 ? 100 : HSV[2];

                double saturationValue;

                if (delta > 0) {
                    HSV[1] = ((saturationValue = HSV[1] + delta) > 100) ? 100 : saturationValue;
                } else {
                    HSV[1] = ((saturationValue = HSV[1] + delta) < 0) ? 0 : saturationValue;
                }

                int[] RGB = ComputerVisionUtility.evaluateRGB(HSV);
                imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void changeValue(int delta) {
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                Color color = new Color(imagePanel.getImage().getRGB(i, j));
                double[] HSV = ComputerVisionUtility.evaluateHSV(color.getRed(), color.getGreen(), color.getBlue());

                Color defaultColor = imageController.getPixelButtons()[i][j].getDefaultColor();
                double[] originalHSV = ComputerVisionUtility.evaluateHSV(defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue());

                HSV[0] = originalHSV[0] + hueSlider.getValue();
                HSV[1] = originalHSV[1] + saturationSlider.getValue();
                HSV[2] = originalHSV[2];

                HSV[0] = HSV[0] < 0 ? 360 - HSV[0] : HSV[0] > 360 ? HSV[0] - 360 : HSV[0];
                HSV[1] = HSV[1] < 0 ? 0 : HSV[1] > 100 ? 100 : HSV[1];

                double valueValue;

                if (delta > 0) {
                    HSV[2] = ((valueValue = HSV[2] + delta) > 100) ? 100 : valueValue;
                } else {
                    HSV[2] = ((valueValue = HSV[2] + delta) < 0) ? 0 : valueValue;
                }

                int[] RGB = ComputerVisionUtility.evaluateRGB(HSV);
                imagePanel.getImage().setRGB(i, j, new Color(RGB[0], RGB[1], RGB[2]).getRGB());
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }
}
