package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.auth.application.port.out.LoadUserPort;
import com.livecomerce.shared.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserDetailsAdapter implements UserDetailsService {

    private final LoadUserPort loadUserPort;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        UUID userId;
        try {
            userId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new UsernameNotFoundException("Invalid user id: " + id);
        }
        return loadUserPort.loadById(userId)
                .map(user -> {
                    var contact = user.getEmail() != null ? user.getEmail() : user.getPhone();
                    return new UserPrincipal(
                            user.getId(),
                            contact,
                            user.getPasswordHash(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                            user.isActive()
                    );
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
    }
}
