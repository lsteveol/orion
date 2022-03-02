package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class RegFork[T <: Data](
  gen     : T, 
  dataInit: Int = 0, 
  paInit  : Int = 0, 
  pbInit  : Int = 1,
  pcInit  : Int = 1
)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  override lazy val module = new LazyModuleImp(this) {
    
    require(node.in.size  == 1, s"RegFork requires one input channel but saw ${node.in.size}")
    require(node.out.size == 2, s"RegFork requires two output channels but saw ${node.out.size}")
    
    //-------------
    class orion_reg_fork[T <: Data](
      gen       : T,
      width     : Int,
      dataInit  : Int,
      paInit    : Int,
      pbInit    : Int,
      pcInit    : Int
    ) extends BlackBox(Map(
      "WIDTH"   -> width,
      "INIT"    -> dataInit,
      "PA_INIT" -> paInit,
      "PB_INIT" -> pbInit,
      "PC_INIT" -> pcInit
    )) with HasBlackBoxResource {
      val io = IO(new Bundle{
        val reset     = Input (Reset())
        val inA_ack   = Output(Bool())
        val inA_req   = Input (Bool())
        val inA_data  = Input (gen)
        
        val outB_ack  = Input (Bool())
        val outB_req  = Output(Bool())
        val outB_data = Output(gen)
        
        val outC_ack  = Input (Bool())
        val outC_req  = Output(Bool())
        val outC_data = Output(gen)
      })
      
      addResource("vsrc/orion_reg_fork.v")
    }
    //-------------
    
    val in_ch  = node.in.head._1
    val (out_ch, edgesOut)   = node.out.unzip
    
    val reg_fork = Module(new orion_reg_fork(gen, gen.getWidth, dataInit, paInit, pbInit, pcInit))
    
    reg_fork.io.reset     := reset
    in_ch.ack             := reg_fork.io.inA_ack
    reg_fork.io.inA_req   := in_ch.req
    reg_fork.io.inA_data  := in_ch.data
    
    reg_fork.io.outB_ack  := out_ch(0).ack
    out_ch(0).req         := reg_fork.io.outB_req
    out_ch(0).data        := reg_fork.io.outB_data
    
    reg_fork.io.outC_ack  := out_ch(1).ack
    out_ch(1).req         := reg_fork.io.outC_req
    out_ch(1).data        := reg_fork.io.outC_data
    
  }
}
