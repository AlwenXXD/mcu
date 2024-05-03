import chisel3._
import chisel3.util.experimental._

class CoreArbiter2to1IO extends Bundle{
  val core_master = Vec(2,Flipped(new CoreBundle))
  val core_slave  = Vec(1,new CoreBundle)
}
class CoreArbiter2to1 extends Module{
  val io = IO(new CoreArbiter2to1IO)
  val master_req_sel  = WireInit(0.U)
  val slave_req_sel   = WireInit(0.U)
  val master_resp_sel = WireInit(0.U)
  val slave_resp_sel  = WireInit(0.U)

  io.core_master.foreach(i=>{
    i.req.ready:=false.B
    i.resp.valid:=false.B
    i.resp.bits.init()
  })
  io.core_slave.foreach(i=>{
    i.resp.ready:=false.B
    i.req.valid:=false.B
    i.req.bits.init()
  })

  for(i <- 0 until 2){
    when(io.core_master(i).req.valid){
      master_req_sel := i.U
    }
  }


  when(io.core_slave(slave_resp_sel).resp.bits.id === 1.U){
    master_resp_sel := 1.U
  }.otherwise{
    master_resp_sel := 0.U
  }

  io.core_slave(slave_req_sel).req <> io.core_master(master_req_sel).req
  io.core_slave(slave_resp_sel).resp <> io.core_master(master_resp_sel).resp
}
class CoreArbiter2to2IO extends Bundle{
  val core_master = Vec(2,Flipped(new CoreBundle))
  val core_slave  = Vec(3,new CoreBundle)
}
class CoreArbiter2to3 extends Module{
  val io = IO(new CoreArbiter2to2IO)
  val master_req_sel  = WireInit(0.U)
  val slave_resp_sel  = WireInit(0.U)

  io.core_master.foreach(i=>{
    i.req.ready:=false.B
    i.resp.valid:=false.B
    i.resp.bits.init()
  })
  io.core_slave.foreach(i=>{
    i.resp.ready:=false.B
    i.req.valid:=false.B
    i.req.bits.init()
  })

  for(i <- 0 until 2){
    when(io.core_master(i).req.valid){
      master_req_sel := i.U
    }
  }

  for (i <- 0 until 3) {
    when(io.core_slave(i).resp.valid) {
      slave_resp_sel := i.U
    }
  }

  when(io.core_master(master_req_sel).req.bits.addr === AddrMap.CLINT){
    io.core_master(master_req_sel).req <> io.core_slave(1).req
  }.elsewhen(io.core_master(master_req_sel).req.bits.addr === AddrMap.SOBEL) {
    io.core_master(master_req_sel).req <> io.core_slave(2).req
  }.otherwise{
    io.core_master(master_req_sel).req <> io.core_slave(0).req
  }

  when(io.core_slave(slave_resp_sel).resp.bits.id === 1.U) {
    io.core_slave(slave_resp_sel).resp <> io.core_master(1).resp
  }.otherwise {
    io.core_slave(slave_resp_sel).resp <> io.core_master(0).resp
  }

}

