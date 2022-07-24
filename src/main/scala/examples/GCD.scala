package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class GCDBundle(width: Int = 8) extends Bundle{
  val a = UInt(width.W)
  val b = UInt(width.W)
}


/** 
  *
  *
  */
class GCD(gen : GCDBundle)(implicit p: Parameters) extends LazyModule{
  
  // Note : No (or minimal) companion object use just to keep the ambiguity down
  //        and to more closely match the diagrams in the base click repo
  
  val input   = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(gen, "dataIn")))))
  val result  = new OrionSinkNode(Seq(OrionPullPortParameters(Seq(OrionPullParameters(gen, "result")))))
  
  val MX0     = LazyModule(new OrionMux(gen))
  val RF0     = LazyModule(new RegFork(gen, pbInit=0, pcInit=0))
  val RF1     = LazyModule(new RegFork(gen, pbInit=0, pcInit=0))
  val R0      = LazyModule(new DecoupReg(Bool(), dataInit=1, poInit=1))
  val F0      = LazyModule(new Fork(Bool()))
  
  // a != b : Output is Bool
  val CL0     = LazyModule(new FunctionBlock(gen, Bool())(f => {
    f.out.data := (f.in.data.a =/= f.in.data.b)
  }))
  
  val DX0     = LazyModule(new Demux(gen))
  
  // a > b : Output is Bool
  val CL1     = LazyModule(new FunctionBlock(gen, Bool())(f => {
    f.out.data := (f.in.data.a > f.in.data.b)
  }))
  
  val DX1     = LazyModule(new Demux(gen))
  
  
  val CL2     = LazyModule(new FunctionBlock(gen, gen)(f => {
    f.out.data.a := (f.in.data.a - f.in.data.b)
    f.out.data.b := f.in.data.b
  }))
  val CL3     = LazyModule(new FunctionBlock(gen, gen)(f => {
    f.out.data.a := f.in.data.a
    f.out.data.b := (f.in.data.b - f.in.data.a)
  }))
  
  val ME0     = LazyModule(new Merge(gen))
  
  
  // For Mux/Demux the order of the connections is important. This is why
  // the companion objects should generally be used for more clarity
  MX0.sel     := R0.out
  MX0.node    := input      // 0
  MX0.node    := ME0.node   // 1
  
  RF0.in      := MX0.node
  
  CL0.node    := RF0.out0
  F0.node     := CL0.node
  
  R0.in       := F0.node
  
  DX0.sel     := F0.node
  DX0.node    := RF0.out1
  
  
  result      := DX0.node   // 0
  RF1.in      := DX0.node   // 1
  
  CL1.node    := RF1.out0
  
  DX1.sel     := CL1.node
  DX1.node    := RF1.out1
  
  CL3.node    := DX1.node
  CL2.node    := DX1.node
  
  ME0.node    := CL2.node
  ME0.node    := CL3.node
  
  
  
  val in  = InModuleBody { input.makeIOs() }
  val out = InModuleBody { result.makeIOs() }
  
  override lazy val module = new LazyModuleImp(this){
    
  }
}


object GCDTest extends App {  
  
  implicit val p: Parameters = Parameters.empty
  
  val verilog = (new ChiselStage).emitVerilog(
    LazyModule(new GCD(new GCDBundle(8))(p)).module,

    //args
    Array("--target-dir", "output")
  )
  
  GenElabArts.gen("GCD")
  
  
  
}
