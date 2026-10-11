package com.example.revolt.data.model;

import java.util.List;

public class PickupRequest {
    public static final String TYPE_RECYCLE = "recycle";
    public static final String TYPE_REUSE = "reuse";

    public long id;
    public String type;
    public String deviceName;
    public String category;
    public String categoryLabel;
    public String condition;
    public String conditionLabel;
    public int quantity;
    public String description;
    public String pickupAddress;
    public String preferredDate;
    public String imageUrl;
    public String status;
    public String statusLabel;
    public double estimatedWeightKg;
    public OrgSummary organization;
    public String createdAt;
    public String updatedAt;
    public List<RequestEvent> events;

    public boolean isActive() {
        return "pending".equals(status) || "scheduled".equals(status) || "picked_up".equals(status);
    }

    public boolean isCancellable() {
        return "pending".equals(status) || "scheduled".equals(status);
    }

    public static class OrgSummary {
        public long id;
        public String name;
        public String logoKey;
        public String city;
    }
}
