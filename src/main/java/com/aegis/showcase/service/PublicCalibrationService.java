package com.aegis.showcase.service;

/** A coarse public calibration curve used only by the showcase edition. */
public final class PublicCalibrationService {
    public double calibrate(double rawConfidence) {
        double calibrated;
        if (rawConfidence >= 0.90) calibrated = 0.92;
        else if (rawConfidence >= 0.80) calibrated = 0.85;
        else if (rawConfidence >= 0.70) calibrated = 0.76;
        else if (rawConfidence >= 0.60) calibrated = 0.66;
        else calibrated = 0.50;
        return Math.round(calibrated * 1000.0) / 1000.0;
    }
}
