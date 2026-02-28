package com.pgall.battle.filter;

import com.pgall.battle.entity.GameCharacter;
import com.pgall.battle.repository.GameCharacterRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP 기반 캐릭터 소유권 필터.
 *
 * 보호 (IP 일치해야 접근):
 * - DELETE  /api/characters/{id}
 * - PUT/POST /api/characters/{id}/** (equip, unequip, sell)
 * - POST    /api/gacha/{id}
 * - GET/POST /api/shop/{id}/**
 *
 * 공개 (누구나):
 * - GET  /api/characters, /ranking, /random-stats, /mine, /{id}
 * - POST /api/characters  (생성 - IP 중복은 서비스에서 검증)
 * - POST /api/battle       (attacker IP는 BattleController에서 검증)
 */
@Component
@RequiredArgsConstructor
public class IpOwnershipFilter extends OncePerRequestFilter {

    private final GameCharacterRepository characterRepository;

    private static final Pattern CHAR_PATTERN = Pattern.compile("^/api/characters/(\\d+)");
    private static final Pattern GACHA_PATTERN = Pattern.compile("^/api/gacha/(\\d+)");
    private static final Pattern SHOP_PATTERN = Pattern.compile("^/api/shop/(\\d+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        Long charId = extractProtectedCharacterId(path, method);

        if (charId != null) {
            String requestIp = extractIp(request);
            Optional<GameCharacter> opt = characterRepository.findById(charId);

            if (opt.isPresent()) {
                String ownerIp = opt.get().getIpAddress();
                if (ownerIp != null && !ownerIp.equals(requestIp)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"다른 유저의 캐릭터에 접근할 수 없습니다.\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private Long extractProtectedCharacterId(String path, String method) {
        // /api/characters/{id}... - DELETE, PUT, POST만 보호
        Matcher charMatcher = CHAR_PATTERN.matcher(path);
        if (charMatcher.find()) {
            if ("DELETE".equals(method) || "PUT".equals(method) || "POST".equals(method)) {
                return Long.valueOf(charMatcher.group(1));
            }
            return null;
        }

        // /api/gacha/{id}
        Matcher gachaMatcher = GACHA_PATTERN.matcher(path);
        if (gachaMatcher.find()) {
            return Long.valueOf(gachaMatcher.group(1));
        }

        // /api/shop/{id}/**
        Matcher shopMatcher = SHOP_PATTERN.matcher(path);
        if (shopMatcher.find()) {
            return Long.valueOf(shopMatcher.group(1));
        }

        return null;
    }

    /** 요청자 IP 추출 (프록시 헤더 우선) */
    public static String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
