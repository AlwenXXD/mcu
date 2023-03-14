import chisel3._
import chisel3.util._

class RedirectInfo extends Bundle{
  val addr = UInt(32.W)
}
class BjuRedirectInfo extends RedirectInfo {
  val is_taken = Bool()
}
class InstInfo extends Bundle{
  val inst = UInt(32.W)
  val except_type = ExceptSel()
}

class InstFetchIO extends Bundle{
  val axi_rw = new CoreBundle()
  val pc = Output(UInt(32.W))
  val inst = Decoupled(new InstInfo)
  val bju_redirect = Flipped(Valid(new BjuRedirectInfo()))
  val except_redirect = Flipped(Valid(new RedirectInfo()))
}

class InstFetch extends Module {
  val io = IO(new InstFetchIO())
  val pc_en = RegInit(false.B)
  pc_en := true.B
  var PC_INIT = "h80000000"
  val pc = RegInit(PC_INIT.U(32.W))

  val is_except = io.except_redirect.valid
  val is_bju = io.bju_redirect.valid && io.bju_redirect.bits.is_taken
  val need_redirect = is_bju || is_except

  when(is_except){
    pc := io.except_redirect.bits.addr
  }.elsewhen(is_bju){
    pc := io.bju_redirect.bits.addr
  }
  val read_valid = !need_redirect && pc_en

  val sIDLE::sWAIT::sEND::Nil = Enum(3)
  val state = RegInit(sIDLE)
  val inst_read = RegInit(0.U(64.W))
  val except_type = RegInit(ExceptSel.is_null)
  val complete = RegInit(false.B)
  val had_redirect = RegInit(false.B)
  when(need_redirect){
    had_redirect:=true.B
  }

  io.axi_rw.req.valid:=false.B
  io.axi_rw.resp.ready:=false.B
  switch(state){
    is(sIDLE){
      when(read_valid){
        io.axi_rw.req.valid:=true.B
      }
      when(io.axi_rw.req.fire){
        state:=sWAIT
      }
    }
    is(sWAIT){
      io.axi_rw.resp.ready:=true.B
      when(io.axi_rw.resp.fire){
        inst_read:=io.axi_rw.resp.bits.data_read
        except_type:=Mux(io.axi_rw.resp.bits.resp === 0.U,ExceptSel.is_null,ExceptSel.is_inst_pgfault)
        when(need_redirect || had_redirect){
          had_redirect:=false.B
          state:=sIDLE
        }.otherwise{
          complete:=true.B
          state:=sEND
        }
      }

    }
    is(sEND){
      when(io.inst.fire){
        complete:=false.B
        when(need_redirect || had_redirect){
          had_redirect:=false.B
        }.otherwise{
          pc := pc +4.U
        }
        state:=sIDLE
      }

    }
  }

  io.axi_rw.req.bits.id := 0.U
  io.axi_rw.req.bits.addr := pc
  io.axi_rw.req.bits.size := 3.U(3.W)

  io.axi_rw.req.bits.data_write := 0.U
  io.axi_rw.req.bits.is_write := false.B
  io.axi_rw.req.bits.byte_mask := 0.U

  io.pc := pc
  io.inst.bits.inst := Mux(pc(2),inst_read(63, 32),inst_read(31, 0))
  io.inst.bits.except_type := except_type
  io.inst.valid := complete
}
