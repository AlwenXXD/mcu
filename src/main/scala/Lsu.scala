import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import scala.collection.immutable.Nil

class LsuIO extends UnitIO {
  val core_rw = new CoreBundle
}

class Lsu extends Module {
  val io             = IO(new LsuIO)
  val dispatch_info  = Wire(new DispatchInfo)
  val dispatch_valid = WireInit(false.B)
  val wb_info        = Wire(new WriteBackInfo())
  wb_info.init()
  val wb_info_valid  = WireInit(false.B)
  dispatch_info := io.dispatch_info.bits
  dispatch_valid := io.dispatch_info.valid
  io.wb_info.bits := wb_info
  io.wb_info.valid := wb_info_valid

  def gen_write_data(data: UInt, uop: uOP.Type): UInt = {
    val byte_data  = Fill(8, data(7, 0))
    val half_data  = Fill(4, data(15, 0))
    val word_data  = Fill(2, data(31, 0))
    val final_data = MuxLookup(uop.asUInt, data,
      Seq(
        uOP.Lsu_SB.asUInt -> byte_data,
        uOP.Lsu_SH.asUInt -> half_data,
        uOP.Lsu_SW.asUInt -> word_data,
      ))
    final_data
  }

  def gen_load_data(data:UInt,mem_addr:UInt,uop:uOP.Type):UInt={
    val byte_data = (0 until 8).map(i => Fill(8, mem_addr(2, 0) === i.U(3.W)) & data(i * 8 + 7, i * 8)).reduce(_ | _)
    val half_data = (0 until 4).map(i => Fill(16, mem_addr(2,1) === i.U(2.W)) & data(i * 16 + 15, i * 16)).reduce(_ | _)
    val word_data = (0 until 2).map(i => Fill(32, mem_addr(2) === i.U(1.W)) & data(i * 32 + 31, i * 32)).reduce(_ | _)

    val final_data = MuxLookup(uop.asUInt, data,
      Seq(
        uOP.Lsu_LB.asUInt -> Cat(Fill(24, byte_data(7)), byte_data),
        uOP.Lsu_LBU.asUInt -> Cat(Fill(24, false.B), byte_data),
        uOP.Lsu_LH.asUInt -> Cat(Fill(16, half_data(15)), half_data),
        uOP.Lsu_LHU.asUInt -> Cat(Fill(16, false.B), half_data),
        uOP.Lsu_LW.asUInt -> word_data,
      ))
    final_data
  }



  val mem_addr    = (dispatch_info.imm_data.asSInt + dispatch_info.op1_data.asSInt).asUInt
  val write_data  = gen_write_data(dispatch_info.op2_data, dispatch_info.uop)
  val is_ld       = MuxLookup(dispatch_info.uop.asUInt, false.B,
    Seq(
      uOP.Lsu_LB.asUInt -> true.B,
      uOP.Lsu_LBU.asUInt -> true.B,
      uOP.Lsu_LH.asUInt -> true.B,
      uOP.Lsu_LHU.asUInt -> true.B,
      uOP.Lsu_LW.asUInt -> true.B,
    ))
  val is_st       = MuxLookup(dispatch_info.uop.asUInt, false.B,
    Seq(
      uOP.Lsu_SB.asUInt -> true.B,
      uOP.Lsu_SH.asUInt -> true.B,
      uOP.Lsu_SW.asUInt -> true.B,
    ))
  val byte_mask   = MuxCase(0.U(8.W),
    Seq(
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b000".U(4.W)) -> "b00000001".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b001".U(4.W)) -> "b00000010".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b010".U(4.W)) -> "b00000100".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b011".U(4.W)) -> "b00001000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b100".U(4.W)) -> "b00010000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b101".U(4.W)) -> "b00100000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b110".U(4.W)) -> "b01000000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SB && mem_addr(2, 0) === "b111".U(4.W)) -> "b10000000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SH && mem_addr(2, 1) === "b00".U(2.W)) -> "b00000011".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SH && mem_addr(2, 1) === "b01".U(2.W)) -> "b00001100".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SH && mem_addr(2, 1) === "b10".U(2.W)) -> "b00110000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SH && mem_addr(2, 1) === "b11".U(2.W)) -> "b11000000".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SW && mem_addr(2) === "b0".U(1.W)) -> "b00001111".U(8.W),
      (dispatch_info.uop === uOP.Lsu_SW && mem_addr(2) === "b1".U(1.W)) -> "b11110000".U(8.W),
    ))

  val size       = MuxLookup(dispatch_info.uop.asUInt, 0.U(3.W),
    Seq(
      uOP.Lsu_SB.asUInt -> 0.U(3.W),
      uOP.Lsu_SH.asUInt -> 1.U(3.W),
      uOP.Lsu_SW.asUInt -> 2.U(3.W),
      uOP.Lsu_LB.asUInt -> 0.U(3.W),
      uOP.Lsu_LBU.asUInt ->0.U(3.W),
      uOP.Lsu_LH.asUInt -> 1.U(3.W),
      uOP.Lsu_LHU.asUInt ->1.U(3.W),
      uOP.Lsu_LW.asUInt -> 2.U(3.W),
    ))

  val sIDLE::sWAIT::sEND::Nil = Enum(3)
  val state = RegInit(sIDLE)
  val data_read = RegInit(0.U(64.W))
  val except_type = RegInit(ExceptSel.is_null)
  val complete = WireInit(false.B)
  io.core_rw.req.valid:=false.B
  io.core_rw.resp.ready:=false.B
  switch(state){
    is(sIDLE){
      when(dispatch_valid){
        io.core_rw.req.valid:=true.B
      }
      when(io.core_rw.req.fire){
        state:=sWAIT
      }
    }
    is(sWAIT){
      io.core_rw.resp.ready:=true.B
      when(io.core_rw.resp.fire){
        data_read:=io.core_rw.resp.bits.data_read
        except_type:=Mux(io.core_rw.resp.bits.resp === 0.U,ExceptSel.is_null,Mux(is_st,ExceptSel.is_store_pgfault,ExceptSel.is_load_pgfault))
        state:=sEND
      }
    }
    is(sEND){
      complete:=true.B
      state:=sIDLE
    }
  }

  io.core_rw.req.bits.id := 1.U
  io.core_rw.req.bits.addr := mem_addr

  io.core_rw.req.bits.size := 3.U(3.W)


  io.core_rw.req.bits.data_write := write_data
  io.core_rw.req.bits.is_write := is_st
  io.core_rw.req.bits.byte_mask := byte_mask



  wb_info.data := gen_load_data(data_read,mem_addr,dispatch_info.uop)
  wb_info.inst_addr := dispatch_info.inst_addr
  wb_info.inst:=dispatch_info.inst
  wb_info.des_addr := dispatch_info.des_addr
  wb_info.except_type:= Mux(dispatch_info.except_type === ExceptSel.is_null,except_type,dispatch_info.except_type)
  wb_info.mem_addr:=mem_addr
  wb_info.atomic := true.B
  wb_info_valid := complete

  io.dispatch_info.ready := complete || !dispatch_valid

}


