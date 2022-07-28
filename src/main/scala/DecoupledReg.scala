package orion

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._



/**
  *   Decoupled HS Reg
  *   Input and output data types remain the same
  */
class DecoupReg[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters) extends LazyModule{
  
  val in  = new OrionSinkNode[T](Seq(OrionPullPortParameters[T](Seq(OrionPullParameters(gen, "drIn", Some(this))))))
  val out = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "drOut", Some(this))))))
  
  override lazy val module = new LazyModuleImp(this) {
    
    val in_ch  = in.in.head._1
    val out_ch = out.out.head._1
    
    //println(in.in.head._1.getClass)
    //println(in.in.head._2)
    //println(OrionUtils.getPath(in.in.head._2.push.pushes.inst.get))
    //println(OrionUtils.getPath(wrapper))
    //println(OrionUtils.getPath(in.in.head._2.push.pushes(0).inst))
    //println(OrionUtils.getPath(out.out.head._2.pull.pulls(0).inst))
    
    val pi            = Wire(Bool())
    val po            = Wire(Bool())
    
    val click         = OrionDelay(((in_ch.req ^ pi) & ~(out_ch.ack ^ po)), 10)
    
    val pi_pre        = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~pi, init=piInit.U)}
    val po_pre        = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~po, init=poInit.U)}
    val data_pre      = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(in_ch.data, init=dataInit.U)}
    
    pi                := OrionDelay(pi_pre, 10)
    po                := OrionDelay(po_pre, 10)
    out_ch.data       := OrionDelay(data_pre, 10)
    
    out_ch.req        := po
    in_ch.ack         := pi
    
//     //OLD
//     val decoup_reg = Module(new orion_decoup_reg(gen, gen.getWidth, dataInit, piInit, poInit))
//     decoup_reg.io.reset   := reset
//     in_ch.ack             := decoup_reg.io.in_ack
//     decoup_reg.io.in_req  := in_ch.req
//     decoup_reg.io.in_data := in_ch.data
//     
//     decoup_reg.io.out_ack := out_ch.ack
//     out_ch.req            := decoup_reg.io.out_req
//     out_ch.data           := decoup_reg.io.out_data
    
  }
}


// class orion_decoup_reg[T <: Data](
//   gen       : T,
//   width     : Int,
//   dataInit  : Int,
//   piInit    : Int,
//   poInit    : Int
// ) extends BlackBox(Map(
//   "WIDTH"   -> width,
//   "D_INIT"  -> dataInit,
//   "PI_INIT" -> piInit,
//   "PO_INIT" -> poInit
// )) with HasBlackBoxResource {
//   val io = IO(new Bundle{
//     val reset     = Input (Reset())
//     val in_ack    = Output(Bool())
//     val in_req    = Input (Bool())
//     val in_data   = Input (gen)
// 
//     val out_ack   = Input (Bool())
//     val out_req   = Output(Bool())
//     val out_data  = Output(gen)
//   })
// 
//   addResource("vsrc/orion_decoup_reg.v")
// }

// object DecoupReg{
//   def apply[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters): OrionAdapterNode[T] = {
//     val decoupReg = LazyModule(new DecoupReg(gen, dataInit, piInit, poInit)(p))
//     decoupReg.node
//   }
// }

// /**
//   *   Decoupled HS Reg
//   *   Input and output data types remain the same
//   */
// class DecoupReg[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters) extends LazyModule{
//   
//   val node = OrionAdapterNode[T](
//     sourceFn  = { seq => seq.copy() },
//     sinkFn    = { seq => seq.copy() }
//   )
//   
//   override lazy val module = new LazyModuleImp(this) {
//     
//     //-------------
//     class orion_decoup_reg[T <: Data](
//       gen       : T,
//       width     : Int,
//       dataInit  : Int,
//       piInit    : Int,
//       poInit    : Int
//     ) extends BlackBox(Map(
//       "WIDTH"   -> width,
//       "D_INIT"  -> dataInit,
//       "PI_INIT" -> piInit,
//       "PO_INIT" -> poInit
//     )) with HasBlackBoxResource {
//       val io = IO(new Bundle{
//         val reset     = Input (Reset())
//         val in_ack    = Output(Bool())
//         val in_req    = Input (Bool())
//         val in_data   = Input (gen)
//         
//         val out_ack   = Input (Bool())
//         val out_req   = Output(Bool())
//         val out_data  = Output(gen)
//       })
//       
//       addResource("vsrc/orion_decoup_reg.v")
//     }
//     //-------------
//     
//     val in_ch  = node.in.head._1
//     val out_ch = node.out.head._1
//     
// //     println(in_ch.data)
// //     
// //     //var b = Seq.empty[Data]
// //     //if(in_ch.data.isInstanceOf[Bundle]){
// //     val blah = in_ch.data match{
// //       case _: Bundle => println("it's a bundle")
// //       case _         => println("it's not a bundle")
// //     }
// //       //var b = in_ch.data.elements.map(_._2)
// //     //}
//     
//     val decoup_reg = Module(new orion_decoup_reg(gen, gen.getWidth, dataInit, piInit, poInit))
//     decoup_reg.io.reset   := reset
//     in_ch.ack             := decoup_reg.io.in_ack
//     decoup_reg.io.in_req  := in_ch.req
//     decoup_reg.io.in_data := in_ch.data
//     
//     decoup_reg.io.out_ack := out_ch.ack
//     out_ch.req            := decoup_reg.io.out_req
//     out_ch.data           := decoup_reg.io.out_data
//     
//   }
// }
// 
// 
// object DecoupReg{
//   def apply[T <: Data](gen : T, dataInit: Int = 0, piInit: Int = 0, poInit: Int = 0)(implicit p: Parameters): OrionAdapterNode[T] = {
//     val decoupReg = LazyModule(new DecoupReg(gen, dataInit, piInit, poInit)(p))
//     decoupReg.node
//   }
// }
