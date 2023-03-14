import chisel3.experimental.ChiselEnum
import chisel3.util.BitPat

object uOP extends ChiselEnum {
  val
  NOP,
  //bju
  Bju_BEQ,
  Bju_BNE,
  Bju_BLT,
  Bju_BGE,
  Bju_BLTU,
  Bju_BGEU,
  Bju_JALR,
  Bju_JAL,
  //alu
  Alu_LUI,
  Alu_AUIPC,
  Alu_ADD,
  Alu_SUB,
  Alu_SLL,
  Alu_SLT,
  Alu_SLTU,
  Alu_XOR,
  Alu_SRL,
  Alu_SRA,
  Alu_OR,
  Alu_AND,
  //lsu
  Lsu_LB,
  Lsu_LH,
  Lsu_LW,
  Lsu_LBU,
  Lsu_LHU,
  Lsu_SB,
  Lsu_SH,
  Lsu_SW,

  //mdu
  Mdu_MUL,
  Mdu_MULH,
  Mdu_MULHSU,
  Mdu_MULHU,
  Mdu_DIV,
  Mdu_DIVU,
  Mdu_REM,
  Mdu_REMU,
  //privilege
  ECALL,
  EBREAK,
  MRET,
  SRET,
  FENCE,
  FENCE_I,
  SFENCE_VMA,
  WFI,
  //csr
  Csr_CSRRW,
  Csr_CSRRS,
  Csr_CSRRC = Value
}


object UnitSel extends ChiselEnum {
  val
  is_Null,
  is_Alu,
  is_Lsu,
  is_Mdu,
  is_Csr,
  is_Bju = Value
}


object ImmSel extends ChiselEnum {
  val
  is_I,
  is_U,
  is_J,
  is_S,
  is_B,
  is_zimm,
  is_shamt,
  no_imm = Value
}

object OpSel extends ChiselEnum {
  val
  is_rs1,
  is_rs2,
  is_rd,
  is_csr,
  is_null = Value
}

object ExceptSel extends ChiselEnum{
  val
  is_null,
  decode_wfi,
  decode_sfence_vma,
  decode_csr,
  is_illegal,
  is_inst_pgfault,
  is_load_pgfault,
  is_store_pgfault,
  is_mret,
  is_sret,
  is_ebreak,
  is_ecall,
  is_halt = Value
}

object AddrMap{
  def CLINT = BitPat("b00000000_00000000_00000000_00000000_00000010_00000000_????????_????????")
  def MEM_N   = BitPat("b00000000_00000000_00000000_00000000_0???????_????????_????????_????????")
}



