package orion.examples

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import orion._

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

import freechips.rocketchip.util._




/**
  *   Custom Join Block for this adder
  */
class FibAdderBundle(gen : UInt) extends Bundle{
  val a = Output(gen)
  val b = Output(gen)
}

// class JoinFibAdder(itype : UInt, otype : FibAdderBundle, pInit: Int = 0)(implicit p: Parameters) extends Join[UInt,FibAdderBundle](itype, otype, pInit){
// 
//   override lazy val module = new LazyModuleImp(this) {
//     joinBase(this)
// 
//     out.data.a := ina.data
//     out.data.b := inb.data
// 
//   }
// }


class FibAdder(itype : FibAdderBundle, otype : UInt)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionMixedAdapterNode[FibAdderBundle,UInt](
    sourceFn  = { seq => 
      OrionPushPortParameters[UInt](Seq(OrionPushParameters(otype, "fibAddOut")))
    },
    sinkFn    = { seq => 
      OrionPullPortParameters[FibAdderBundle](Seq(OrionPullParameters(itype, "fibAddIn")))
    },
  )
  
  override lazy val module = new LazyModuleImp(this) {
    val in  = node.in.head._1
    val out = node.out.head._1
    
    in.ack := out.ack
    out.req:= in.req
    
    out.data  := in.data.a + in.data.b
    
  }
}


class Fib(gen : UInt)(implicit p: Parameters) extends LazyModule{
  
  val R0      = LazyModule(new DecoupReg(gen))
  val RF0     = LazyModule(new RegFork(gen, dataInit=1))
  val RF1     = LazyModule(new RegFork(gen, dataInit=1))
  val J0      = LazyModule(new Join(gen, new FibAdderBundle(gen))({j => 
    j.out.data.a := j.ina.data
    j.out.data.b := j.inb.data
  }))
  //val CL0     = LazyModule(new FibAdder(Flipped(new FibAdderBundle(gen)), gen))
  
  val barrier = LazyModule(new Barrier(gen))
  val output  = new OrionSinkNode(Seq(OrionPullPortParameters(Seq(OrionPullParameters(gen, "sink")))))
  
  RF0.in   := R0.out
  RF1.in   := RF0.out0
  
  J0.node  := RF0.out1
  J0.node  := barrier.node := RF1.out0
  
//   CL0.node := J0.node
//   
//   R0.in    := CL0.node
  R0.in := FunctionBlock(new FibAdderBundle(gen), gen)(f=> {
    f.out.data  := f.in.data.a + f.in.data.b
  }) := J0.node
  
  output   := RF1.out1

  val out = InModuleBody { output.makeIOs() }
  
  lazy val module = new FibImp(this)
  
  ElaborationArtefacts.add("graphml", module.wrapper.graphML)
  
}

class FibImp(override val wrapper: Fib)(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val start = IO(Input (Bool()))
    
  wrapper.barrier.module.start := start
  
//   val mydc = RegNext(start)
//   dontTouch(mydc)
//   sdcDontTouch(mydc)
  
}



  
//   //new for mux
//   val sel = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(Bool(), "sel")))))
//   val d0  = new OrionSourceNode(Seq(OrionPushPortParameters(Seq(OrionPushParameters(gen, "d0")))))
//   
//   output := OrionMux(sel, RF1.node, d0)
  



object FibTest extends App {  
  
  implicit val p: Parameters = Parameters.empty
  
  val verilog = (new ChiselStage).emitVerilog(
    LazyModule(new Fib(UInt(16.W))(p)).module,

    //args
    Array("--target-dir", "output")
  )
  
  GenElabArts.gen("Fib")
}
