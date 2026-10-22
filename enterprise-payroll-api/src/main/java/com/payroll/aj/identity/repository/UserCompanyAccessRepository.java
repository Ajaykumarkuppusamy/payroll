package com.payroll.aj.identity.repository;

import com.payroll.aj.identity.entity.UserCompanyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Repository for {@link UserCompanyAccess} RBAC mappings.
 *
 * <p>Provides a projection query that returns only the company IDs
 * a user is granted access to, avoiding full entity hydration during
 * authentication and JWT generation.</p>
 */
@Repository
public interface UserCompanyAccessRepository extends JpaRepository<UserCompanyAccess, Long> {

    /**
     * Returns the set of company IDs the given user may access.
     *
     * @param userId the system operator's surrogate key
     * @return an unordered set of allowed company IDs
     */
    @Query("SELECT ucoa.company.id FROM UserCompanyAccess ucoa WHERE ucoa.user.id = :userId")
    Set<Long> findCompanyIdsByUserId(@Param("userId") Long userId);
}
