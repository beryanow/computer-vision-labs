package ru.nsu.g.beryanov.controller;

import lombok.SneakyThrows;
import nu.pattern.OpenCV;
import org.imgscalr.Scalr;
import org.opencv.calib3d.StereoBM;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.nsu.g.beryanov.model.sift.tools.Tools;
import ru.nsu.g.beryanov.view.ImagePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Service
public class EpipolarGeometryController {
    @Autowired
    @Qualifier("imagePanel")
    private ImagePanel imagePanel;

    @Autowired
    private HistogramController histogramController;

    @SneakyThrows
    public static BufferedImage Mat2BufferedImage(Mat matrix) {
        MatOfByte matOfByte = new MatOfByte();

        Imgcodecs.imencode(".jpg", matrix, matOfByte);
        byte[] byteArray = matOfByte.toArray();

        InputStream in = new ByteArrayInputStream(byteArray);
        BufferedImage bufImage = ImageIO.read(in);

        return bufImage;
    }

    public static void displayImage(Image img) {
        ImageIcon icon = new ImageIcon(img);

        JFrame frame = new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(img.getWidth(null) + 50, img.getHeight(null) + 50);

        JLabel lbl = new JLabel();
        lbl.setIcon(icon);

        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void evaluateDisparityMap() {
        OpenCV.loadLocally();
        Mat leftImage = Tools.fileToMat("src/main/resources/geometry/left.png");
        Mat rightImage = Tools.fileToMat("src/main/resources/geometry/right.png");

        Imgproc.cvtColor(leftImage, leftImage, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(rightImage, rightImage, Imgproc.COLOR_RGBA2GRAY);

        Mat disparity = new Mat();
        StereoBM stereoBM = StereoBM.create(48, 11);
        stereoBM.compute(leftImage, rightImage, disparity);

        BufferedImage disparityMap = Mat2BufferedImage(disparity);

        disparityMap = disparityMap.getSubimage(70, 10, 350, 350);
        disparityMap = Scalr.resize(disparityMap, imagePanel.getImage().getWidth(), imagePanel.getImage().getHeight());

        for (int i = 0; i < imagePanel.getImage().getWidth(); i++) {
            for (int j = 0; j < imagePanel.getImage().getHeight(); j++) {
                imagePanel.getImage().setRGB(i, j, disparityMap.getRGB(i, j));
            }
        }

        imagePanel.repaint();
        histogramController.updateHistogram();
    }
}
