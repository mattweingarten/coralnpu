package coralwrapper

final case class EmitConfig(
  name: String,
  moduleName: String,
  outDir: String,
  emitArgs: Seq[String]
)

object Configs {
  val all: Map[String, EmitConfig] = Seq(
    EmitConfig(
      name = "CoreConfig",
      moduleName = "Core",
      outDir = "build/verilog/Core",
      emitArgs = Seq(
        "--moduleName=Core"
      )
    ),
    EmitConfig(
      name = "CoreScalarConfig",
      moduleName = "CoreScalar",
      outDir = "build/verilog/CoreScalar",
      emitArgs = Seq(
        "--moduleName=CoreScalar"
      )
    ),
    EmitConfig(
      name = "CoreMiniConfig",
      moduleName = "CoreMini",
      outDir = "build/verilog/CoreMini",
      emitArgs = Seq(
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--moduleName=CoreMini"
      )
    ),
    EmitConfig(
      name = "CoreMiniAxiConfig",
      moduleName = "CoreMiniAxi",
      outDir = "build/verilog/CoreMiniAxi",
      emitArgs = Seq(
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableFloat=True",
        "--moduleName=CoreMini",
        "--useAxi"
      )
    ),
    EmitConfig(
      name = "CoreMiniVerificationAxiConfig",
      moduleName = "CoreMiniVerificationAxi",
      outDir = "build/verilog/CoreMiniVerificationAxi",
      emitArgs = Seq(
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableFloat=True",
        "--moduleName=CoreMiniVerification",
        "--useAxi",
        "--enableVerification=True"
      )
    ),
    EmitConfig(
      name = "CoreMiniHighmemAxiConfig",
      moduleName = "CoreMiniHighmemAxi",
      outDir = "build/verilog/CoreMiniHighmemAxi",
      emitArgs = Seq(
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableFloat=True",
        "--moduleName=CoreMini",
        "--useAxi",
        "--itcmSizeKBytes=1024",
        "--dtcmSizeKBytes=1024"
      )
    ),
    EmitConfig(
      name = "CoreMiniITCM512DTCM512AxiConfig",
      moduleName = "CoreMini_ITCM512KB_DTCM512KBAxi",
      outDir = "build/verilog/CoreMini_ITCM512KB_DTCM512KBAxi",
      emitArgs = Seq(
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableFloat=True",
        "--moduleName=CoreMini",
        "--useAxi",
        "--itcmSizeKBytes=512",
        "--dtcmSizeKBytes=512"
      )
    ),
    EmitConfig(
      name = "RvvCoreMiniTlulConfig",
      moduleName = "RvvCoreMiniTlul",
      outDir = "build/verilog/RvvCoreMiniTlul",
      emitArgs = Seq(
        "--moduleName=RvvCoreMini",
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableRvv=True",
        "--enableFloat=True",
        "--useTlul=True"
      )
    ),
    EmitConfig(
      name = "RvvCoreMiniAxiConfig",
      moduleName = "RvvCoreMiniAxi",
      outDir = "build/verilog/RvvCoreMiniAxi",
      emitArgs = Seq(
        "--moduleName=RvvCoreMini",
        "--itcmSizeKBytes=8",
        "--dtcmSizeKBytes=32",
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableRvv=True",
        "--enableFloat=True",
        "--useAxi"
      )
    ),
    EmitConfig(
      name = "RvvCoreMiniVerificationAxiConfig",
      moduleName = "RvvCoreMiniVerificationAxi",
      outDir = "build/verilog/RvvCoreMiniVerificationAxi",
      emitArgs = Seq(
        "--moduleName=RvvCoreMiniVerification",
        "--enableVerification=True",
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableRvv=True",
        "--enableFloat=True",
        "--useAxi"
      )
    ),
    EmitConfig(
      name = "RvvCoreMiniHighmemAxiConfig",
      moduleName = "RvvCoreMiniHighmemAxi",
      outDir = "build/verilog/RvvCoreMiniHighmemAxi",
      emitArgs = Seq(
        "--moduleName=RvvCoreMini",
        "--itcmSizeKBytes=1024",
        "--dtcmSizeKBytes=1024",
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableRvv=True",
        "--enableFloat=True",
        "--useAxi"
      )
    ),
    EmitConfig(
      name = "RvvCoreMiniITCM512DTCM512AxiConfig",
      moduleName = "RvvCoreMini_ITCM512KB_DTCM512KBAxi",
      outDir = "build/verilog/RvvCoreMini_ITCM512KB_DTCM512KBAxi",
      emitArgs = Seq(
        "--moduleName=RvvCoreMini",
        "--itcmSizeKBytes=512",
        "--dtcmSizeKBytes=512",
        "--enableFetchL0=False",
        "--fetchDataBits=128",
        "--lsuDataBits=128",
        "--enableRvv=True",
        "--enableFloat=True",
        "--useAxi"
      )
    )
  ).map(c => c.name -> c).toMap

  private val aliases: Map[String, String] = Map(
    "CoralNPUConfig" -> "RvvCoreMiniVerificationAxiConfig"
  )

  def byName(name: String): Option[EmitConfig] = all.get(name)
    .orElse(aliases.get(name).flatMap(all.get))

  def known: Seq[String] = (all.keys ++ aliases.keys).toSeq.sorted
}
