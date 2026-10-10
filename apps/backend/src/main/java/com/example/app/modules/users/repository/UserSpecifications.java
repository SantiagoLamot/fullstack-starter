package com.example.app.modules.users.repository;

import com.example.app.common.security.Role;
import com.example.app.modules.users.entity.User;
import com.example.app.modules.users.entity.UserStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the admin user listing (JPA Criteria, no hand-written queries). */
public final class UserSpecifications {

    private static final char ESCAPE = '\\';

    private UserSpecifications() {
    }

    /** Case-insensitive "contains" over email, first name and last name. Blank text matches everything. */
    public static Specification<User> textContains(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%";
            Predicate email = cb.like(cb.lower(root.get("email")), pattern, ESCAPE);
            Predicate firstName = cb.like(cb.lower(root.get("firstName")), pattern, ESCAPE);
            Predicate lastName = cb.like(cb.lower(root.get("lastName")), pattern, ESCAPE);
            return cb.or(email, firstName, lastName);
        };
    }

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? cb.conjunction() : cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }


    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
