import chisel3._
import chisel3.util._


class Mdu extends Module {
  val io            = IO(new UnitIO)
  val dispatch_info = Wire(new DispatchInfo)
  dispatch_info := io.dispatch_info.bits
  val dispatch_valid = WireInit(false.B)
  dispatch_valid := io.dispatch_info.valid


  def sign_convert(n:UInt):UInt = {
    n.do_unary_~ + 1.U
  }


  val multiplicand_dividend    = RegInit(0.U(64.W))
  val multiplier_divisor    = RegInit(0.U(64.W))
  val md_nd    = WireInit(0.U(32.W))
  val md_r = WireInit(0.U(32.W))
  val product_quotient = RegInit(0.U(64.W))
  val is_mul = WireInit(false.B)
  val is_negative = WireInit(false.B)
  val op1_data = dispatch_info.op1_data
  val op2_data = dispatch_info.op2_data
  val op1_data_t = sign_convert(dispatch_info.op1_data)
  val op2_data_t = sign_convert(dispatch_info.op2_data)

  val product_quotient_t = sign_convert(product_quotient)
  val reminder_t = sign_convert(multiplicand_dividend(31,0))
  val reminder = multiplicand_dividend
  val result = WireInit(0.U(32.W))
  val div_zero = op2_data === 0.U


  switch(dispatch_info.uop){
    is(uOP.Mdu_MUL   ){
      md_nd:=Mux(op1_data(31),op1_data_t,op1_data)
      md_r:=Mux(op2_data(31),op2_data_t,op2_data)
      is_negative:=op1_data(31) ^ op2_data(31)
      is_mul:=true.B
      result:=Mux(is_negative,product_quotient_t(31,0),product_quotient(31,0))
    }
    is(uOP.Mdu_MULH  ){
      md_nd:=Mux(op1_data(31),op1_data_t,op1_data)
      md_r:=Mux(op2_data(31),op2_data_t,op2_data)
      is_negative:=op1_data(31) ^ op2_data(31)
      is_mul:=true.B
      result:=Mux(is_negative,product_quotient_t(63,32),product_quotient(63,32))
    }
    is(uOP.Mdu_MULHU ){
      md_nd:=op1_data
      md_r:= op2_data
      is_negative:=false.B
      is_mul:=true.B
      result:=product_quotient(63,32)
    }
    is(uOP.Mdu_MULHSU){
      md_nd:=Mux(op1_data(31),op1_data_t,op1_data)
      md_r:=op2_data
      is_negative:=op1_data(31)
      is_mul:=true.B
      result:=Mux(is_negative,product_quotient_t(63,32),product_quotient(63,32))
    }

    is(uOP.Mdu_DIV   ){
      md_nd:=Mux(op1_data(31),op1_data_t,op1_data)
      md_r:=Mux(op2_data(31),op2_data_t,op2_data)
      is_negative:=op1_data(31) ^ op2_data(31)
      is_mul:=false.B
      result:=Mux(div_zero,(-1).S(32.W).asUInt,Mux(is_negative,product_quotient_t(31,0),product_quotient(31,0)))
    }
    is(uOP.Mdu_DIVU  ){
      md_nd:=op1_data
      md_r:=op2_data
      is_negative:=false.B
      is_mul:=false.B
      result:=Mux(div_zero,(-1).S(32.W).asUInt,product_quotient(31,0))
    }
    is(uOP.Mdu_REM   ){
      md_nd:=Mux(op1_data(31),op1_data_t,op1_data)
      md_r:=Mux(op2_data(31),op2_data_t,op2_data)
      is_negative:=op1_data(31)
      is_mul:=false.B
      result:=Mux(div_zero,op1_data,Mux(is_negative,reminder_t(31,0),reminder(31,0)))
    }
    is(uOP.Mdu_REMU  ){
      md_nd:=op1_data
      md_r:=op2_data
      is_negative:=false.B
      is_mul:=false.B
      result:=Mux(div_zero,op1_data,reminder(31,0))
    }
  }


  val sIDLE::sMUL::sDIV::sEND::Nil = Enum(4)
  val state  = RegInit(sIDLE)
  val count = RegInit(0.U(6.W))
  val complete = WireInit(false.B)
  switch(state){
    is(sIDLE){
      when(dispatch_valid){
        when(is_mul){
          product_quotient :=0.U
          multiplier_divisor :=Cat(0.U(32.W),md_r)
          multiplicand_dividend :=Cat(0.U(32.W),md_nd)
          count:=0.U
          state:=sMUL
        }.otherwise{
          when(div_zero){
            complete:=true.B
          }.otherwise{
            product_quotient :=0.U
            multiplier_divisor :=Cat(0.U(1.W),md_r,0.U(31.W))
            multiplicand_dividend :=Cat(0.U(32.W),md_nd)
            count:=31.U
            state:=sDIV
          }
        }
      }
    }
    is(sMUL){
      product_quotient:=Mux(multiplicand_dividend(0),product_quotient+multiplier_divisor,product_quotient)
      count:=count+1.U
      multiplier_divisor:=Cat(multiplier_divisor(62,0),0.U(1.W))
      multiplicand_dividend:=multiplicand_dividend>>1
      when(count === 31.U){
        state:=sEND
      }
    }
    is(sDIV){
      val bt = multiplier_divisor>multiplicand_dividend
      multiplicand_dividend:=Mux(bt,multiplicand_dividend,multiplicand_dividend-multiplier_divisor)
      count:=count-1.U
      multiplier_divisor:=multiplier_divisor>>1
      product_quotient:= Cat(product_quotient(62,0),!bt)
      when(count === 0.U){
        state:=sEND
      }
    }
    is(sEND){
      complete:=true.B
      state:=sIDLE
    }
  }

  val wb_info       = Wire(new WriteBackInfo())
  val wb_info_valid = WireInit(false.B)
  io.wb_info.bits := wb_info
  io.wb_info.valid := wb_info_valid

  wb_info_valid:=complete
  wb_info.des_addr:=dispatch_info.des_addr
  wb_info.inst_addr:=dispatch_info.inst_addr
  wb_info.inst:=dispatch_info.inst
  wb_info.data:=result
  wb_info.csr_wen:=0.U
  wb_info.csr_idx:=0.U
  wb_info.csr_wdata:=0.U
  wb_info.except_type:=dispatch_info.except_type
  wb_info.mem_addr:=0.U
  wb_info.atomic := false.B

  io.dispatch_info.ready := complete || !dispatch_valid

}

