package com.home.backend.entities.identifiers;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HomeUserId implements Serializable {
    private Long homeId;
    private String userId;

    protected HomeUserId() {
    }

    public HomeUserId(Long homeId, String userId) {
        this.homeId = homeId;
        this.userId = userId;
    }

    public Long getHomeId() {
        return homeId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HomeUserId that)) return false;
        return Objects.equals(homeId, that.homeId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeId, userId);
    }
}