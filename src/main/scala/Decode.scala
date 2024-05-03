import chisel3._
import chisel3.util._
import Instructions._

class DecodeInfo extends Bundle{
  val inst_addr = UInt(32.W)
  val inst = UInt(32.W)
  val op1_addr      = UInt(5.W)
  val op2_addr      = UInt(5.W)
  val des_addr      = UInt(5.W)
  val imm_data      = UInt(32.W)
  val uop           = uOP()
  val unit_sel      = UnitSel()
  val need_imm      = Bool()
  val csr_idx = UInt(12.W)
  val except_type = ExceptSel()
}
class DecodeIO extends Bundle{
  val icache_resp = Flipped(Decoupled(new IcacheResp))
  val decode_info = Decoupled(new DecodeInfo())
  val vsr_info = Input(new VSRInfo)
  val cur_priv = Input(UInt(2.W))
}

class Decode extends Module {
  val io = IO(new DecodeIO)
  val inst = io.icache_resp.bits.inst

  val decoder = Wire(new Decoder())
  decoder.decode(inst)

  val cur_priv = io.cur_priv
  val is_m = cur_priv ===3.U
  val is_s = cur_priv ===1.U
  val is_u = cur_priv ===0.U
  val except_type = WireInit(decoder.except_type)

  switch(decoder.except_type){
    is(ExceptSel.decode_wfi){
      when(!is_m && io.vsr_info.mstatus_tw){
        except_type:=ExceptSel.is_illegal
      }.otherwise{
        except_type:=ExceptSel.is_null
      }
    }
    is(ExceptSel.decode_sfence_vma){
      when((is_s && io.vsr_info.mstatus_tvm) || is_u){
        except_type:=ExceptSel.is_illegal
      }.otherwise{
        except_type:=ExceptSel.is_null
      }
    }
    is(ExceptSel.is_sret){
      when((is_s && io.vsr_info.mstatus_tsr) || is_u){
        except_type:=ExceptSel.is_illegal
      }
    }
    is(ExceptSel.is_mret){
      when(!is_m){
        except_type:=ExceptSel.is_illegal
      }
    }
    is(ExceptSel.decode_csr){
      val m_csrs = MuxLookup(decoder.csr_idx,false.B,Array(
          CsrAddr.mhartid->  true.B,
          CsrAddr.mstatus->  true.B,
          CsrAddr.misa->     true.B,
          CsrAddr.medeleg->  true.B,
          CsrAddr.mideleg->  true.B,
          CsrAddr.mie->      true.B,
          CsrAddr.mtvec->    true.B,
          CsrAddr.mscratch-> true.B,
          CsrAddr.mepc->     true.B,
          CsrAddr.mcause->   true.B,
          CsrAddr.mtval->    true.B,
          CsrAddr.mip->      true.B,
          CsrAddr.mcycle->   true.B,
          CsrAddr.minstret-> true.B,
      ).toSeq)
      val is_satp = decoder.csr_idx ===CsrAddr.satp
      when((is_s && m_csrs) || is_u || (is_s && is_satp && io.vsr_info.mstatus_tvm)){
        except_type:=ExceptSel.is_illegal
      }.otherwise{
        except_type:=ExceptSel.is_null
      }
    }
  }

  io.decode_info.bits.inst_addr:= io.icache_resp.bits.addr
  io.decode_info.bits.inst:= io.icache_resp.bits.inst
  io.decode_info.bits.op1_addr := decoder.op1_addr
  io.decode_info.bits.op2_addr := decoder.op2_addr
  io.decode_info.bits.des_addr := decoder.des_addr
  io.decode_info.bits.imm_data := decoder.imm_data
  io.decode_info.bits.uop      := decoder.uop
  io.decode_info.bits.unit_sel := decoder.unit_sel
  io.decode_info.bits.need_imm := decoder.need_imm
  io.decode_info.bits.csr_idx := decoder.csr_idx
  io.decode_info.bits.except_type:= except_type

  io.decode_info.valid := io.icache_resp.valid


  io.icache_resp.ready:=io.decode_info.ready
}
