package com.prashant.milkdairy.Model;

import java.util.Locale;

/**
 * A single FAT / SNF / Rate combination in the generated rate chart.
 */
public class RateEntry {

    public double fat;
    public double snf;
    public double rate;

    public RateEntry(double fat, double snf, double rate) {
        this.fat  = fat;
        this.snf  = snf;
        this.rate = rate;
    }

    /** Firestore / in-memory map key used to store and look up this rate. */
    public String key() {
        return String.format(Locale.US, "%.2f_%.2f", fat, snf);
    }
}