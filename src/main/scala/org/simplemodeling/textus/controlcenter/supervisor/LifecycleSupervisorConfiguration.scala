/*
 * @version Jul. 22, 2026
 */
package org.simplemodeling.textus.controlcenter.supervisor

import java.net.URI
import java.time.Duration
import scala.util.Try

final case class LifecycleSupervisorConfiguration(
  supervisorId: String,
  endpoint: URI,
  timeout: Duration,
  tokenEnv: String
)

sealed trait LifecycleSupervisorConfigurationError {
  def message: String
}

object LifecycleSupervisorConfigurationError {
  final case class Invalid(detail: String) extends LifecycleSupervisorConfigurationError {
    val message: String = detail
  }
}

object LifecycleSupervisorConfiguration {
  val SUPERVISOR_ID: String = "textus-control-center.lifecycle.supervisor.id"
  val ENDPOINT: String = "textus-control-center.lifecycle.supervisor.endpoint"
  val TIMEOUT: String = "textus-control-center.lifecycle.supervisor.timeout"
  val TOKEN_ENV: String = "textus-control-center.lifecycle.supervisor.token-env"
  val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(5)
  val MAXIMUM_TIMEOUT: Duration = Duration.ofSeconds(30)

  def fromProperties(properties: Map[String, String]): Either[LifecycleSupervisorConfigurationError, Option[LifecycleSupervisorConfiguration]] = {
    val values = Vector(SUPERVISOR_ID, ENDPOINT, TIMEOUT, TOKEN_ENV).flatMap(key => properties.get(key).map(value => key -> value.trim)).toMap
    if (values.isEmpty) {
      Right(None)
    } else {
      for {
        supervisorid <- _required(values, SUPERVISOR_ID)
        endpointtext <- _required(values, ENDPOINT)
        tokenenv <- _required(values, TOKEN_ENV)
        endpoint <- _endpoint(endpointtext)
        timeout <- _timeout(values.get(TIMEOUT))
      } yield Some(LifecycleSupervisorConfiguration(supervisorid, endpoint, timeout, tokenenv))
    }
  }

  private def _required(values: Map[String, String], key: String): Either[LifecycleSupervisorConfigurationError, String] =
    values.get(key).filter(_.nonEmpty).toRight(LifecycleSupervisorConfigurationError.Invalid(s"$key is required when lifecycle supervisor dispatch is configured"))

  private def _endpoint(value: String): Either[LifecycleSupervisorConfigurationError, URI] =
    Try(URI.create(value)).toOption.filter(_is_loopback_endpoint).toRight(LifecycleSupervisorConfigurationError.Invalid("lifecycle supervisor endpoint must be an absolute loopback HTTP URI"))

  private def _timeout(value: Option[String]): Either[LifecycleSupervisorConfigurationError, Duration] = {
    value match {
      case Some(text) => _parse_duration(text).toRight(LifecycleSupervisorConfigurationError.Invalid("lifecycle supervisor timeout is invalid")).flatMap(_validate_timeout)
      case None => Right(DEFAULT_TIMEOUT)
    }
  }

  private def _parse_duration(value: String): Option[Duration] =
    if (value.matches("[0-9]+ms")) Try(Duration.ofMillis(value.dropRight(2).toLong)).toOption
    else Try(Duration.parse(if (value.matches("[0-9]+s")) s"PT${value.dropRight(1)}S" else value)).toOption

  private def _validate_timeout(timeout: Duration): Either[LifecycleSupervisorConfigurationError, Duration] =
    if (timeout.isNegative || timeout.isZero) Left(LifecycleSupervisorConfigurationError.Invalid("lifecycle supervisor timeout must be positive"))
    else if (timeout.compareTo(MAXIMUM_TIMEOUT) > 0) Left(LifecycleSupervisorConfigurationError.Invalid("lifecycle supervisor timeout must not exceed 30 seconds"))
    else Right(timeout)

  private def _is_loopback_endpoint(value: URI): Boolean = {
    val host = Option(value.getHost).map(_.toLowerCase)
    value.isAbsolute &&
      value.getScheme == "http" &&
      host.exists(Set("127.0.0.1", "::1", "localhost")) &&
      value.getUserInfo == null &&
      value.getQuery == null &&
      value.getFragment == null
  }
}
