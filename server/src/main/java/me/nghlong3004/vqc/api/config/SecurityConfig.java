package me.nghlong3004.vqc.api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.oauth.handler.OAuth2LoginSuccessHandler;
import me.nghlong3004.vqc.api.oauth.userinfo.ProviderAwareOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * @author nghlong3004 (Nguyen Hoang Long)
 * @since 5/24/2026
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private static final String COMMON_PATH = "/api";
  private static final String API_VERSION = "/v1";
  private static final String API_BASE_PATH = COMMON_PATH + API_VERSION;
  private static final String[] API_POST_PUBLIC = {
    API_BASE_PATH + "/auth/login",
    API_BASE_PATH + "/auth/register",
    API_BASE_PATH + "/auth/verify-email",
    API_BASE_PATH + "/auth/forgot-password",
    API_BASE_PATH + "/auth/reset-password",
    API_BASE_PATH + "/auth/refresh-token",
    API_BASE_PATH + "/auth/logout",
  };

  private static final String[] API_GET_PUBLIC = {};

  private static final String[] SWAGGER_PATHS = {
    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
  };

  private static final String[] INTERNAL_PATHS = {};

  private static final String[] ACTUATOR_GET_PUBLIC = {};

  @Value("${vqc.client.base-url}")
  private String webBaseUrl;

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final ProviderAwareOAuth2UserService providerAwareOAuth2UserService;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(SWAGGER_PATHS)
                    .permitAll()
                    .requestMatchers(INTERNAL_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, API_POST_PUBLIC)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, API_GET_PUBLIC)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, ACTUATOR_GET_PUBLIC)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        authorization ->
                            authorization.baseUri(API_BASE_PATH + "/oauth2/authorization"))
                    .redirectionEndpoint(
                        redirection -> redirection.baseUri(API_BASE_PATH + "/login/oauth2/code/*"))
                    .userInfoEndpoint(
                        userInfo -> userInfo.userService(providerAwareOAuth2UserService))
                    .successHandler(oAuth2LoginSuccessHandler))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .bearerTokenResolver(bearerTokenResolver())
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
    return http.build();
  }

  @Bean
  public BearerTokenResolver bearerTokenResolver() {
    DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
    return request -> {
      if (isPublicRequest(request)) {
        return null;
      }
      return headerResolver.resolve(request);
    };
  }

  private boolean isPublicRequest(HttpServletRequest request) {
    String path = request.getServletPath();
    String method = request.getMethod();

    if (HttpMethod.OPTIONS.matches(method)) {
      return true;
    }

    if (HttpMethod.POST.matches(method)) {
      for (String publicPath : API_POST_PUBLIC) {
        if (publicPath.equals(path)) {
          return true;
        }
      }
    }

    if (HttpMethod.GET.matches(method)) {
      for (String publicPath : API_GET_PUBLIC) {
        if (publicPath.equals(path)) {
          return true;
        }
      }

      for (String publicPath : ACTUATOR_GET_PUBLIC) {
        if (publicPath.equals(path)) {
          return true;
        }
      }
    }

    return false;
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
    authenticationProvider.setPasswordEncoder(passwordEncoder);
    var providerManager = new ProviderManager(authenticationProvider);
    providerManager.setEraseCredentialsAfterAuthentication(true);

    return providerManager;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(webBaseUrl));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");
    grantedAuthoritiesConverter.setAuthorityPrefix("");
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }
}
