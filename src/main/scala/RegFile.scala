import chisel3._
import chisel3.util.Valid
import chisel3.util.experimental._

class RegReadIO extends Bundle{
  val rs1_addr = Input(UInt(5.W))
  val rs2_addr = Input(UInt(5.W))
  val rs1_data = Output(UInt(32.W))
  val rs2_data = Output(UInt(32.W))
}

class CommitInfo extends Bundle{
  val commit_addr = UInt(5.W)
  val commit_data = UInt(32.W)
}

class RegFileIO extends Bundle{
  val reg_read = new RegReadIO
  val commit_info = Flipped(Valid(new CommitInfo))
  val bypass = Flipped(Valid(new CommitInfo))
}

class RegFile extends Module {
  val io = IO(new RegFileIO)

  val rf = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when (io.commit_info.valid && (io.commit_info.bits.commit_addr =/= 0.U)) {
    rf(io.commit_info.bits.commit_addr) := io.commit_info.bits.commit_data
  }

  when(io.reg_read.rs1_addr === 0.U) {
    io.reg_read.rs1_data := 0.U
  }.elsewhen(io.bypass.valid && io.bypass.bits.commit_addr === io.reg_read.rs1_addr){
    io.reg_read.rs1_data := io.bypass.bits.commit_data
  }.elsewhen(io.commit_info.valid && io.commit_info.bits.commit_addr === io.reg_read.rs1_addr){
    io.reg_read.rs1_data := io.commit_info.bits.commit_data
  }.otherwise{
    io.reg_read.rs1_data := rf(io.reg_read.rs1_addr)
  }

  when(io.reg_read.rs2_addr === 0.U) {
    io.reg_read.rs2_data := 0.U
  }.elsewhen(io.bypass.valid && io.bypass.bits.commit_addr === io.reg_read.rs2_addr) {
    io.reg_read.rs2_data := io.bypass.bits.commit_data
  }.elsewhen(io.commit_info.valid && io.commit_info.bits.commit_addr === io.reg_read.rs2_addr) {
    io.reg_read.rs2_data := io.commit_info.bits.commit_data
  }.otherwise {
    io.reg_read.rs2_data := rf(io.reg_read.rs2_addr)
  }

  val val_a0 = WireInit(rf(10))
  BoringUtils.addSource(val_a0, "val_a0")
}
