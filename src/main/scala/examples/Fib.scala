package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._



/**
  *   Custom Join Block for this adder
  */
class FibAdderBundle(gen : UInt) extends Bundle{
  val a = Output(gen)
  val b = Output(gen)
}

class JoinFibAdder(itype : UInt, otype : FibAdderBundle, pInit: Int = 0)(implicit p: Parameters) extends Join[UInt,FibAdderBundle](itype, otype, pInit){

  override lazy val module = new LazyModuleImp(this) {
    joinBase(this)

    out.data.a := ina.data
    out.data.b := inb.data

  }
}


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
    
  val J0      = LazyModule(new JoinFibAdder(gen, new FibAdderBundle(gen)))
  
  val CL0     = LazyModule(new FibAdder(Flipped(new FibAdderBundle(gen)), gen))
  
  val barrier = LazyModule(new Barrier(gen))
  
  val output  = new OrionSinkNode[UInt](Seq(OrionPullPortParameters[UInt](Seq(OrionPullParameters(gen, "sink")))))
  
  
  
  RF0.node := R0.out
  RF1.node := RF0.node
  
  J0.node  := RF0.node
  J0.node  := barrier.node := RF1.node
  
  CL0.node := J0.node
  
  R0.in    := CL0.node
  
  output   := RF1.node
  
  
  
  val out = InModuleBody { output.makeIOs() }
  
  lazy val module = new FibImp(this)
  
}

class FibImp(override val wrapper: Fib)(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val start = IO(Input (Bool()))
    
  wrapper.barrier.module.start := start
}


object FibTest extends App {  
  
  implicit val p: Parameters = Parameters.empty
  
  val verilog = (new ChiselStage).emitVerilog(
    LazyModule(new Fib(UInt(16.W))(p)).module,

    //args
    Array("--target-dir", "output")
  )
  
}
