SHELL := /bin/bash

CONFIG ?= CoralNPUConfig
OUT_DIR ?= build/verilog/$(CONFIG)
SBT ?= ./scripts/sbt

.PHONY: all help init-submodules print-config emit clean

all: emit

help:
	@echo "CoralWrapper make targets:"
	@echo "  make init-submodules              Initialize all wrapper submodules"
	@echo "  make print-config CONFIG=<name>   Show resolved emitter arguments"
	@echo "  make emit CONFIG=<name>           Emit Verilog for selected config"
	@echo "  make clean                        Remove generated build outputs"
	@echo ""
	@echo "Example:"
	@echo "  make emit CONFIG=CoralNPUConfig"

init-submodules:
	git submodule update --init --recursive coralwrapper/coralnpu coralwrapper/external/common_cells coralwrapper/external/cvfpu coralwrapper/external/fpu_div_sqrt_mvp coralwrapper/external/RVVI

print-config:
	@$(SBT) "runMain coralwrapper.EmitWrapper --config=$(CONFIG) --print-only=True --out-dir=$(OUT_DIR)"

emit:
	@$(SBT) "runMain coralwrapper.EmitWrapper --config=$(CONFIG) --out-dir=$(OUT_DIR)"

clean:
	rm -rf build target project/target
