package com.payroll.aj.identity.repository;

import com.payroll.aj.identity.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link AppUser} (system operators).
 *
 * <p>Supports flexible login: the same value is matched against both
 * {@code username} and {@code email} columns, letting operators
 * authenticate with either credential.</p>
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by username <em>or</em> email.
     *
     * <p>Callers should pass the same login value for both parameters:
     * {@code findByUsernameOrEmail(login, login)}.</p>
     *
     * @param username the login value to match against username
     * @param email    the login value to match against email
     * @return the matching user, if any
     */
    Optional<AppUser> findByUsernameOrEmail(String username, String email);
}
