import chisel3._
import chisel3.util.experimental._


class Core extends Module {
  val io = IO(new Bundle {
    val irw = new CoreBundle()
    val drw = new CoreBundle()
    val instr_info = Input(new InstrInfo)
  })

  val fetch = Module(new InstFetch)
  val decode = Module(new Decode)
  val rf = Module(new RegFile)
  val execution = Module(new Execution)
  val writeback = Module(new WriteBack)
  val csr = Module(new Csr)

  fetch.io.inst<>decode.io.inst
  fetch.io.pc<>decode.io.pc
  fetch.io.axi_rw<>io.irw
  fetch.io.bju_redirect<>execution.io.redirect_info
  fetch.io.except_redirect<>writeback.io.redirect_info


  decode.io.decode_info<>execution.io.decode_info

  execution.io.reg_read<>rf.io.reg_read
  execution.io.writeback_info<>writeback.io.writeback_info
  execution.io.axi_rw<>io.drw
  execution.io.csr_read<>csr.io.csr_read

  writeback.io.commit_info<>rf.io.commit_info
  writeback.io.csr_info<>csr.io.csr_info
  writeback.io.except_info<>csr.io.except_info
  writeback.io.csr_write_info<>csr.io.csr_commit_info
  writeback.io.current_privilege<>csr.io.current_privilege

  csr.io.instr_info<>io.instr_info


  csr.io.vsr_info<>decode.io.vsr_info
  csr.io.current_privilege<>decode.io.cur_priv


}


