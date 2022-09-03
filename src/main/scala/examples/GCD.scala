package orion.examples

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import orion._

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

/**
  *   Input and output of the GCD is two UInts. We can set the
  *   width as a paramter
  */
class GCDBundle(width: Int = 8) extends Bundle{
  val a = UInt(width.W)
  val b = UInt(width.W)
}


class GCD(gen : GCDBundle, isTop: Boolean = false)(implicit p: Parameters) extends LazyModule{
  
  // Note : No (or minimal) companion object use just to keep the ambiguity down
  //        and to more closely match the diagrams in the base click repo
  
  // input and result are going to be our "ports" that come in/out of the block
  val input   = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(gen, "dataIn")))))
  val result  = new OrionSinkNode  (Seq(OrionPullPortParameters(Seq(OrionPullParameters(gen, "result")))))
  
  // Defining cells
  val MX0     = LazyModule(new OrionMux(gen))
  val RF0     = LazyModule(new RegFork(gen, pbInit=0, pcInit=0))
  val RF1     = LazyModule(new RegFork(gen, pbInit=0, pcInit=0))
  val R0      = LazyModule(new DecoupReg(Bool(), dataInit=0, poInit=1))
  val F0      = LazyModule(new Fork(Bool()))
  
  // a != b : 
  // Input is two UInts (our GCDBundle)
  // Output is Bool
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
  
  // Connecting everything together
  
  // For Mux/Demux the order of the connections is important. This is why
  // the companion objects could generally be used for more clarity
  
  // Remember that as you connect multiple edges to a node, you can
  // think of each connection as "appending" to an array, so the first
  // connection is going to be on channel 0, the second on channel 1
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


  override lazy val module = new GCDImp(this, gen, isTop)
  
  ElaborationArtefacts.add("graphml", module.wrapper.graphML)
}


/** 
  *   Implementation class to pull out ports if we want to use this as a
  *   top level
  */
class GCDImp(override val wrapper: GCD, gen: GCDBundle, isTop: Boolean = false)(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val in  = if (isTop) Some(IO(Flipped(new OrionBundle(gen)))) else None
  val out = if (isTop) Some(IO(new OrionBundle(gen)))          else None
  
  if(isTop){
    in.get  <> wrapper.input.out.head._1
    out.get <> wrapper.result.in.head._1
  }
}

// 
// object GCDTest extends App {  
//   
//   implicit val p: Parameters = Parameters.empty
//   
//   val verilog = (new ChiselStage).emitVerilog(
//     LazyModule(new GCD(new GCDBundle(8), true)(p)).module,
// 
//     //args
//     Array("--target-dir", "output"/*, "--no-dce"*/)
//   )
//   
//   GenElabArts.gen("GCD")
//   
//   
//   
// }


