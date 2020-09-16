package ru.nsu.g.beryanov.configuration;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.nsu.g.beryanov.behaviour.MouseClickedListener;
import ru.nsu.g.beryanov.behaviour.MouseReleasedListener;
import ru.nsu.g.beryanov.controller.FilterController;
import ru.nsu.g.beryanov.controller.HsvController;
import ru.nsu.g.beryanov.controller.ImageController;
import ru.nsu.g.beryanov.model.PixelOption;
import ru.nsu.g.beryanov.utility.ComputerVisionUtility;
import ru.nsu.g.beryanov.view.HistogramPanel;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Configuration
public class ComputerVisionConfiguration {
    @Autowired
    private FilterController filterController;

    @Autowired
    private ImageController imageController;

    @Autowired
    private HsvController hsvController;

    @Autowired
    @Qualifier("sigmaSlider")
    private JSlider sigmaSlider;

    @SneakyThrows
    @Bean(name = "computerVisionFrame")
    public JFrame computerVisionFrame() {
        final Taskbar taskbar = Taskbar.getTaskbar();
        File fileImage = new File("src/main/resources/icon.png");
        taskbar.setIconImage(ImageIO.read(fileImage));

        ComputerVisionUtility.setFileChooserRussian();

        imageController.setPixelButtons(new PixelOption[ComputerVisionUtility.imageSize][ComputerVisionUtility.imageSize]);

        for (int i = 0; i < ComputerVisionUtility.imageSize; i++) {
            for (int j = 0; j < ComputerVisionUtility.imageSize; j++) {
                imageController.getPixelButtons()[i][j] = new PixelOption();
            }
        }

        imageController.updateImage();

        JFrame computerVisionFrame = new JFrame("ПАОЦИ");

        computerVisionFrame.add(imagePanel());
        computerVisionFrame.add(interactionPanel());
        computerVisionFrame.add(filterPanel());

        computerVisionFrame.getContentPane().setBackground(Color.white);
        computerVisionFrame.setLayout(new FlowLayout());
        computerVisionFrame.setSize(1295, 800);
        computerVisionFrame.setLocationRelativeTo(null);

        computerVisionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        computerVisionFrame.setResizable(false);
        computerVisionFrame.setVisible(true);

        return computerVisionFrame;
    }

    @SneakyThrows
    @Bean(name = "imagePanel")
    public ImagePanel imagePanel() {
        ComputerVisionUtility.imageName = "siba";
        File fileImage = new File("src/main/resources/" + ComputerVisionUtility.imageName + ".jpg");

        ComputerVisionUtility.defaultImage = ImageIO.read(fileImage);
        ComputerVisionUtility.imageSize = ComputerVisionUtility.defaultImage.getHeight();

        fileImage = new File("src/main/resources/histogram.jpg");
        ComputerVisionUtility.histogramImage = ImageIO.read(fileImage);

        ImagePanel imagePanel = new ImagePanel();

        imagePanel.addMouseListener((MouseClickedListener) e -> {
            int x = e.getX();
            int y = e.getY();
            if (x - 25 >= 0 && x - 25 <= 715 && y - 30 >= 0 && y - 30 <= 710) {
                imageController.processSelectedPixel((int) ((x - 25) / (double) 715 * ComputerVisionUtility.imageSize), (int) ((y - 30) / (double) 710 * ComputerVisionUtility.imageSize));
            }
        });

        imagePanel.setBackground(Color.white);
        imagePanel.setLayout(new GridLayout(ComputerVisionUtility.imageSize, ComputerVisionUtility.imageSize));
        imagePanel.setPreferredSize(new Dimension(765, 765));
        imagePanel.setBorder(BorderFactory.createTitledBorder("ИНТЕРАКТИВНАЯ ОБЛАСТЬ"));

        imagePanel.setImage(ComputerVisionUtility.defaultImage);

        return imagePanel;
    }

    @Bean(name = "interactionPanel")
    public JPanel interactionPanel() {
        JPanel interactionPanel = new JPanel();

        interactionPanel.setBackground(Color.white);
        interactionPanel.setLayout(new GridLayout(3, 1));
        interactionPanel.setPreferredSize(new Dimension(300, 765));

        interactionPanel.add(controlPanel());
        interactionPanel.add(infoPanel());
        interactionPanel.add(histogramPanel());

        return interactionPanel;
    }

    @Bean(name = "filterPanel")
    public JPanel filterPanel() {
        JPanel filterPanel = new JPanel();

        filterPanel.setBackground(Color.white);
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setPreferredSize(new Dimension(200, 765));
        filterPanel.setBorder(BorderFactory.createTitledBorder("ФИЛЬТР"));

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(sobelFilterButton());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(gaussFilterButton());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(sigmaLabel());
        filterPanel.add(sigmaSlider());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(cannyFilterButton());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(thresholdHLabel());
        filterPanel.add(thresholdHSlider());

        filterPanel.add(thresholdNLabel());
        filterPanel.add(thresholdNSlider());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(gaborFilterButton());

        filterPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        filterPanel.add(otsuMethodButton());

        return filterPanel;
    }

    @Bean(name = "controlPanel")
    public JPanel controlPanel() {
        JPanel controlPanel = new JPanel();

        controlPanel.setBackground(Color.white);
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBorder(BorderFactory.createTitledBorder("ДЕЙСТВИЯ"));

        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(hueLabel());
        controlPanel.add(hueSlider());

        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(saturationLabel());
        controlPanel.add(saturationSlider());

        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(valueLabel());
        controlPanel.add(valueSlider());

        return controlPanel;
    }

    @Bean(name = "infoPanel")
    public JPanel infoPanel() {
        JPanel infoPanel = new JPanel();

        infoPanel.setBackground(Color.white);
        infoPanel.setLayout(new GridLayout(1, 2));

        infoPanel.add(buttonPanel());
        infoPanel.add(pixelInfoPanel());

        return infoPanel;
    }

    @Bean(name = "buttonPanel")
    public JPanel buttonPanel() {
        JPanel buttonPanel = new JPanel();

        buttonPanel.setBackground(Color.white);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("ИЗОБРАЖЕНИЕ"));

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(loadImageButton());

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(saveImageButton());

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(defaultImageButton());

        return buttonPanel;
    }

    @Bean(name = "pixelInfoPanel")
    public JPanel pixelInfoPanel() {
        JPanel pixelInfoPanel = new JPanel();

        pixelInfoPanel.setBackground(Color.WHITE);
        pixelInfoPanel.setLayout(new BoxLayout(pixelInfoPanel, BoxLayout.Y_AXIS));
        pixelInfoPanel.setBorder(BorderFactory.createTitledBorder("ИНФОРМАЦИЯ"));

        pixelInfoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        pixelInfoPanel.add(selectedPixelRedLabel());
        pixelInfoPanel.add(selectedPixelGreenLabel());
        pixelInfoPanel.add(selectedPixelBlueLabel());

        pixelInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pixelInfoPanel.add(selectedPixelXLabel());
        pixelInfoPanel.add(selectedPixelYLabel());
        pixelInfoPanel.add(selectedPixelZLabel());

        pixelInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pixelInfoPanel.add(selectedPixelHLabel());
        pixelInfoPanel.add(selectedPixelSLabel());
        pixelInfoPanel.add(selectedPixelVLabel());

        pixelInfoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pixelInfoPanel.add(selectedPixelLLabel());
        pixelInfoPanel.add(selectedPixelALabel());
        pixelInfoPanel.add(selectedPixelBLabel());

        return pixelInfoPanel;
    }

    @Bean(name = "histogramPanel")
    public HistogramPanel histogramPanel() {
        HistogramPanel histogramPanel = new HistogramPanel();

        histogramPanel.setBackground(Color.white);
        histogramPanel.setBorder(BorderFactory.createTitledBorder("ГИСТОГРАММА L-КОМПОНЕНТЫ"));
        histogramPanel.setImage(ComputerVisionUtility.histogramImage);

        return histogramPanel;
    }

    @Bean("loadImageButton")
    public JButton loadImageButton() {
        JButton loadImageButton = new JButton("Добавить");

        loadImageButton.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            int ret = fileChooser.showDialog(null, "Открыть файл");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                fileChooser.updateUI();
                fileChooser.getUI();

                String path = selectedFile.getPath();
                String[] paths = path.split("/");

                BufferedImage image = null;
                try {
                    image = ImageIO.read(selectedFile);
                    if (image.getHeight() == image.getWidth()) {
                        ComputerVisionUtility.defaultImage = image;
                        ComputerVisionUtility.imageName = paths[paths.length - 1].split("\\.")[0];
                        imageController.updateImage();
                    }
                } catch (IOException ignored) {}
            }
        });

        loadImageButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return loadImageButton;
    }

    @Bean("saveImageButton")
    public JButton saveImageButton() {
        JButton saveImageButton = new JButton("Сохранить");

        saveImageButton.addActionListener(actionEvent -> imageController.saveImage());
        saveImageButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return saveImageButton;
    }

    @Bean("sobelFilterButton")
    public JButton sobelFilterButton() {
        JButton sobelFilterButton = new JButton("Фильтр Собеля");

        sobelFilterButton.addActionListener(actionEvent -> filterController.processSobelFilter());
        sobelFilterButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return sobelFilterButton;
    }

    @Bean("gaussFilterButton")
    public JButton gaussFilterButton() {
        JButton gaussFilterButton = new JButton("Фильтр Гаусса");

        gaussFilterButton.addActionListener(actionEvent -> filterController.processGaussFilter(sigmaSlider.getValue(), 9));
        gaussFilterButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return gaussFilterButton;
    }

    @Bean("cannyFilterButton")
    public JButton cannyFilterButton() {
        JButton cannyFilterButton = new JButton("Алгоритм Канни");

        cannyFilterButton.addActionListener(actionEvent -> filterController.processCannyFilter());
        cannyFilterButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return cannyFilterButton;
    }

    @Bean("gaborFilterButton")
    public JButton gaborFilterButton() {
        JButton gaborFilterButton = new JButton("Фильтр Габора");

        gaborFilterButton.addActionListener(actionEvent -> filterController.processGaborFilter());
        gaborFilterButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return gaborFilterButton;
    }

    @Bean("otsuMethodButton")
    public JButton otsuMethodButton() {
        JButton otsuMethodButton = new JButton("Метод Оцу");

        otsuMethodButton.addActionListener(actionEvent -> filterController.processOtsuMethod());
        otsuMethodButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return otsuMethodButton;
    }

    @Bean("defaultImageButton")
    public JButton defaultImageButton() {
        JButton defaultImageButton = new JButton("По умолчанию");

        defaultImageButton.addActionListener(actionEvent -> imageController.setDefaultImage());
        defaultImageButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return defaultImageButton;
    }

    @Bean("selectedPixelRedLabel")
    public JLabel selectedPixelRedLabel() {
        JLabel selectedPixelRedLabel = new JLabel();

        selectedPixelRedLabel.setText(" ");

        return selectedPixelRedLabel;
    }

    @Bean("selectedPixelGreenLabel")
    public JLabel selectedPixelGreenLabel() {
        JLabel selectedPixelGreenLabel = new JLabel();

        selectedPixelGreenLabel.setText(" ");

        return selectedPixelGreenLabel;
    }


    @Bean("selectedPixelBlueLabel")
    public JLabel selectedPixelBlueLabel() {
        JLabel selectedPixelBlueLabel = new JLabel();

        selectedPixelBlueLabel.setText(" ");

        return selectedPixelBlueLabel;
    }

    @Bean("selectedPixelXLabel")
    public JLabel selectedPixelXLabel() {
        JLabel selectedPixelXLabel = new JLabel();

        selectedPixelXLabel.setText(" ");

        return selectedPixelXLabel;
    }

    @Bean("selectedPixelYLabel")
    public JLabel selectedPixelYLabel() {
        JLabel selectedPixelYLabel = new JLabel();

        selectedPixelYLabel.setText(" ");

        return selectedPixelYLabel;
    }

    @Bean("selectedPixelZLabel")
    public JLabel selectedPixelZLabel() {
        JLabel selectedPixelZLabel = new JLabel();

        selectedPixelZLabel.setText("\t\t\t\t\t\t\t\t\t\tпиксель");

        return selectedPixelZLabel;
    }

    @Bean("selectedPixelHLabel")
    public JLabel selectedPixelHLabel() {
        JLabel selectedPixelHLabel = new JLabel();

        selectedPixelHLabel.setText("\t\t\t\t\t\t\t\tне выбран");

        return selectedPixelHLabel;
    }

    @Bean("selectedPixelSLabel")
    public JLabel selectedPixelSLabel() {
        JLabel selectedPixelSLabel = new JLabel();

        selectedPixelSLabel.setText(" ");

        return selectedPixelSLabel;
    }

    @Bean("selectedPixelVLabel")
    public JLabel selectedPixelVLabel() {
        JLabel selectedPixelVLabel = new JLabel();

        selectedPixelVLabel.setText(" ");

        return selectedPixelVLabel;
    }

    @Bean("selectedPixelLLabel")
    public JLabel selectedPixelLLabel() {
        JLabel selectedPixelLLabel = new JLabel();

        selectedPixelLLabel.setText(" ");

        return selectedPixelLLabel;
    }

    @Bean("selectedPixelALabel")
    public JLabel selectedPixelALabel() {
        JLabel selectedPixelALabel = new JLabel();

        selectedPixelALabel.setText(" ");

        return selectedPixelALabel;
    }

    @Bean("selectedPixelBLabel")
    public JLabel selectedPixelBLabel() {
        JLabel selectedPixelBLabel = new JLabel();

        selectedPixelBLabel.setText(" ");

        return selectedPixelBLabel;
    }

    @Bean("hueLabel")
    public JLabel hueLabel() {
        JLabel hueLabel = new JLabel("ЦВЕТОВОЙ ТОН");

        hueLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return hueLabel;
    }

    @Bean("saturationLabel")
    public JLabel saturationLabel() {
        JLabel saturationLabel = new JLabel("НАСЫЩЕННОСТЬ");

        saturationLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return saturationLabel;
    }

    @Bean("valueLabel")
    public JLabel valueLabel() {
        JLabel valueLabel = new JLabel("ЯРКОСТЬ");

        valueLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return valueLabel;
    }

    @Bean("sigmaLabel")
    public JLabel sigmaLabel() {
        JLabel sigmaLabel = new JLabel("СИГМА");

        sigmaLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return sigmaLabel;
    }

    @Bean("thresholdHLabel")
    public JLabel thresholdHLabel() {
        JLabel thresholdHLabel = new JLabel("ВЕРХНИЙ ПОРОГ");

        thresholdHLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return thresholdHLabel;
    }

    @Bean("thresholdNLabel")
    public JLabel thresholdNLabel() {
        JLabel thresholdNLabel = new JLabel("НИЖНИЙ ПОРОГ");

        thresholdNLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        return thresholdNLabel;
    }

    @Bean("hueSlider")
    public JSlider hueSlider() {
        JSlider hueSlider = new JSlider(-180, 180, 0);

        hueSlider.setMajorTickSpacing(180);
        hueSlider.setPaintLabels(true);
        hueSlider.addMouseListener((MouseReleasedListener) mouseEvent -> hsvController.changeHue(hueSlider.getValue()));

        return hueSlider;
    }

    @Bean("saturationSlider")
    public JSlider saturationSlider() {
        JSlider saturationSlider = new JSlider(-100, 100, 0);

        saturationSlider.setMajorTickSpacing(100);
        saturationSlider.setPaintLabels(true);
        saturationSlider.addMouseListener((MouseReleasedListener) mouseEvent -> hsvController.changeSaturation(saturationSlider.getValue()));

        return saturationSlider;
    }

    @Bean("valueSlider")
    public JSlider valueSlider() {
        JSlider valueSlider = new JSlider(-100, 100, 0);

        valueSlider.setMajorTickSpacing(100);
        valueSlider.setPaintLabels(true);
        valueSlider.addMouseListener((MouseReleasedListener) mouseEvent -> hsvController.changeValue(valueSlider.getValue()));

        return valueSlider;
    }

    @Bean("sigmaSlider")
    public JSlider sigmaSlider() {
        JSlider sigmaSlider = new JSlider(1, 10, 1);

        sigmaSlider.setMajorTickSpacing(9);
        sigmaSlider.setMinorTickSpacing(1);
        sigmaSlider.setSnapToTicks(true);
        sigmaSlider.setPaintLabels(true);

        return sigmaSlider;
    }

    @Bean("thresholdHSlider")
    public JSlider thresholdHSlider() {
        JSlider thresholdHSlider = new JSlider(0, 100, 30);

        thresholdHSlider.setMajorTickSpacing(100);
        thresholdHSlider.setMinorTickSpacing(1);
        thresholdHSlider.setSnapToTicks(true);
        thresholdHSlider.setPaintLabels(true);

        return thresholdHSlider;
    }

    @Bean("thresholdNSlider")
    public JSlider thresholdNSlider() {
        JSlider thresholdNSlider = new JSlider(0, 100, 20);

        thresholdNSlider.setMajorTickSpacing(100);
        thresholdNSlider.setMinorTickSpacing(1);
        thresholdNSlider.setSnapToTicks(true);
        thresholdNSlider.setPaintLabels(true);

        return thresholdNSlider;
    }
}
