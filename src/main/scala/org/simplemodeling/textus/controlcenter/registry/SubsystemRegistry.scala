/*
 *  version Jul. 19, 2026
 * @version Aug. 14, 2026
 */
package org.simplemodeling.textus.controlcenter.registry

import java.time.{Duration, Instant}

final case class RegistrationInput(
  protocolVersion: Int,
  instanceId: String,
  launcherKind: String,
  target: String,
  artifactId: Option[String],
  executionMode: Option[String],
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: Option[String],
  baseUrl: String,
  hostLabel: String,
  startedAt: Instant,
  launcherState: String,
  applicationUrl: Option[String] = None
)

final case class RegisteredSubsystem(
  protocolVersion: Int,
  instanceId: String,
  launcherKind: String,
  target: String,
  artifactId: Option[String],
  executionMode: Option[String],
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: Option[String],
  baseUrl: String,
  hostLabel: String,
  startedAt: Instant,
  lastSeenAt: Instant,
  launcherState: String,
  registrationPrincipalId: String,
  applicationUrl: Option[String] = None
)

final case class SubsystemProjection(
  protocolVersion: Int,
  instanceId: String,
  launcherKind: String,
  target: String,
  artifactId: Option[String],
  executionMode: Option[String],
  developmentDirectory: Option[String],
  subsystemName: Option[String],
  subsystemVersion: Option[String],
  runtimeVersion: Option[String],
  baseUrl: String,
  hostLabel: String,
  startedAt: Instant,
  lastSeenAt: Instant,
  launcherState: String,
  status: String,
  dashboardUrl: String,
  systemAdminUrl: String,
  applicationUrl: Option[String] = None
)

sealed trait RegistryError {
  def message: String
}

object RegistryError {
  final case class Invalid(message: String) extends RegistryError
  final case class Missing(instanceId: String) extends RegistryError {
    val message: String = s"Subsystem instance '$instanceId' is not registered."
  }
  final case class Conflict(instanceId: String, detail: String) extends RegistryError {
    val message: String = s"Subsystem instance '$instanceId' conflicts: $detail"
  }
  final case class Unauthorized(instanceId: String) extends RegistryError {
    val message: String = s"Registration principal is not authorized for subsystem instance '$instanceId'."
  }
}

object SubsystemRegistry {
  final val protocolVersion: Int = 1
  final val starting: String = "starting"
  final val running: String = "running"
  final val stopped: String = "stopped"
  final val stale: String = "stale"

  def register(
    current: Option[RegisteredSubsystem],
    input: RegistrationInput,
    principalId: String,
    receivedAt: Instant
  ): Either[RegistryError, RegisteredSubsystem] = {
    _validate_input(input, starting).flatMap { _ =>
      current match {
        case None => Right(_registered(input, principalId, receivedAt))
        case Some(existing) =>
          _validate_owner(existing, principalId).flatMap { _ =>
            _validate_identity(existing, input).map { _ =>
              _registered(input, principalId, receivedAt)
            }
          }
      }
    }
  }

  def heartbeat(
    current: Option[RegisteredSubsystem],
    input: RegistrationInput,
    principalId: String,
    receivedAt: Instant
  ): Either[RegistryError, RegisteredSubsystem] = {
    _validate_input(input, running).flatMap { _ =>
      current.toRight(RegistryError.Missing(input.instanceId)).flatMap { existing =>
        if (existing.launcherState == stopped) {
          Left(RegistryError.Conflict(input.instanceId, "the instance has already stopped"))
        } else {
          _validate_owner(existing, principalId).flatMap { _ =>
            _validate_identity(existing, input).map { _ =>
              _registered(input, principalId, receivedAt)
            }
          }
        }
      }
    }
  }

  def deregister(
    current: Option[RegisteredSubsystem],
    instanceId: String,
    principalId: String,
    receivedAt: Instant
  ): Either[RegistryError, RegisteredSubsystem] = {
    current.toRight(RegistryError.Missing(instanceId)).flatMap { existing =>
      _validate_owner(existing, principalId).map { _ =>
        existing.copy(lastSeenAt = receivedAt, launcherState = stopped)
      }
    }
  }

  def projection(
    source: RegisteredSubsystem,
    now: Instant,
    staleThreshold: Duration
  ): Either[RegistryError, SubsystemProjection] = {
    if (staleThreshold.isZero || staleThreshold.isNegative) {
      Left(RegistryError.Invalid("stale threshold must be positive"))
    } else {
      val status = _status(source, now, staleThreshold)
      val baseurl = source.baseUrl.stripSuffix("/")
      Right(SubsystemProjection(
        protocolVersion = source.protocolVersion,
        instanceId = source.instanceId,
        launcherKind = source.launcherKind,
        target = source.target,
        artifactId = source.artifactId,
        executionMode = source.executionMode,
        developmentDirectory = source.developmentDirectory,
        subsystemName = source.subsystemName,
        subsystemVersion = source.subsystemVersion,
        runtimeVersion = source.runtimeVersion,
        baseUrl = source.baseUrl,
        hostLabel = source.hostLabel,
        startedAt = source.startedAt,
        lastSeenAt = source.lastSeenAt,
        launcherState = source.launcherState,
        status = status,
        dashboardUrl = s"$baseurl/web/system/dashboard",
        systemAdminUrl = s"$baseurl/web/system/admin",
        applicationUrl = source.applicationUrl
      ))
    }
  }

  def ordered(
    sources: Vector[RegisteredSubsystem]
  ): Vector[RegisteredSubsystem] =
    sources.sortBy(source => (-source.lastSeenAt.toEpochMilli, source.instanceId))

  private def _registered(
    input: RegistrationInput,
    principalId: String,
    receivedAt: Instant
  ): RegisteredSubsystem =
    RegisteredSubsystem(
      protocolVersion = input.protocolVersion,
      instanceId = input.instanceId,
      launcherKind = input.launcherKind,
      target = input.target,
      artifactId = input.artifactId,
      executionMode = input.executionMode,
      developmentDirectory = input.developmentDirectory,
      subsystemName = input.subsystemName,
      subsystemVersion = input.subsystemVersion,
      runtimeVersion = input.runtimeVersion,
      baseUrl = input.baseUrl,
      hostLabel = input.hostLabel,
      startedAt = input.startedAt,
      lastSeenAt = receivedAt,
      launcherState = input.launcherState,
      registrationPrincipalId = principalId,
      applicationUrl = input.applicationUrl
    )

  private def _validate_input(
    input: RegistrationInput,
    expectedState: String
  ): Either[RegistryError, Unit] = {
    if (input.protocolVersion != protocolVersion) {
      Left(RegistryError.Invalid(s"Unsupported protocol version '${input.protocolVersion}'."))
    } else if (!_is_nonempty(input.instanceId)) {
      Left(RegistryError.Invalid("instanceId is required"))
    } else if (!_launcher_kinds.contains(input.launcherKind)) {
      Left(RegistryError.Invalid(s"Unknown launcher kind '${input.launcherKind}'."))
    } else if (!_is_nonempty(input.target)) {
      Left(RegistryError.Invalid("target is required"))
    } else if (!_is_http_url(input.baseUrl)) {
      Left(RegistryError.Invalid("baseUrl must be an absolute HTTP URL"))
    } else if (!_valid_application_url(input)) {
      Left(RegistryError.Invalid(_application_url_error(input)))
    } else if (!_is_nonempty(input.hostLabel)) {
      Left(RegistryError.Invalid("hostLabel is required"))
    } else if (input.launcherState != expectedState) {
      Left(RegistryError.Invalid(s"launcherState must be '$expectedState'."))
    } else {
      Right(())
    }
  }

  private def _validate_identity(
    existing: RegisteredSubsystem,
    input: RegistrationInput
  ): Either[RegistryError, Unit] = {
    if (existing.protocolVersion != input.protocolVersion) {
      Left(RegistryError.Conflict(input.instanceId, "protocol version differs"))
    } else if (existing.launcherKind != input.launcherKind) {
      Left(RegistryError.Conflict(input.instanceId, "launcher kind differs"))
    } else if (existing.target != input.target) {
      Left(RegistryError.Conflict(input.instanceId, "target differs"))
    } else if (_different_known_value(existing.artifactId, input.artifactId)) {
      Left(RegistryError.Conflict(input.instanceId, "artifact identity differs"))
    } else if (_different_known_value(existing.executionMode, input.executionMode)) {
      Left(RegistryError.Conflict(input.instanceId, "execution mode differs"))
    } else if (_different_known_value(existing.developmentDirectory, input.developmentDirectory)) {
      Left(RegistryError.Conflict(input.instanceId, "development directory differs"))
    } else {
      Right(())
    }
  }

  private def _validate_owner(
    existing: RegisteredSubsystem,
    principalId: String
  ): Either[RegistryError, Unit] =
    if (existing.registrationPrincipalId == principalId) Right(())
    else Left(RegistryError.Unauthorized(existing.instanceId))

  private def _status(
    source: RegisteredSubsystem,
    now: Instant,
    staleThreshold: Duration
  ): String = {
    if (source.launcherState == stopped) {
      stopped
    } else if (!now.isBefore(source.lastSeenAt.plus(staleThreshold))) {
      stale
    } else {
      source.launcherState
    }
  }

  private def _is_nonempty(value: String): Boolean = value.trim.nonEmpty

  private def _different_known_value(existing: Option[String], incoming: Option[String]): Boolean =
    existing.exists(value => incoming.exists(_ != value))

  private def _is_http_url(value: String): Boolean = {
    _http_uri(value).isDefined
  }

  private def _valid_application_url(input: RegistrationInput): Boolean =
    input.applicationUrl.forall(value => ApplicationUrlPolicy.sameOrigin(input.baseUrl, value).isRight)

  private def _application_url_error(input: RegistrationInput): String =
    input.applicationUrl.flatMap(value => ApplicationUrlPolicy.sameOrigin(input.baseUrl, value).left.toOption) match {
      case Some("must have the same origin as baseUrl") => "applicationUrl must have the same origin as baseUrl"
      case Some(ApplicationUrlPolicy.INVALID_PATH) => "applicationUrl must use a canonical /web application path"
      case _ => "applicationUrl must be an absolute HTTP(S) URL"
    }

  private def _http_uri(value: String): Option[java.net.URI] =
    scala.util.Try(java.net.URI.create(value)).toOption.filter { uri =>
      uri.isAbsolute &&
        Option(uri.getScheme).exists(scheme => Set("http", "https").contains(scheme.toLowerCase)) &&
        Option(uri.getHost).exists(_.nonEmpty)
    }

  private val _launcher_kinds = Set("textus", "cncf")
}
