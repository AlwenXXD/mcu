import chisel3._
import chisel3.util._
import chisel3.util.experimental._


object CsrAddr {
  val mhartid  = "hf14".U
  val mstatus  = "h300".U
  val misa     = "h301".U
  val medeleg  = "h302".U
  val mideleg  = "h303".U
  val mie      = "h304".U
  val mtvec    = "h305".U
  val mscratch = "h340".U
  val mepc     = "h341".U
  val mcause   = "h342".U
  val mtval    = "h343".U
  val mip      = "h344".U
  val mcycle   = "hb00".U
  val minstret = "hb02".U

  val sstatus  = "h100".U
  val sie      = "h104".U
  val stvec    = "h105".U
  val sscratch = "h140".U
  val sepc     = "h141".U
  val scause   = "h142".U
  val stval    = "h143".U
  val sip      = "h144".U
  val satp     = "h180".U

}
class CsrWriteInfo extends Bundle{
  val csr_idx = UInt(12.W)
  val csr_data = UInt(32.W)
  val csr_wen = Bool()
}

class VSRInfo extends Bundle{
  val mstatus_tvm  = Bool() //20
  val mstatus_tsr  = Bool() //22
  val mstatus_tw   = Bool() //21
}

class CsrInfo extends Bundle{

  val medeleg_illegal  = Bool() //2
  val medeleg_break    = Bool()//3
  val medeleg_ecall_u  = Bool() //8
  val medeleg_ecall_s  = Bool() //9
  val medeleg_ipgfault = Bool() //12
  val medeleg_lpgfault = Bool() //13
  val medeleg_spgfault = Bool() //15

  val mideleg_ssi  = Bool()
  val mideleg_sti  = Bool()

  val mtvec_base        = UInt(30.W) //31:2
  val en_meip          = Bool() //11
  val en_mtip          = Bool() //7
  val en_stip          = Bool() //5
  val en_ssip          = Bool() //1
  val mepc              = UInt(32.W)
  val stvec_base        = UInt(30.W) //31:2
  val sepc              = UInt(32.W)
}
class InstrInfo extends Bundle{
  val intr_t = Bool()
  val intr_e = Bool()
}

class CsrIO extends Bundle {
  val csr_read = new CsrReadIO
  val csr_info     = Output(new CsrInfo)
  val csr_commit_info = Flipped(Valid(new CsrWriteInfo))
  val except_info = Flipped(Valid(new ExceptInfo))
  val instr_info = Input(new InstrInfo)
  val current_privilege = Output(UInt(2.W))
  val vsr_info = Output(new VSRInfo)
}



class Csr extends Module {
  val io             = IO(new CsrIO)

  val current_privilege = RegInit(3.U(2.W))
  io.current_privilege:=current_privilege
  val is_m = current_privilege ===3.U
  val is_s = current_privilege ===1.U
  val is_u = current_privilege ===0.U


  val mcycle  = RegInit(0.U(64.W))
  mcycle := mcycle + 1.U
  val mhartid  = WireInit(0.U(32.W))
  val misa_mxl  = WireInit(1.U(2.W))
  val misa_extention  = WireInit(((1 << 8) | (1 << 18) | (1 << 20)).U(26.W))

  val mstatus_mie  = RegInit(0.U(1.W)) //3
  val mstatus_sie  = RegInit(0.U(1.W)) //1
  val mstatus_mpp  = RegInit(3.U(2.W)) //12:11
  val mstatus_mpie = RegInit(0.U(1.W)) //7
  val mstatus_spp  = RegInit(1.U(1.W)) //8
  val mstatus_spie = RegInit(0.U(1.W)) //5
  val mstatus_mxr  = RegInit(0.U(1.W)) //19
  val mstatus_mprv = RegInit(0.U(1.W)) //17
  val mstatus_sum  = RegInit(0.U(1.W)) //18
  val mstatus_ube  = WireInit(0.U(1.W)) //6
  val mstatus_tvm  = RegInit(0.U(1.W)) //20
  val mstatus_tsr  = RegInit(0.U(1.W)) //22
  val mstatus_tw   = RegInit(0.U(1.W)) //21
  val mstatus_xs   = RegInit(0.U(2.W)) //16:15
  val mstatus_fs   = RegInit(0.U(2.W)) //14:13
  val mstatus_sd  = mstatus_xs ==="b11".U(2.W) || mstatus_fs ==="b11".U(2.W)//31

  val mtvec_base    = RegInit(0.U(30.W))//31:2
  val mtvec_mode    = WireInit(0.U(2.W))//1:0

  val medeleg_imisalign = RegInit(0.U(1.W)) //0
  val medeleg_iafault   = RegInit(0.U(1.W)) //1
  val medeleg_illegal   = RegInit(0.U(1.W)) //2
  val medeleg_break     = RegInit(0.U(1.W)) //3
  val medeleg_ldmisalign    = RegInit(0.U(1.W)) //4
  val medeleg_ldafault    = RegInit(0.U(1.W)) //5
  val medeleg_stmisalign    = RegInit(0.U(1.W)) //6
  val medeleg_stafault    = RegInit(0.U(1.W)) //7
  val medeleg_ecall_u   = RegInit(0.U(1.W)) //8
  val medeleg_ecall_s   = RegInit(0.U(1.W)) //9
  val medeleg_ipgfault  = RegInit(0.U(1.W)) //12
  val medeleg_lpgfault  = RegInit(0.U(1.W)) //13
  val medeleg_reserve  = RegInit(0.U(1.W)) //14
  val medeleg_spgfault  = RegInit(0.U(1.W)) //15

  val mideleg_ssi  = RegInit(0.U(1.W))//1
  val mideleg_sti  = RegInit(0.U(1.W))//5
  val mideleg_sei  = RegInit(0.U(1.W))//9

  val mie_mei = RegInit(0.U(1.W))//11
  val mie_sei = RegInit(0.U(1.W))//9
  val mie_mti = RegInit(0.U(1.W))//7
  val mie_sti = RegInit(0.U(1.W))//5
  val mie_ssi = RegInit(0.U(1.W))//1

  val mip_meip   = io.instr_info.intr_e//11
  val mip_mtip   = io.instr_info.intr_t//7
  val mip_stip   = RegInit(0.U(1.W))//5
  val mip_ssip   = RegInit(0.U(1.W))//1


  val mscratch = RegInit(0.U(32.W))
  val mepc     = RegInit(0.U(32.W))
  val mcause   = RegInit(0.U(32.W))
  val mtval = RegInit(0.U(32.W))


  val stvec_base    = RegInit(0.U(30.W))//31:2
  val stvec_mode    = WireInit(0.U(2.W))//1:0

  val satp_mode    = RegInit(0.U(1.W))//31
  val satp_asid    = WireInit(0.U(9.W))//30:22
  val satp_ppn    = RegInit(0.U(22.W))//21:0

  val sscratch = RegInit(0.U(32.W))
  val sepc     = RegInit(0.U(32.W))
  val scause   = RegInit(0.U(32.W))
  val stval    = RegInit(0.U(32.W))

  val en_meip   = (is_m & mip_meip & mie_mei & mstatus_mie) | (!is_m & mip_meip & mie_mei)
  val en_mtip   = (is_m & mip_mtip & mie_mti & mstatus_mie) | (!is_m & mip_mtip & mie_mti)
  val en_stip   = (is_m & mip_stip & mie_sti & mstatus_mie & !mideleg_sti) | (is_s & mip_stip & mie_sti & mstatus_sie & mideleg_sti) | (is_u & mip_stip & mip_stip & mie_sti)
  val en_ssip   = (is_m & mip_ssip & mie_ssi & mstatus_mie & !mideleg_ssi) | (is_s & mip_ssip & mie_ssi & mstatus_sie & mideleg_ssi) | (is_u & mip_ssip & mip_ssip & mie_ssi)

  def non_write(wdata:UInt):Unit={
  }
  def gen_write(reg:UInt): UInt=>Unit ={
    def write(wdata:UInt):Unit={
      reg:=wdata
    }
    write
  }
  def read_misa:UInt = {
    Cat(misa_mxl,0.U(4.W),misa_extention)
  }
  def read_mstatus:UInt = {
    Cat(mstatus_sd,0.U(8.W),mstatus_tsr,mstatus_tw,mstatus_tvm,mstatus_mxr,mstatus_sum,mstatus_mprv,mstatus_xs
      ,mstatus_fs,mstatus_mpp,0.U(2.W),mstatus_spp,mstatus_mpie,mstatus_ube,mstatus_spie
      ,0.U(1.W),mstatus_mie,0.U(1.W),mstatus_sie,0.U(1.W))
  }
  def write_mstatus(wdata:UInt):Unit = {
    mstatus_mie :=  wdata(3) //3
    mstatus_sie :=  wdata(1) //1
    mstatus_mpp :=  wdata(12,11) //12:11
    mstatus_mpie := wdata(7) //7
    mstatus_spp :=  wdata(8) //8
    mstatus_spie := wdata(5) //5
    mstatus_mxr :=  wdata(19) //19
    mstatus_mprv := wdata(17) //17
    mstatus_sum :=  wdata(18) //18
    mstatus_tvm :=  wdata(20) //20
    mstatus_tsr:=wdata(22)
    mstatus_tw:=wdata(21)
    mstatus_xs :=   wdata(16,15) //16:15
    mstatus_fs :=   wdata(14,13) //14:13
  }
  def read_mtvec:UInt = {
    Cat(mtvec_base,mtvec_mode)
  }
  def write_mtvec(wdata:UInt):Unit = {
    mtvec_base    := wdata(31,2)
  }
  def read_medeleg:UInt = {
    Cat(0.U(16.W),medeleg_spgfault,medeleg_reserve,medeleg_ipgfault,medeleg_lpgfault,0.U(2.W)
      ,medeleg_ecall_s,medeleg_ecall_u,medeleg_stafault,medeleg_stmisalign,medeleg_ldafault
      ,medeleg_ldmisalign,medeleg_break,medeleg_illegal,medeleg_iafault,medeleg_imisalign)
  }
  def write_medeleg(wdata:UInt):Unit = {
    medeleg_imisalign  := wdata(0)
    medeleg_iafault    := wdata(1)
    medeleg_illegal    := wdata(2)
    medeleg_break      := wdata(3)
    medeleg_ldmisalign := wdata(4)
    medeleg_ldafault   := wdata(5)
    medeleg_stmisalign := wdata(6)
    medeleg_stafault   := wdata(7)
    medeleg_ecall_u    := wdata(8)
    medeleg_ecall_s    := wdata(9)
    medeleg_ipgfault   := wdata(12)
    medeleg_lpgfault   := wdata(13)
    medeleg_reserve    := wdata(14)
    medeleg_spgfault   := wdata(15)
  }
  def read_mideleg:UInt = {
    Cat(0.U(22.W),mideleg_sei,0.U(3.W),mideleg_sti,0.U(3.W),mideleg_ssi,0.U(1.W))
  }
  def write_mideleg(wdata:UInt):Unit = {
    mideleg_sei:=wdata(9)
    mideleg_sti:=wdata(5)
    mideleg_ssi:=wdata(1)
  }
  def read_mip:UInt = {
    Cat(0.U(20.W),mip_meip,0.U(3.W),mip_mtip,0.U(1.W),mip_stip,0.U(3.W),mip_ssip,0.U(1.W))
  }
  def write_mip(wdata:UInt):Unit = {
    mip_stip:=wdata(5)
    mip_ssip:=wdata(1)
  }
  def read_mie:UInt = {
    Cat(0.U(20.W),mie_mei,0.U(1.W),mie_sei,0.U(1.W),mie_mti,0.U(1.W),mie_sti,0.U(3.W),mie_ssi,0.U(1.W))
  }
  def write_mie(wdata:UInt):Unit = {
    mie_mei:=wdata(11)
    mie_sei:=wdata(9)
    mie_mti:=wdata(7)
    mie_sti:=wdata(5)
    mie_ssi:=wdata(1)
  }

  def read_sstatus:UInt = {
    Cat(mstatus_sd,0.U(11.W),mstatus_mxr,mstatus_sum,0.U(1.W)
      ,mstatus_xs,mstatus_fs,0.U(4.W),mstatus_spp,0.U(1.W),mstatus_ube,mstatus_spie,0.U(3.W),mstatus_sie,0.U(1.W))
  }
  def write_sstatus(wdata:UInt):Unit = {
    mstatus_sie :=  wdata(1) //1
    mstatus_spp :=  wdata(8) //8
    mstatus_spie := wdata(5) //5
    mstatus_mxr :=  wdata(19) //19
    mstatus_sum :=  wdata(18) //18
    mstatus_xs :=   wdata(16,15) //16:15
    mstatus_fs :=   wdata(14,13) //14:13
  }
  def read_stvec:UInt = {
    Cat(stvec_base,stvec_mode)
  }
  def write_stvec(wdata:UInt):Unit = {
    stvec_base    := wdata(31,2)
  }
  def read_sie:UInt = {
    Cat(0.U(22.W),mie_sei,0.U(3.W),mie_sti,0.U(3.W),mie_ssi,0.U(1.W))
  }
  def write_sie(wdata:UInt):Unit = {
    when(mideleg_sti.asBool){
      mie_sti:=wdata(5)
    }
    when(mideleg_ssi.asBool){
      mie_ssi:=wdata(1)
    }
    when(mideleg_sei.asBool){
      mie_sei:=wdata(9)
    }
  }
  def read_sip:UInt = {
    Cat(0.U(26.W),mip_stip&mideleg_sti,0.U(3.W),mip_ssip&mideleg_ssi,0.U(1.W))
  }
  def write_sip(wdata:UInt):Unit = {
    mip_stip:=wdata(5)
    mip_ssip:=wdata(1)
  }
  def read_satp:UInt = {
    Cat(satp_mode,satp_asid,satp_ppn)
  }
  def write_satp(wdata:UInt):Unit = {
    satp_mode  := wdata(31)
    satp_ppn   := wdata(21,0)
  }

  io.csr_info.mtvec_base        := mtvec_base
  io.csr_info.medeleg_illegal := medeleg_illegal
  io.csr_info.medeleg_break     := medeleg_break
  io.csr_info.medeleg_ecall_u   := medeleg_ecall_u
  io.csr_info.medeleg_ecall_s   := medeleg_ecall_s
  io.csr_info.medeleg_ipgfault  := medeleg_ipgfault
  io.csr_info.medeleg_lpgfault  := medeleg_lpgfault
  io.csr_info.medeleg_spgfault  := medeleg_spgfault
  io.csr_info.mideleg_ssi  := mideleg_ssi
  io.csr_info.mideleg_sti  := mideleg_sti
  io.csr_info.en_meip           := en_meip
  io.csr_info.en_mtip           := en_mtip
  io.csr_info.en_stip           := en_stip
  io.csr_info.en_ssip           := en_ssip
  io.csr_info.mepc              := mepc
  io.csr_info.stvec_base        := stvec_base
  io.csr_info.sepc              := sepc



  //csr_addr read_csr write_csr read_handler write_handler
  val csr_map:Map[UInt,(UInt,UInt=>Unit)] = Map(
    (CsrAddr.mcycle  , (mcycle,gen_write(mcycle))),
    (CsrAddr.mhartid , (0.U(32.W),non_write)),
    (CsrAddr.misa , (read_misa,non_write)),
    (CsrAddr.mstatus , (read_mstatus,write_mstatus)),
    (CsrAddr.medeleg , (read_medeleg,write_medeleg)),
    (CsrAddr.mideleg , (read_mideleg,write_mideleg)),
    (CsrAddr.mie     , (read_mie     ,write_mie)),
    (CsrAddr.mip     , (read_mip,write_mip)),
    (CsrAddr.mtvec   , (read_mtvec   ,write_mtvec)),
    (CsrAddr.mscratch, (mscratch,gen_write(mscratch))),
    (CsrAddr.mepc    , (mepc    ,gen_write(mepc    ))),
    (CsrAddr.mcause  , (mcause  ,gen_write(mcause  ))),
    (CsrAddr.mtval   , (mtval   ,gen_write(mtval   ))),

    (CsrAddr.sstatus , (read_sstatus,write_sstatus)),
    (CsrAddr.sie     , (read_sie  ,write_sie)),
    (CsrAddr.sip     , (read_sip  ,write_sip)),
    (CsrAddr.satp    , (read_satp ,write_satp)),
    (CsrAddr.stvec   , (read_stvec,write_stvec)),
    (CsrAddr.sscratch, (sscratch  ,gen_write(sscratch))),
    (CsrAddr.sepc    , (sepc      ,gen_write(sepc    ))),
    (CsrAddr.scause  , (scause    ,gen_write(scause  ))),
    (CsrAddr.stval   , (stval     ,gen_write(stval   ))),

  )
  val rdata   = WireInit(0.U(32.W))

  csr_map.foreach {
    case (csr_addr,(read_handler,write_handler)) =>
      when(io.csr_read.csr_idx === csr_addr) {
        rdata := read_handler
      }
  }
  csr_map.foreach {
    case (csr_addr,(read_handler,write_handler)) =>
        when(io.csr_commit_info.bits.csr_wen&&io.csr_commit_info.valid&&io.csr_commit_info.bits.csr_idx===csr_addr){
          val wdata = io.csr_commit_info.bits.csr_data
            write_handler(wdata)
        }
  }


  when(io.except_info.valid){
    when(io.except_info.bits.is_ei){
      when(io.except_info.bits.is_delegate){
        scause:=io.except_info.bits.cause
        sepc:=io.except_info.bits.epc
        stval:=io.except_info.bits.tval
        mstatus_sie:=0.U
        current_privilege:=1.U
        mstatus_spie:=mstatus_sie
        mstatus_spp:=current_privilege(0)
      }.otherwise{
        mcause:=io.except_info.bits.cause
        mepc:=io.except_info.bits.epc
        mtval:=io.except_info.bits.tval
        mstatus_mie:=0.U
        current_privilege:=3.U
        mstatus_mpie:=mstatus_mie
        mstatus_mpp:=current_privilege
      }
    }.elsewhen(io.except_info.bits.is_mret){
      mstatus_mie:=mstatus_mpie
      current_privilege:=mstatus_mpp
      mstatus_mpie:=1.U
      mstatus_mpp:=0.U
      when(mstatus_mpp =/= 3.U){
        mstatus_mprv:=0.U
      }
    }.elsewhen(io.except_info.bits.is_sret){
      mstatus_sie:=mstatus_spie
      current_privilege:=Cat(0.U(1.W),mstatus_spp)
      mstatus_spie:=1.U
      mstatus_spp:=0.U
      when(mstatus_spp =/= 3.U){
        mstatus_mprv:=0.U
      }
    }
  }


  io.csr_read.csr_data:=rdata


  io.vsr_info.mstatus_tvm:=mstatus_tvm
  io.vsr_info.mstatus_tsr:=mstatus_tsr
  io.vsr_info.mstatus_tw:=mstatus_tw


}
