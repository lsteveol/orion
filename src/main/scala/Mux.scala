package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class OrionMux[T <: Data](
  gen     : T,
  paInit  : Int = 0, 
  pbInit  : Int = 0,
  pcInit  : Int = 0,
  pselInit: Int = 0
)(implicit p: Parameters) extends LazyModule{

  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  val sel  = new OrionSinkNode[Bool](Seq(OrionPullPortParameters[Bool](Seq(OrionPullParameters(Bool(), "sel")))))
  
  override lazy val module = new LazyModuleImp(this) {
    require(node.in.size  == 2, s"OrionMux requires two input channels but saw ${node.in.size}")
    require(node.out.size == 1, s"OrionMux requires one output channel but saw ${node.out.size}")
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // The Orion Mux Verilog implemenation actually has A active when sel==1, and 
    // B active when sel==0, so I'm swapping these here 
    val (in_ch, edgesIn)    = node.in.unzip
    val out_ch              = node.out.head._1
    val sel_ch              = sel.in.head._1
    
    val click_req       = Wire(Bool())
    val click_ack       = Wire(Bool())
    
    val pa              = Wire(Bool())
    val pb              = Wire(Bool())
    val pc              = Wire(Bool())
    val psel            = Wire(Bool())
    
    val inA_token       = OrionDelay((pa   ^ in_ch(1).req), 10)
    val inB_token       = OrionDelay((pb   ^ in_ch(0).req), 10)
    val inSel_token     = OrionDelay((psel ^ sel_ch.req),   10)
    
    click_req           := OrionDelay(((inA_token & inSel_token & sel_ch.data) |
                                       (inB_token & inSel_token & ~sel_ch.data)), 10)
    click_ack           := OrionDelay(~(pc ^ out_ch.ack), 10)
    
    
    out_ch.data         := Mux(sel_ch.data, in_ch(1).data, in_ch(0).data) //Add delay?
    out_ch.req          := pc
    
    sel_ch.ack          := psel
    
    val pc_pre          = withClockAndReset(click_req.asClock, reset.asAsyncReset){RegNext(~pc, init=pcInit.U)}
    
    val pa_pre          = withClockAndReset(click_ack.asClock, reset.asAsyncReset){RegNext((pa ^  sel_ch.data), init=paInit.U)}
    val pb_pre          = withClockAndReset(click_ack.asClock, reset.asAsyncReset){RegNext((pb ^ ~sel_ch.data), init=pbInit.U)}
    val psel_pre        = withClockAndReset(click_ack.asClock, reset.asAsyncReset){RegNext(sel_ch.req,          init=pselInit.U)}
    
    pa                  := OrionDelay(pa_pre,   10)
    pb                  := OrionDelay(pb_pre,   10)
    pc                  := OrionDelay(pc_pre,   10)
    psel                := OrionDelay(psel_pre, 10)
    
    in_ch(1).ack        := pa
    in_ch(0).ack        := pb

    
  }
  
}

/**
` *   See "Type Constraints" in Scala for the Impatient
  */
object OrionMux {
  def apply[T <: Data, SN <: MixedNode[OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool],
                                       OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool]],
                              BN](gen: T)(sel : SN, b: BN, a : BN)(implicit p: Parameters, 
     ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
                          OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): OrionNexusNode[T] = {
    val omux = LazyModule(new OrionMux(gen))
    
    omux.node := a
    omux.node := b
    omux.sel  := sel
    
    omux.node
  }
}

