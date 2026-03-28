import sbt._
import sbt.Keys._

ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "coralwrapper",
    organization := "com.google.coral",
    version := "0.1.0",
    Compile / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "bus",
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "common",
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "coralnpu",
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "coralnpu" / "float",
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "coralnpu" / "rvv",
      baseDirectory.value / "coralnpu" / "hdl" / "chisel" / "src" / "coralnpu" / "scalar"
    ),
    Compile / unmanagedResourceDirectories := Seq(
      baseDirectory.value / "src" / "main" / "resources",
      baseDirectory.value / "src" / "main" / "resources" / "hdl",
      baseDirectory.value / "src" / "main" / "resources" / "external"
    ),
    // Exclude test-only and optional modules not needed for core emission.
    Compile / unmanagedSources / excludeFilter := (
      "*Test.scala" || "Spi2TLUL.scala" || "TlulFifoAsync.scala" || "SpiMaster.scala"
    ),
    // Mirror coralnpu's autogen_scm_info behavior.
    Compile / sourceGenerators += Def.task {
      val outDir = (Compile / sourceManaged).value / "coralnpu"
      val outFile = outDir / "CoralNPUScmInfo.scala"
      val revision = sys.process
        .Process("git rev-parse --verify HEAD", baseDirectory.value / "coralnpu")
        .!!.trim
      val sanitizedRevision =
        if (revision.matches("[0-9a-fA-F]{40}")) revision else "f" * 40
      IO.createDirectory(outDir)
      IO.write(
        outFile,
        s"""package coralnpu
           |
           |class ScmInfo {
           |  val revision = BigInt("$sanitizedRevision", 16)
           |}
           |""".stripMargin
      )
      Seq(outFile)
    }.taskValue,
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % "7.0.0-RC1",
      "org.chipsalliance" %% "firtool-resolver" % "2.0.0",
      compilerPlugin(
        "org.chipsalliance" % "chisel-plugin" % "7.0.0-RC1"
          cross CrossVersion.full
      )
    ),
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      "-explaintypes",
      "-feature",
      "-language:reflectiveCalls",
      "-unchecked",
      "-deprecation",
      "-Xcheckinit",
      "-Xlint:infer-any",
      "-Xlint:unused"
    )
  )

