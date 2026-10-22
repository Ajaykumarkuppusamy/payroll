package com.payroll.aj.identity.repository;

import com.payroll.aj.identity.entity.UserCountryAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Repository for {@link UserCountryAccess} RBAC mappings.
 *
 * <p>Provides a projection query that returns only the country IDs
 * a user is granted access to, avoiding full entity hydration during
 * authentication and JWT generation.</p>
 */
@Repository
public interface UserCountryAccessRepository extends JpaRepository<UserCountryAccess, Long> {

    /**
     * Returns the set of country IDs the given user may access.
     *
     * @param userId the system operator's surrogate key
     * @return an unordered set of allowed country IDs
     */
    @Query("SELECT uca.country.id FROM UserCountryAccess uca WHERE uca.user.id = :userId")
    Set<Long> findCountryIdsByUserId(@Param("userId") Long userId);
}
