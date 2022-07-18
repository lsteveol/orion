package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class OrionDemux[T <: Data](
  paInit  : Int = 0, 
  pbInit  : Int = 0,
  pcInit  : Int = 0
)(implicit p: Parameters) extends LazyModule{

  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  val sel_node  = new OrionSinkNode[Bool](Seq(OrionPullPortParameters[Bool](Seq(OrionPullParameters(Bool(), "sel")))))
  
  override lazy val module = new LazyModuleImp(this) {
    require(node.in.size  == 1, s"OrionMux requires one input channel but saw ${node.in.size}")
    require(node.out.size == 2, s"OrionMux requires two output channels but saw ${node.out.size}")
    
    //-------------
    class orion_demux[T <: Data](
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
                
        val inSel_ack = Output(Bool())
        val inSel_req = Input (Bool())
        val inSel_data= Input (Bool())
                
        val outB_ack   = Input (Bool())
        val outB_req   = Output(Bool())
        val outB_data  = Output(gen)
        
        val outC_ack   = Input (Bool())
        val outC_req   = Output(Bool())
        val outC_data  = Output(gen)
      })
      
      addResource("vsrc/orion_demux.v")
    }
    //-------------
    
    val in_ch               = node.in.head._1
    val (out_ch, edgesOut)  = node.out.unzip
    val sel_ch              = sel_node.in.head._1
    
    val odemux = Module(new orion_demux(edgesOut(0).pull.pulls(0).gen, edgesOut(0).pull.pulls(0).gen.getWidth, paInit, pbInit, pcInit))
    
    odemux.io.reset     := reset
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // The Orion Mux Verilog implemenation has B selected when sel==1, and C when sel==0
    
    in_ch.ack           := odemux.io.inA_ack
    odemux.io.inA_req   := in_ch.req
    odemux.io.inA_data  := in_ch.data
    
    
    sel_ch.ack          := odemux.io.inSel_ack
    odemux.io.inSel_req := sel_ch.req
    odemux.io.inSel_data:= sel_ch.data
    
    
    odemux.io.outC_ack  := out_ch(0).ack
    out_ch(0).req       := odemux.io.outC_req
    out_ch(0).data      := odemux.io.outC_data
    
    odemux.io.outB_ack  := out_ch(1).ack
    out_ch(1).req       := odemux.io.outB_req
    out_ch(1).data      := odemux.io.outB_data
    
  }
}


object OrionDemux{
  def apply[T <: Data, SN <: MixedNode[OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool],
                                       OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool]],
                              BN](in: BN, sel : SN, out0: BN, out1 : BN)(implicit p: Parameters, 
     ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
                          OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): Unit = {
    val odemux = LazyModule(new OrionDemux[T])
    
    odemux.node     := in
    odemux.node     := out1
    odemux.sel_node := sel
    
    out0            := odemux.node   
    out1            := odemux.node
  }
}
