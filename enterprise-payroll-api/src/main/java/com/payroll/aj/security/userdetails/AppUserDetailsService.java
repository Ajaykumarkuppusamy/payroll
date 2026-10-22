package com.payroll.aj.security.userdetails;

import com.payroll.aj.identity.entity.AppUser;
import com.payroll.aj.identity.repository.AppUserRepository;
import com.payroll.aj.identity.repository.UserCompanyAccessRepository;
import com.payroll.aj.identity.repository.UserCountryAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Custom {@link UserDetailsService} that loads an {@link AppUser}
 * together with the user's RBAC scope (country and company access sets).
 *
 * <p>The login value is matched against <em>both</em> the
 * {@code username} and {@code email} columns, so operators can
 * authenticate with either credential.</p>
 *
 * <p>Spring Security's {@code AuthenticationManager} delegates to
 * this service during {@code UsernamePasswordAuthenticationToken}
 * authentication.</p>
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final UserCountryAccessRepository countryAccessRepository;
    private final UserCompanyAccessRepository companyAccessRepository;
    private final com.payroll.aj.organization.repository.CompanyRepository companyRepository;

    /**
     * Loads the user by username or email and assembles a
     * {@link UserPrincipal} with the full RBAC scope.
     *
     * @param login the username or email provided at login
     * @return a fully populated {@link UserPrincipal}
     * @throws UsernameNotFoundException if no matching user exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsernameOrEmail(login, login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with login: " + login));

        Set<Long> countryIds = countryAccessRepository.findCountryIdsByUserId(user.getId());
        Set<Long> companyIds = companyAccessRepository.findCompanyIdsByUserId(user.getId());

        if (user.getRole() == com.payroll.aj.common.enums.UserRole.SUPER_ADMIN && (companyIds == null || companyIds.isEmpty())) {
            companyIds = companyRepository.findByTenantIdWithCountry(user.getTenant().getId())
                    .stream()
                    .map(com.payroll.aj.organization.entity.Company::getId)
                    .collect(java.util.stream.Collectors.toSet());
        }

        return new UserPrincipal(user, countryIds, companyIds);
    }
}
