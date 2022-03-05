package orion

import chisel3._
import chisel3.util._
import chisel3.experimental.{annotate, ChiselAnnotation}
import chisel3.stage.ChiselStage

import firrtl._
import firrtl.annotations._

/**
  *   This annotation will place a `(* dont_touch = "true" *)` statement in the verilog
  *   prior to the data object
  *
  */
object sdcDontTouch {
  
  def apply[T <: Data](data : T): Unit = {
    annotate(new ChiselAnnotation {
      override def toFirrtl = AttributeAnnotation(data.toTarget, "dont_touch = \"true\"")
    })
  }
  
}
