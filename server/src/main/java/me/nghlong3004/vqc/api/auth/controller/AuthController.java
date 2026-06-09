package me.nghlong3004.vqc.api.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.nghlong3004.vqc.api.auth.cookie.AuthCookieFactory;
import me.nghlong3004.vqc.api.auth.request.ForgotPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.LoginRequest;
import me.nghlong3004.vqc.api.auth.request.RegisterRequest;
import me.nghlong3004.vqc.api.auth.request.ResetPasswordRequest;
import me.nghlong3004.vqc.api.auth.request.VerifyEmailRequest;
import me.nghlong3004.vqc.api.auth.response.LoginResponse;
import me.nghlong3004.vqc.api.auth.service.AuthService;
import me.nghlong3004.vqc.api.exception.ErrorResponse;
import me.nghlong3004.vqc.api.user.response.UserResponse;
import me.nghlong3004.vqc.api.user.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and local account registration APIs")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final AuthCookieFactory authCookieFactory;

  @Operation(
      summary = "Login local user",
      description = "Authenticates a local user, returns an access token, and sets refresh token as an HttpOnly cookie.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Login payload",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = LoginRequest.class),
              examples =
                  @ExampleObject(
                      name = "LoginRequest",
                      value =
                          """
                          {
                            "email": "qc.demo@example.com",
                            "password": "password123"
                          }
                          """)))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Login successful. Refresh token is returned only in Set-Cookie.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class),
                examples =
                    @ExampleObject(
                        name = "LoginResponse",
                        value =
                            """
                            {
                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                              "tokenType": "Bearer",
                              "expiresInSeconds": 900,
                              "user": {
                                "publicId": "7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d",
                                "email": "qc.demo@example.com",
                                "displayName": "QC Demo",
                                "role": "QC_MEMBER",
                                "status": "ACTIVE",
                                "lastLoginAt": "2026-06-09T10:00:00Z"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Invalid email or password",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        name = "BadCredentials",
                        value =
                            """
                            {
                              "type": "https://vqc.nghlong3004.me/errors/bad-credentials",
                              "title": "Bad Credentials",
                              "status": 401,
                              "detail": "Invalid email or password.",
                              "instance": "/api/v1/auth/login",
                              "code": "BAD_CREDENTIALS"
                            }
                            """))),
    @ApiResponse(
        responseCode = "403",
        description = "Email is not verified",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        name = "EmailNotVerified",
                        value =
                            """
                            {
                              "type": "https://vqc.nghlong3004.me/errors/email-not-verified",
                              "title": "Email Not Verified",
                              "status": 403,
                              "detail": "Account email has not been verified",
                              "instance": "/api/v1/auth/login",
                              "code": "EMAIL_NOT_VERIFIED"
                            }
                            """)))
  })
  @PostMapping(
      value = "/login",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    var result = authService.login(request);
    var refreshCookie =
        authCookieFactory.refreshTokenCookie(
            result.refreshToken(), result.refreshTokenMaxAgeSeconds());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(result.response());
  }

  @Operation(
      summary = "Refresh access token",
      description =
          "Uses the HttpOnly refresh_token cookie to issue a new access token and rotate the refresh token cookie.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Token refresh successful. New refresh token is returned only in Set-Cookie.",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Missing, invalid, or expired refresh token",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(value = "/refresh-token", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LoginResponse> refreshToken(
      @CookieValue(name = "refresh_token", required = false) String refreshToken) {
    var result = authService.refreshToken(refreshToken);
    var refreshCookie =
        authCookieFactory.refreshTokenCookie(
            result.refreshToken(), result.refreshTokenMaxAgeSeconds());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
        .body(result.response());
  }

  @Operation(
      summary = "Logout",
      description = "Clears the HttpOnly refresh_token cookie for the current browser session.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Logout successful"),
    @ApiResponse(
        responseCode = "500",
        description = "Unexpected server error",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    var clearCookie = authCookieFactory.clearRefreshTokenCookie();
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
        .build();
  }

  @Operation(
      summary = "Verify email",
      description = "Activates a pending local account using the email verification token.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Email verification payload",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = VerifyEmailRequest.class),
              examples =
                  @ExampleObject(
                      name = "VerifyEmailRequest",
                      value =
                          """
                          {
                            "token": "raw-email-verification-token"
                          }
                          """)))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Email verified",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid, expired, or already used token",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/verify-email",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public UserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return authService.verifyEmail(request);
  }

  @Operation(
      summary = "Request password reset",
      description =
          "Creates a password reset token and sends a reset link when the email belongs to an account. Always returns 204 to avoid account enumeration.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Password reset email payload",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ForgotPasswordRequest.class),
              examples =
                  @ExampleObject(
                      name = "ForgotPasswordRequest",
                      value =
                          """
                          {
                            "email": "qc.demo@example.com"
                          }
                          """)))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Password reset request accepted"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/forgot-password",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
  }

  @Operation(
      summary = "Reset password",
      description = "Sets a new password using a valid password reset token.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Password reset confirmation payload",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ResetPasswordRequest.class),
              examples =
                  @ExampleObject(
                      name = "ResetPasswordRequest",
                      value =
                          """
                          {
                            "token": "raw-password-reset-token",
                            "newPassword": "newPassword123"
                          }
                          """)))
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Password reset successful"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request body or password reset token",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/reset-password",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
  }

  @Operation(
      summary = "Register local user",
      description =
          "Creates a pending local user account with the default QC_MEMBER role and sends an email verification link.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "Registration payload",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = RegisterRequest.class),
              examples =
                  @ExampleObject(
                      name = "RegisterRequest",
                      value =
                          """
                          {
                            "email": "qc.demo@example.com",
                            "password": "password123",
                            "displayName": "QC Demo"
                          }
                          """)))
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponse.class),
                examples =
                    @ExampleObject(
                        name = "UserResponse",
                        value =
                            """
                            {
                              "publicId": "7b7b7d42-5f42-4c5a-9281-8d1d36f6f59d",
                              "email": "qc.demo@example.com",
                              "displayName": "QC Demo",
                              "role": "QC_MEMBER",
                              "status": "PENDING_EMAIL_VERIFICATION",
                              "lastLoginAt": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        name = "ValidationError",
                        value =
                            """
                            {
                              "type": "https://vqc.nghlong3004.me/errors/validation-error",
                              "title": "Validation Error",
                              "status": 400,
                              "detail": "Validation failed for input data.",
                              "instance": "/api/v1/auth/register",
                              "code": "VALIDATION_ERROR",
                              "errors": [
                                {
                                  "field": "email",
                                  "message": "Email must be a valid email address."
                                }
                              ]
                            }
                            """))),
    @ApiResponse(
        responseCode = "409",
        description = "Email already exists",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class),
                examples =
                    @ExampleObject(
                        name = "EmailAlreadyExists",
                        value =
                            """
                            {
                              "type": "https://vqc.nghlong3004.me/errors/email-already-exists",
                              "title": "Email Already Exists",
                              "status": 409,
                              "detail": "Email is already in use.",
                              "instance": "/api/v1/auth/register",
                              "code": "EMAIL_ALREADY_EXISTS"
                            }
                            """))),
    @ApiResponse(
        responseCode = "500",
        description = "Unexpected server error",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ErrorResponse.class)))
  })
  @PostMapping(
      value = "/register",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(@Valid @RequestBody RegisterRequest request) {
    return userService.register(request);
  }
}
