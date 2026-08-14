/*
 * @since   Aug. 14, 2026
 * @version Aug. 14, 2026
 */
package org.simplemodeling.textus.controlcenter.registry

import java.net.URI

private[controlcenter] object ApplicationUrlPolicy {
  val INVALID_URL: String = "must be an absolute HTTP(S) URL"
  val INVALID_PATH: String = "must use a canonical /web application path"

  def parse(value: String): Either[String, URI] =
    scala.util.Try(URI.create(value)).toOption.filter(_is_http_uri).toRight(INVALID_URL).flatMap { uri =>
      if (_is_application_path(uri)) Right(uri)
      else Left(INVALID_PATH)
    }

  def sameOrigin(baseUrl: String, applicationUrl: String): Either[String, Unit] =
    for {
      baseuri <- _base_uri(baseUrl).toRight(INVALID_URL)
      applicationuri <- parse(applicationUrl)
      _ <- Either.cond(_same_origin(baseuri, applicationuri), (), "must have the same origin as baseUrl")
    } yield ()

  private def _base_uri(value: String): Option[URI] =
    scala.util.Try(URI.create(value)).toOption.filter(_is_http_uri)

  private def _is_http_uri(uri: URI): Boolean =
    uri.isAbsolute &&
      Option(uri.getScheme).exists(scheme => Set("http", "https").contains(scheme.toLowerCase)) &&
      Option(uri.getHost).exists(_.nonEmpty) &&
      uri.getUserInfo == null &&
      uri.getQuery == null &&
      uri.getFragment == null

  private def _is_application_path(uri: URI): Boolean = {
    val rawpath = Option(uri.getRawPath).getOrElse("")
    val path = Option(uri.getPath).getOrElse("")
    val segments = path.split("/", -1).toVector.drop(1)
    rawpath == path &&
      (path == "/web" || path.startsWith("/web/")) &&
      path != "/web/system" && !path.startsWith("/web/system/") &&
      !path.contains("//") &&
      segments.forall(segment => segment.nonEmpty && segment != "." && segment != "..")
  }

  private def _same_origin(baseuri: URI, applicationuri: URI): Boolean =
    baseuri.getScheme.equalsIgnoreCase(applicationuri.getScheme) &&
      baseuri.getHost.equalsIgnoreCase(applicationuri.getHost) &&
      _effective_port(baseuri) == _effective_port(applicationuri)

  private def _effective_port(uri: URI): Int =
    if (uri.getPort >= 0) uri.getPort
    else if (uri.getScheme.equalsIgnoreCase("https")) 443
    else 80
}
