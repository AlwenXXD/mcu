import chisel3._
import chisel3.util._
import chisel3.util.experimental._


class CoreReq extends Bundle{
  val id         = UInt(2.W)
  val is_write   = Bool()
  val data_write = UInt(64.W)
  val byte_mask  = UInt(8.W)
  val addr       = UInt(32.W)
  val size       = UInt(3.W)
  def init(): Unit ={
    id   := 0.U
    is_write   := false.B
    data_write := 0.U
    byte_mask  := 0.U
    addr       := 0.U
    size       := 0.U
  }
}
class CoreResp extends Bundle{
  val id   = UInt(4.W)
  val data_read  = UInt(64.W)
  val resp        = UInt(2.W)
  def init(): Unit ={
    id            := 0.U
    data_read       := 0.U
    resp            := 0.U
  }
}

class CoreBundle extends Bundle{
  val req      = Decoupled(new CoreReq)
  val resp      = Flipped(Decoupled(new CoreResp))
}


