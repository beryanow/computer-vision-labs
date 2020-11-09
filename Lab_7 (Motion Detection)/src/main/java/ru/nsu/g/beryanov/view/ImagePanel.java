package ru.nsu.g.beryanov.view;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class ImagePanel extends JPanel {
    private BufferedImage image;

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 35, 30, 715, 710, this);
        }
    }
}
