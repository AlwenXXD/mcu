#!/bin/bash
riscv64-unknown-elf-gcc -static -nostdlib -nostartfiles -mcmodel=medany -march=rv32i -mabi=ilp32 -e _entry -Ttext=0x80000000 -o tick tick.s
riscv64-unknown-elf-objdump -D ./tick > ./tick.dump
riscv64-unknown-elf-objcopy -O binary ./tick ./tick.bin

