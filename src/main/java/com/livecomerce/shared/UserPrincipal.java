package com.livecomerce.shared;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final List<GrantedAuthority> authorities;
    private final boolean active;

    public UserPrincipal(UUID userId, String email, String passwordHash,
                         List<GrantedAuthority> authorities, boolean active) {
        this.userId       = userId;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.authorities  = authorities;
        this.active       = active;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override public String getUsername()                                      { return email; }
    @Override public String getPassword()                                      { return passwordHash; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities()  { return authorities; }
    @Override public boolean isAccountNonLocked()                              { return active; }
    @Override public boolean isEnabled()                                       { return active; }
    @Override public boolean isAccountNonExpired()                             { return true; }
    @Override public boolean isCredentialsNonExpired()                         { return true; }
}
