/*
 * @version Aug.  9, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import org.goldenport.cncf.component.identity.{ComponentId, ComponentIdentityProjection}
import org.yaml.snakeyaml.Yaml

/** Standalone host provider. cncf-car-lint: ignore provider/bootstrap boundary. */
object StandaloneDevelopmentCatalogProvider {
  def discover(root: DevelopmentRoot, observedAt: Instant): Vector[ManagedCarSource] = {
    val base = Path.of(root.path)
    if (!Files.isDirectory(base)) Vector.empty
    else Using.resource(Files.list(base)) { stream =>
      stream.iterator.asScala.toVector
        .filter(path => Files.isDirectory(path) && path.getFileName.toString.startsWith(root.includePrefix))
        .flatMap(path => _descriptor_source(root.sourceId, path, observedAt))
        .sortBy(source => (source.artifactId, source.sourceId))
    }
  }

  private def _descriptor_source(sourceid: String, project: Path, observedat: Instant): Option[ManagedCarSource] = {
    val descriptor = project.resolve("project.yaml")
    if (!Files.isRegularFile(descriptor)) None
    else {
      val document = Files.readString(descriptor, StandardCharsets.UTF_8)
      for {
        root <- Try(new Yaml().load[AnyRef](document)).toOption.collect {
          case values: java.util.Map[?, ?] => _mapping(values)
        }
        projectmetadata <- root.get("project").map(_mapping)
        kind <- _string(projectmetadata, "kind") if kind == "car"
        namespace <- _string(projectmetadata, "namespace")
        id <- _string(projectmetadata, "id")
        projection <- Try(ComponentIdentityProjection.of(ComponentId.require(s"$namespace.$id"))).toOption
      } yield {
        val componentmetadata = projectmetadata.get("component").map(_mapping).getOrElse(Map.empty)
        ManagedCarSource(
          projection.mavenArtifactId(),
          ManagedCarSourceKind.Development,
          sourceid,
          Some(projection.qualifiedId()),
          _string(componentmetadata, "version").toVector,
          ManagedCarRefreshState.Available,
          observedat,
          None,
          Some(project.toString)
        )
      }
    }
  }

  private def _mapping(value: Any): Map[String, Any] = value match {
    case values: java.util.Map[?, ?] =>
      values.asScala.collect { case (key: String, item) => key -> item }.toMap
    case _ => Map.empty
  }

  private def _string(values: Map[String, Any], key: String): Option[String] =
    values.get(key).collect { case value: String if value.trim.nonEmpty => value.trim }
}
