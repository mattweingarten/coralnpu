package coralwrapper

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.nio.file.StandardCopyOption
import scala.sys.process._

object EmitWrapper extends App {
  private def argValue(key: String, default: String): String = {
    args.find(_.startsWith(s"--$key=")).map(_.split("=", 2)(1)).getOrElse(default)
  }

  private val configName = argValue("config", "CoralNPUConfig")
  private val outDir = argValue("out-dir", "build/generated")

  val printOnly = argValue("print-only", "False").equalsIgnoreCase("true")

  val configOpt = Configs.byName(configName)
  if (configOpt.isEmpty) {
    System.err.println(s"Unknown CONFIG '$configName'. Supported: ${Configs.known.mkString(", ")}")
    sys.exit(2)
  }
  val cfg = configOpt.get

  if (printOnly) {
    println(s"CONFIG=${cfg.name}")
    println(s"MODULE=${cfg.moduleName}")
    println(s"OUT_DIR=$outDir")
    println(s"ARGS=${cfg.emitArgs.mkString(" ")}")
    sys.exit(0)
  }

  val root = Paths.get(".").toAbsolutePath.normalize
  val coralnpuDir = root.resolve("coralnpu")
  val coralnpuBuildSbt = coralnpuDir.resolve("build.sbt")
  val resourcesDir = coralnpuDir.resolve("src/main/resources")
  val hdlLink = resourcesDir.resolve("hdl")
  val externalLink = resourcesDir.resolve("external")
  val rstSyncLink = resourcesDir.resolve("RstSync.sv")
  val clockGateLink = resourcesDir.resolve("ClockGate.sv")

  Files.createDirectories(resourcesDir)
  if (!Files.isSymbolicLink(hdlLink)) {
    if (Files.exists(hdlLink)) Files.delete(hdlLink)
    Files.createSymbolicLink(hdlLink, Paths.get("../../../hdl"))
  }
  if (!Files.isSymbolicLink(externalLink)) {
    if (Files.exists(externalLink)) Files.delete(externalLink)
    Files.createSymbolicLink(externalLink, Paths.get("../../../external"))
  }
  if (!Files.isSymbolicLink(rstSyncLink)) {
    if (Files.exists(rstSyncLink)) Files.delete(rstSyncLink)
    Files.createSymbolicLink(rstSyncLink, Paths.get("../../../hdl/verilog/RstSync.sv"))
  }
  if (!Files.isSymbolicLink(clockGateLink)) {
    if (Files.exists(clockGateLink)) Files.delete(clockGateLink)
    Files.createSymbolicLink(clockGateLink, Paths.get("../../../hdl/verilog/ClockGate.sv"))
  }

  // Keep CoralNPU submodule untouched by writing a temporary overlay build.sbt.
  val generatedBuildSbt =
    s"""import sbt._
       |import sbt.Keys._
       |
       |ThisBuild / scalaVersion := "2.13.16"
       |
       |lazy val root = (project in file("."))
       |  .settings(
       |    name := "coralnpu-wrapper-emitter",
       |    organization := "com.google.coral",
       |    version := "0.1.0",
       |    Compile / unmanagedSourceDirectories ++= Seq(
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "bus",
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "common",
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu",
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "float",
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "rvv",
       |      baseDirectory.value / "hdl" / "chisel" / "src" / "coralnpu" / "scalar"
       |    ),
       |    Compile / unmanagedSources / excludeFilter := (
       |      "*Test.scala" || "Spi2TLUL.scala" || "TlulFifoAsync.scala" || "SpiMaster.scala"
       |    ),
       |    Compile / unmanagedResourceDirectories += baseDirectory.value / "src" / "main" / "resources",
       |    Compile / sourceGenerators += Def.task {
       |      val outDir = (Compile / sourceManaged).value / "coralnpu"
       |      val outFile = outDir / "CoralNPUScmInfo.scala"
       |      val revision = sys.process
       |        .Process("git rev-parse --verify HEAD", baseDirectory.value)
       |        .!!.trim
       |      val sanitizedRevision =
       |        if (revision.matches("[0-9a-fA-F]{40}")) revision else "f" * 40
       |      IO.createDirectory(outDir)
       |      IO.write(
       |        outFile,
       |        s\"\"\"package coralnpu
       |           |
       |           |class ScmInfo {
       |           |  val revision = BigInt("$$sanitizedRevision", 16)
       |           |}
       |           |\"\"\".stripMargin
       |      )
       |      Seq(outFile)
       |    }.taskValue,
       |    libraryDependencies ++= Seq(
       |      "org.chipsalliance" %% "chisel" % "7.0.0-RC1",
       |      "org.chipsalliance" %% "firtool-resolver" % "2.0.0",
       |      compilerPlugin(
       |        "org.chipsalliance" % "chisel-plugin" % "7.0.0-RC1"
       |          cross CrossVersion.full
       |      )
       |    ),
       |    scalacOptions ++= Seq(
       |      "-Ymacro-annotations",
       |      "-explaintypes",
       |      "-feature",
       |      "-language:reflectiveCalls",
       |      "-unchecked",
       |      "-deprecation",
       |      "-Xcheckinit",
       |      "-Xlint:infer-any",
       |      "-Xlint:unused"
       |    )
       |  )
       |""".stripMargin

  Files.write(
    coralnpuBuildSbt,
    generatedBuildSbt.getBytes(StandardCharsets.UTF_8)
  )

  try {
    val runMainArg = ("runMain coralnpu.EmitCore" +: cfg.emitArgs :+ s"--target-dir=$outDir").mkString(" ")
    val emitCmd = Seq("../scripts/sbt", runMainArg)
    val exit = Process(emitCmd, coralnpuDir.toFile).!
    if (exit != 0) sys.exit(exit)

    // Copy the primary top-level outputs to standard names too.
    val outPath = root.resolve(outDir)
    Files.createDirectories(outPath)
    val coralOutPath = coralnpuDir.resolve(outDir)
    val emittedTop = coralOutPath.resolve(s"${cfg.moduleName}.sv")
    val canonicalTop = outPath.resolve("generated.sv")
    if (Files.exists(emittedTop)) {
      Files.copy(
        emittedTop,
        outPath.resolve(s"${cfg.moduleName}.sv"),
        StandardCopyOption.REPLACE_EXISTING
      )
      Files.copy(emittedTop, canonicalTop, StandardCopyOption.REPLACE_EXISTING)
    }
  } finally {
    // Leave generated build.sbt as-is for incremental runs in CI/local wrappers.
  }
}
