import chisel3._
import chisel3.util._

class SobelIO extends Bundle{
  val bus    = Flipped(new CoreBundle())
  val intr_e = Output(new Bool())
}

class Sobel extends Module {
  val io = IO(new SobelIO)

  val update_mtimecmp = WireInit(false.B)
  val mat = RegInit(VecInit(Seq.fill(9)(0.S(32.W))))
  val kernel = RegInit(VecInit(Seq.fill(9)(0.S(32.W))))
  val result   = RegInit(0.S(32.W))
  val end   = RegInit(0.U(32.W))
  val start = RegInit(0.U(32.W))

  io.intr_e:= end =/= 0.U
  val is_start = start =/= 0.U

  val sIDLE::sWAIT::sDELAY::sEND::Nil = Enum(4)
  val delay = RegInit(0.U(16.W))
  val state = RegInit(sIDLE)
  val bus_req = Reg(new CoreReq)
  val bus_resp = Reg(new CoreResp)

  def byte2bit(byte_mask: UInt): UInt = {
    val bit_mask = Wire(Vec(byte_mask.getWidth * 8,Bool()))
    for (i <- 0 until bit_mask.getWidth) {
      bit_mask(i) := byte_mask(i / 8)
    }
    bit_mask.asUInt
  }
  val sel_mat = bus_req.addr(31,8) === "h0100_00".U(32.W)
  val sel_kernel = bus_req.addr(31,8) === "h0100_10".U(32.W)
  val sel_end = bus_req.addr(31,0) === "h0100_2000".U(32.W)
  val sel_start = bus_req.addr(31,0) === "h0100_3000".U(32.W)
  val sel_result = bus_req.addr(31,0) === "h0100_4000".U(32.W)


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
        when(sel_mat){
          mat(bus_req.addr(7,2)) := bus_req.data_write(31,0).asSInt
        }
        when(sel_kernel) {
          kernel(bus_req.addr(7, 2)) := bus_req.data_write(31,0).asSInt
        }
        when(sel_end) {
          end := bus_req.data_write(31,0)
        }
        when(sel_start) {
          start := bus_req.data_write(31,0)
        }
        when(sel_result) {
          result := bus_req.data_write(31,0).asSInt
        }
      }
      when(sel_kernel){
        bus_resp.data_read:=Cat(0.U(32.W),mat(bus_req.addr(7,2)))
      }
      when(sel_mat){
        bus_resp.data_read:=Cat(0.U(32.W),kernel(bus_req.addr(7, 2)))
      }
      when(sel_end) {
        bus_resp.data_read := Cat(0.U(32.W),end)
      }
      when(sel_start) {
        bus_resp.data_read := Cat(0.U(32.W),start)
      }
      when(sel_result) {
        bus_resp.data_read := Cat(0.U(32.W),result)
      }
      bus_resp.resp:=0.U
      bus_resp.id:=bus_req.id

      delay := 0.U
      state:=sDELAY
    }
    is(sDELAY) {
      when(delay === 0.U) {
        state := sEND
      }.otherwise{
        delay := delay - 1.U
      }
    }
    is(sEND){
      io.bus.resp.valid:=true.B
      when(io.bus.resp.fire){
        state:=sIDLE
      }
    }
  }
  io.bus.resp.bits:=bus_resp

  val tmp = WireInit(VecInit(Seq.fill(9)(0.S(32.W))))
  for (i <- 0 until 9) {
    tmp(i) := mat(i) * kernel(i)
  }

  when(is_start){
    result := tmp.reduce(_ + _)
    start := 0.U
    end := 1.U
  }


  when(reset.asBool){
    bus_resp.init()
    bus_req.init()
  }

}
