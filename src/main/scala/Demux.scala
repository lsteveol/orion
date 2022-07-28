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
    
//     //-------------
//     class orion_demux[T <: Data](
//       gen       : T,
//       width     : Int,
//       paInit    : Int,
//       pbInit    : Int,
//       pcInit    : Int
//     ) extends BlackBox(Map(
//       "WIDTH"     -> width,
//       "PA_INIT"   -> paInit,
//       "PB_INIT"   -> pbInit,
//       "PC_INIT"   -> pcInit
//     )) with HasBlackBoxResource {
//       val io = IO(new Bundle{
//         val reset     = Input (Reset())
//         val inA_ack   = Output(Bool())
//         val inA_req   = Input (Bool())
//         val inA_data  = Input (gen)
//                 
//         val inSel_ack = Output(Bool())
//         val inSel_req = Input (Bool())
//         val inSel_data= Input (Bool())
//                 
//         val outB_ack   = Input (Bool())
//         val outB_req   = Output(Bool())
//         val outB_data  = Output(gen)
//         
//         val outC_ack   = Input (Bool())
//         val outC_req   = Output(Bool())
//         val outC_data  = Output(gen)
//       })
//       
//       addResource("vsrc/orion_demux.v")
//     }
//     //-------------
    
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
    
    
//     //OLD
//     val odemux = Module(new orion_demux(edgesOut(0).pull.pulls(0).gen, edgesOut(0).pull.pulls(0).gen.getWidth, paInit, pbInit, pcInit))
//     
//     odemux.io.reset     := reset
//     
//     //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
//     // The Orion Mux Verilog implemenation has B selected when sel==1, and C when sel==0
//     
//     in_ch.ack           := odemux.io.inA_ack
//     odemux.io.inA_req   := in_ch.req
//     odemux.io.inA_data  := in_ch.data
//     
//     
//     sel_ch.ack          := odemux.io.inSel_ack
//     odemux.io.inSel_req := sel_ch.req
//     odemux.io.inSel_data:= sel_ch.data
//     
//     
//     odemux.io.outC_ack  := out_ch(0).ack
//     out_ch(0).req       := odemux.io.outC_req
//     out_ch(0).data      := odemux.io.outC_data
//     
//     odemux.io.outB_ack  := out_ch(1).ack
//     out_ch(1).req       := odemux.io.outB_req
//     out_ch(1).data      := odemux.io.outB_data
    
  }
}


// object Demux{
//   def apply[T <: Data, SN <: MixedNode[OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool],
//                                        OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool]],
//                               BN](in: BN, sel : SN, out0: BN, out1 : BN)(implicit p: Parameters, 
//      ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
//                           OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): Unit = {
//     val odemux = LazyModule(new Demux[T])
//     
//     odemux.node     := in
//     odemux.node     := out1
//     odemux.sel := sel
//     
//     out0            := odemux.node   
//     out1            := odemux.node
//   }
// }
