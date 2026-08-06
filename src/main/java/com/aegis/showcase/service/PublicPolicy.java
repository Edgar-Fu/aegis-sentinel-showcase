package com.aegis.showcase.service;

/**
 * Deliberately simplified showcase policy. These values are illustrative and
 * are not the thresholds, calibration tables, or routing rules used by the
 * private Aegis Sentinel implementation.
 */
public final class PublicPolicy {
    public static final String VERSION = "public-showcase-1.0";

    public static final double MIN_SHARPNESS = 18.0;
    public static final double MIN_BRIGHTNESS = 35.0;
    public static final double MAX_BRIGHTNESS = 225.0;
    public static final double MAX_OVEREXPOSED_RATIO = 0.45;
    public static final int MIN_WIDTH = 320;
    public static final int MIN_HEIGHT = 200;

    public static final double MIN_AUTO_TRUST = 0.78;
    public static final double MIN_AUTO_CONFIDENCE = 0.75;

    private PublicPolicy() {}
}
