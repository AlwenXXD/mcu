import chisel3._
import chisel3.util._

object Instructions {
  val BEQ                = BitPat("b?????????????????000?????1100011")
  val BNE                = BitPat("b?????????????????001?????1100011")
  val BLT                = BitPat("b?????????????????100?????1100011")
  val BGE                = BitPat("b?????????????????101?????1100011")
  val BLTU               = BitPat("b?????????????????110?????1100011")
  val BGEU               = BitPat("b?????????????????111?????1100011")
  val JALR               = BitPat("b?????????????????000?????1100111")
  val JAL                = BitPat("b?????????????????????????1101111")
  val LUI                = BitPat("b?????????????????????????0110111")
  val AUIPC              = BitPat("b?????????????????????????0010111")
  val ADDI               = BitPat("b?????????????????000?????0010011")
  val SLLI               = BitPat("b000000???????????001?????0010011")
  val SLTI               = BitPat("b?????????????????010?????0010011")
  val SLTIU              = BitPat("b?????????????????011?????0010011")
  val XORI               = BitPat("b?????????????????100?????0010011")
  val SRLI               = BitPat("b000000???????????101?????0010011")
  val SRAI               = BitPat("b010000???????????101?????0010011")
  val ORI                = BitPat("b?????????????????110?????0010011")
  val ANDI               = BitPat("b?????????????????111?????0010011")
  val ADD                = BitPat("b0000000??????????000?????0110011")
  val SUB                = BitPat("b0100000??????????000?????0110011")
  val SLL                = BitPat("b0000000??????????001?????0110011")
  val SLT                = BitPat("b0000000??????????010?????0110011")
  val SLTU               = BitPat("b0000000??????????011?????0110011")
  val XOR                = BitPat("b0000000??????????100?????0110011")
  val SRL                = BitPat("b0000000??????????101?????0110011")
  val SRA                = BitPat("b0100000??????????101?????0110011")
  val OR                 = BitPat("b0000000??????????110?????0110011")
  val AND                = BitPat("b0000000??????????111?????0110011")


  val LB                 = BitPat("b?????????????????000?????0000011")
  val LH                 = BitPat("b?????????????????001?????0000011")
  val LW                 = BitPat("b?????????????????010?????0000011")
  val LBU                = BitPat("b?????????????????100?????0000011")
  val LHU                = BitPat("b?????????????????101?????0000011")
  val SB                 = BitPat("b?????????????????000?????0100011")
  val SH                 = BitPat("b?????????????????001?????0100011")
  val SW                 = BitPat("b?????????????????010?????0100011")

  val FENCE              = BitPat("b?????????????????000?????0001111")
  val FENCE_I            = BitPat("b?????????????????001?????0001111")
  val ECALL              = BitPat("b00000000000000000000000001110011")
  val EBREAK             = BitPat("b00000000000100000000000001110011")
  val MRET               = BitPat("b00110000001000000000000001110011")
  val SRET               = BitPat("b00010000001000000000000001110011")
  val SFENCE_VMA         = BitPat("b0001001??????????000000001110011")
  val WFI                = BitPat("b00010000010100000000000001110011")
  val CSRRW              = BitPat("b?????????????????001?????1110011")
  val CSRRS              = BitPat("b?????????????????010?????1110011")
  val CSRRC              = BitPat("b?????????????????011?????1110011")
  val CSRRWI             = BitPat("b?????????????????101?????1110011")
  val CSRRSI             = BitPat("b?????????????????110?????1110011")
  val CSRRCI             = BitPat("b?????????????????111?????1110011")

  val MUL                = BitPat("b0000001??????????000?????0110011")
  val MULH               = BitPat("b0000001??????????001?????0110011")
  val MULHSU             = BitPat("b0000001??????????010?????0110011")
  val MULHU              = BitPat("b0000001??????????011?????0110011")
  val DIV                = BitPat("b0000001??????????100?????0110011")
  val DIVU               = BitPat("b0000001??????????101?????0110011")
  val REM                = BitPat("b0000001??????????110?????0110011")
  val REMU               = BitPat("b0000001??????????111?????0110011")

  //sim
  val HALT    = BitPat("b00000000000000000000000001101011")
  val PUTCH   = BitPat("b00000000000000000000000001111011")

  //                              uop unit_sel imm_sel op1_sel op2_sel des_sel exception_type
  val decode_default = List(uOP.NOP, UnitSel.is_Null, ImmSel.no_imm, OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_illegal)
  val decode_table = Array(
    BEQ        -> List(uOP.Bju_BEQ    , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    BNE        -> List(uOP.Bju_BNE    , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    BLT        -> List(uOP.Bju_BLT    , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    BGE        -> List(uOP.Bju_BGE    , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    BLTU       -> List(uOP.Bju_BLTU   , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    BGEU       -> List(uOP.Bju_BGEU   , UnitSel.is_Bju,  ImmSel.is_B,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    JALR       -> List(uOP.Bju_JALR   , UnitSel.is_Bju,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    JAL        -> List(uOP.Bju_JAL    , UnitSel.is_Bju,  ImmSel.is_J,     OpSel.is_null, OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    LUI        -> List(uOP.Alu_LUI    , UnitSel.is_Alu,  ImmSel.is_U,     OpSel.is_null, OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    AUIPC      -> List(uOP.Alu_AUIPC  , UnitSel.is_Alu,  ImmSel.is_U,     OpSel.is_null, OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    ADDI       -> List(uOP.Alu_ADD    , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SLLI       -> List(uOP.Alu_SLL    , UnitSel.is_Alu,  ImmSel.is_shamt, OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SLTI       -> List(uOP.Alu_SLT    , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SLTIU      -> List(uOP.Alu_SLTU   , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    XORI       -> List(uOP.Alu_XOR    , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SRLI       -> List(uOP.Alu_SRL    , UnitSel.is_Alu,  ImmSel.is_shamt, OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SRAI       -> List(uOP.Alu_SRA    , UnitSel.is_Alu,  ImmSel.is_shamt, OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    ORI        -> List(uOP.Alu_OR     , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    ANDI       -> List(uOP.Alu_AND    , UnitSel.is_Alu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    ADD        -> List(uOP.Alu_ADD    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SUB        -> List(uOP.Alu_SUB    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SLL        -> List(uOP.Alu_SLL    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SLT        -> List(uOP.Alu_SLT    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SLTU       -> List(uOP.Alu_SLTU   , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    XOR        -> List(uOP.Alu_XOR    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SRL        -> List(uOP.Alu_SRL    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    SRA        -> List(uOP.Alu_SRA    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    OR         -> List(uOP.Alu_OR     , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),
    AND        -> List(uOP.Alu_AND    , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_rd,ExceptSel.is_null),



    LB         -> List(uOP.Lsu_LB     , UnitSel.is_Lsu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    LH         -> List(uOP.Lsu_LH     , UnitSel.is_Lsu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    LW         -> List(uOP.Lsu_LW     , UnitSel.is_Lsu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    LBU        -> List(uOP.Lsu_LBU    , UnitSel.is_Lsu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    LHU        -> List(uOP.Lsu_LHU    , UnitSel.is_Lsu,  ImmSel.is_I,     OpSel.is_rs1,  OpSel.is_null, OpSel.is_rd,ExceptSel.is_null),
    SB         -> List(uOP.Lsu_SB     , UnitSel.is_Lsu,  ImmSel.is_S,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    SH         -> List(uOP.Lsu_SH     , UnitSel.is_Lsu,  ImmSel.is_S,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),
    SW         -> List(uOP.Lsu_SW     , UnitSel.is_Lsu,  ImmSel.is_S,     OpSel.is_rs1,  OpSel.is_rs2,  OpSel.is_null,ExceptSel.is_null),

    CSRRW      -> List(uOP.Csr_CSRRW  , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    CSRRS      -> List(uOP.Csr_CSRRS  , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    CSRRC      -> List(uOP.Csr_CSRRC  , UnitSel.is_Alu,  ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    CSRRWI     -> List(uOP.Csr_CSRRW  , UnitSel.is_Alu,  ImmSel.is_zimm,  OpSel.is_null, OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    CSRRSI     -> List(uOP.Csr_CSRRS  , UnitSel.is_Alu,  ImmSel.is_zimm,  OpSel.is_null, OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    CSRRCI     -> List(uOP.Csr_CSRRC  , UnitSel.is_Alu,  ImmSel.is_zimm,  OpSel.is_null, OpSel.is_csr,  OpSel.is_rd,ExceptSel.decode_csr),
    FENCE      -> List(uOP.FENCE      , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_null),
    FENCE_I    -> List(uOP.FENCE_I    , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_null),
    ECALL      -> List(uOP.ECALL      , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_ecall),
    EBREAK     -> List(uOP.EBREAK     , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_ebreak),
    MRET       -> List(uOP.MRET       , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_null, OpSel.is_null,ExceptSel.is_mret),
    SRET       -> List(uOP.SRET       , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_rs1,  OpSel.is_null, OpSel.is_null,ExceptSel.is_sret),
    SFENCE_VMA -> List(uOP.SFENCE_VMA , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.decode_sfence_vma),
    WFI        -> List(uOP.WFI        , UnitSel.is_Null, ImmSel.no_imm,   OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.decode_wfi),
    //diff
    HALT         -> List(uOP.NOP, UnitSel.is_Null, ImmSel.no_imm, OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_halt),
    PUTCH        -> List(uOP.NOP, UnitSel.is_Null, ImmSel.no_imm, OpSel.is_null, OpSel.is_null, OpSel.is_null,ExceptSel.is_null),
    )

}


