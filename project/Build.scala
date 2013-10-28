import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object Versions {
  val scala     = "2.10.3"
  val scalatest = "1.9.1"
}

object AndroidMasterKeys extends Build {  
  val projectName = "AndroidMasterKeys"
  val projVer = "0.1"
  val mainClassName = "android.master.keys.Main"

  lazy val masterKeys = Project(projectName, file("."), settings = masterKeySettings)
  lazy val masterKeySettings = Defaults.defaultSettings ++ assemblySettings ++ Seq(
    name := projectName,
    version := projVer,
    scalaVersion := Versions.scala,
    jarName in assembly := projectName + ".jar",
    mainClass in assembly := Some(mainClassName),
    mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
      {
        case "application.conf" => MergeStrategy.concat
        case "reference.conf"   => MergeStrategy.concat
        case "mime.types"       => MergeStrategy.filterDistinctLines
        case PathList("org", "hamcrest", _ @ _*) => MergeStrategy.first
        case PathList("com", "google", "common", _ @ _*) => MergeStrategy.first
        case PathList("org", "xmlpull", _ @ _*) => MergeStrategy.first
        case PathList(ps @ _*) if ps.last.toLowerCase.startsWith("notice") ||
                                  ps.last.toLowerCase == "license" || 
                                  ps.last.toLowerCase == "license.txt" || 
                                  ps.last.toLowerCase == "asm-license.txt" ||
                                  ps.last.endsWith(".html") => MergeStrategy.rename 
        case PathList("META-INF", xs @ _*) =>
          (xs map {_.toLowerCase}) match {
            case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
              MergeStrategy.discard
            case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa")  =>
              MergeStrategy.discard
            case "services" :: xs =>
              MergeStrategy.filterDistinctLines
            case _ => MergeStrategy.deduplicate
          }
        case _ => MergeStrategy.deduplicate
      }
    },
    libraryDependencies ++= Seq("com.github.scopt" %% "scopt" % "3.1.0",
                               "org.scalatest" %% "scalatest" % Versions.scalatest % "test",
                               "org.apache.commons" % "commons-compress" % "1.5"),
    mainClass in Compile := Some(mainClassName),
    aggregate in run := false
  )
}
