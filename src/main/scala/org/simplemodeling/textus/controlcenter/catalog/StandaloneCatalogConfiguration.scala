/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Duration

final case class DevelopmentRoot(
  sourceId: String,
  path: String,
  includePrefix: String = "textus-",
  explicitProjects: Vector[String] = Vector.empty
)

final case class LocalRepositoryCatalog(
  sourceId: String,
  catalogRoot: String
)

final case class PublicRepositoryCatalog(
  sourceId: String,
  catalogBaseUrl: String,
  subscriptions: Vector[String]
)

final case class StandaloneCatalogConfiguration(
  developmentRoots: Vector[DevelopmentRoot],
  localRepositoryCatalog: Option[LocalRepositoryCatalog],
  publicRepositoryCatalogs: Vector[PublicRepositoryCatalog],
  refreshTimeout: Duration
) {
  def isSubscribed(artifactId: String): Boolean =
    publicRepositoryCatalogs.exists(_.subscriptions.contains(artifactId))
}

sealed trait StandaloneCatalogConfigurationError {
  def message: String
}

object StandaloneCatalogConfigurationError {
  final case class Invalid(detail: String) extends StandaloneCatalogConfigurationError {
    val message: String = detail
  }
}

object StandaloneCatalogConfiguration {
  val DEFAULT_REFRESH_TIMEOUT: Duration = Duration.ofSeconds(5)
  val DEVELOPMENT_INCLUDE_PREFIX: String = "textus-"
  val DEFAULT_CATALOG_FILENAME: String = "catalog.yaml"

  def resolve(
    explicit: Option[StandaloneCatalogConfiguration],
    default: StandaloneCatalogConfiguration
  ): StandaloneCatalogConfiguration =
    explicit.getOrElse(default)

  def validate(
    configuration: StandaloneCatalogConfiguration
  ): Either[StandaloneCatalogConfigurationError, StandaloneCatalogConfiguration] = {
    val developmentids = configuration.developmentRoots.map(_.sourceId)
    val publicids = configuration.publicRepositoryCatalogs.map(_.sourceId)
    if (configuration.refreshTimeout.isZero || configuration.refreshTimeout.isNegative) {
      Left(StandaloneCatalogConfigurationError.Invalid("refreshTimeout must be positive"))
    } else if (configuration.refreshTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
      Left(StandaloneCatalogConfigurationError.Invalid("refreshTimeout must not exceed 30 seconds"))
    } else if (_has_duplicates(developmentids ++ configuration.localRepositoryCatalog.map(_.sourceId).toVector ++ publicids)) {
      Left(StandaloneCatalogConfigurationError.Invalid("sourceId values must be unique"))
    } else if (configuration.developmentRoots.exists(root => !_is_development_root(root))) {
      Left(StandaloneCatalogConfigurationError.Invalid("development roots must use absolute paths and the textus- include prefix"))
    } else if (configuration.localRepositoryCatalog.exists(catalog => !_is_absolute_path(catalog.catalogRoot))) {
      Left(StandaloneCatalogConfigurationError.Invalid("local repository catalog root must be an absolute path"))
    } else if (configuration.publicRepositoryCatalogs.exists(repository => !_is_public_repository(repository))) {
      Left(StandaloneCatalogConfigurationError.Invalid("public repositories require HTTPS catalog bases and unique explicit artifact subscriptions"))
    } else {
      Right(configuration)
    }
  }

  private def _is_development_root(root: DevelopmentRoot): Boolean =
    root.sourceId.trim.nonEmpty &&
      _is_absolute_path(root.path) &&
      root.includePrefix == DEVELOPMENT_INCLUDE_PREFIX &&
      root.explicitProjects.forall(_is_artifact_id)

  private def _is_public_repository(repository: PublicRepositoryCatalog): Boolean = {
    val uri = scala.util.Try(java.net.URI.create(repository.catalogBaseUrl)).toOption
    repository.sourceId.trim.nonEmpty &&
      uri.exists(value => value.isAbsolute && value.getScheme == "https" && value.getHost != null) &&
      repository.subscriptions.nonEmpty &&
      repository.subscriptions.forall(_is_artifact_id) &&
      !_has_duplicates(repository.subscriptions)
  }

  private def _is_absolute_path(value: String): Boolean = value.trim.startsWith("/")

  private def _is_artifact_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")

  private def _has_duplicates(values: Vector[String]): Boolean = values.map(_.trim).groupBy(identity).exists(_._2.size > 1)
}
