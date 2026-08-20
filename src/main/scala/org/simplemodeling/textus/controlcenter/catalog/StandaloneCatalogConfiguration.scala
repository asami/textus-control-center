/*
 *  version Jul. 21, 2026
 * @version Aug. 10, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.time.Duration
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import org.yaml.snakeyaml.Yaml

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

  def parseYaml(text: String): Either[StandaloneCatalogConfigurationError, StandaloneCatalogConfiguration] =
    val parsed: Either[StandaloneCatalogConfigurationError, Map[String, Any]] = scala.util.Try(new Yaml().load[AnyRef](text)).toOption match {
      case Some(values: java.util.Map[?, ?]) => Right(values.asScala.collect { case (key: String, value) => key -> value.asInstanceOf[Any] }.toMap)
      case _ => Left(StandaloneCatalogConfigurationError.Invalid("catalog configuration must be a YAML mapping"))
    }
    parsed.flatMap { root =>
      def string(value: Any): Option[String] = Option(value).collect { case s: String if s.trim.nonEmpty => s.trim }
      def mapping(value: Any): Map[String, Any] = value match { case m: java.util.Map[?, ?] => m.asScala.collect { case (k: String, v) => k -> v }.toMap; case _ => Map.empty }
      def mappings(value: Any): Vector[Map[String, Any]] = value match { case v: java.util.List[?] => v.asScala.toVector.map(mapping); case _ => Vector.empty }
      def strings(value: Any): Vector[String] = value match { case v: java.util.List[?] => v.asScala.toVector.flatMap(string); case _ => Vector.empty }
      val schema = string(root.getOrElse("schema", ""))
      val refresh = mapping(root.getOrElse("refresh", Map.empty))
      val development = mapping(root.getOrElse("development", Map.empty))
      val local = mapping(root.getOrElse("local-repository", Map.empty))
      val public = mappings(root.getOrElse("public-repositories", Vector.empty))
      val timeout = string(refresh.getOrElse("timeout", "5s")).flatMap(value => scala.util.Try(Duration.parse(if (value.matches("[0-9]+s")) s"PT${value.dropRight(1)}S" else value)).toOption).getOrElse(DEFAULT_REFRESH_TIMEOUT)
      val configuration = StandaloneCatalogConfiguration(
        mappings(development.getOrElse("roots", Vector.empty)).map(v => DevelopmentRoot(string(v.getOrElse("id", "")).getOrElse(""), string(v.getOrElse("path", "")).getOrElse(""), string(v.getOrElse("include-prefix", DEVELOPMENT_INCLUDE_PREFIX)).getOrElse(DEVELOPMENT_INCLUDE_PREFIX), strings(v.getOrElse("explicit-projects", Vector.empty)))),
        if (local.isEmpty) None else Some(LocalRepositoryCatalog(string(local.getOrElse("id", "")).getOrElse(""), string(local.getOrElse("catalog-root", "")).getOrElse(""))),
        public.map(v => PublicRepositoryCatalog(string(v.getOrElse("id", "")).getOrElse(""), string(v.getOrElse("catalog-base-url", "")).getOrElse(""), strings(v.getOrElse("subscriptions", Vector.empty)))),
        timeout
      )
      if (schema.contains("textus-control-center.catalog.v1")) validate(configuration)
      else Left(StandaloneCatalogConfigurationError.Invalid("unsupported catalog schema"))
    }

  def load(path: Path): Either[StandaloneCatalogConfigurationError, StandaloneCatalogConfiguration] =
    scala.util.Try(Files.readString(path)).toEither.left.map(_ => StandaloneCatalogConfigurationError.Invalid("catalog configuration is unavailable")).flatMap(parseYaml) // cncf-car-lint: ignore standalone catalog provider configuration boundary.

  def configuredFile(explicit: Option[String], home: Option[String]): Option[Path] =
    explicit.map(_.trim).filter(_.nonEmpty).map(Path.of(_)).orElse(home.map(_.trim).filter(_.nonEmpty).map(value => Path.of(value, DEFAULT_CATALOG_FILENAME)))

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
      Left(StandaloneCatalogConfigurationError.Invalid("public repositories require HTTPS catalog bases and unique artifact subscriptions"))
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
      repository.subscriptions.forall(_is_artifact_id) &&
      !_has_duplicates(repository.subscriptions)
  }

  private def _is_absolute_path(value: String): Boolean = value.trim.startsWith("/")

  private def _is_artifact_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")

  private def _has_duplicates(values: Vector[String]): Boolean = values.map(_.trim).groupBy(identity).exists(_._2.size > 1)
}
