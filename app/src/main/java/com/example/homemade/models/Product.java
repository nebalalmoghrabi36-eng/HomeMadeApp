package com.example.homemade.models;

public class Product {
    private String id;
    private String name;
    private String description;
    private double price;
    private float rating;
    private int reviewCount;
    private String imageUrl;
    private boolean isFeatured;

    public Product() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public float getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public String getImageUrl() { return imageUrl; }
    public boolean isFeatured() { return isFeatured; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setRating(float rating) { this.rating = rating; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setFeatured(boolean featured) { isFeatured = featured; }
}