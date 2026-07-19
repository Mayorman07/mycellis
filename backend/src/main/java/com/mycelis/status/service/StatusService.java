package com.mycelis.status.service;

import com.mycelis.status.dto.PublicStatusResponse;

/**
 * Contract for assembling public, unauthenticated status page data.
 */
public interface StatusService {

    /**
     * Looks up a workspace by its public slug and assembles its status page.
     *
     * @param slug organization slug (public identifier, distinct from its UUID)
     * @return public status snapshot
     * @throws com.mycelis.shared.exception.ResourceNotFoundException if no organization has this slug
     */
    PublicStatusResponse getPublicStatus(String slug);
}
