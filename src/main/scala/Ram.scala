import chisel3._
import chisel3.util._
import chisel3.util.experimental._

class RAMHelper extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clk=Input(Clock())
    val en=Input(Bool())
    val rIdx  = Input(UInt(64.W))
    val rdata = Output(UInt(64.W))
    val wIdx  = Input(UInt(64.W))
    val wdata = Input(UInt(64.W))
    val wmask = Input(UInt(64.W))
    val wen   = Input(Bool())
  })
  addResource("/ram.v")
}
class RamIO extends Bundle{
  val bus    = Flipped(new CoreBundle())
}
class Ram extends Module {
  val io = IO(new RamIO)

  val sIDLE :: sWAIT :: sEND :: Nil = Enum(3)
  val state = RegInit(sIDLE)
  val bus_req = Reg(new CoreReq)
  val bus_resp = Reg(new CoreResp)
  val ramhelper = Module(new RAMHelper)
  ramhelper.io.clk := clock
  ramhelper.io.en := true.B
  ramhelper.io.rIdx  := 0.U
  ramhelper.io.wIdx  := 0.U
  ramhelper.io.wdata := 0.U
  ramhelper.io.wmask := 0.U
  ramhelper.io.wen   := false.B

  def byte2bit(byte_mask: UInt): UInt = {
    val bit_mask = Wire(Vec(byte_mask.getWidth * 8, Bool()))
    for (i <- 0 until bit_mask.getWidth) {
      bit_mask(i) := byte_mask(i / 8)
    }
    bit_mask.asUInt
  }

  io.bus.resp.valid := false.B
  io.bus.req.ready := false.B
  switch(state) {
    is(sIDLE) {
      io.bus.req.ready := true.B
      bus_req := io.bus.req.bits
      when(io.bus.req.fire) {
        state := sWAIT
      }
    }
    is(sWAIT) {
      val bit_mask = byte2bit(bus_req.byte_mask)
      ramhelper.io.wen := bus_req.is_write
      ramhelper.io.wdata := bus_req.data_write
      ramhelper.io.wmask := bit_mask
      ramhelper.io.wIdx := bus_req.addr(30,3)
      ramhelper.io.rIdx := bus_req.addr(30,3)

      bus_resp.data_read := ramhelper.io.rdata
      bus_resp.resp := 0.U
      bus_resp.id := bus_req.id

      state := sEND
    }
    is(sEND) {
      io.bus.resp.valid := true.B
      when(io.bus.resp.fire) {
        state := sIDLE
      }
    }
  }
  io.bus.resp.bits := bus_resp

  when(reset.asBool) {
    bus_resp.init()
    bus_req.init()
  }
}
