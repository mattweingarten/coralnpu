package coralwrapper

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}
import java.io.FileOutputStream
import scala.sys.process._

import chisel3.RawModule
import coralnpu._
import _root_.circt.stage.ChiselStage

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

  final case class CoreEmitOptions(
    moduleName: String = "Core",
    enableFetchL0: Option[Boolean] = None,
    fetchDataBits: Option[Int] = None,
    enableRvv: Option[Boolean] = None,
    enableFloat: Option[Boolean] = None,
    enableVerification: Option[Boolean] = None,
    lsuDataBits: Option[Int] = None,
    itcmSizeKBytes: Option[Int] = None,
    dtcmSizeKBytes: Option[Int] = None,
    useAxi: Boolean = false,
    useTlul: Boolean = false
  )

  def parseOptions(emitArgs: Seq[String]): CoreEmitOptions = {
    emitArgs.foldLeft(CoreEmitOptions()) { (opt, arg) =>
      if (arg.startsWith("--moduleName=")) opt.copy(moduleName = arg.split("=", 2)(1))
      else if (arg.startsWith("--enableFetchL0=")) opt.copy(enableFetchL0 = Some(arg.split("=", 2)(1).toBoolean))
      else if (arg.startsWith("--fetchDataBits=")) opt.copy(fetchDataBits = Some(arg.split("=", 2)(1).toInt))
      else if (arg.startsWith("--enableRvv=")) opt.copy(enableRvv = Some(arg.split("=", 2)(1).toBoolean))
      else if (arg.startsWith("--enableFloat=")) opt.copy(enableFloat = Some(arg.split("=", 2)(1).toBoolean))
      else if (arg.startsWith("--enableVerification=")) opt.copy(enableVerification = Some(arg.split("=", 2)(1).toBoolean))
      else if (arg.startsWith("--lsuDataBits=")) opt.copy(lsuDataBits = Some(arg.split("=", 2)(1).toInt))
      else if (arg.startsWith("--itcmSizeKBytes=")) opt.copy(itcmSizeKBytes = Some(arg.split("=", 2)(1).toInt))
      else if (arg.startsWith("--dtcmSizeKBytes=")) opt.copy(dtcmSizeKBytes = Some(arg.split("=", 2)(1).toInt))
      else if (arg == "--useAxi" || arg == "--useAxi=True") opt.copy(useAxi = true)
      else if (arg == "--useTlul" || arg == "--useTlul=True") opt.copy(useTlul = true)
      else opt
    }
  }

  def finalModuleBaseName(opts: CoreEmitOptions): String = {
    val itcm = opts.itcmSizeKBytes.getOrElse(Parameters.itcmSizeKBytesDefault)
    val dtcm = opts.dtcmSizeKBytes.getOrElse(Parameters.dtcmSizeKBytesDefault)
    if (itcm == Parameters.itcmSizeKBytesDefault && dtcm == Parameters.dtcmSizeKBytesDefault) {
      opts.moduleName
    } else if (itcm == Parameters.itcmSizeKBytesHighmem && dtcm == Parameters.dtcmSizeKBytesHighmem) {
      s"${opts.moduleName}Highmem"
    } else {
      s"${opts.moduleName}_ITCM${itcm}KB_DTCM${dtcm}KB"
    }
  }

  def topModuleName(opts: CoreEmitOptions): String = {
    val base = finalModuleBaseName(opts)
    if (opts.useAxi) s"${base}Axi"
    else if (opts.useTlul) s"${base}Tlul"
    else base
  }

  def mkParams(opts: CoreEmitOptions): Parameters = {
    val p = new Parameters
    opts.enableFetchL0.foreach(v => p.enableFetchL0 = v)
    opts.fetchDataBits.foreach(v => p.fetchDataBits = v)
    opts.enableRvv.foreach(v => p.enableRvv = v)
    opts.enableFloat.foreach(v => p.enableFloat = v)
    opts.enableVerification.foreach(v => p.enableVerification = v)
    opts.lsuDataBits.foreach(v => p.lsuDataBits = v)
    opts.itcmSizeKBytes.foreach(v => p.itcmSizeKBytes = v)
    opts.dtcmSizeKBytes.foreach(v => p.dtcmSizeKBytes = v)

    val baseName = finalModuleBaseName(opts)
    val memoryRegions = if (
      p.itcmSizeKBytes == Parameters.itcmSizeKBytesDefault &&
      p.dtcmSizeKBytes == Parameters.dtcmSizeKBytesDefault
    ) {
      MemoryRegions.default
    } else {
      MemoryRegions.highmem(p.itcmSizeKBytes, p.dtcmSizeKBytes)
    }

    p
  }

  def mkModuleAndParams(opts: CoreEmitOptions): (RawModule, Parameters) = {
    val p = mkParams(opts)
    val baseName = finalModuleBaseName(opts)
    val memoryRegions = if (
      p.itcmSizeKBytes == Parameters.itcmSizeKBytesDefault &&
      p.dtcmSizeKBytes == Parameters.dtcmSizeKBytesDefault
    ) {
      MemoryRegions.default
    } else {
      MemoryRegions.highmem(p.itcmSizeKBytes, p.dtcmSizeKBytes)
    }

    val m: RawModule = if (opts.useAxi) {
      p.m = memoryRegions
      new CoreAxi(p, baseName)
    } else if (opts.useTlul) {
      p.m = memoryRegions
      new CoreTlul(p, baseName)
    } else {
      p.m = Seq(new MemoryRegion(0x0, 0x400000, MemoryRegionType.DMEM))
      new Core(p, baseName)
    }
    (m, p)
  }

  val opts = parseOptions(cfg.emitArgs)
  require(!(opts.useAxi && opts.useTlul), "useAxi and useTlul are mutually exclusive")
  val topName = topModuleName(opts)

  val outPath = Paths.get(outDir)
  Files.createDirectories(outPath)

  // Ensure blackbox resources resolve on the runtime classpath.
  val classesDir = Paths.get("target", "scala-2.13", "classes")
  Files.createDirectories(classesDir)
  Process(Seq("rm", "-rf", classesDir.resolve("hdl").toString), new java.io.File(".")).!
  Process(Seq("rm", "-rf", classesDir.resolve("external").toString), new java.io.File(".")).!
  // Copy resources idempotently; avoid noisy failures when source==dest.
  if (!Files.exists(classesDir.resolve("hdl"))) {
    Process(Seq("cp", "-a", "coralnpu/hdl", classesDir.toString), new java.io.File(".")).!
  }
  if (!Files.exists(classesDir.resolve("external"))) {
    Process(Seq("cp", "-a", "external", classesDir.toString), new java.io.File(".")).!
  }
  Process(
    Seq("cp", "-f", "coralnpu/hdl/verilog/RstSync.sv", classesDir.resolve("RstSync.sv").toString),
    new java.io.File(".")
  ).!
  Process(
    Seq("cp", "-f", "coralnpu/hdl/verilog/ClockGate.sv", classesDir.resolve("ClockGate.sv").toString),
    new java.io.File(".")
  ).!

  System.setProperty(
    "java.class.path",
    Paths.get("target", "scala-2.13", "classes").toAbsolutePath.normalize.toString
  )

  val fir = ChiselStage.emitCHIRRTL(mkModuleAndParams(opts)._1)
  Files.write(
    outPath.resolve(s"$topName.fir"),
    fir.getBytes(StandardCharsets.UTF_8)
  )
  Files.write(
    outPath.resolve("generated.fir"),
    fir.getBytes(StandardCharsets.UTF_8)
  )

  val firtoolOpts = Array(
    "--lowering-options=disallowLocalVariables,locationInfoStyle=none",
    "-enable-layers=Verification"
  )
  val sv = ChiselStage.emitSystemVerilog(mkModuleAndParams(opts)._1, Array.empty, firtoolOpts)
  val resourcesSeparator =
    "// ----- 8< ----- FILE \"firrtl_black_box_resource_files.f\" ----- 8< -----"
  val strippedSv = sv.split(resourcesSeparator)(0).replace("exclude_file", "exclude_module")
  Files.write(
    outPath.resolve(s"$topName.sv"),
    strippedSv.getBytes(StandardCharsets.UTF_8)
  )
  Files.write(
    outPath.resolve("generated.sv"),
    strippedSv.getBytes(StandardCharsets.UTF_8)
  )

  val header = EmitParametersHeader(mkParams(opts))
  Files.write(
    outPath.resolve(s"V${topName}_parameters.h"),
    header.getBytes(StandardCharsets.UTF_8)
  )

  val zipPath = outPath.resolve(s"$topName.zip")
  val zip = new ZipOutputStream(new FileOutputStream(zipPath.toFile))
  try {
    Seq(
      outPath.resolve(s"$topName.sv"),
      outPath.resolve(s"$topName.fir"),
      outPath.resolve(s"V${topName}_parameters.h")
    ).foreach { p =>
      if (Files.exists(p)) {
        zip.putNextEntry(new ZipEntry(p.getFileName.toString))
        zip.write(Files.readAllBytes(p))
        zip.closeEntry()
      }
    }
  } finally {
    zip.close()
  }
}
