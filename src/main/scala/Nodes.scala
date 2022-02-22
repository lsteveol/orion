package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._



class MyBundle extends Bundle{
  val sig1 = Bool()
  val sig2 = UInt(3.W)
}


/**
  *
  */
class MySource[T <: Data](gen : T)(implicit p: Parameters) extends LazyModule{
  
  val node = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "source")))))
  
  override lazy val module = new LazyModuleImp(this) {
    val channel = node.out.head._1
    
    dontTouch(channel)
  }
}


class MySink[T <: Data](gen : T)(implicit p: Parameters) extends LazyModule{
  
  val node = new OrionSinkNode[T](Seq(OrionPullPortParameters[T](Seq(OrionPullParameters(gen, "sink")))))
  
  override lazy val module = new LazyModuleImp(this) {
    val channel = node.in.head._1
    
    dontTouch(channel)
    
  }
}

// class MyTop()(implicit p: Parameters) extends LazyModule{
//   
//   val src1  = LazyModule(new MySource(UInt(4.W)))
//   val src2  = LazyModule(new MySource(UInt(4.W)))
//   val sink  = LazyModule(new MySink(UInt(8.W)))
// 
//   
//   val j     = LazyModule(new JoinUInt(UInt(4.W), UInt(4.W), UInt(8.W)))
//   
//   //sink.node := DecoupReg(new MyBundle) := src.node
//   
//   j.a := src1.node
//   j.b := src2.node
//   sink.node := j.o
//   
//   
//   override lazy val module = new LazyModuleImp(this) {
//   
//   }
// }
// 
// 
// object Test extends App {  
//   
//   implicit val p: Parameters = Parameters.empty
//   
//   val verilog = (new ChiselStage).emitVerilog(
//     LazyModule(new MyTop()(p)).module,
// 
//     //args
//     Array("--target-dir", "output")
//   )
//   
// }

