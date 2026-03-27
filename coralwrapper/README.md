# coralwrapper

`coralwrapper` is a Chipyard-style wrapper around CoralNPU that lets you emit
Verilog from named configs using:

```bash
make CONFIG=CoralNPUConfig
```

## Repository layout

- `coralnpu/` is a Git submodule pointing to the CoralNPU repository.
- `external/*` are Git submodules used by CoralNPU Chisel BlackBox resources:
  - `external/common_cells`
  - `external/cvfpu`
  - `external/fpu_div_sqrt_mvp`
  - `external/RVVI`

## Setup

```bash
git submodule update --init --recursive
```

## Build

Default:

```bash
make
```

Equivalent explicit:

```bash
make CONFIG=CoralNPUConfig
```

Print resolved args for a config:

```bash
make print-config CONFIG=CoralNPUConfig
```

Build output:

- `coralnpu/build/verilog/<CONFIG>/`

Primary artifacts:

- `<ModuleName>.sv`
- `V<ModuleName>_parameters.h`
- `<ModuleName>.zip`

## Configs

The wrapper currently supports all existing CoralNPU core-emission configs:

- `CoralNPUConfig` -> `Core.sv`
- `CoralNPUScalarConfig` -> `CoreScalar.sv`
- `CoralNPUMiniConfig` -> `CoreMini.sv`
- `CoralNPUMiniAxiConfig` -> `CoreMiniAxi.sv`
- `CoralNPUMiniVerificationAxiConfig` -> `CoreMiniVerificationAxi.sv`
- `CoralNPUMiniHighmemAxiConfig` -> `CoreMiniHighmemAxi.sv`
- `CoralNPUMiniITCM512DTCM512AxiConfig` -> `CoreMini_ITCM512KB_DTCM512KBAxi.sv`
- `CoralNPURvvMiniTlulConfig` -> `RvvCoreMiniTlul.sv`
- `CoralNPURvvMiniAxiConfig` -> `RvvCoreMiniAxi.sv`
- `CoralNPURvvMiniVerificationAxiConfig` -> `RvvCoreMiniVerificationAxi.sv`
- `CoralNPURvvMiniHighmemAxiConfig` -> `RvvCoreMiniHighmemAxi.sv`
- `CoralNPURvvMiniITCM512DTCM512AxiConfig` -> `RvvCoreMini_ITCM512KB_DTCM512KBAxi.sv`
