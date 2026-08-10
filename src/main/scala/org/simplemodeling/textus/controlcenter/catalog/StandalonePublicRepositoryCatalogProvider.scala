/*
 *  version Jul. 24, 2026
 * @version Aug. 10, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import io.circe.parser.parse

/** Standalone HTTPS provider. The caller owns the bounded HTTP request. */
object StandalonePublicRepositoryCatalogProvider {
  def indexUrl(repository: PublicRepositoryCatalog): String =
    repository.catalogBaseUrl.stripSuffix("/car") + "/index.json"

  def indexArtifactIds(document: String): Either[String, Vector[String]] =
    parse(document).left.map(_ => "public-index-invalid").flatMap { json =>
      val cursor = json.hcursor
      val schema = cursor.get[String]("schema").orElse(cursor.get[String]("schemaVersion")).toOption
      val artifacts = cursor.downField("artifacts").as[Vector[io.circe.Json]].toOption.getOrElse(Vector.empty)
        .flatMap(_.hcursor.get[String]("artifactId").toOption)
        .map(_.trim).filter(_.matches("[A-Za-z0-9][A-Za-z0-9._-]*")).distinct.sorted
      Either.cond(schema.contains("cncf.component-repository-index.v1"), artifacts, "public-index-invalid")
    }
  def catalog_url(repository: PublicRepositoryCatalog, artifactId: String): String =
    s"${repository.catalogBaseUrl.stripSuffix("/")}/${URLEncoder.encode(artifactId.trim, StandardCharsets.UTF_8)}.yaml"

  def available(
    repository: PublicRepositoryCatalog,
    requestedArtifactId: String,
    document: String,
    observedAt: Instant
  ): ManagedCarSource = {
    val artifactid = requestedArtifactId.trim
    val declared = _value(document, "artifactId")
    if (_value(document, "kind").contains("car") && declared.contains(artifactid)) {
      ManagedCarSource(
        artifactid,
        ManagedCarSourceKind.PublicRepository,
        repository.sourceId,
        _nested_value(document, "component"),
        Vector(_value(document, "recommended"), _value(document, "latestStable")).flatten.distinct,
        ManagedCarRefreshState.Available,
        observedAt,
        None,
        None
      )
    } else {
      invalid(repository, artifactid, observedAt)
    }
  }

  def unavailable(
    repository: PublicRepositoryCatalog,
    artifactId: String,
    observedAt: Instant
  ): ManagedCarSource =
    _failed(repository, artifactId, ManagedCarRefreshState.Unavailable, observedAt, "public-catalog-unavailable")

  def invalid(
    repository: PublicRepositoryCatalog,
    artifactId: String,
    observedAt: Instant
  ): ManagedCarSource =
    _failed(repository, artifactId, ManagedCarRefreshState.Invalid, observedAt, "invalid-public-catalog")

  private def _failed(
    repository: PublicRepositoryCatalog,
    artifactid: String,
    state: ManagedCarRefreshState,
    observedat: Instant,
    diagnostic: String
  ): ManagedCarSource =
    ManagedCarSource(
      artifactid.trim,
      ManagedCarSourceKind.PublicRepository,
      repository.sourceId,
      None,
      Vector.empty,
      state,
      observedat,
      Some(diagnostic),
      None
    )

  private def _value(document: String, key: String): Option[String] =
    document.linesIterator.collectFirst {
      case line if line.nonEmpty && !line.head.isWhitespace && line.startsWith(s"$key:") =>
        line.stripPrefix(s"$key:").trim.stripPrefix("\"").stripSuffix("\"")
    }.filter(_.nonEmpty)

  private def _nested_value(document: String, key: String): Option[String] =
    document.linesIterator.collectFirst {
      case line if line.nonEmpty && line.head.isWhitespace && line.trim.startsWith(s"$key:") =>
        line.trim.stripPrefix(s"$key:").trim.stripPrefix("\"").stripSuffix("\"")
    }.filter(_.nonEmpty)
}
