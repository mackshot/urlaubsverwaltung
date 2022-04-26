package org.synyx.urlaubsverwaltung.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@ExtendWith(MockitoExtension.class)
class ReloadAuthenticationAuthoritiesFilterTest {

    private ReloadAuthenticationAuthoritiesFilter sut;

    @Mock
    private PersonService personService;
    @Mock
    private SessionService<HttpSession> sessionService;

    @BeforeEach
    void setUp() {
        sut = new ReloadAuthenticationAuthoritiesFilter(personService, sessionService);
    }

    @Test
    void ensuresFilterSetsAuthenticationWithNewAuthorities() throws ServletException, IOException {

        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain filterChain = new MockFilterChain();

        request.getSession().setAttribute("reloadAuthorities", true);

        final Person signedInUser = new Person("marlene", "Muster", "Marlene", "muster@example.org");
        signedInUser.setPermissions(List.of(USER, OFFICE));
        when(personService.getSignedInUser()).thenReturn(signedInUser);

        SecurityContextHolder.getContext().setAuthentication(prepareAuthentication());

        sut.doFilterInternal(request, response, filterChain);

        final List<String> updatedAuthorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(toList());
        assertThat(updatedAuthorities).containsExactly("USER", "OFFICE");
        assertThat(request.getSession().getAttribute("reloadAuthorities")).isNull();

        verify(sessionService).save(any(HttpSession.class));
    }

    @Test
    void ensuresFilterSetsAuthenticationWithNewAuthoritiesButSessionIsNullDoNothing() {

        final MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getSession()).thenReturn(null);

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void ensuresFilterSetsNoNewAuthenticationIfReloadIsNotDefined() {

        final MockHttpServletRequest request = new MockHttpServletRequest();

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    void ensuresFilterSetsNoNewAuthenticationIfReloadIsFalse() {

        final MockHttpServletRequest request = new MockHttpServletRequest();

        request.getSession().setAttribute("reloadAuthorities", false);

        final boolean shouldNotFilter = sut.shouldNotFilter(request);
        assertThat(shouldNotFilter).isTrue();
    }

    private Authentication prepareAuthentication() {
        final Authentication authentication = mock(Authentication.class);
        final OidcUser oidcUser = mock(OidcUser.class);
        when(authentication.getPrincipal()).thenReturn(oidcUser);

        return authentication;
    }
}
