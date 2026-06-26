package com.prashant.milkdairy.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single FAT or SNF differential slab rule.
 *
 * Example: From 3.0 To 4.0, diff = 0.10
 * Meaning: for every 0.1 step between "from" and "to", add "diff" to the rate.
 */
public class RuleSlab {

    public double from;
    public double to;
    public double diff;

    public RuleSlab(double from, double to, double diff) {
        this.from = from;
        this.to   = to;
        this.diff = diff;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("from", from);
        map.put("to",   to);
        map.put("diff", diff);
        return map;
    }
}