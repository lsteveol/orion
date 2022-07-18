package orion

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

class OrionDemetReset[T <: Data](gen : T) extends Module{
  val in  = IO(Input (gen.cloneType))
  val out = IO(Output(gen.cloneType))
  
  val ff1 = RegNext(in,  0.U)
  val ff2 = RegNext(ff1, 0.U)
  out     := ff2
}


object OrionDemetReset{
  def apply[T <: Data](in : T): T = {
    val demet = Module(new OrionDemetReset(in))
    demet.in := in
    demet.out
  }
}
