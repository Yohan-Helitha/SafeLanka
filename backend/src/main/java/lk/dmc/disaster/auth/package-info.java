/**
 * Authentication: sign-up with SMS verification, login, JWT access tokens and rotating refresh
 * tokens. Bridges the verified identity to the shared {@code ActingUser}, so module code and
 * {@code @RequiresRole} checks do not know how the user logged in.
 *
 * <p>Public contract: none. Other modules use {@code shared.actor.ActingUserContext}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Auth")
package lk.dmc.disaster.auth;
