package com.aegis.showcase.service;

import com.aegis.showcase.model.IntegrityMetrics;
import com.aegis.showcase.model.IntegrityReport;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImageIntegrityService {
    public IntegrityReport verify(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] gray = grayscale(image);
        double brightness = mean(gray);
        double sharpness = varianceOfLaplacian(gray);
        double overexposedRatio = pixelRatio(gray, 245.0, true);
        double underexposedRatio = pixelRatio(gray, 15.0, false);

        List<String> failures = new ArrayList<>();
        if (sharpness < PublicPolicy.MIN_SHARPNESS) failures.add("BLUR");
        if (brightness < PublicPolicy.MIN_BRIGHTNESS) failures.add("LOW_LIGHT");
        if (brightness > PublicPolicy.MAX_BRIGHTNESS || overexposedRatio > PublicPolicy.MAX_OVEREXPOSED_RATIO) {
            failures.add("OVEREXPOSED");
        }
        if (width < PublicPolicy.MIN_WIDTH || height < PublicPolicy.MIN_HEIGHT) failures.add("LOW_RESOLUTION");

        double sharpnessScore = clamp100(100.0 * sharpness / PublicPolicy.MIN_SHARPNESS);
        double brightnessScore = brightnessScore(brightness);
        double exposureScore = clamp100(100.0 * (1.0 - Math.max(overexposedRatio, underexposedRatio)));
        double resolutionScore = clamp100(100.0 * Math.min(
                width / (double) PublicPolicy.MIN_WIDTH,
                height / (double) PublicPolicy.MIN_HEIGHT));
        double total = round2(0.40 * sharpnessScore + 0.25 * brightnessScore
                + 0.20 * exposureScore + 0.15 * resolutionScore);

        Map<String, Double> components = new LinkedHashMap<>();
        components.put("sharpness", round2(sharpnessScore));
        components.put("brightness", round2(brightnessScore));
        components.put("exposure", round2(exposureScore));
        components.put("resolution", round2(resolutionScore));

        IntegrityMetrics metrics = new IntegrityMetrics(
                round2(sharpness), round2(brightness), round4(overexposedRatio), round4(underexposedRatio), width, height);
        return new IntegrityReport(failures.isEmpty() ? "PASS" : "REJECT", total, metrics, failures, components);
    }

    private double[][] grayscale(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] gray = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                gray[y][x] = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            }
        }
        return gray;
    }

    private double varianceOfLaplacian(double[][] gray) {
        int height = gray.length;
        int width = gray[0].length;
        if (height < 3 || width < 3) return 0.0;
        int count = (height - 2) * (width - 2);
        double sum = 0.0;
        double sumSquares = 0.0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double value = -4.0 * gray[y][x]
                        + gray[y - 1][x] + gray[y + 1][x]
                        + gray[y][x - 1] + gray[y][x + 1];
                sum += value;
                sumSquares += value * value;
            }
        }
        double average = sum / count;
        return Math.max(0.0, (sumSquares / count) - average * average);
    }

    private double mean(double[][] values) {
        double sum = 0.0;
        long count = 0;
        for (double[] row : values) {
            for (double value : row) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private double pixelRatio(double[][] values, double threshold, boolean above) {
        long matched = 0;
        long count = 0;
        for (double[] row : values) {
            for (double value : row) {
                if (above ? value >= threshold : value <= threshold) matched++;
                count++;
            }
        }
        return count == 0 ? 0.0 : matched / (double) count;
    }

    private double brightnessScore(double brightness) {
        double ideal = 130.0;
        if (brightness <= ideal) {
            return clamp100(100.0 * (brightness - PublicPolicy.MIN_BRIGHTNESS) / (ideal - PublicPolicy.MIN_BRIGHTNESS));
        }
        return clamp100(100.0 * (PublicPolicy.MAX_BRIGHTNESS - brightness) / (PublicPolicy.MAX_BRIGHTNESS - ideal));
    }

    private static double clamp100(double value) { return Math.max(0.0, Math.min(100.0, value)); }
    private static double round2(double value) { return Math.round(value * 100.0) / 100.0; }
    private static double round4(double value) { return Math.round(value * 10000.0) / 10000.0; }
}
