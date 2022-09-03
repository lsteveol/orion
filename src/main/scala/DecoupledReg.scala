package orion

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._



/**
  *   Decoupled HS Reg
  *   Input and output data types remain the same
  */
class DecoupReg[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters) extends LazyModule{
  
  //val in  = new OrionSinkNode[T]  (Seq(OrionPullPortParameters[T](Seq(OrionPullParameters(gen, "drIn",  Some(this))))))
  //val out = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "drOut", Some(this))))))
  
  val in  = new OrionSinkNode  (Seq(OrionPullPortParameters(Seq(OrionPullParameters(gen, "drIn",  Some(this))))))
  val out = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(gen, "drOut", Some(this))))))
    
  override lazy val module = new LazyModuleImp(this) {
    
    val in_ch  = in.in.head._1
    val out_ch = out.out.head._1
    
    //println(in.in.head._1.getClass)
    //println(in.in.head._2)
    //println(OrionUtils.getPath(in.in.head._2.push.pushes.inst.get))
    //println(OrionUtils.getPath(wrapper))
    //println(OrionUtils.getPath(in.in.head._2.push.pushes(0).inst))
    //println(OrionUtils.getPath(out.out.head._2.pull.pulls(0).inst))
    
    val pi            = Wire(Bool())
    val po            = Wire(Bool())
    
    val click         = OrionDelay(((in_ch.req ^ pi) & ~(out_ch.ack ^ po)), 10)
    
    val pi_pre        = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~pi, init=piInit.U)}
    val po_pre        = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~po, init=poInit.U)}
    
    // Getting and error that data_pre Width could not be determined? 
    // This was a workaround but would like to look into this
    //val data_pre_in   = Wire(gen)
    //data_pre_in       := in_ch.data
    //val data_pre      = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(data_pre_in/*in_ch.data*/, init=dataInit.U)}
    
    val data_pre_dly  = Wire(gen)
    withClockAndReset(click.asClock, reset.asAsyncReset){
      val data_reg = RegInit(gen, init=dataInit.U)
      data_reg     := in_ch.data
      data_pre_dly := data_reg
    }
    
    pi                := OrionDelay(pi_pre, 10)
    po                := OrionDelay(po_pre, 10)
    //out_ch.data       := OrionDelay(data_pre, 10)
    out_ch.data       := OrionDelay(data_pre_dly, 10)
    
    out_ch.req        := po
    in_ch.ack         := pi
    
    
  }
}


// 
// object DecoupReg{
//   def apply[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters): OrionAdapterNode[T] = {
//     val decoupReg = LazyModule(new DecoupReg(gen, dataInit, piInit, poInit)(p))
//     decoupReg.node
//   }
// }
