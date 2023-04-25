import chisel3._
import chisel3.util._
import chisel3.util.experimental._

class DispatchInfo extends Bundle{
  val uop           = uOP()
  val need_imm      = Bool()
  val inst_addr     = UInt(32.W)
  val inst     = UInt(32.W)
  val des_addr      = UInt(5.W)
  val op1_data      = UInt(32.W)
  val op2_data      = UInt(32.W)
  val imm_data      = UInt(32.W)
  val csr_idx       = UInt(12.W)
  val csr_data = UInt(32.W)
  val unit_sel      = UnitSel()
  val except_type = ExceptSel()
  def init(): Unit ={
    uop           := uOP.NOP
    need_imm      := false.B
    inst_addr     := 0.U(32.W)
    inst    := 0.U(32.W)
    des_addr      := 0.U(5.W)
    op1_data      := 0.U(32.W)
    op2_data      := 0.U(32.W)
    imm_data      := 0.U(32.W)
    csr_idx       := 0.U(12.W)
    csr_data := 0.U
    unit_sel      := UnitSel.is_Null
    except_type := ExceptSel.is_null
  }
}
class WriteBackInfo extends Bundle{
  val inst_addr   = UInt(32.W)
  val inst   = UInt(32.W)
  val data        = UInt(32.W)
  val des_addr    = UInt(5.W)
  val csr_wen     = Bool()
  val csr_idx     = UInt(12.W)
  val csr_wdata   = UInt(32.W)
  val except_type = ExceptSel()
  val mem_addr = UInt(32.W)
  val atomic = Bool()
  def init(): Unit ={
    inst_addr   := 0.U
    inst        := 0.U
    data        := 0.U
    des_addr    := 0.U
    csr_wen     := 0.U
    csr_idx     := 0.U
    csr_wdata    := 0.U
    except_type := ExceptSel.is_null
    mem_addr := 0.U
    atomic := 0.U
  }
}

class UnitIO extends Bundle {
  val dispatch_info = Flipped(Decoupled(new DispatchInfo()))
  val wb_info       = Valid(new WriteBackInfo())
}

class CsrReadIO extends Bundle{
  val csr_idx     = Input(UInt(12.W))
  val csr_data = Output(UInt(32.W))
}

class ExecutionIO extends Bundle{
  val reg_read = Flipped(new RegReadIO())
  val csr_read = Flipped(new CsrReadIO())
  val decode_info = Flipped(Decoupled(new DecodeInfo()))
  val writeback_info = Valid(new WriteBackInfo())
  val bypass = Valid(new CommitInfo())
  val redirect_info = Valid(new BjuRedirectInfo())
  val axi_rw = new CoreBundle()
  val flush = Input(Bool())
}

class Execution extends Module {
  val io = IO(new ExecutionIO)
  io.reg_read.rs1_addr:=io.decode_info.bits.op1_addr
  io.reg_read.rs2_addr:=io.decode_info.bits.op2_addr
  io.csr_read.csr_idx:=io.decode_info.bits.csr_idx

  val dispatch_info = Reg(new DispatchInfo)
  val dispatch_info_valid = RegInit(false.B)

  val all_ready = WireInit(false.B)

  val sIDLE::sWAIT::sEND::Nil = Enum(3)
  val state = RegInit(sIDLE)
  io.decode_info.ready:=false.B
  switch(state){
    is(sIDLE){
      when(all_ready){
        io.decode_info.ready:=true.B
        dispatch_info.uop := io.decode_info.bits.uop
        dispatch_info.need_imm := io.decode_info.bits.need_imm
        dispatch_info.inst_addr := io.decode_info.bits.inst_addr
        dispatch_info.inst := io.decode_info.bits.inst
        dispatch_info.des_addr := io.decode_info.bits.des_addr
        dispatch_info.op1_data := io.reg_read.rs1_data
        dispatch_info.op2_data := io.reg_read.rs2_data
        dispatch_info.imm_data := io.decode_info.bits.imm_data
        dispatch_info.csr_idx := io.decode_info.bits.csr_idx
        dispatch_info.csr_data := io.csr_read.csr_data
        dispatch_info.unit_sel := io.decode_info.bits.unit_sel
        dispatch_info.except_type := io.decode_info.bits.except_type
        dispatch_info_valid := io.decode_info.valid
      }
    }
  }



  val writeback_info        = Reg(new WriteBackInfo)
  val writeback_info_valid  = RegInit(false.B)
  io.writeback_info.bits:=writeback_info
  io.writeback_info.valid:=writeback_info_valid

  writeback_info_valid:=dispatch_info_valid
  writeback_info.des_addr:=0.U
  writeback_info.inst_addr:=dispatch_info.inst_addr
  writeback_info.inst:=dispatch_info.inst
  writeback_info.data:=0.U
  writeback_info.csr_wen:=0.U
  writeback_info.csr_idx:=0.U
  writeback_info.csr_wdata:=0.U
  writeback_info.except_type:=dispatch_info.except_type
  writeback_info.mem_addr:=0.U

  io.bypass.valid := false.B
  io.bypass.bits.commit_addr := 0.U
  io.bypass.bits.commit_data := 0.U


  val alu = Module(new Alu)
  alu.io.dispatch_info.bits:=dispatch_info
  alu.io.dispatch_info.valid:=dispatch_info_valid && dispatch_info.unit_sel === UnitSel.is_Alu

  val bju = Module(new Bju)
  bju.io.dispatch_info.bits:=dispatch_info
  bju.io.dispatch_info.valid:=dispatch_info_valid && dispatch_info.unit_sel === UnitSel.is_Bju
  bju.io.redirect_info<>io.redirect_info

  val lsu = Module(new Lsu)
  lsu.io.dispatch_info.bits:=dispatch_info
  lsu.io.dispatch_info.valid:=dispatch_info_valid && dispatch_info.unit_sel === UnitSel.is_Lsu
  lsu.io.core_rw<>io.axi_rw


  val unit = alu.io::bju.io::lsu.io::Nil

  all_ready := unit.map(i=>i.dispatch_info.ready).reduce(_ & _)
  switch(dispatch_info.unit_sel){
    is(UnitSel.is_Alu){
      writeback_info := alu.io.wb_info.bits
      writeback_info_valid := alu.io.wb_info.valid
      io.bypass.valid := alu.io.wb_info.valid
      io.bypass.bits.commit_addr := dispatch_info.des_addr
      io.bypass.bits.commit_data := alu.io.wb_info.bits.data
    }
    is(UnitSel.is_Bju){
      writeback_info := bju.io.wb_info.bits
      writeback_info_valid := bju.io.wb_info.valid
      io.bypass.valid := bju.io.wb_info.valid
      io.bypass.bits.commit_addr := dispatch_info.des_addr
      io.bypass.bits.commit_data := bju.io.wb_info.bits.data
    }
    is(UnitSel.is_Lsu){
      writeback_info := lsu.io.wb_info.bits
      writeback_info_valid := lsu.io.wb_info.valid
      io.bypass.valid := lsu.io.wb_info.valid
      io.bypass.bits.commit_addr := dispatch_info.des_addr
      io.bypass.bits.commit_data := lsu.io.wb_info.bits.data
    }
  }

//TODO MDU flush
  when(io.flush) {
    writeback_info_valid := false.B
    dispatch_info_valid := false.B
  }

  when(reset.asBool){
    dispatch_info.init()
    writeback_info.init()
  }

}
