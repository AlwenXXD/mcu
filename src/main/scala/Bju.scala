import chisel3._
import chisel3.util._

class BjuIO extends UnitIO{
  val redirect_info = Valid(new BjuRedirectInfo())
}

class Bju extends Module{
  val io = IO(new BjuIO)

  val dispatch_info         = Wire(new DispatchInfo)
  dispatch_info :=io.dispatch_info.bits
  val dispatch_info_valid        = WireInit(false.B)
  dispatch_info_valid:=io.dispatch_info.valid
  io.dispatch_info.ready:=true.B

  val target_addr = WireInit(0.U(32.W))
  val next_addr = dispatch_info.inst_addr+4.U(32.W)
  val branch_addr = dispatch_info.inst_addr+dispatch_info.imm_data
  val is_taken = WireInit(false.B)

  val eq=dispatch_info.op1_data===dispatch_info.op2_data

  switch(dispatch_info.uop){
    is(uOP.Bju_JAL ){
      is_taken:=true.B
      target_addr:=branch_addr
    }
    is(uOP.Bju_JALR ){
      is_taken:=true.B
      target_addr:=dispatch_info.op1_data+dispatch_info.imm_data
    }
    is(uOP.Bju_BEQ ){
      is_taken:=eq
      target_addr:=branch_addr
    }
    is(uOP.Bju_BNE ){
      is_taken:= !eq
      target_addr:=branch_addr
    }
    is(uOP.Bju_BGE ){
      is_taken:= dispatch_info.op1_data.asSInt >= dispatch_info.op2_data.asSInt
      target_addr:=branch_addr
    }
    is(uOP.Bju_BGEU ){
      is_taken:= dispatch_info.op1_data.asUInt >= dispatch_info.op2_data.asUInt
      target_addr:=branch_addr
    }
    is(uOP.Bju_BLT ){
      is_taken:= dispatch_info.op1_data.asSInt < dispatch_info.op2_data.asSInt
      target_addr:=branch_addr
    }
    is(uOP.Bju_BLTU ){
      is_taken:= dispatch_info.op1_data.asUInt < dispatch_info.op2_data.asUInt
      target_addr:=branch_addr
    }

  }

  io.wb_info.valid:= dispatch_info_valid
  io.wb_info.bits.init()
  io.wb_info.bits.data:=next_addr
  io.wb_info.bits.des_addr:=dispatch_info.des_addr
  io.wb_info.bits.inst_addr:=dispatch_info.inst_addr
  io.wb_info.bits.inst:=dispatch_info.inst
  io.wb_info.bits.atomic := true.B
  io.wb_info.bits.except_type:=dispatch_info.except_type


  io.redirect_info.valid:= dispatch_info_valid
  io.redirect_info.bits.is_taken := is_taken
  io.redirect_info.bits.addr:=target_addr


}
