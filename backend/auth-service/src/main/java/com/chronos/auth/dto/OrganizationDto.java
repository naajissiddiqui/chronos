package com.chronos.auth.dto;

import com.chronos.auth.entity.Organization;

import java.time.Instant;
import java.util.UUID;

public class OrganizationDto {

    private UUID id;
    private String name;
    private Instant createdAt;

    public OrganizationDto() {
    }

    public static OrganizationDto fromEntity(Organization organization) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setCreatedAt(organization.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
