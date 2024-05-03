import chisel3._
import chisel3.util._



class Alu extends Module {
  val io            = IO(new UnitIO)
  val dispatch_info = Wire(new DispatchInfo)
  dispatch_info := io.dispatch_info.bits
  val dispatch_valid = WireInit(false.B)
  dispatch_valid := io.dispatch_info.valid
  io.dispatch_info.ready := true.B


  val op1_data    = dispatch_info.op1_data
  val op2_data    = Mux(dispatch_info.need_imm, dispatch_info.imm_data, dispatch_info.op2_data)
  val csr_data = dispatch_info.csr_data
  val csr_wdata = WireInit(0.U(32.W))
  val csr_op_data = Mux(dispatch_info.need_imm,dispatch_info.imm_data,dispatch_info.op1_data)
  val csr_wen = WireInit(false.B)
  val result_data = WireInit(0.U(32.W))

  switch(dispatch_info.uop) {
    is(uOP.Alu_LUI) {
      result_data := dispatch_info.imm_data
    }
    is(uOP.Alu_AUIPC) {
      result_data := dispatch_info.inst_addr + dispatch_info.imm_data
    }
    is(uOP.Alu_ADD) {
      result_data := op1_data + op2_data
    }
    is(uOP.Alu_SUB) {
      result_data := op1_data - op2_data
    }
    is(uOP.Alu_SLL) {
      result_data := (op1_data.asUInt << op2_data(4,0))(31,0)
    }
    is(uOP.Alu_SLT) {
      result_data := op1_data.asSInt < op2_data.asSInt
    }
    is(uOP.Alu_SLTU) {
      result_data := op1_data.asUInt < op2_data.asUInt
    }
    is(uOP.Alu_XOR) {
      result_data := op1_data ^ op2_data
    }
    is(uOP.Alu_SRL) {
      result_data := op1_data.asUInt >> op2_data(4,0)
    }
    is(uOP.Alu_SRA) {
      result_data := (op1_data.asSInt >> op2_data(4,0)).asUInt
    }
    is(uOP.Alu_OR) {
      result_data := op1_data | op2_data
    }
    is(uOP.Alu_AND) {
      result_data := op1_data & op2_data
    }
    is(uOP.Csr_CSRRC) {
      csr_wdata := csr_data & csr_op_data.do_unary_~
      result_data:=csr_data
      csr_wen:=true.B
    }
    is(uOP.Csr_CSRRS) {
      csr_wdata := csr_data | csr_op_data
      result_data:=csr_data
      csr_wen:=true.B
    }
    is(uOP.Csr_CSRRW) {
      csr_wdata := csr_op_data
      result_data:=csr_data
      csr_wen:=true.B
    }
  }
  val wb_info       = Wire(new WriteBackInfo())
  val wb_info_valid = WireInit(false.B)
  io.wb_info.bits := wb_info
  io.wb_info.valid := wb_info_valid

  wb_info.data:=result_data
  wb_info.inst_addr:=dispatch_info.inst_addr
  wb_info.inst:=dispatch_info.inst
  wb_info.des_addr:=dispatch_info.des_addr
  wb_info.csr_idx:=dispatch_info.csr_idx
  wb_info.csr_wdata:=csr_wdata
  wb_info.csr_wen:=csr_wen
  wb_info.except_type:=dispatch_info.except_type
  wb_info.mem_addr:=0.U
  wb_info_valid:=dispatch_valid
  wb_info.atomic := false.B

}

