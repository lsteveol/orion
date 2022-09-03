package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class Demux[T <: Data](
  gen     : T,
  paInit  : Int = 0, 
  pbInit  : Int = 0,
  pcInit  : Int = 0
)(implicit p: Parameters) extends LazyModule{

  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  val sel  = new OrionSinkNode[Bool](Seq(OrionPullPortParameters[Bool](Seq(OrionPullParameters(Bool(), "sel")))))
  
  override lazy val module = new LazyModuleImp(this) {
    require(node.in.size  == 1, s"OrionDemux requires one input channel but saw ${node.in.size}")
    require(node.out.size == 2, s"OrionDemux requires two output channels but saw ${node.out.size}")
    
    val in_ch               = node.in.head._1
    val (out_ch, edgesOut)  = node.out.unzip
    val sel_ch              = sel.in.head._1
    
    val click_req     = Wire(Bool())
    val click_ack     = Wire(Bool())
    val pa            = Wire(Bool())
    val pb            = Wire(Bool())
    val pc            = Wire(Bool())
    
    sel_ch.ack        := pa
    in_ch.ack         := pa
    
    out_ch(1).req     := pb
    out_ch(1).data    := in_ch.data
    
    out_ch(0).req     := pc
    out_ch(0).data    := in_ch.data
    
    click_req         := OrionDelay(((sel_ch.req & ~pa & in_ch.req) | (~sel_ch.req & pa & ~in_ch.req)), 10)
    click_ack         := OrionDelay((~(out_ch(1).ack ^ pb) & ~(out_ch(0).ack ^ pc)), 10)
    
    val pb_pre        = withClockAndReset(click_req.asClock, reset.asAsyncReset){RegNext((pb ^  sel_ch.data), init=pbInit.U)}
    val pc_pre        = withClockAndReset(click_req.asClock, reset.asAsyncReset){RegNext((pc ^ ~sel_ch.data), init=pcInit.U)}
    
    val pa_pre        = withClockAndReset(click_ack.asClock, reset.asAsyncReset){RegNext(~pa, init=paInit.U)}
    
    pa                := OrionDelay(pa_pre, 10)
    pb                := OrionDelay(pb_pre, 10)
    pc                := OrionDelay(pc_pre, 10)

  }
}


object Demux{
  def apply[T <: Data, SN <: MixedNode[OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool],
                                       OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool]],
                              BN](gen: T)(in: BN, sel : SN, out0: BN, out1 : BN)(implicit p: Parameters, 
     ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
                          OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): Unit = {
    val odemux = LazyModule(new Demux(gen))
    
    odemux.node     := in
    odemux.sel      := sel
    
    out0            := odemux.node   
    out1            := odemux.node
  }
}
