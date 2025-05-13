package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String url = request.getRequestURI();

        if (url.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JWT 토큰이 필요합니다.");
            return;
        }

        processJwtToken(request, response, filterChain, bearerToken);
    }

    private void processJwtToken(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain,
            String bearerToken
    ) throws IOException {
        try {
            String jwt = jwtUtil.substringToken(bearerToken);
            Claims claims = jwtUtil.extractClaims(jwt);

            AuthUser authUser = createAuthUserFromClaims(claims);
            setSecurityContextAuthentication(authUser);

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            handleJwtException(response, "만료된 JWT 토큰입니다.", HttpServletResponse.SC_UNAUTHORIZED, e);
        } catch (MalformedJwtException | SecurityException e) {
            handleJwtException(response, "유효하지 않은 JWT 서명입니다.", HttpServletResponse.SC_UNAUTHORIZED, e);
        } catch (UnsupportedJwtException e) {
            handleJwtException(response, "지원되지 않는 JWT 토큰입니다.", HttpServletResponse.SC_BAD_REQUEST, e);
        } catch (Exception e) {
            handleJwtException(response, "서버 내부 오류", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    private AuthUser createAuthUserFromClaims(Claims claims) {
        return new AuthUser(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("nickname", String.class),
                UserRole.valueOf(claims.get("userRole", String.class))
        );
    }

    private void setSecurityContextAuthentication(AuthUser authUser) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + authUser.getUserRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void handleJwtException(
            HttpServletResponse response,
            String message,
            int statusCode,
            Exception e
    ) throws IOException {
        log.error(message, e);
        response.sendError(statusCode, message);
    }
}
