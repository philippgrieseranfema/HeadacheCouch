package com.happyheadache.models;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class Food {
    private String name;
    private float calories;
    private float totalFat;
    private float saturatedFat;
    private float polySaturatedFat;
    private float monoSaturatedFat;
    private float transFat;
    private float carbohydrate;
    private float dietaryFiber;
    private float sugar;
    private float protein;
    private float cholesterol;
    private float sodium;
    private float potassium;
    private float vitaminA;
    private float vitaminC;
    private float calcium;
    private float iron;

    public Food(String name, float calories, float totalFat, float saturatedFat, float polySaturatedFat, float monoSaturatedFat, float transFat, float carbohydrate, float dietaryFiber, float sugar, float protein, float cholesterol, float sodium, float potassium, float vitaminA, float vitaminC, float calcium, float iron) {
        this.calories = calories;
        this.totalFat = totalFat;
        this.saturatedFat = saturatedFat;
        this.polySaturatedFat = polySaturatedFat;
        this.monoSaturatedFat = monoSaturatedFat;
        this.transFat = transFat;
        this.carbohydrate = carbohydrate;
        this.dietaryFiber = dietaryFiber;
        this.sugar = sugar;
        this.protein = protein;
        this.cholesterol = cholesterol;
        this.sodium = sodium;
        this.potassium = potassium;
        this.vitaminA = vitaminA;
        this.vitaminC = vitaminC;
        this.calcium = calcium;
        this.iron = iron;
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ": "
                + calories + " calories, "
                + totalFat + " total fat, "
                + saturatedFat + " saturated fat, "
                + polySaturatedFat + " poly saturated fat, "
                + monoSaturatedFat + " mono saturated fat, "
                + transFat + " trans fat, "
                + carbohydrate + " carbohydrate, "
                + dietaryFiber + " dietary fiber, "
                + sugar + " sugar, "
                + protein + " protein, "
                + cholesterol + " cholesterol, "
                + sodium + " sodium, "
                + potassium + " potassium, "
                + vitaminA + " vitaminA, "
                + vitaminC + " vitaminC, "
                + calcium + " calcium, "
                + iron + " iron";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public float getTotalFat() {
        return totalFat;
    }

    public void setTotalFat(float totalFat) {
        this.totalFat = totalFat;
    }

    public float getSaturatedFat() {
        return saturatedFat;
    }

    public void setSaturatedFat(float saturatedFat) {
        this.saturatedFat = saturatedFat;
    }

    public float getPolySaturatedFat() {
        return polySaturatedFat;
    }

    public void setPolySaturatedFat(float polySaturatedFat) {
        this.polySaturatedFat = polySaturatedFat;
    }

    public float getMonoSaturatedFat() {
        return monoSaturatedFat;
    }

    public void setMonoSaturatedFat(float monoSaturatedFat) {
        this.monoSaturatedFat = monoSaturatedFat;
    }

    public float getTransFat() {
        return transFat;
    }

    public void setTransFat(float transFat) {
        this.transFat = transFat;
    }

    public float getCarbohydrate() {
        return carbohydrate;
    }

    public void setCarbohydrate(float carbohydrate) {
        this.carbohydrate = carbohydrate;
    }

    public float getDietaryFiber() {
        return dietaryFiber;
    }

    public void setDietaryFiber(float dietaryFiber) {
        this.dietaryFiber = dietaryFiber;
    }

    public float getSugar() {
        return sugar;
    }

    public void setSugar(float sugar) {
        this.sugar = sugar;
    }

    public float getProtein() {
        return protein;
    }

    public void setProtein(float protein) {
        this.protein = protein;
    }

    public float getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(float cholesterol) {
        this.cholesterol = cholesterol;
    }

    public float getSodium() {
        return sodium;
    }

    public void setSodium(float sodium) {
        this.sodium = sodium;
    }

    public float getPotassium() {
        return potassium;
    }

    public void setPotassium(float potassium) {
        this.potassium = potassium;
    }

    public float getVitaminA() {
        return vitaminA;
    }

    public void setVitaminA(float vitaminA) {
        this.vitaminA = vitaminA;
    }

    public float getVitaminC() {
        return vitaminC;
    }

    public void setVitaminC(float vitaminC) {
        this.vitaminC = vitaminC;
    }

    public float getCalcium() {
        return calcium;
    }

    public void setCalcium(float calcium) {
        this.calcium = calcium;
    }

    public float getIron() {
        return iron;
    }

    public void setIron(float iron) {
        this.iron = iron;
    }
}
