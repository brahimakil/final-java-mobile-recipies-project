package com.example.recipieproject.model;

public class Recipe {
    private long id;
    private String title;
    private String ingredients;
    private String description;
    private String imagePath;
    private byte[] imageData;
    private long timestamp;

    public Recipe(String title, String ingredients, String description, String imagePath) {
        this.title = title;
        this.ingredients = ingredients;
        this.description = description;
        this.imagePath = imagePath;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
} 