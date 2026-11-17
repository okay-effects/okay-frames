/** okay-frames: a meaning read by choices among finite options — the space, its laws, a reading as a value
 * (specs/frames.md). Pure, no dependency; the JVM, Scala.js and Scala Native */
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

ThisBuild / organization := "io.github.okay-effects"
ThisBuild / homepage := Some(url("https://github.com/okay-effects/okay-frames"))
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := "3.3.8"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-Wunused:all", "-Werror")
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / developers := List(Developer("sergey-scherbina", "Sergiy Shcherbyna", "sergey.scherbina@gmail.com",
  url("https://github.com/sergey-scherbina")))
ThisBuild / scmInfo := Some(ScmInfo(url("https://github.com/okay-effects/okay-frames"),
  "scm:git@github.com:okay-effects/okay-frames.git"))
ThisBuild / versionScheme := Some("early-semver")
/** RELEASES, ON GITHUB PAGES: `sbt publish` writes into `target/pages-maven/` (RELEASING.md) */
val pagesRepository = settingKey[File]("the Maven repository a release is written to, for GitHub Pages")
ThisBuild / pagesRepository := (ThisBuild / baseDirectory).value / "target" / "pages-maven"
ThisBuild / publishTo := Some(MavenCache("pages", (ThisBuild / pagesRepository).value))
ThisBuild / Compile / packageDoc / publishArtifact := false

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform).crossType(CrossType.Pure).in(file("core"))
  .settings(name := "okay-frames", libraryDependencies += "org.scalameta" %%% "munit" % "1.2.3" % Test)

lazy val root = (project in file(".")).aggregate(core.jvm, core.js, core.native)
  .settings(publish / skip := true)
