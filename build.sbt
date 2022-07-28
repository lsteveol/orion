// See README.md for license details.

ThisBuild / scalaVersion     := "2.12.12"
//ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.github.lsteveol"


val chiselVersion = "3.5.1"
val chiselTestVersion = "0.5.0"

lazy val rocketChip = RootProject(file("./rocket-chip"))

lazy val root = (project in file("."))
  .settings(
    name := "orion",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.0" % "test",
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % chiselTestVersion % "test",
      "com.github.scopt" %% "scopt" % "4.0.1"
    ),
    scalacOptions ++= Seq(
      //"-Xsource:2.11",
      "-Xsource:2.13",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-P:chiselplugin:genBundleElements",  //THIS IS NEEDED FOR CHISEL 3.5+
    ),
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
  )
  .dependsOn(rocketChip)


