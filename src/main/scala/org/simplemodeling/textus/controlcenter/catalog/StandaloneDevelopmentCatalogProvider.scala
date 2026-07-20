/*
 * @version Jul. 21, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters.*

/** Standalone host provider. cncf-car-lint: ignore provider/bootstrap boundary. */
object StandaloneDevelopmentCatalogProvider {
  def discover(root: DevelopmentRoot, observedAt: Instant): Vector[ManagedCarSource] = {
    val base = Path.of(root.path)
    if (!Files.isDirectory(base)) Vector.empty
    else Files.list(base).iterator.asScala.toVector
      .filter(path => Files.isDirectory(path) && path.getFileName.toString.startsWith(root.includePrefix))
      .flatMap(path => _descriptor_source(root.sourceId, path, observedAt))
      .sortBy(source => (source.artifactId, source.sourceId))
  }

  private def _descriptor_source(sourceid: String, project: Path, observedat: Instant): Option[ManagedCarSource] = {
    val descriptor = project.resolve("project.yaml")
    if (!Files.isRegularFile(descriptor)) None
    else {
      val document = Files.readString(descriptor, StandardCharsets.UTF_8)
      val projectname = _section_value(document, "project", "name")
      val projectkind = _section_value(document, "project", "kind")
      val componentname = _section_value(document, "component", "name")
      if (projectkind.contains("car") && projectname.exists(_is_artifact_id))
        Some(ManagedCarSource(projectname.get, ManagedCarSourceKind.Development, sourceid, componentname, Vector.empty, ManagedCarRefreshState.Available, observedat, None, Some(project.toString)))
      else None
    }
  }

  private def _section_value(document: String, section: String, key: String): Option[String] = {
    val lines = document.linesIterator.toVector
    val start = lines.indexWhere(_.trim == s"$section:")
    if (start < 0) None
    else lines.drop(start + 1).takeWhile(line => line.takeWhile(_ == ' ').length >= 2 || line.trim.isEmpty).collectFirst {
      case line if line.trim.startsWith(s"$key:") => line.trim.stripPrefix(s"$key:").trim.stripPrefix("\"").stripSuffix("\"")
    }.filter(_.nonEmpty)
  }

  private def _is_artifact_id(value: String): Boolean = value.matches("[A-Za-z0-9][A-Za-z0-9._-]*")
}
