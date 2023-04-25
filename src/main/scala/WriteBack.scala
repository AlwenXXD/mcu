import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

class ExceptInfo extends Bundle{
  val is_ei  = Bool()
  val is_mret = Bool()
  val is_sret = Bool()
  val is_delegate = Bool()
  val epc    = UInt(32.W)
  val tval = UInt(32.W)
  val cause = UInt(32.W)
}



class WriteBackIO extends Bundle{
  val writeback_info = Flipped(Valid(new WriteBackInfo()))
  val csr_info = Input(new CsrInfo)
  val commit_info = Valid(new CommitInfo())
  val redirect_info = Valid(new RedirectInfo)
  val except_info = Valid(new ExceptInfo)
  val csr_write_info = Valid(new CsrWriteInfo)
  val current_privilege = Input(UInt(2.W))
}

class WriteBack extends Module{
  val io = IO(new WriteBackIO())

  val current_privilege = io.current_privilege
  val is_m = current_privilege ===3.U
  val is_s = current_privilege ===1.U
  val is_u = current_privilege ===0.U

  val nxt_pc = WireInit(0.U(32.W))
  val nxt_pc_trap_m = Cat(io.csr_info.mtvec_base,0.U(2.W))
  val nxt_pc_trap_s = Cat(io.csr_info.stvec_base,0.U(2.W))

  val is_except   = WireInit(false.B)
  val is_mret      = WireInit(false.B)
  val is_sret      = WireInit(false.B)
  val is_delegate = WireInit(false.B)
  val nxt_cause   = WireInit(0.U(32.W))
  val nxt_epc     = io.writeback_info.bits.inst_addr
  val nxt_tval    = WireInit(0.U(32.W))

  val is_halt = WireInit(false.B)
  BoringUtils.addSource(is_halt, "is_halt")


  val is_error = WireInit(false.B)
  BoringUtils.addSource(is_error, "is_error")  

  switch(io.writeback_info.bits.except_type){
    is(ExceptSel.is_ecall){
      is_except :=true.B
      nxt_cause := MuxLookup(current_privilege,11.U(32.W),Array(
        0.U -> 8.U(32.W),
        1.U -> 9.U(32.W),
        2.U -> 10.U(32.W),
        3.U -> 11.U(32.W),
      ).toSeq)
      when((io.csr_info.medeleg_ecall_s&is_s)|(io.csr_info.medeleg_ecall_u&is_u)){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
    }
    is(ExceptSel.is_ebreak){
      is_except :=true.B
      nxt_cause := 3.U
      when(io.csr_info.medeleg_break && !is_m){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
    }
    is(ExceptSel.is_mret){
      is_mret :=true.B
      nxt_pc:=io.csr_info.mepc
    }
    is(ExceptSel.is_sret){
      is_sret :=true.B
      nxt_pc:=io.csr_info.sepc
    }
    is(ExceptSel.is_inst_pgfault){
      is_except :=true.B
      nxt_cause := 12.U
      nxt_tval := io.writeback_info.bits.inst_addr
      when(io.csr_info.medeleg_ipgfault&&(!is_m)){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
    }
    is(ExceptSel.is_load_pgfault){
      is_except :=true.B
      nxt_cause := 13.U
      nxt_tval := io.writeback_info.bits.mem_addr
      when(io.csr_info.medeleg_lpgfault&&(!is_m)){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
    }
    is(ExceptSel.is_store_pgfault){
      is_except :=true.B
      nxt_cause := 15.U
      nxt_tval := io.writeback_info.bits.mem_addr
      when(io.csr_info.medeleg_spgfault&&(!is_m)){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
    }
    is(ExceptSel.is_illegal){
      is_except :=true.B
      nxt_cause := 2.U
      nxt_tval := io.writeback_info.bits.inst
      when(io.csr_info.medeleg_illegal&&(!is_m)){
        nxt_pc:=nxt_pc_trap_s
        is_delegate:=true.B
      }.otherwise{
        nxt_pc:=nxt_pc_trap_m
      }
      is_error := io.writeback_info.valid
    }
    is(ExceptSel.is_halt) {
      is_halt := true.B
    }
  }

  val has_mei =  io.csr_info.en_meip && !io.writeback_info.bits.atomic
  val has_mti =  io.csr_info.en_mtip && !io.writeback_info.bits.atomic
  val has_sti =  io.csr_info.en_stip && !io.writeback_info.bits.atomic
  val has_ssi =  io.csr_info.en_ssip && !io.writeback_info.bits.atomic
  when(has_mei){
    nxt_cause:=Cat(1.U(1.W),11.U(31.W))
    nxt_pc:=nxt_pc_trap_m
  }.elsewhen(has_mti){
    nxt_cause:=Cat(1.U(1.W),7.U(31.W))
    nxt_pc:=nxt_pc_trap_m
  }.elsewhen(has_sti){
    nxt_cause:=Cat(1.U(1.W),5.U(31.W))
    when(io.csr_info.mideleg_sti){
      nxt_pc:=nxt_pc_trap_s
      is_delegate:=true.B
    }.otherwise{
      nxt_pc:=nxt_pc_trap_m
    }
  }.elsewhen(has_ssi){
    nxt_cause:=Cat(1.U(1.W),1.U(31.W))
    when(io.csr_info.mideleg_ssi){
      nxt_pc:=nxt_pc_trap_s
      is_delegate:=true.B
    }.otherwise{
      nxt_pc:=nxt_pc_trap_m
    }
  }
  val is_intr = has_ssi||has_sti||has_mti||has_mei
  val is_ret = is_mret | is_sret

  io.except_info.valid:= io.writeback_info.valid
  io.except_info.bits.is_delegate:=is_delegate
  io.except_info.bits.is_ei:=is_except || is_intr
  io.except_info.bits.is_mret:=is_mret
  io.except_info.bits.is_sret:=is_sret
  io.except_info.bits.cause :=nxt_cause
  io.except_info.bits.epc   :=nxt_epc
  io.except_info.bits.tval  :=nxt_tval

  io.redirect_info.valid:=io.writeback_info.valid&&(is_intr || is_except || is_ret)
  io.redirect_info.bits.addr:=nxt_pc

  io.commit_info.valid:=io.writeback_info.valid && !is_except && !is_intr
  io.commit_info.bits.commit_addr:=io.writeback_info.bits.des_addr
  io.commit_info.bits.commit_data:=io.writeback_info.bits.data

  io.csr_write_info.valid:=io.writeback_info.valid && !is_except && !is_intr
  io.csr_write_info.bits.csr_idx:=io.writeback_info.bits.csr_idx
  io.csr_write_info.bits.csr_data:=io.writeback_info.bits.csr_wdata
  io.csr_write_info.bits.csr_wen:=io.writeback_info.bits.csr_wen

  val val_a0 = WireInit(0.U(32.W))
  BoringUtils.addSink(val_a0, "val_a0")
  when(io.writeback_info.valid && !is_except && !is_intr && (io.writeback_info.bits.inst === "h0000_007b".U(32.W))) {
    printf("%c", val_a0(7,0))
  }

}
