package com.mycelis.organization.service;

import com.mycelis.organization.constant.PlanTier;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.organization.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final int MAX_SLUG_ATTEMPTS = 100;

    private final OrganizationRepository organizationRepository;
    private final SlugGenerator slugGenerator;

    @Override
    public Organization createForOwner(String name, UUID ownerId) {
        String slug = generateUniqueSlug(name);

        Organization org = Organization.builder()
                .name(name)
                .slug(slug)
                .planTier(PlanTier.FREE)
                .ownerId(ownerId)
                .build();

        Organization saved = organizationRepository.save(org);
        log.info("Created organization: {} (slug={}, owner={})", saved.getName(), saved.getSlug(), ownerId);
        return saved;
    }

    private String generateUniqueSlug(String name) {
        String base = slugGenerator.slugify(name);
        if (base.isEmpty()) {
            base = "org";
        }

        String candidate = base;
        int attempt = 1;
        while (organizationRepository.existsBySlug(candidate)) {
            attempt++;
            if (attempt > MAX_SLUG_ATTEMPTS) {
                //  fallback to UUID suffix
                candidate = base + "-" + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
            candidate = base + "-" + attempt;
        }
        return candidate;
    }
}