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



class OrionDelay[T <: Data](gen: T, delayPs: Int = 0) extends Module{
  val in  = IO(Input (gen.cloneType))
  val out = IO(Output(gen.cloneType))
  
  
  gen match {
    case record:  Record  => {
      record.elements.foreach {
        case (name, data) => {
          val delay = Module(new OrionDelayVerilog(data.getWidth, delayPs))
          delay.suggestName(s"${name}_delay")

          delay.io.in                             := in.asInstanceOf[Record].elements(name)
          out.asInstanceOf[Record].elements(name) := delay.io.out
        }
      }
    }    
    //Data should be a catch all for non-bundles
    case data: Data    => {
      val delay = Module(new OrionDelayVerilog(data.getWidth, delayPs))
      delay.io.in := in
      out         := delay.io.out
    }
  }
  
  
  class OrionDelayVerilog(width: Int = 1, delayPs: Int = 0)extends BlackBox(Map("WIDTH" -> width, "DELAY_PS" -> delayPs))  with HasBlackBoxInline{
    val io = IO(new Bundle{
      val in  = Input (UInt(width.W))
      val out = Output(UInt(width.W))
    })
    
    setInline("OrionDelayVerilog.v",
    """`timescale 1ps/1fs
    |module OrionDelayVerilog #(
    |  parameter WIDTH     = 1,
    |  parameter DELAY_PS  = 0
    |)(
    |  input  wire [WIDTH-1:0] in,
    |  output wire [WIDTH-1:0] out
    |);
    |assign #(DELAY_PS * 1ps) out = in;
    |endmodule
    """.stripMargin)
  }
}


object OrionDelay{
  def apply[T <: Data](in: T, delayPs: Int = 0): T = {
    val orion_delay = Module(new OrionDelay(in, delayPs))
    orion_delay.in := in
    orion_delay.out
  }
}
