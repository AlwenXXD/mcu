.text
.globl _entry
_entry:
	la t0, trap_entry
	csrw mtvec, t0
	li t0, (1<<3)
	csrs mstatus, t0
	li t0, (1<<7)
	csrs mie, t0
	li s0, 0x02004000
	li s3, 0x00080000
	mv s1, s3
	sw s1,0(s0)
wait:
	j wait
.align 2
trap_entry:
	li t1, (1<<31)|7
	csrr t2, mcause
	bne t1,t2,fail
	
	li a0, 33
	.4byte 0x7b
	li a0, 10
	.4byte 0x7b
	li t3, 0x0200bff8
	lw s1, 0(t3)
	add s1, s1, s3
	sw s1,0(s0)
	mret
fail:
	li a0, 1
	.4byte 0x6b
	
