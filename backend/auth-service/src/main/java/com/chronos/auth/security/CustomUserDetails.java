package com.chronos.auth.security;

import com.chronos.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final UUID organizationId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
