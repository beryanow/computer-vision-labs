package ru.nsu.g.beryanov.utility;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

@UtilityClass
public class ComputerVisionUtility {
    public int imageSize;
    public String imageName;
    public BufferedImage defaultImage;
    public BufferedImage histogramImage;

    public final String selectedPixelRedFormat = " R: %s";
    public final String selectedPixelGreenFormat = " G: %s";
    public final String selectedPixelBlueFormat = " B: %s";

    public final String selectedPixelXFormat = " X: %.7f";
    public final String selectedPixelYFormat = " Y: %.7f";
    public final String selectedPixelZFormat = " Z: %.7f";

    public final String selectedPixelHFormat = " H: %.7f";
    public final String selectedPixelSFormat = " S: %.7f";
    public final String selectedPixelVFormat = " V: %.7f";

    public final String selectedPixelLFormat = " L: %.7f";
    public final String selectedPixelAFormat = " A: %.7f";
    public final String selectedPixelBFormat = " B: %.7f";

    public void setFileChooserRussian() {
        UIManager.put("FileChooser.saveTitleText", "Сохранить");
        UIManager.put("FileChooser.openTitleText", "Открыть");
        UIManager.put("FileChooser.newFolderTitleText", "Новая папка");
        UIManager.put("FileChooser.newFolderButtonText", "Новая папка");
        UIManager.put("FileChooser.helpButtonText", "Помощь");
        UIManager.put("FileChooser.chooseButtonText", "Выбрать");
        UIManager.put("FileChooser.newFolderErrorText", "Ошибка при создании папки");
        UIManager.put("FileChooser.newFolderExistsErrorText", " Папка уже существует");
        UIManager.put("FileChooser.newFolderDialogPrompt", "Имя новой папки:");
        UIManager.put("FileChooser.newFolderDefaultName", "Новая папка");
        UIManager.put("FileChooser.newFileDefaultName", "Новый файл");
        UIManager.put("FileChooser.createButtonText", "Создать");

        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.lookInLabelText", "Смотреть в");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файла");

        UIManager.put("FileChooser.saveButtonText", "Сохранить");
        UIManager.put("FileChooser.saveButtonToolTipText", "Сохранить");
        UIManager.put("FileChooser.openButtonText", "Открыть");
        UIManager.put("FileChooser.openButtonToolTipText", "Открыть");
        UIManager.put("FileChooser.cancelButtonText", "Отмена");
        UIManager.put("FileChooser.cancelButtonToolTipText", "Отмена");

        UIManager.put("FileChooser.lookInLabelText", "Папка");
        UIManager.put("FileChooser.saveInLabelText", "Папка");
        UIManager.put("FileChooser.fileNameLabelText", "Имя файла");
        UIManager.put("FileChooser.filesOfTypeLabelText", "Тип файлов");

        UIManager.put("FileChooser.upFolderToolTipText", "На один уровень вверх");
        UIManager.put("FileChooser.newFolderToolTipText", "Создание новой папки");
        UIManager.put("FileChooser.listViewButtonToolTipText", "Список");
        UIManager.put("FileChooser.detailsViewButtonToolTipText", "Таблица");
        UIManager.put("FileChooser.fileNameHeaderText", "Имя");
        UIManager.put("FileChooser.fileSizeHeaderText", "Размер");
        UIManager.put("FileChooser.fileTypeHeaderText", "Тип");
        UIManager.put("FileChooser.fileDateHeaderText", "Изменен");
        UIManager.put("FileChooser.fileAttrHeaderText", "Атрибуты");

        UIManager.put("FileChooser.acceptAllFileFilterText", "Все файлы");
    }

    public double[][] getMatrixM() {
        return new double[][]{new double[]{0.4124564, 0.3575761, 0.1804375},
                new double[]{0.2126729, 0.7151522, 0.0721750},
                new double[]{0.0193339, 0.1191920, 0.9503041}};
    }

    public double[][] getSobelFilterX() {
        return new double[][]{new double[]{-1, 0, 1},
                new double[]{-2, 0, 2},
                new double[]{-1, 0, 1}};
    }

    public double[][] getSobelFilterY() {
        return new double[][]{new double[]{-1, -2, -1},
                new double[]{0, 0, 0},
                new double[]{1, 2, 1}};
    }

    public double[] evaluateHSV(int r, int g, int b) {
        ArrayList<Integer> colors = Lists.newArrayList(r, g, b);

        int max = Collections.max(colors);
        int min = Collections.min(colors);

        int colorMax = 255;
        int valueMax = 100;

        double hue = 0;
        if (max - min != 0) {
            if (max == r && g >= b) {
                hue = 60 * ((double) (g - b) / (max - min));
            } else if (max == r) {
                hue = 60 * ((double) (g - b) / (max - min)) + 360;
            } else if (max == g) {
                hue = 60 * ((double) (g - b) / (max - min)) + 120;
            } else if (max == b) {
                hue = 60 * ((double) (g - b) / (max - min)) + 240;
            }
        }

        double saturation = 0;
        if (max != 0) {
            saturation = (1 - (double) min / max) * 100;
        }

        double value = (double) Collections.max(colors) / colorMax * valueMax;

        return new double[]{hue, saturation, value};
    }

    public int[] evaluateRGB(double[] HSV) {
        double hNormalized = HSV[0];
        double sNormalized = HSV[1] / 100;
        double vNormalized = HSV[2] / 100;

        double red;
        double green;
        double blue;

        if (sNormalized == 0) {
            red = vNormalized;
            green = red;
            blue = green;
            return new int[]{red > 1 ? 255 : (int) (red < 0 ? 0 : red * 255 + 0.5), green > 1 ? 255 : (int) (green < 0 ? 0 : green * 255 + 0.5), blue > 1 ? 255 : (int) (blue < 0 ? 0 : blue * 255 + 0.5)};
        }

        double sectorH = hNormalized / 60;
        int sectorHInt = (int) sectorH;
        double sectorHPart = sectorH - sectorHInt;

        double p = vNormalized * (1 - sNormalized);
        double q = vNormalized * (1 - sNormalized * sectorHPart);
        double t = vNormalized * (1 - sNormalized * (1 - sectorHPart));

        switch (sectorHInt) {
            case 0:
                red = vNormalized;
                green = t;
                blue = p;
                break;
            case 1:
                red = q;
                green = vNormalized;
                blue = p;
                break;
            case 2:
                red = p;
                green = vNormalized;
                blue = t;
                break;
            case 3:
                red = p;
                green = q;
                blue = vNormalized;
                break;
            case 4:
                red = t;
                green = p;
                blue = vNormalized;
                break;
            default:
                red = vNormalized;
                green = p;
                blue = q;
                break;
        }

        return new int[]{red > 1 ? 255 : (int) (red < 0 ? 0 : red * 255 + 0.5), green > 1 ? 255 : (int) (green < 0 ? 0 : green * 255 + 0.5), blue > 1 ? 255 : (int) (blue < 0 ? 0 : blue * 255 + 0.5)};
    }

    public double[] evaluateXYZ(int r, int g, int b) {
        double rdouble = r / 255.0;
        double gdouble = g / 255.0;
        double bdouble = b / 255.0;

        double[][] matrixM = ComputerVisionUtility.getMatrixM();

        rdouble = rdouble > 0.04045 ? Math.pow((rdouble + 0.055) / 1.055, 2.2) : rdouble / 12.92;
        gdouble = gdouble > 0.04045 ? Math.pow((gdouble + 0.055) / 1.055, 2.2) : gdouble / 12.92;
        bdouble = bdouble > 0.04045 ? Math.pow((bdouble + 0.055) / 1.055, 2.2) : bdouble / 12.92;

        double[] matrixSRGB = new double[]{rdouble, gdouble, bdouble};

        double X = 100.0;
        double Y = 100.0;
        double Z = 100.0;

        for (int i = 0; i < 3; i++) {
            double result = 0;
            for (int j = 0; j < 3; j++) {
                result += matrixM[i][j] * matrixSRGB[j];
            }
            switch (i) {
                case 0:
                    X *= result;
                    break;
                case 1:
                    Y *= result;
                    break;
                case 2:
                    Z *= result;
                    break;
            }
        }

        return new double[]{X, Y, Z};
    }

    public double f(double x) {
        if (x > Math.pow(6.0 / 29, 3)) {
            return Math.pow(x, 1.0 / 3);
        } else {
            return 1.0 / 3 * Math.pow(29.0 / 6, 2) * x + 4.0 / 29;
        }
    }

    public double[] evaluateLAB(double[] XYZ) {
        double[] LAB = new double[3];

        double Xn = 95.04;
        double Yn = 100.0;
        double Zn = 108.88;

        LAB[0] = 116 * f(XYZ[1] / Yn) - 16;
        LAB[1] = 500 * (f(XYZ[0] / Xn) - f(XYZ[1] / Yn));
        LAB[2] = 200 * (f(XYZ[1] / Yn) - f(XYZ[2] / Zn));

        return LAB;
    }
}
