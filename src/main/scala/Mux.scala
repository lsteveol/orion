package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class OrionMux[T <: Data](
  //gen     : T,
  paInit  : Int = 0, 
  pbInit  : Int = 0,
  pcInit  : Int = 0,
  pselInit: Int = 0
)(implicit p: Parameters) extends LazyModule{

  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  val sel_node  = new OrionSinkNode[Bool](Seq(OrionPullPortParameters[Bool](Seq(OrionPullParameters(Bool(), "sel")))))
  
  override lazy val module = new LazyModuleImp(this) {
    require(node.in.size  == 2, s"OrionMux requires two input channels but saw ${node.in.size}")
    require(node.out.size == 1, s"OrionMux requires one output channel but saw ${node.out.size}")
    
    //-------------
    class orion_mux[T <: Data](
      gen       : T,
      width     : Int,
      paInit    : Int,
      pbInit    : Int,
      pcInit    : Int,
      pselInit  : Int
    ) extends BlackBox(Map(
      "WIDTH"     -> width,
      "PA_INIT"   -> paInit,
      "PB_INIT"   -> pbInit,
      "PC_INIT"   -> pcInit,
      "PSEL_INIT" -> pselInit
    )) with HasBlackBoxResource {
      val io = IO(new Bundle{
        val reset     = Input (Reset())
        val inA_ack   = Output(Bool())
        val inA_req   = Input (Bool())
        val inA_data  = Input (gen)
        
        val inB_ack   = Output(Bool())
        val inB_req   = Input (Bool())
        val inB_data  = Input (gen)
        
        val inSel_ack = Output(Bool())
        val inSel_req = Input (Bool())
        val inSel_data= Input (Bool())
                
        val out_ack   = Input (Bool())
        val out_req   = Output(Bool())
        val out_data  = Output(gen)
      })
      
      addResource("vsrc/orion_mux.v")
    }
    //-------------
    
    
    val (in_ch, edgesIn)    = node.in.unzip
    val out_ch              = node.out.head._1
    val sel_ch              = sel_node.in.head._1
    
    val omux = Module(new orion_mux(edgesIn(0).push.pushes(0).gen, edgesIn(0).push.pushes(0).gen.getWidth, paInit, pbInit, pcInit, pselInit))
    
    omux.io.reset     := reset
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // The Orion Mux Verilog implemenation actually has A active when sel==1, and 
    // B active when sel==0, so I'm swapping these here
    
    in_ch(0).ack      := omux.io.inB_ack
    omux.io.inB_req   := in_ch(0).req
    omux.io.inB_data  := in_ch(0).data
    
    in_ch(1).ack      := omux.io.inA_ack
    omux.io.inA_req   := in_ch(1).req
    omux.io.inA_data  := in_ch(1).data
    
    sel_ch.ack        := omux.io.inSel_ack
    omux.io.inSel_req := sel_ch.req
    omux.io.inSel_data:= sel_ch.data
    
    
    omux.io.out_ack   := out_ch.ack
    out_ch.req        := omux.io.out_req
    out_ch.data       := omux.io.out_data
    
  }
  
}

/**
` *   See "Type Constraints" in Scala for the Impatient
  */
object OrionMux {
  def apply[T <: Data, SN <: MixedNode[OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool],
                                       OrionPushPortParameters[Bool], OrionPullPortParameters[Bool], OrionEdgeParameters[Bool], OrionBundle[Bool]],
                              BN](sel : SN, b: BN, a : BN)(implicit p: Parameters, 
     ev: BN <:< MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
                          OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]): OrionNexusNode[T] = {
    val omux = LazyModule(new OrionMux[T])
    
    omux.node     := a
    omux.node     := b
    omux.sel_node := sel
    
    omux.node
  }
}

// object OrionMux {
//   def apply[T <: Data, 
//             BN <: MixedNode[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T],
//                             OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionBundle[T]]]
//     (sel : BN, b: BN, a : BN)(implicit p: Parameters): OrionNexusNode[T] = {
//     val omux = LazyModule(new OrionMux[T])
//     
//     omux.node := a
//     omux.node := b
//     omux.node := sel
//     
//     omux.node
//   }
// }
