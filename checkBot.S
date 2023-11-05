.section .text
.align 2
.globl checkBot
#include "GPIO.inc"
#include "memoryMap.inc"
.equ ON, 0x0
.equ OFF, 0x1
checkBot:
addi sp, sp, -16 # allocating stack frame
sw ra, 12(sp) # Saving return address to the stack
li t0, GPIO_CTRL_ADDR # load GPIO base address
lw t1, GPIO_INPUT_VAL(t0) # add the offset for the READ register
mv t2,a0 # load the BIT offset of PIN2
and t2, t1, t2 # AND the READ register with the PIN2 offset
beqz t2, pinOFF # if t2 is 0x0 the READ register has 0 at the PIN2 offset, branch to pinOFF
li a0, ON # return ON comand back to the main
j exit
pinOFF:
li a0, OFF # return OFF comand back to the main
j exit
exit:
lw ra, 12(sp) # restore the return address
addi sp, sp, 16 # deallocate the frame
ret