package club.muimi.backend.service.user;

import club.muimi.backend.exception.UnauthorizedException;
import club.muimi.backend.security.auth.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public LoginUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new UnauthorizedException();
        }
        return loginUser;
    }
}
