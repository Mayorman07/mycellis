package com.mycelis.organization.service;

import com.mycelis.organization.entity.Organization;

import java.util.UUID;

public interface OrganizationService {

    /**
     * Create a new organization with the given owner. Generates a unique slug.
     * Must be called within an active transaction (from UserServiceImpl).
     */
    Organization createForOwner(String name, UUID ownerId);
}