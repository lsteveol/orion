package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._




/**
  *   Join
  *   
  *   
  */
class Join[I <: Data, O <: Data](itype : I, otype : O, pInit: Int = 0)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionMixedNexusNode[I,O](
    sourceFn  = { seq => 
      OrionPushPortParameters[O](Seq(OrionPushParameters(otype, "joinOut")))
    },
    sinkFn    = { seq => 
      OrionPullPortParameters[I](Seq(OrionPullParameters(itype, "joinIn")))
    },
  )
  
  lazy val (in_ch, edgesIn)    = node.in.unzip
  lazy val (out_ch, edgesOut)  = node.out.unzip
  
  lazy val ina = in_ch(0)
  lazy val inb = in_ch(1)
  lazy val out = out_ch(0)
  
  // MUST be called for your implementation
  def joinBase(wrapper: LazyModuleImp): Unit = {
    //-------------
    class orion_join(
      pInit    : Int
    ) extends BlackBox(Map(
      "P_INIT" -> pInit
    )) with HasBlackBoxResource {
      val io = IO(new Bundle{
        val reset     = Input (Reset())
        val inA_ack   = Output(Bool())
        val inA_req   = Input (Bool())
        val inB_ack   = Output(Bool())
        val inB_req   = Input (Bool())
        
        val outC_ack  = Input (Bool())
        val outC_req  = Output(Bool())
      })
      
      addResource("vsrc/orion_join.v")
    }
    //-------------
    
    require(node.in.size  == 2, s"Join requires two input channels but saw ${node.in.size}")
    require(node.out.size == 1, s"Join requires one output channel but saw ${node.out.size}")
        
    val ojoin = Module(new orion_join(pInit))
    ojoin.io.reset    := wrapper.reset
    ina.ack           := ojoin.io.inA_ack
    ojoin.io.inA_req  := ina.req
    inb.ack           := ojoin.io.inB_ack
    ojoin.io.inB_req  := inb.req
    
    ojoin.io.outC_ack := out.ack
    out.req           := ojoin.io.outC_req
    
    
  }
  
  override lazy val module = new LazyModuleImp(this) {
    joinBase(this)  
    
  }
}




// /**
//   *   Join Mixed
//   *   Similar to Join with the exception that the two input channels can be different
//   */
// class JoinMixed[A <: Data, B <: Data, O <: Data](atype : A, btype : B, otype : O, pInit: Int = 0)(implicit p: Parameters) extends LazyModule{
//   
//   val a = new OrionSinkNode[A]  (Seq(OrionPullPortParameters[A](Seq(OrionPullParameters(atype, "a")))))
//   val b = new OrionSinkNode[B]  (Seq(OrionPullPortParameters[B](Seq(OrionPullParameters(btype, "b")))))
//   val o = new OrionSourceNode[O](Seq(OrionPushPortParameters[O](Seq(OrionPushParameters(otype, "o")))))
//   
//   // MUST be called for your implementation
//   def joinBase(wrapper: LazyModuleImp): Unit = {
//     //-------------
//     class orion_join(
//       pInit    : Int
//     ) extends BlackBox(Map(
//       "P_INIT" -> pInit
//     )) with HasBlackBoxResource {
//       val io = IO(new Bundle{
//         val reset     = Input (Reset())
//         val inA_ack   = Output(Bool())
//         val inA_req   = Input (Bool())
//         val inB_ack   = Output(Bool())
//         val inB_req   = Input (Bool())
//         
//         val outC_ack  = Input (Bool())
//         val outC_req  = Output(Bool())
//       })
//       
//       addResource("vsrc/orion_join.v")
//     }
//     //-------------
//     
//     val ach = a.in.head._1
//     val bch = b.in.head._1
//     val och = o.out.head._1
//     
//     
//     val ojoin = Module(new orion_join(pInit))
//     ojoin.io.reset    := wrapper.reset
//     ach.ack           := ojoin.io.inA_ack
//     ojoin.io.inA_req  := ach.req
//     bch.ack           := ojoin.io.inB_ack
//     ojoin.io.inB_req  := bch.req
//     
//     ojoin.io.outC_ack := och.ack
//     och.req           := ojoin.io.outC_req
//   }
//   
//   override lazy val module = new LazyModuleImp(this) {
//     joinBase(this)  
//     
//   }
// }


