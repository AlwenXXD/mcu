.section entry, "ax"
.globl _start
.type _start, @function

_start:
  mv s0, zero
  la sp, _stack_pointer
  jal main
  mv a0, a0
  .4byte 0x6b
_loop:
  j _loop