/*
 *  version Jul. 18, 2026
 * @version Aug.  8, 2026
 */
package org.simplemodeling.textus.controlcenter.impl

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import org.goldenport.Consequence
import org.goldenport.cncf.config.ConfigurationAccess
import org.goldenport.cncf.context.{Capability, ExecutionContext, PrincipalId, SecurityLevel, SubjectKind}
import org.goldenport.cncf.security.{AuthenticationProvider, AuthenticationRequest, AuthenticationResult}
import org.goldenport.configuration.ResolvedConfiguration

/** Authenticates the Phase 1 launcher machine credential configured on Textus Control Center.
  *
  * This provider never grants human administration capabilities. An absent or
  * mismatched token is deliberately a non-match, so an assembly can later add
  * an independent human identity provider without this provider blocking it.
  */
private[controlcenter] final class TextusControlCenterLauncherRegistrationAuthenticationProvider(
  acceptedtoken: Option[String],
  principalid: String
) extends AuthenticationProvider {
  val name: String = TextusControlCenterLauncherRegistrationAuthenticationProvider.NAME

  def authenticate(
    request: AuthenticationRequest
  )(using ExecutionContext): Consequence[Option[AuthenticationResult]] =
    (acceptedtoken, request.accessToken) match {
      case (Some(expected), Some(actual)) if _matches(expected, actual) =>
        Consequence.success(Some(AuthenticationResult(
          principalId = PrincipalId(principalid),
          attributes = Map(
            "authenticated" -> "true",
            "role" -> "launcher_registration"
          ),
          capabilities = Set(Capability(TextusControlCenterLauncherRegistrationAuthenticationProvider.CAPABILITY)),
          level = SecurityLevel("service"),
          subjectKind = SubjectKind.Service
        )))
      case _ =>
        Consequence.success(None)
    }

  private def _matches(
    expected: String,
    actual: String
  ): Boolean =
    MessageDigest.isEqual(
      expected.getBytes(StandardCharsets.UTF_8),
      actual.getBytes(StandardCharsets.UTF_8)
    )
}

private[controlcenter] object TextusControlCenterLauncherRegistrationAuthenticationProvider {
  val NAME = "textus-control-center-launcher-registration"
  val CAPABILITY = "launcher_registration"
  val TOKEN_KEY = "textus-control-center.registration.authentication.token"
  val PRINCIPAL_ID_KEY = "textus-control-center.registration.authentication.principal-id"
  val DEFAULT_PRINCIPAL_ID = "textus-control-center-launcher"

  def fromConfiguration(
    configuration: ResolvedConfiguration
  ): TextusControlCenterLauncherRegistrationAuthenticationProvider =
    new TextusControlCenterLauncherRegistrationAuthenticationProvider(
      ConfigurationAccess.getString(configuration, TOKEN_KEY).map(_.trim).filter(_.nonEmpty),
      ConfigurationAccess.getString(configuration, PRINCIPAL_ID_KEY)
        .map(_.trim)
        .filter(_.nonEmpty)
        .getOrElse(DEFAULT_PRINCIPAL_ID)
    )
}
