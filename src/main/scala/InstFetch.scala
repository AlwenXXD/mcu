import chisel3._
import chisel3.util._

class RedirectInfo extends Bundle{
  val addr = UInt(32.W)
}
class BjuRedirectInfo extends RedirectInfo {
  val is_taken = Bool()
}

class InstFetchIO extends Bundle{
  val icache_req = Decoupled(new IcacheReq)
  val bju_redirect = Flipped(Valid(new BjuRedirectInfo()))
  val except_redirect = Flipped(Valid(new RedirectInfo()))
}

class InstFetch extends Module {
  val io = IO(new InstFetchIO())
  val pc_en = RegInit(false.B)
  pc_en := true.B
  val pc = RegInit("h80000000".U(32.W))

  val is_except = io.except_redirect.valid
  val is_bju = io.bju_redirect.valid && io.bju_redirect.bits.is_taken
  val need_redirect = is_bju || is_except

  when(is_except){
    pc := io.except_redirect.bits.addr
  }.elsewhen(is_bju){
    pc := io.bju_redirect.bits.addr
  }.elsewhen(io.icache_req.fire) {
    pc := pc + 4.U
  }

  val pc_valid = !need_redirect && pc_en
  io.icache_req.valid := pc_valid
  io.icache_req.bits.addr := pc
}
