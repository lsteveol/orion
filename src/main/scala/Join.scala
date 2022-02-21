package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._




/**
  *   Join
  *   
  *   This is arguably the most difficult component to implement.
  */
class Join[A <: Data, B <: Data, O <: Data](atype : A, btype : B, otype : O, pInit: Int = 0)(implicit p: Parameters) extends LazyModule{
  
  val a = new OrionSinkNode[A]  (Seq(OrionPullPortParameters[A](Seq(OrionPullParameters(atype, "a")))))
  val b = new OrionSinkNode[B]  (Seq(OrionPullPortParameters[B](Seq(OrionPullParameters(btype, "b")))))
  val o = new OrionSourceNode[O](Seq(OrionPushPortParameters[O](Seq(OrionPushParameters(otype, "o")))))
  
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
    
    val ach = a.in.head._1
    val bch = b.in.head._1
    val och = o.out.head._1
    
    
    val ojoin = Module(new orion_join(pInit))
    ojoin.io.reset    := wrapper.reset
    ach.ack           := ojoin.io.inA_ack
    ojoin.io.inA_req  := ach.req
    bch.ack           := ojoin.io.inB_ack
    ojoin.io.inB_req  := bch.req
    
    ojoin.io.outC_ack := och.ack
    och.req           := ojoin.io.outC_req
  }
  
  override lazy val module = new LazyModuleImp(this) {
    joinBase(this)  
    
  }
}




/**
  *   A Join wrapper for UInt which simply concatenates 
  */
class JoinUInt[A <: UInt, B <: UInt, O <: UInt](atype : A, btype : B, otype : O, pInit: Int = 0)(implicit p: Parameters) extends Join[A,B,O](atype, btype, otype, pInit){
  
  override lazy val module = new LazyModuleImp(this) {
    joinBase(this)
    
    val ach = a.in.head._1
    val bch = b.in.head._1
    val och = o.out.head._1
    
    och.data := Cat(ach.data, bch.data)
    
  }
}
