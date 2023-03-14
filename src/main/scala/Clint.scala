import chisel3._
import chisel3.util._
import chisel3.util.experimental._

class ClintIO extends Bundle{
  val bus    = Flipped(new CoreBundle())
  val intr_t = Output(new Bool())
}

class Clint extends Module {
  val io = IO(new ClintIO)

  val update_mtimecmp = WireInit(false.B)
  val mtime = RegInit(0.U(64.W))
  val mtimecmp = RegInit("h00000000_00000000".U(64.W))
  val intr_t   = RegInit(false.B)

  val cnt = RegInit(0.U(2.W))
  val stop = RegInit(true.B)

  cnt:=cnt+1.U
  when(cnt === 0.U && !stop){
    mtime:=mtime+1.U
  }

  when(mtime>mtimecmp){
    intr_t:=true.B
    stop :=true.B
  }

  when(update_mtimecmp){
    intr_t:=false.B
    stop :=false.B
  }

  io.intr_t:=intr_t

  val sIDLE::sWAIT::sEND::Nil = Enum(3)
  val state = RegInit(sIDLE)
  val bus_req = Reg(new CoreReq)
  val bus_resp = Reg(new CoreResp)
  val sel_mtime = bus_req.addr === "h0200_bff8".U(32.W)
  val sel_mtimecmp = bus_req.addr === "h0200_4000".U(32.W)

  def byte2bit(byte_mask: UInt): UInt = {
    val bit_mask = Wire(Vec(byte_mask.getWidth * 8,Bool()))
    for (i <- 0 until bit_mask.getWidth) {
      bit_mask(i) := byte_mask(i / 8)
    }
    bit_mask.asUInt
  }

  io.bus.resp.valid:=false.B
  io.bus.req.ready:=false.B
  switch(state){
    is(sIDLE){
      io.bus.req.ready:=true.B
      bus_req:=io.bus.req.bits
      when(io.bus.req.fire){
        state:=sWAIT
      }
    }
    is(sWAIT){
      when(bus_req.is_write){
        val bit_mask = byte2bit(bus_req.byte_mask)
        when(sel_mtime){
          mtime:= (mtime & bit_mask.do_unary_~)|(bus_req.data_write & bit_mask)
        }
        when(sel_mtimecmp){
          mtimecmp:= (mtimecmp & bit_mask.do_unary_~)|(bus_req.data_write & bit_mask)
          update_mtimecmp := true.B
        }
      }
      when(sel_mtime){
        bus_resp.data_read:=mtime
      }
      when(sel_mtimecmp){
        bus_resp.data_read:=mtimecmp
      }
      bus_resp.resp:=0.U
      bus_resp.id:=bus_req.id

      state:=sEND
    }
    is(sEND){
      io.bus.resp.valid:=true.B
      when(io.bus.resp.fire){
        state:=sIDLE
      }
    }
  }
  io.bus.resp.bits:=bus_resp

  when(reset.asBool){
    bus_resp.init()
    bus_req.init()
  }

}
