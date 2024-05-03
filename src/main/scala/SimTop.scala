import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

//
class SimTop extends Module {
  val io = IO(new Bundle {
    val intr_e = Input(Bool())
    val is_halt = Output(Bool())
    val is_error = Output(Bool())
    val val_a0 = Output(UInt(32.W))
  })

  val core = Module(new Core)
  val arbiter_core= Module(new CoreArbiter2to3)
  val clint = Module(new Clint)
  val sobel = Module(new Sobel)
  val ram = Module(new Ram)
  core.io.drw<>arbiter_core.io.core_master(1)
  core.io.irw<>arbiter_core.io.core_master(0)
  core.io.instr_info.intr_t<>clint.io.intr_t
  core.io.instr_info.intr_e<>sobel.io.intr_e


  ram.io.bus <> arbiter_core.io.core_slave(0)
  clint.io.bus <> arbiter_core.io.core_slave(1)
  sobel.io.bus <> arbiter_core.io.core_slave(2)

  val is_halt = WireInit(false.B)
  BoringUtils.addSink(is_halt, "is_halt")
  io.is_halt <> is_halt
  val is_error = WireInit(false.B)
  BoringUtils.addSink(is_error, "is_error")
  io.is_error <> is_error
  val val_a0 = WireInit(0.U(64.W))
  BoringUtils.addSink(val_a0, "val_a0")
  io.val_a0 <> val_a0


}
