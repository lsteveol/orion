package orion.examples

import orion._
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

import freechips.rocketchip.util._



class OrionCounterExample(width: Int = 8)(implicit p: Parameters) extends LazyModule{
  val en    = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(Bool(),        "en")))))
  val clear = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(Bool(),        "clear")))))
  val count = new OrionSinkNode  (Seq(OrionPullPortParameters(Seq(OrionPullParameters(UInt(width.W), "count")))))
  
  
  val counter     = LazyModule(new DecoupReg(UInt(width.W), dataInit=0, piInit=0, poInit=1))
  val counterBub  = LazyModule(new DecoupReg(UInt(width.W), dataInit=0, piInit=0, poInit=0))
  val counterFork = LazyModule(new Fork(UInt(width.W)))
  
  val plus1       = FunctionBlock(UInt(width.W), UInt(width.W))(f => {f.out.data := f.in.data + 1.U})
  val zeroOut     = FunctionBlock(UInt(width.W), UInt(width.W))(f => {f.out.data := 0.U})
  
  counterFork.node  := counter.out
  
  val en0Branch     = OrionIdentityNode[UInt]()
  val enDemux       = Demux(UInt(width.W))(in = counterFork.node, sel = en, out0 = en0Branch, out1 = plus1)
  val enMerge       = Merge(UInt(width.W))(in0 = en0Branch, in1 = plus1) 
  
  val clear0Branch  = OrionIdentityNode[UInt]()
  val clearDemux    = Demux(UInt(width.W))(in = enMerge, sel = clear, out0 = clear0Branch, out1 = zeroOut)
  val clearMerge    = Merge(UInt(width.W))(in0 = clear0Branch, in1 = zeroOut)
  
  counterBub.in     := clearMerge
  counter.in        := counterBub.out
  
  //Final connection
  count             := counterFork.node
  
  override lazy val module = new OrionCounterExampleImp(this, width)
  
}


class OrionCounterExampleImp(override val wrapper: OrionCounterExample, width: Int)(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val en    = IO(Flipped(new OrionBundle(Bool())))
  val clear = IO(Flipped(new OrionBundle(Bool())))
  val count = IO(new OrionBundle(UInt(width.W)))
  
  en      <> wrapper.en.out.head._1
  clear   <> wrapper.clear.out.head._1
  count   <> wrapper.count.in.head._1
}
