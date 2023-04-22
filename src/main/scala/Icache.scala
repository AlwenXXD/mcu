import chisel3._
import chisel3.util._
import chisel3.util.experimental._

class IcacheReq extends Bundle{
  val addr = UInt(32.W)
}

class IcacheResp extends Bundle{
  val inst = UInt(32.W)
  val addr = UInt(32.W)
}

class ICacheIO extends Bundle {
  val icache_req   = Flipped(Decoupled(new IcacheReq))
  val icache_resp = Decoupled(new IcacheResp)
  val core_rw = new CoreBundle
  val flush = Input(Bool())
}

class ICache extends Module {
  val io  = IO(new ICacheIO)

  io.icache_resp.bits.addr:=0.U
  io.icache_resp.bits.inst:=0.U

  io.core_rw.req.bits.data_write := 0.U
  io.core_rw.req.bits.is_write := false.B
  io.core_rw.req.bits.byte_mask := 0.U
  io.core_rw.req.bits.id := 0.U
  io.core_rw.req.bits.addr := 0.U
  io.core_rw.req.bits.size := 3.U(3.W)

  val s1_pc = RegInit(0.U(32.W))
  val s1_inst = RegInit(0.U(32.W))
  val s0::s1::s2::s3::Nil = Enum(4)
  val m0 :: m1 :: m2 :: m3 :: m4 :: Nil = Enum(5)
  val state = RegInit(s0)
  val miss = RegInit(m0)

  val cache_valid = RegInit(VecInit(Seq.fill(64)(false.B)))
  val cache_tag = SyncReadMem(64, UInt(22.W))
  val cache_data = Seq.fill(4)(SyncReadMem(64, UInt(32.W)))
  val cache_en = WireInit(true.B)
  val cache_we = WireInit(false.B)
  val cache_valid_we= WireInit(false.B)
  val cache_read_tag = WireInit(0.U(22.W))
  val cache_read_data = WireInit(VecInit(Seq.fill(4)(0.U(32.W))))
  val cache_write_addr = WireInit(0.U(32.W))
  val cache_write_tag = WireInit( 0.U(22.W))
  val cache_write_data = WireInit(VecInit(Seq.fill(4)(0.U(32.W))))
  val hit_cache = cache_read_tag===s1_pc(31,10)&&cache_valid(s1_pc(9,4))

  val had_flush = RegInit(false.B)

  io.core_rw.req.valid:=false.B
  io.core_rw.resp.ready:=false.B
  io.icache_req.ready:=false.B
  io.icache_resp.valid:=false.B


  switch(state){
    is(s0){
      when(io.icache_resp.ready){
        io.icache_req.ready := true.B
        when(io.icache_req.valid){
          state := s1
          s1_pc := io.icache_req.bits.addr
        }
      }
    }
    is(s1){
      s1_inst := cache_read_data(s1_pc(3,2))
      io.icache_resp.bits.inst := cache_read_data(s1_pc(3,2))
      io.icache_resp.bits.addr := s1_pc

      when(hit_cache){
        io.icache_resp.valid := true.B
        when(io.icache_resp.ready) {
          io.icache_req.ready := true.B
          when(io.icache_req.valid) {
            state := s1
            s1_pc := io.icache_req.bits.addr
          }.otherwise {
            state := s0
          }
        }.otherwise {
          state := s2
        }
      }.otherwise{
        io.icache_resp.valid := false.B
        io.icache_req.ready := false.B
        miss := m1
        state := s3
      }

    }
    is(s2) {
      io.icache_resp.bits.inst := s1_inst
      io.icache_resp.bits.addr := s1_pc
      io.icache_resp.valid := true.B
      when(io.icache_resp.ready) {
        io.icache_req.ready := true.B
        when(io.icache_req.valid){
          state := s1
          s1_pc := io.icache_req.bits.addr
        }.otherwise{
          state := s0
        }
      }.otherwise{
        state := s2
      }
    }
    is(s3){}
  }

  val m1_data = RegInit(VecInit(Seq.fill(2)(0.U(32.W))))
  switch(miss){
    is(m0){}
    is(m1){
      io.core_rw.req.valid := true.B
      io.core_rw.req.bits.id := 0.U
      io.core_rw.req.bits.addr := Cat(s1_pc(31,4),0.U(1.W),0.U(3.W))
      io.core_rw.req.bits.size := 3.U(3.W)
      when(io.core_rw.req.fire){
        miss := m2
      }
    }
    is(m2) {
      io.core_rw.resp.ready := true.B
      when(io.core_rw.resp.fire) {
        val data_read = io.core_rw.resp.bits.data_read.asTypeOf(Vec(2,UInt(32.W)))
        m1_data := data_read
        miss := m3
      }
    }
    is(m3) {
      io.core_rw.req.valid := true.B
      io.core_rw.req.bits.id := 0.U
      io.core_rw.req.bits.addr := Cat(s1_pc(31,4),1.U(1.W),0.U(3.W))
      io.core_rw.req.bits.size := 3.U(3.W)
      when(io.core_rw.req.fire) {
        miss := m4
      }
    }
    is(m4){
      io.core_rw.resp.ready := true.B
      when(io.core_rw.resp.fire){
        cache_valid_we := true.B
        cache_we := true.B
        cache_write_addr := s1_pc
        cache_write_tag := s1_pc(31,10)
        val data_read = io.core_rw.resp.bits.data_read.asTypeOf(Vec(2,UInt(32.W)))
        cache_write_data := m1_data ++ data_read
        miss := m0
        state := s2
        s1_inst := cache_write_data(s1_pc(3,2))
        when(had_flush){
          state := s0
          had_flush := false.B
        }
      }
    }
  }




  when(cache_en){
    when(cache_we){
      cache_tag.write(cache_write_addr(9,4),cache_write_tag)
      cache_data.zip(cache_write_data).foreach{case(cache_word,write_word)=>cache_word.write(cache_write_addr(9,4),write_word)}
      cache_read_tag := DontCare
      cache_read_data := DontCare
    }.otherwise{
      cache_read_tag := cache_tag.read(io.icache_req.bits.addr(9,4))
      cache_read_data := VecInit(cache_data.map(i=>i.read(io.icache_req.bits.addr(9,4))))
    }
  }


  when(cache_valid_we){
    cache_valid(cache_write_addr(9,4)):=true.B
  }

  when(io.flush){
    io.icache_resp.valid := false.B
    io.icache_req.ready := false.B
    when(miss === m0 || miss === m4){
      state := s0
      miss := m0
    }.otherwise{
      had_flush := true.B
    }

  }


}
