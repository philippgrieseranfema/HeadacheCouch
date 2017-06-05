package com.happyheadache.home;

/**
 * Created by Alexandra Fritzen on 14/10/2016.
 */

class MyHelpItem extends HomeItem {

    private int iconId;
    private String title;
    private String shortDescription;
    private String longDescription;

    MyHelpItem(int iconId, String title, String shortDescription, String longDescription) {
        this.iconId = iconId;
        this.title = title;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
    }

    int getIconId() {
        return iconId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    String getLongDescription() {
        return longDescription;
    }

    public void setLongDescription(String longDescription) {
        this.longDescription = longDescription;
    }
}
