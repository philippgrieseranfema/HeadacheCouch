package com.happyheadache.home;

/**
 * Created by Alexandra Fritzen on 14/10/2016.
 */

class TriggerItem extends HomeItem {

    private int probability;
    private String[] factors;

    TriggerItem(int probability, String[] factors) {
        this.probability = probability;
        this.factors = factors;
    }

    String[] getFactors() {
        return factors;
    }

    int getProbability() {
        return probability;
    }
}
