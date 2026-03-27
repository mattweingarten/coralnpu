# Coral NPU

Coral NPU is a hardware accelerator for ML inferencing. Coral NPU is an Open Source IP designed by Google Research and is freely available for integration into ultra-low-power System-on-Chips (SoCs) targeting wearable devices such as hearables, augmented reality (AR) glasses and smart watches.

Coral NPU is a neural processing unit (NPU), also known as an AI accelerator or deep-learning processor. Coral NPU is based on the 32-bit RISC-V Instruction Set Architecture (ISA).

Coral NPU includes three distinct processor components that work together: matrix, vector (SIMD), and scalar.

![Coral NPU Archicture](doc/images/arch_data_flow.png)
[Coral NPU Architecture Datasheet](https://developers.google.com/coral/guides/hardware/datasheet)

## Coral NPU Features
Coral NPU offers the following top-level feature set:

* RV32IMF_Zve32x RISC-V instruction set (specifically `rv32imf_zve32x_zicsr_zifencei_zbb`)
* 32-bit address space for applications and operating system kernels
* Four-stage processor, in-order dispatch, out-of-order retire
* Four-way scalar, two-way vector dispatch
* 128-bit SIMD, 256-bit (future) pipeline
* 8 KB ITCM memory (tightly-coupled memory for instructions)
* 32 KB DTCM memory (tightly-coupled memory for data)
* Both memories are single-cycle-latency SRAM, more efficient than cache memory
* AXI4 bus interfaces, functioning as both manager and subordinate, to interact with external memory and allow external CPUs to configure Coral NPU

## System Requirements (core generation only)

This setup focuses only on generating core Verilog from Scala/Chisel.

Required packages:

* `git`
* `openjdk-17-jdk` (or newer JDK)
* `curl` (for local SBT launcher bootstrap)

Install with apt:

```bash
sudo apt-get update
sudo apt-get install -y git openjdk-17-jdk curl
```

## Initialize required git submodules

The core generator references HDL sources from these dependency repositories
through Chisel blackbox resources:

* `external/common_cells`
* `external/cvfpu`
* `external/fpu_div_sqrt_mvp`
* `external/RVVI`

Initialize/update them after cloning:

```bash
git submodule update --init --recursive external/common_cells external/cvfpu external/fpu_div_sqrt_mvp external/RVVI
```

## Generate the core Verilog with SBT

Use the local wrapper (auto-downloads sbt-extras if needed):

```bash
./scripts/sbt emitRvvCoreMiniVerificationAxi
```

Generated outputs:

* `build/verilog/RvvCoreMiniVerificationAxi.sv`
* `build/verilog/VRvvCoreMiniVerificationAxi_parameters.h`
* `build/verilog/RvvCoreMiniVerificationAxi.zip`

## Direct runMain (optional)

If you want to invoke the emitter manually:

```bash
./scripts/sbt "runMain coralnpu.EmitCore --moduleName=RvvCoreMiniVerification --enableVerification=True --enableFetchL0=False --fetchDataBits=128 --lsuDataBits=128 --enableRvv=True --enableFloat=True --useAxi --target-dir=build/verilog"
```


![](doc/images/Coral_Logo_200px-2x.png)
