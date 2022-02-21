package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

/**
  *   Decoupled HS Reg
  *   Input and output data types remain the same
  */
class DecoupReg[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionAdapterNode[T](
    sourceFn  = { seq => seq.copy() },
    sinkFn    = { seq => seq.copy() }
  )
  
  override lazy val module = new LazyModuleImp(this) {
    
    //-------------
    class orion_decoup_reg[T <: Data](
      gen       : T,
      width     : Int,
      dataInit  : Int,
      piInit    : Int,
      poInit    : Int
    ) extends BlackBox(Map(
      "WIDTH"   -> width,
      "D_INIT"  -> dataInit,
      "PI_INIT" -> piInit,
      "PO_INIT" -> poInit
    )) with HasBlackBoxResource {
      val io = IO(new Bundle{
        val reset     = Input (Reset())
        val in_ack    = Output(Bool())
        val in_req    = Input (Bool())
        val in_data   = Input (gen)
        
        val out_ack   = Input (Bool())
        val out_req   = Output(Bool())
        val out_data  = Output(gen)
      })
      
      addResource("vsrc/orion_decoup_reg.v")
    }
    //-------------
    
    val in_ch  = node.in.head._1
    val out_ch = node.out.head._1
    
//     println(in_ch.data)
//     
//     //var b = Seq.empty[Data]
//     //if(in_ch.data.isInstanceOf[Bundle]){
//     val blah = in_ch.data match{
//       case _: Bundle => println("it's a bundle")
//       case _         => println("it's not a bundle")
//     }
//       //var b = in_ch.data.elements.map(_._2)
//     //}
    
    val decoup_reg = Module(new orion_decoup_reg(gen, gen.getWidth, dataInit, piInit, poInit))
    decoup_reg.io.reset   := reset
    in_ch.ack             := decoup_reg.io.in_ack
    decoup_reg.io.in_req  := in_ch.req
    decoup_reg.io.in_data := in_ch.data
    
    decoup_reg.io.out_ack := out_ch.ack
    out_ch.req            := decoup_reg.io.out_req
    out_ch.data           := decoup_reg.io.out_data
    
  }
}


object DecoupReg{
  def apply[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters): OrionAdapterNode[T] = {
    val decoupReg = LazyModule(new DecoupReg(gen, dataInit, piInit, poInit)(p))
    decoupReg.node
  }
}


class MyBundle extends Bundle{
  val sig1 = Bool()
  val sig2 = UInt(3.W)
}


/**
  *
  */
class MySource[T <: Data](gen : T)(implicit p: Parameters) extends LazyModule{
  
  val node = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "source")))))
  
  override lazy val module = new LazyModuleImp(this) {
    val channel = node.out.head._1
    
    dontTouch(channel)
  }
}


class MySink[T <: Data](gen : T)(implicit p: Parameters) extends LazyModule{
  
  val node = new OrionSinkNode[T](Seq(OrionPullPortParameters[T](Seq(OrionPullParameters(gen, "sink")))))
  
  override lazy val module = new LazyModuleImp(this) {
    val channel = node.in.head._1
    
    dontTouch(channel)
    
  }
}

class MyTop()(implicit p: Parameters) extends LazyModule{
  
  val src1  = LazyModule(new MySource(UInt(4.W)))
  val src2  = LazyModule(new MySource(UInt(4.W)))
  val sink  = LazyModule(new MySink(UInt(8.W)))

  
  val j     = LazyModule(new JoinUInt(UInt(4.W), UInt(4.W), UInt(8.W)))
  
  //sink.node := DecoupReg(new MyBundle) := src.node
  
  j.a := src1.node
  j.b := src2.node
  sink.node := j.o
  
  
  override lazy val module = new LazyModuleImp(this) {
  
  }
}


object Test extends App {  
  
  implicit val p: Parameters = Parameters.empty
  
  val verilog = (new ChiselStage).emitVerilog(
    LazyModule(new MyTop()(p)).module,

    //args
    Array("--target-dir", "output")
  )
  
}

