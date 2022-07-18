package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._



class OrionMerge[T <: Data](
  paInit  : Int = 0, 
  pbInit  : Int = 0,
  pcInit  : Int = 0
)(implicit p: Parameters) extends LazyModule{

  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
    
  override lazy val module = new LazyModuleImp(this) {
    require(node.in.size  == 2, s"OrionMux requires two input channels but saw ${node.in.size}")
    require(node.out.size == 1, s"OrionMux requires one output channel but saw ${node.out.size}")
    
    //-------------
    class orion_merge[T <: Data](
      gen       : T,
      width     : Int,
      paInit    : Int,
      pbInit    : Int,
      pcInit    : Int
    ) extends BlackBox(Map(
      "WIDTH"     -> width,
      "PA_INIT"   -> paInit,
      "PB_INIT"   -> pbInit,
      "PC_INIT"   -> pcInit
    )) with HasBlackBoxResource {
      val io = IO(new Bundle{
        val reset     = Input (Reset())
        val inA_ack   = Output(Bool())
        val inA_req   = Input (Bool())
        val inA_data  = Input (gen)
        
        val inB_ack   = Output(Bool())
        val inB_req   = Input (Bool())
        val inB_data  = Input (gen)
                        
        val out_ack   = Input (Bool())
        val out_req   = Output(Bool())
        val out_data  = Output(gen)
      })
      
      addResource("vsrc/orion_merge.v")
    }
    //-------------
    
    val (in_ch, edgesIn)    = node.in.unzip
    val out_ch              = node.out.head._1
    
    val omerge = Module(new orion_merge(edgesIn(0).push.pushes(0).gen, edgesIn(0).push.pushes(0).gen.getWidth, paInit, pbInit, pcInit))
    
    omerge.io.reset     := reset
    
    
    in_ch(0).ack        := omerge.io.inA_ack
    omerge.io.inA_req   := in_ch(0).req
    omerge.io.inA_data  := in_ch(0).data
    
    in_ch(1).ack        := omerge.io.inB_ack
    omerge.io.inB_req   := in_ch(1).req
    omerge.io.inB_data  := in_ch(1).data
    
    omerge.io.out_ack   := out_ch.ack
    out_ch.req          := omerge.io.out_req
    out_ch.data         := omerge.io.out_data
    
  }
}


object OrionMerge{
  def apply[T <: Data, BN](in0: BN, in1: BN)(implicit p: Parameters, 
     ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
                          OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): OrionNexusNode[T] = {
    val omerge = LazyModule(new OrionMerge[T])
    omerge.node := in0
    omerge.node := in1
    omerge.node
  }
                        
}
