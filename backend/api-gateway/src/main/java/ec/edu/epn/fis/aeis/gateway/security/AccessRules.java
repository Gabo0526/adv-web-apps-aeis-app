package ec.edu.epn.fis.aeis.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * Reglas de autorización gruesa del gateway: qué rutas son públicas y qué
 * prefijos exigen rol ADMIN. Mapa simple de (método, patrón) -> rol, evaluado
 * en orden hasta el primer match (ver PLAN.md §3.2).
 */
public final class AccessRules {

    private record PublicRoute(HttpMethod method, String pattern) {
    }

    private record RoleRule(Set<HttpMethod> methods, String pattern, String role) {
    }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/api/auth/login"),
            new PublicRoute(HttpMethod.POST, "/api/auth/register"),
            new PublicRoute(HttpMethod.GET, "/api/auth/verify"),
            new PublicRoute(HttpMethod.POST, "/api/auth/forgot-password"),
            new PublicRoute(HttpMethod.POST, "/api/auth/reset-password"),
            new PublicRoute(HttpMethod.GET, "/api/payments/payphone/confirm"),
            new PublicRoute(null, "/actuator/**")
    );

    private static final Set<HttpMethod> WRITE_METHODS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH);

    private static final List<RoleRule> ROLE_RULES = List.of(
            new RoleRule(null, "/api/users/**", "ADMIN"),
            new RoleRule(null, "/api/rentals/admin/**", "ADMIN"),
            new RoleRule(null, "/api/excel/**", "ADMIN"),
            new RoleRule(WRITE_METHODS, "/api/periods/**", "ADMIN"),
            new RoleRule(WRITE_METHODS, "/api/locker-blocks/**", "ADMIN"),
            new RoleRule(WRITE_METHODS, "/api/lockers/**", "ADMIN")
    );

    private AccessRules() {
    }

    public static boolean isPublic(HttpMethod method, String path) {
        return PUBLIC_ROUTES.stream().anyMatch(route ->
                (route.method() == null || route.method().equals(method)) && MATCHER.match(route.pattern(), path));
    }

    public static String requiredRole(HttpMethod method, String path) {
        return ROLE_RULES.stream()
                .filter(rule -> (rule.methods() == null || rule.methods().contains(method))
                        && MATCHER.match(rule.pattern(), path))
                .map(RoleRule::role)
                .findFirst()
                .orElse(null);
    }
}
