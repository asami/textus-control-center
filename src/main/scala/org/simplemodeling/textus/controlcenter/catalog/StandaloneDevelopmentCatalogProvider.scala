/*
 * @version Aug. 10, 2026
 */
package org.simplemodeling.textus.controlcenter.catalog

import java.net.{HttpURLConnection, URL}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}
import io.circe.parser.parse
import org.goldenport.cncf.component.identity.{ComponentId, ComponentIdentityProjection}
import org.yaml.snakeyaml.Yaml

final case class StandaloneDevelopmentRuntimeObservation(
  artifactId: String,
  version: String,
  baseUrl: String
)

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

  def observe(project: Path): Option[StandaloneDevelopmentRuntimeObservation] =
    _read_document(project.resolve("project.yaml")).flatMap(document => observe(document, _fetch_descriptor))

  private[catalog] def observe(
    document: String,
    fetch: String => Option[(Int, String)]
  ): Option[StandaloneDevelopmentRuntimeObservation] =
    for {
      root <- _yaml_mapping(document)
      projectmetadata <- root.get("project").map(_mapping)
      kind <- _string(projectmetadata, "kind") if kind == "car"
      namespace <- _string(projectmetadata, "namespace")
      id <- _string(projectmetadata, "id")
      projection <- Try(ComponentIdentityProjection.of(ComponentId.require(s"$namespace.$id"))).toOption
      componentmetadata <- projectmetadata.get("component").map(_mapping)
      version <- _string(componentmetadata, "version")
      configuration <- componentmetadata.get("config").map(_mapping)
      porttext <- _string(configuration, "textus.server.default-port")
      port <- Try(porttext.toInt).toOption.filter(port => 1 <= port && port <= 65535)
      baseurl = s"http://127.0.0.1:$port"
      response <- fetch(s"$baseurl/rest/v1/admin/assembly/descriptor")
      (status, body) = response
      if status / 100 == 2
      descriptor <- parse(body).toOption
      subsystem <- descriptor.hcursor.get[String]("subsystem").toOption.map(_.trim).filter(_.nonEmpty)
      responseversion <- descriptor.hcursor.get[String]("version").toOption.map(_.trim).filter(_.nonEmpty)
      if subsystem == projection.mavenArtifactId()
      if responseversion == version
    } yield StandaloneDevelopmentRuntimeObservation(projection.mavenArtifactId(), version, baseurl)

  private def _read_document(path: Path): Option[String] =
    Try(Files.readString(path, StandardCharsets.UTF_8)).toOption

  private def _yaml_mapping(document: String): Option[Map[String, Any]] =
    Try(new Yaml().load[AnyRef](document)).toOption.collect {
      case values: java.util.Map[?, ?] => _mapping(values)
    }

  private def _fetch_descriptor(url: String): Option[(Int, String)] =
    Try {
      val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("GET")
      connection.setConnectTimeout(400)
      connection.setReadTimeout(400)
      connection.setRequestProperty("Accept", "application/json")
      try {
        val status = connection.getResponseCode
        val body = if (status / 100 == 2) {
          Using.resource(connection.getInputStream)(input => String(input.readAllBytes(), StandardCharsets.UTF_8))
        } else ""
        status -> body
      } finally {
        connection.disconnect()
      }
    }.toOption

  private def _descriptor_source(sourceid: String, project: Path, observedat: Instant): Option[ManagedCarSource] = {
    val descriptor = project.resolve("project.yaml")
    if (!Files.isRegularFile(descriptor)) None
    else {
      for {
        document <- _read_document(descriptor)
        root <- _yaml_mapping(document)
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
