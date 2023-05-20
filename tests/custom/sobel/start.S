.section entry, "ax"
.globl _start
.type _start, @function

_start:
  la sp, _stack_pointer
  la t0, _trap_entry
  csrw mtvec, t0
  li t0, (1<<3)
	csrs mstatus, t0
	li t0, (1<<11)
	csrs mie, t0
  mv t0, zero
  mv s0, zero
  jal main
  nop
  .4byte 0x6b

