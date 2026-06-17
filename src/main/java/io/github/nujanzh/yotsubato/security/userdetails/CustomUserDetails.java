package io.github.nujanzh.yotsubato.security.userdetails;

import io.github.nujanzh.yotsubato.model.user.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/*
   For now isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired, isEnabled are always true
*/
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getId().toString();
    }

    public User getUser() {
        return user;
    }
}
