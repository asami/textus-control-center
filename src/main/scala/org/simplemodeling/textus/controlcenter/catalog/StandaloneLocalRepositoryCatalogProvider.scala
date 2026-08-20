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
    if (!Files.isDirectory(root)) Vector.empty // cncf-car-lint: ignore standalone local repository catalog provider filesystem boundary.
    else Files.list(root).iterator.asScala.toVector // cncf-car-lint: ignore standalone local repository catalog provider filesystem boundary.
      .filter(path => Files.isRegularFile(path) && path.getFileName.toString.endsWith(".yaml") && !path.getFileName.toString.endsWith(".model-metadata.yaml")) // cncf-car-lint: ignore standalone local repository catalog provider filesystem boundary.
      .flatMap(path => _catalog_source(catalog.sourceId, path, observedAt))
      .sortBy(source => (source.artifactId, source.sourceId))
  }

  private def _catalog_source(sourceid: String, path: Path, observedat: Instant): Option[ManagedCarSource] = {
    val document = Files.readString(path, StandardCharsets.UTF_8) // cncf-car-lint: ignore standalone local repository catalog provider filesystem boundary.
    val artifactid = _value(document, "artifactId")
    artifactid.filter(_ => _value(document, "kind").contains("car")).filter(_is_artifact_id).map { id =>
      val archives = _version_files(document).flatMap { case (version, locator) =>
        _archive_path(path, locator).filter(Files.isRegularFile(_)).map(_ => version) // cncf-car-lint: ignore standalone local repository catalog provider filesystem boundary.
      }.distinct
      if (archives.nonEmpty)
        ManagedCarSource(id, ManagedCarSourceKind.LocalRepository, sourceid, None, archives, ManagedCarRefreshState.Available, observedat, None, Some(path.toString))
      else
        ManagedCarSource(id, ManagedCarSourceKind.LocalRepository, sourceid, None, Vector.empty, ManagedCarRefreshState.Unavailable, observedat, Some("local-car-archive-unavailable"), Some(path.toString))
    }
  }

  private def _value(document: String, key: String): Option[String] =
    document.linesIterator.collectFirst { case line if line.trim.startsWith(s"$key:") => line.trim.stripPrefix(s"$key:").trim.stripPrefix("\"").stripSuffix("\"") }.filter(_.nonEmpty)

  private def _version_files(document: String): Vector[(String, String)] = {
    var currentversion: Option[String] = None
    document.linesIterator.foldLeft(Vector.empty[(String, String)]) { (z, line) =>
      val trimmed = line.trim
      if (trimmed.startsWith("- version:")) {
        currentversion = _value(trimmed, "- version")
        z
      } else if (trimmed.startsWith("file:")) {
        val next = currentversion.flatMap(version => _value(trimmed, "file").map(locator => version -> locator)).toVector
        currentversion = None
        z ++ next
      } else {
        z
      }
    }
  }

  private def _archive_path(catalog: Path, locator: String): Option[Path] =
    Option(catalog.getParent).flatMap(parent => Option(parent.getParent)).flatMap(parent => Option(parent.getParent)).flatMap(parent => Option(parent.getParent)).map(_.resolve(locator))

  private def _is_artifact_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")
}
