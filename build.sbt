import org.goldenport.cozy.CozyPlugin.autoImport._
import sbt.Keys.*

lazy val root = project
  .in(file("."))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .settings(
    organization := ControlCenterProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.organization"),
    name := ControlCenterProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.name"),
    version := ControlCenterProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.component.version"),
    scalaVersion := ControlCenterProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.scalaVersion"),
    useCoursier := false,

    resolvers += Resolver.defaultLocal,
    resolvers += Resolver.file("Local Ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    resolvers += "Local Maven Repository" at ("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven",
    libraryDependencies ++= ControlCenterProjectYamlBuild.dependencies(cozyProjectMetadata.value),

    cozyGeneratorBackend := "cozy",
    cozyDelegateProjectDir := None,
    cozyDelegateCommand := Seq("cozy"),
    cozyManifestMetadata ++=
      cozyProjectMetadata.value.mapUnder("packaging.car.manifest_metadata") ++
        Map("component" -> ControlCenterProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "project.component.name"))
  )
