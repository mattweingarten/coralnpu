import sbt._
import sbt.Keys._

ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "coralnpu-chisel",
    organization := "com.google.coral",
    version := "0.1.0",
    Compile / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / "hdl" / "chisel" / "src" / "bus",
      baseDirectory.value / "hdl" / "chisel" / "src" / "common",
      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu",
      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "float",
      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "rvv",
      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "scalar"
    ),
    Compile / unmanagedResourceDirectories ++= Seq(
      baseDirectory.value / "hdl" / "verilog",
      baseDirectory.value / "external"
    ),
    Compile / unmanagedSources / excludeFilter := (
      "*Test.scala" || "Spi2TLUL.scala" || "TlulFifoAsync.scala" || "SpiMaster.scala"
    ),
    // Mirror Bazel autogen_scm_info() with the same fallback revision.
    Compile / sourceGenerators += Def.task {
      val outDir = (Compile / sourceManaged).value / "coralnpu"
      val outFile = outDir / "CoralNPUScmInfo.scala"
      val revision = sys.process
        .Process("git rev-parse --verify HEAD", baseDirectory.value)
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

// Emit the same target as:
// //hdl/chisel/src/coralnpu:rvv_core_mini_verification_axi_cc_library_emit_verilog
addCommandAlias(
  "emitRvvCoreMiniVerificationAxi",
  "runMain coralnpu.EmitCore --moduleName=RvvCoreMiniVerification --enableVerification=True --enableFetchL0=False --fetchDataBits=128 --lsuDataBits=128 --enableRvv=True --enableFloat=True --useAxi --target-dir=build/verilog"
)
