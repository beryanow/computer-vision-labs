package ru.nsu.g.beryanov.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.nsu.g.beryanov.model.PixelOption;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.HistogramPanel;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.Timestamp;

@Component
@Getter
@Setter
public class ImageController {
    private PixelOption[][] pixelButtons;

    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    @Qualifier("infoPanel")
    private JPanel infoPanel;

    @Autowired
    @Qualifier("histogramPanel")
    private HistogramPanel histogramPanel;

    @Autowired
    private HistogramController histogramController;

    @Autowired
    @Qualifier("selectedPixelRedLabel")
    private JLabel selectedPixelRedLabel;

    @Autowired
    @Qualifier("selectedPixelGreenLabel")
    private JLabel selectedPixelGreenLabel;

    @Autowired
    @Qualifier("selectedPixelBlueLabel")
    private JLabel selectedPixelBlueLabel;

    @Autowired
    @Qualifier("selectedPixelHLabel")
    private JLabel selectedPixelHLabel;

    @Autowired
    @Qualifier("selectedPixelSLabel")
    private JLabel selectedPixelSLabel;

    @Autowired
    @Qualifier("selectedPixelVLabel")
    private JLabel selectedPixelVLabel;

    @Autowired
    @Qualifier("selectedPixelXLabel")
    private JLabel selectedPixelXLabel;

    @Autowired
    @Qualifier("selectedPixelYLabel")
    private JLabel selectedPixelYLabel;

    @Autowired
    @Qualifier("selectedPixelZLabel")
    private JLabel selectedPixelZLabel;

    @Autowired
    @Qualifier("selectedPixelLLabel")
    private JLabel selectedPixelLLabel;

    @Autowired
    @Qualifier("selectedPixelALabel")
    private JLabel selectedPixelALabel;

    @Autowired
    @Qualifier("selectedPixelBLabel")
    private JLabel selectedPixelBLabel;

    @Autowired
    @Qualifier("hueSlider")
    private JSlider hueSlider;

    @Autowired
    @Qualifier("saturationSlider")
    private JSlider saturationSlider;

    @Autowired
    @Qualifier("valueSlider")
    private JSlider valueSlider;

    @SneakyThrows
    public void saveImage() {
        File savedImage = new File("src/main/resources/saved/" + ComputerVisionUtility.imageName + " " + new Timestamp(System.currentTimeMillis()) + ".jpg");

        ImageIO.write(imagePanel.getImage(), "jpg", savedImage);
    }

    public void updateImage() {
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int pixelColorRGB = ComputerVisionUtility.defaultImage.getRGB(i, j);
                imagePanel.getImage().setRGB(i, j, pixelColorRGB);
                pixelButtons[i][j].setDefaultColor(new Color(pixelColorRGB));
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }

    public void setDefaultImage() {
        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                int pixelColorRGB = pixelButtons[i][j].getDefaultColor().getRGB();
                imagePanel.getImage().setRGB(i, j, pixelColorRGB);
            }
        }

        selectedPixelRedLabel.setText(" ");
        selectedPixelGreenLabel.setText(" ");
        selectedPixelBlueLabel.setText(" ");

        selectedPixelHLabel.setText("\t\t\t\t\t\t\t\tне выбран");
        selectedPixelSLabel.setText(" ");
        selectedPixelVLabel.setText(" ");

        selectedPixelXLabel.setText(" ");
        selectedPixelYLabel.setText(" ");
        selectedPixelZLabel.setText("\t\t\t\t\t\t\t\t\t\tпиксель");

        selectedPixelLLabel.setText(" ");
        selectedPixelALabel.setText(" ");
        selectedPixelBLabel.setText(" ");

        hueSlider.setValue(0);
        saturationSlider.setValue(0);
        valueSlider.setValue(0);

        imagePanel.repaint();
        infoPanel.repaint();
        histogramController.updateHistogram();
    }

    public void processSelectedPixel(int x, int y) {
        Color pixelColor = new Color(imagePanel.getImage().getRGB(x, y));

        int red = pixelColor.getRed();
        int green = pixelColor.getGreen();
        int blue = pixelColor.getBlue();

        selectedPixelRedLabel.setText(String.format(ComputerVisionUtility.selectedPixelRedFormat, red));
        selectedPixelGreenLabel.setText(String.format(ComputerVisionUtility.selectedPixelGreenFormat, green));
        selectedPixelBlueLabel.setText(String.format(ComputerVisionUtility.selectedPixelBlueFormat, blue));

        double[] HSV = ComputerVisionUtility.evaluateHSV(red, green, blue);

        selectedPixelHLabel.setText(String.format(ComputerVisionUtility.selectedPixelHFormat, HSV[0]));
        selectedPixelSLabel.setText(String.format(ComputerVisionUtility.selectedPixelSFormat, HSV[1]));
        selectedPixelVLabel.setText(String.format(ComputerVisionUtility.selectedPixelVFormat, HSV[2]));

        double[] XYZ = ComputerVisionUtility.evaluateXYZ(red, green, blue);

        selectedPixelXLabel.setText(String.format(ComputerVisionUtility.selectedPixelXFormat, XYZ[0]));
        selectedPixelYLabel.setText(String.format(ComputerVisionUtility.selectedPixelYFormat, XYZ[1]));
        selectedPixelZLabel.setText(String.format(ComputerVisionUtility.selectedPixelZFormat, XYZ[2]));

        double[] LAB = ComputerVisionUtility.evaluateLAB(XYZ);

        selectedPixelLLabel.setText(String.format(ComputerVisionUtility.selectedPixelLFormat, LAB[0]));
        selectedPixelALabel.setText(String.format(ComputerVisionUtility.selectedPixelAFormat, LAB[1]));
        selectedPixelBLabel.setText(String.format(ComputerVisionUtility.selectedPixelBFormat, LAB[2]));

        infoPanel.repaint();
    }
}
