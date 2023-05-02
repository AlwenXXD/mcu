main: main.c start.S linker.ld
	riscv64-unknown-elf-gcc -static -nostdlib -nostartfiles -mcmodel=medany -march=rv32im -mabi=ilp32 -T linker.ld -o main main.c start.S
	riscv64-unknown-elf-objdump -D ./main > ./main.dump
	riscv64-unknown-elf-objcopy -O binary ./main ./main.bin
