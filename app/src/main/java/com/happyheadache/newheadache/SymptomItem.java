package com.happyheadache.newheadache;

/**
 * Created by Alexandra Fritzen on 14/10/2016.
 */

class SymptomItem {

    private int iconId;
    private String id;
    private String title;
    private String description;

    SymptomItem(int iconId, String id, String title, String description) {
        this.iconId = iconId;
        this.id = id;
        this.title = title;
        this.description = description;
    }

    String getDescription() {
        return description;
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

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
