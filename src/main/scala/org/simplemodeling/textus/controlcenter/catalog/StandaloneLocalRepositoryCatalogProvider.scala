/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters.*

/** Standalone host provider. cncf-car-lint: ignore provider/bootstrap boundary. */
object StandaloneLocalRepositoryCatalogProvider {
  def discover(catalog: LocalRepositoryCatalog, observedAt: Instant): Vector[ManagedCarSource] = {
    val root = Path.of(catalog.catalogRoot)
    if (!Files.isDirectory(root)) Vector.empty
    else Files.list(root).iterator.asScala.toVector
      .filter(path => Files.isRegularFile(path) && path.getFileName.toString.endsWith(".yaml") && !path.getFileName.toString.endsWith(".model-metadata.yaml"))
      .flatMap(path => _catalog_source(catalog.sourceId, path, observedAt))
      .sortBy(source => (source.artifactId, source.sourceId))
  }

  private def _catalog_source(sourceid: String, path: Path, observedat: Instant): Option[ManagedCarSource] = {
    val document = Files.readString(path, StandardCharsets.UTF_8)
    val artifactid = _value(document, "artifactId")
    if (_value(document, "kind").contains("car") && artifactid.exists(_is_artifact_id))
      Some(ManagedCarSource(artifactid.get, ManagedCarSourceKind.LocalRepository, sourceid, None, Vector.empty, ManagedCarRefreshState.Available, observedat, None, Some(path.toString)))
        .map(_.copy(availableVersions = Vector(_value(document, "recommended"), _value(document, "latestStable")).flatten.distinct))
    else None
  }

  private def _value(document: String, key: String): Option[String] =
    document.linesIterator.collectFirst { case line if line.trim.startsWith(s"$key:") => line.trim.stripPrefix(s"$key:").trim.stripPrefix("\"").stripSuffix("\"") }.filter(_.nonEmpty)

  private def _is_artifact_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")
}
