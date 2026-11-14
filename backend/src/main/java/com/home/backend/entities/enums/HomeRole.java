package com.home.backend.entities.enums;

import java.util.EnumSet;
import java.util.Set;

import static com.home.backend.entities.enums.HomePermission.*;

public enum HomeRole {
    OWNER(EnumSet.allOf(HomePermission.class)),
    ADMIN(EnumSet.of(VIEW, EDIT_LISTS, INVITE, MANAGE_MEMBERS)),
    MEMBER(EnumSet.of(VIEW, EDIT_LISTS));

    private final Set<HomePermission> permissions;

    HomeRole(Set<HomePermission> permissions) {
        this.permissions = permissions;
    }

    public boolean can(HomePermission permission) {
        return permissions.contains(permission);
    }
}
