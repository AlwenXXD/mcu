import chisel3._
import chisel3.util._
import Instructions._


class Decoder extends Bundle {
  val uop             = uOP()
  val unit_sel        = UnitSel()
  val need_imm        = Bool()
  val op1_addr        = UInt(5.W)
  val op2_addr        = UInt(5.W)
  val des_addr        = UInt(5.W)
  val imm_data        = UInt(32.W)
  val csr_idx = UInt(12.W)
  val except_type = ExceptSel()

  def decode(inst: UInt) = {
    val imm_sel = Wire(ImmSel())
    val op1_sel = Wire(OpSel())
    val op2_sel = Wire(OpSel())
    val des_sel = Wire(OpSel())

    val decoder = ListLookup(inst, decode_default, decode_table)
    val signals = Seq(uop, unit_sel, imm_sel, op1_sel, op2_sel, des_sel, except_type)
    signals zip decoder foreach { case (s, d) => s := d }

    val rd    = inst(11, 7)
    val rs1   = inst(19, 15)
    val rs2   = inst(24, 20)
    val immI  = Cat(Fill(20,inst(31)),inst(31, 20))
    val immS  = Cat(Fill(20,inst(31)),inst(31, 25), inst(11, 7))
    val immB  = Cat(Fill(19,inst(31)),inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
    val immU  = Cat(inst(31, 12), 0.U(12.W))
    val immJ  = Cat(Fill(11,inst(31)),inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))
    val shamt = Cat(Fill(27,false.B),inst(24, 20))
    val zimm  = Cat(Fill(27,false.B),inst(19, 15))



    need_imm := imm_sel =/= ImmSel.no_imm
    op1_addr := MuxLookup(op1_sel.asUInt, 0.U(5.W), Array(
      OpSel.is_rs1.asUInt -> rs1
      ).toSeq)
    op2_addr := MuxLookup(op2_sel.asUInt, 0.U(5.W), Array(
      OpSel.is_rs2.asUInt -> rs2
      ).toSeq)
    des_addr := MuxLookup(des_sel.asUInt, 0.U(5.W), Array(
      OpSel.is_rd.asUInt -> rd
      ).toSeq)
    imm_data := MuxLookup(imm_sel.asUInt, 0.U(32.W), Array(
      ImmSel.is_I.asUInt -> immI,
      ImmSel.is_S.asUInt -> immS,
      ImmSel.is_B.asUInt -> immB,
      ImmSel.is_U.asUInt -> immU,
      ImmSel.is_J.asUInt -> immJ,
      ImmSel.is_shamt.asUInt -> shamt,
      ImmSel.is_zimm.asUInt -> zimm,
      ).toSeq)
    csr_idx := inst(31, 20)
    this
  }
}
