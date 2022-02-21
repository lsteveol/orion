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
  
  override lazy val module = new LazyModuleImp(this){
    val start = IO(Input (Bool()))
    
    val in  = node.in.head._1
    val out = node.out.head._1
    
    in.ack := out.ack
    out.req:= in.req & start
    
  }
  
}
