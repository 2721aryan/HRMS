package com.company.hrms.auth.security;

import com.company.hrms.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {
    private final Long userId;
    private final Long employeeId;
    private final String username;
    private final String password;
    private final int tokenVersion;
    private final boolean active;
    private final List<String> roles;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserDetailsImpl(User user) {
        this.userId       = user.getId();
        this.employeeId   = user.getEmployeeId();
        this.username     = user.getUsername();
        this.password     = user.getPasswordHash();
        this.tokenVersion = user.getTokenVersion();
        this.active       = user.isActive();
        this.roles        = user.getRoles().stream().map(r -> r.getCode()).toList();
        this.authorities  = this.roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }

    public static UserDetailsImpl build(User user) { return new UserDetailsImpl(user); }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return user_notLocked(); }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return active; }

    // Lock check is done in service layer — return true here
    private boolean user_notLocked() { return true; }
}
