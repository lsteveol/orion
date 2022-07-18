package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class MyBund extends Bundle{
  val a = UInt(8.W)
  val b = Bool()
}

class GCD(gen : MyBund)(implicit p: Parameters) extends LazyModule{
  
  val input   = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(gen, "dataIn")))))
  val output  = new OrionSinkNode(Seq(OrionPullPortParameters(Seq(OrionPullParameters(gen, "sink")))))
    
  
//   val fb = OrionFunctionBlock(input, output){ f =>
//     f.out.data := ~f.in.data
//   }
//   val fb = LazyModule(new OrionFunctionBlock(gen,gen)(f => {
//     println(s"hello from ${f}")
//     f.out.data := ~f.in.data
//   }))
//   output := fb.node := input
  
  output := OrionFunctionBlock(gen,gen)(f => {
    f.out.data.b := f.in.data.a >= 4.U 
    f.out.data.a := f.in.data.a
  }) := input
  
//   val fb      = LazyModule(new OrionFunctionBlock(gen, gen)(
//     println("hello")
//     //fb.module.out.data := ~fb.module.in.data
//   ))
//   output := fb.node := input
  //output := input

  
  val in1  = InModuleBody { input.makeIOs() }
  val out1 = InModuleBody { output.makeIOs() }
  
  override lazy val module = new LazyModuleImp(this){
    
  }
}


object GCDTest extends App {  
  
  implicit val p: Parameters = Parameters.empty
  
  val verilog = (new ChiselStage).emitVerilog(
    LazyModule(new GCD(new MyBund)(p)).module,

    //args
    Array("--target-dir", "output")
  )
  
  GenElabArts.gen("GCD")
  
  
  
}
