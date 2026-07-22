package ec.edu.epn.fis.aeis.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.junit.jupiter.api.Assertions.*;

class AccessRulesTest {

    @Test
    void whenPublicAuthRoutes_thenNoTokenRequired() {
        assertTrue(AccessRules.isPublic(HttpMethod.POST, "/api/auth/login"));
        assertTrue(AccessRules.isPublic(HttpMethod.POST, "/api/auth/register"));
        assertTrue(AccessRules.isPublic(HttpMethod.GET, "/api/auth/verify"));
        assertTrue(AccessRules.isPublic(HttpMethod.POST, "/api/auth/forgot-password"));
        assertTrue(AccessRules.isPublic(HttpMethod.POST, "/api/auth/reset-password"));
        assertTrue(AccessRules.isPublic(HttpMethod.GET, "/api/payments/payphone/confirm"));
        assertTrue(AccessRules.isPublic(HttpMethod.GET, "/actuator/prometheus"));
    }

    @Test
    void whenSameAuthPathWithWrongMethod_thenNotPublic() {
        assertFalse(AccessRules.isPublic(HttpMethod.GET, "/api/auth/login"));
    }

    @Test
    void whenProtectedRoute_thenNotPublic() {
        assertFalse(AccessRules.isPublic(HttpMethod.GET, "/api/lockers"));
    }

    @Test
    void whenUsersRoute_thenRequiresAdminForAnyMethod() {
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.GET, "/api/users"));
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.GET, "/api/users/search"));
    }

    @Test
    void whenLockersReadOnly_thenNoRoleRequired() {
        assertNull(AccessRules.requiredRole(HttpMethod.GET, "/api/lockers/block/1"));
        assertNull(AccessRules.requiredRole(HttpMethod.GET, "/api/locker-blocks"));
    }

    @Test
    void whenLockersWrite_thenRequiresAdmin() {
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.POST, "/api/locker-blocks"));
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.PUT, "/api/lockers/1"));
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.DELETE, "/api/lockers/1"));
    }

    @Test
    void whenPeriodsRead_thenNoRoleRequired_butWriteRequiresAdmin() {
        assertNull(AccessRules.requiredRole(HttpMethod.GET, "/api/periods/active"));
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.POST, "/api/periods"));
    }

    @Test
    void whenRentalsAdminOrExcel_thenRequiresAdmin() {
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.GET, "/api/rentals/admin"));
        assertEquals("ADMIN", AccessRules.requiredRole(HttpMethod.POST, "/api/excel/generate"));
        assertNull(AccessRules.requiredRole(HttpMethod.GET, "/api/rentals/mine"));
    }
}
