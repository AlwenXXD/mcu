PROJECT_HOME = $(shell pwd)
BUILD_DIR = ${PROJECT_HOME}/build
SIMTOP_SRCS = $(shell find ${PROJECT_HOME}/src/main -type f -name "*") ${PROJECT_HOME}/build.sc
EMU_SRCS = $(shell find ${PROJECT_HOME}/src/test -type f -name "*")
SIMTOP_FILE = ${BUILD_DIR}/SimTop.v
EMU_FILE = ${BUILD_DIR}/emu

${SIMTOP_FILE}: ${SIMTOP_SRCS}
	mill -i mcu.runMain TopMain -td build

sim-verilog: ${SIMTOP_FILE}

${EMU_FILE}: ${SIMTOP_FILE} ${EMU_SRCS}
	verilator --cc --exe --build --trace --Mdir build/obj_dir --top SimTop \
	-CFLAGS "-I/usr/include/SDL2" -LDFLAGS " -lSDL2 -lSDL2_image" \
	-o ${EMU_FILE} -j 4 ${EMU_SRCS} ${SIMTOP_FILE} ${BUILD_DIR}/ram.v
emu: ${EMU_FILE}

clean:
	rm -rf ${BUILD_DIR} ${PROJECT_HOME}/out

.PHONY: sim-verilog clean
