package ru.nsu.g.beryanov.model.sift.image;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;

public abstract class Image<T extends Region> {
    protected final List<T> regionOfInterest;
    protected final Mat matImage;

    public Image(Mat matImage) {
        regionOfInterest = new ArrayList<>();
        this.matImage = matImage;
    }

    public List<T> getRegionOfInterest() {
        return regionOfInterest;
    }

    public Mat getMatImage() {
        return matImage;
    }
}
