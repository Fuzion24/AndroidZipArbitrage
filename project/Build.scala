import sbt._
import Keys._

object Versions {
  val scala     = "2.10.2"
  val scalatest = "1.9.1"
}

object AndroidMasterKeys extends Build {  
    lazy val masterKeys = Project("manifestParser", file("."), settings = masterKeySettings)
    lazy val masterKeySettings = Defaults.defaultSettings ++ Seq(
	    name := "Android Master Keys",
	    scalaVersion := Versions.scala,
	    libraryDependencies ++= Seq(
	    )
	)
}
