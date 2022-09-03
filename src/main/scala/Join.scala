package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


/** [[Join]] defines a node in which two incoming channels are joined into a single output channel. Normally
  * a Join is used to simplly concatenate two separate data structures. Similar to a [[FunctionBlock]]
  *  
  * @tparam I The type of input Data
  * @tparam O The type of output Data
  * @param  pInit Join init phase default value
  * @param  body Custom Join implementation 
  *
  */
class Join[I <: Data, O <: Data](itype : I, otype : O, val pInit: Int = 0)(body: JoinImp[I,O] => Unit)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionMixedNexusNode[I,O](
    sourceFn  = { seq => 
      OrionPushPortParameters[O](Seq(OrionPushParameters(otype, "joinOut")))
    },
    sinkFn    = { seq => 
      OrionPullPortParameters[I](Seq(OrionPullParameters(itype, "joinIn")))
    },
  )
  
  override lazy val module = new JoinImp(this, body) {}
}


class JoinImp[I <: Data, O <: Data](override val wrapper: Join[I,O], body: JoinImp[I,O] => Unit)(implicit p: Parameters) extends LazyModuleImp(wrapper) {
  
  require(wrapper.node.in.size  == 2, s"Join requires two input channels but saw ${wrapper.node.in.size}")
  require(wrapper.node.out.size == 1, s"Join requires one output channel but saw ${wrapper.node.out.size}")
  
  val (in_ch, edgesIn)    = wrapper.node.in.unzip
  val (out_ch, edgesOut)  = wrapper.node.out.unzip
  
  val ina = in_ch(0)
  val inb = in_ch(1)
  val out = out_ch(0)
  
  val phase     = Wire(Bool())
  val click     = OrionDelay(((ina.req & inb.req & ~phase) | (~ina.req & ~inb.req & phase)), 10)
  
  val phase_pre = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~phase, init=wrapper.pInit.U)}
  
  phase         := OrionDelay(phase_pre, 10)
  
  out.req       := phase
  ina.ack       := out.ack
  inb.ack       := out.ack
   
  
    
  body(this)
}
