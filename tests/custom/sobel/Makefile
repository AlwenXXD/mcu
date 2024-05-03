sobel: main.c start.S linker.ld
	riscv64-unknown-elf-gcc -static -lgcc -nostartfiles -mcmodel=medany -march=rv32im -mabi=ilp32 -T linker.ld -o sobel main.c start.S
	riscv64-unknown-elf-objdump -D ./sobel > ./sobel.dump
	riscv64-unknown-elf-objcopy -O binary ./sobel ./sobel.bin
