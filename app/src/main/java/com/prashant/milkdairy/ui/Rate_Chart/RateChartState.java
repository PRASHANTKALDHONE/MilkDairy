package com.prashant.milkdairy.ui.Rate_Chart;

import com.prashant.milkdairy.Model.RateEntry;
import com.prashant.milkdairy.Model.RuleSlab;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RateChartState {

    public static final int MAX_RULE_ROWS = 2;
    public static final double FAT_STEP = 0.1;
    public static final double SNF_STEP = 0.1;

    public String milkType = "cow";
    public double baseRate = 0;
    public double fatMin   = 3.0;
    public double fatMax   = 8.0;
    public double snfMin   = 7.5;
    public double snfMax   = 10.5;

    public List<RuleSlab> fatRules = new ArrayList<>();
    public List<RuleSlab> snfRules = new ArrayList<>();

    public Map<String, Double> generatedChart = new HashMap<>();
    public List<Double> generatedFatValues    = new ArrayList<>();
    public List<Double> generatedSnfValues    = new ArrayList<>();

    public String lastGeneratedAt;
    public boolean formulaDirty = true;

    public double calculateSlabIncrease(double value, List<RuleSlab> rules) {
        double total = 0;
        for (RuleSlab rule : rules) {
            if (value <= rule.from) continue;
            double effectiveTo = Math.min(value, rule.to);
            double range       = effectiveTo - rule.from;
            if (range > 0) total += (range / 0.1) * rule.diff;
        }
        return Math.max(0, total);
    }

    public double calculateRate(double fat, double snf) {
        return round2(baseRate
                + calculateSlabIncrease(fat, fatRules)
                + calculateSlabIncrease(snf, snfRules));
    }

    public void generateChart() {
        generatedChart.clear();
        generatedFatValues.clear();
        generatedSnfValues.clear();

        for (double fat = fatMin; fat <= fatMax + 1e-9; fat = round2(fat + FAT_STEP))
            generatedFatValues.add(round2(fat));

        for (double snf = snfMin; snf <= snfMax + 1e-9; snf = round2(snf + SNF_STEP))
            generatedSnfValues.add(round2(snf));

        for (double fat : generatedFatValues)
            for (double snf : generatedSnfValues)
                generatedChart.put(chartKey(fat, snf), calculateRate(fat, snf));

        lastGeneratedAt = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date());
        formulaDirty    = false;
    }

    public void rebuildAxesFromChartKeys() {
        List<Double> fats = new ArrayList<>();
        List<Double> snfs = new ArrayList<>();

        for (String key : generatedChart.keySet()) {
            String[] parts = key.split("_");
            if (parts.length != 2) continue;
            try {
                double fat = Double.parseDouble(parts[0]);
                double snf = Double.parseDouble(parts[1]);
                if (!fats.contains(fat)) fats.add(fat);
                if (!snfs.contains(snf)) snfs.add(snf);
            } catch (NumberFormatException ignored) { }
        }

        java.util.Collections.sort(fats);
        java.util.Collections.sort(snfs);
        generatedFatValues = fats;
        generatedSnfValues = snfs;
    }

    public List<RateEntry> toSortedEntryList() {
        List<RateEntry> list = new ArrayList<>();
        for (Map.Entry<String, Double> entry : generatedChart.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length != 2) continue;
            try {
                list.add(new RateEntry(
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1]),
                        entry.getValue()));
            } catch (NumberFormatException ignored) { }
        }
        list.sort(Comparator.comparingDouble((RateEntry e) -> e.fat)
                .thenComparingDouble(e -> e.snf));
        return list;
    }

    public String chartKey(double fat, double snf) {
        return String.format(Locale.US, "%.2f_%.2f", fat, snf);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}