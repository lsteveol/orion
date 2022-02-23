package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class Barrier[T <: Data](gen : T)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionAdapterNode[T](
    sourceFn  = { seq => seq.copy() },
    sinkFn    = { seq => seq.copy() }
  )
  
  lazy val module = new BarrierImp(this)
  
}

/**
  *   BarrierImp needed due to 3.5 changes
  *   https://www.chisel-lang.org/chisel3/docs/appendix/upgrading-from-chisel-3-4.html#value-io-is-not-a-member-of-chisel3module
  */
class BarrierImp[T <: Data](override val wrapper: Barrier[T])(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val start = IO(Input (Bool()))

  val in  = wrapper.node.in.head._1
  val out = wrapper.node.out.head._1

  in.ack    := out.ack
  out.req   := in.req & start

  out.data  := in.data
}
