package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._


class RegFork[T <: Data](
  gen     : T, 
  dataInit: Int = 0, 
  paInit  : Int = 0, 
  pbInit  : Int = 1,
  pcInit  : Int = 1
)(implicit p: Parameters) extends LazyModule{
  
  // Cycles, need to fix them
  //val node = OrionNexusNode[T](
  //  sourceFn  = { seq => seq(0).copy() },
  //  sinkFn    = { seq => seq(0).copy() }
  //)
  val in   = new OrionSinkNode[T]  (Seq(OrionPullPortParameters[T](Seq(OrionPullParameters(gen, "forkIn", Some(this))))))
  val out0 = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "forkOut0", Some(this))))))
  val out1 = new OrionSourceNode[T](Seq(OrionPushPortParameters[T](Seq(OrionPushParameters(gen, "forkOut1", Some(this))))))
  
  override lazy val module = new LazyModuleImp(this) {
    
    //require(node.in.size  == 1, s"RegFork requires one input channel but saw ${node.in.size}")
    //require(node.out.size == 2, s"RegFork requires two output channels but saw ${node.out.size}")
    
//     //-------------
//     class orion_reg_fork[T <: Data](
//       gen       : T,
//       width     : Int,
//       dataInit  : Int,
//       paInit    : Int,
//       pbInit    : Int,
//       pcInit    : Int
//     ) extends BlackBox(Map(
//       "WIDTH"   -> width,
//       "INIT"    -> dataInit,
//       "PA_INIT" -> paInit,
//       "PB_INIT" -> pbInit,
//       "PC_INIT" -> pcInit
//     )) with HasBlackBoxResource {
//       val io = IO(new Bundle{
//         val reset     = Input (Reset())
//         val inA_ack   = Output(Bool())
//         val inA_req   = Input (Bool())
//         val inA_data  = Input (gen)
//         
//         val outB_ack  = Input (Bool())
//         val outB_req  = Output(Bool())
//         val outB_data = Output(gen)
//         
//         val outC_ack  = Input (Bool())
//         val outC_req  = Output(Bool())
//         val outC_data = Output(gen)
//       })
//       
//       addResource("vsrc/orion_reg_fork.v")
//     }
//     //-------------
    
    val in_ch   = in.in.head._1
    val out_ch0 = out0.out.head._1
    val out_ch1 = out1.out.head._1
    
    
    val inA_token   = Wire(Bool())
    val outB_bubble = Wire(Bool())
    val outC_bubble = Wire(Bool())
    val click       = Wire(Bool())
    
    val pa          = Wire(Bool())
    val pb          = Wire(Bool())
    val pc          = Wire(Bool())
    
    inA_token       := OrionDelay((in_ch.req ^ pa),    10)
    outB_bubble     := OrionDelay(~(pb ^ out_ch0.ack), 10)
    outC_bubble     := OrionDelay(~(pc ^ out_ch1.ack), 10)
    
    click           := OrionDelay((inA_token & outB_bubble & outC_bubble), 10)
    
    val pa_pre      = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~pa,        init=paInit.U)}
    val pb_pre      = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~pb,        init=pbInit.U)}
    val pc_pre      = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~pc,        init=pcInit.U)}
    val data_pre    = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(in_ch.data, init=dataInit.U.asTypeOf(gen))}
    
    pa              := OrionDelay(pa_pre, 10)
    pb              := OrionDelay(pb_pre, 10)
    pc              := OrionDelay(pc_pre, 10)
    val data_reg    = OrionDelay(data_pre, 10)
    
    in_ch.ack       := pa
    out_ch0.req     := pb
    out_ch0.data    := data_reg
    
    out_ch1.req     := pc
    out_ch1.data    := data_reg
    
//     ///OLD
//     val reg_fork = Module(new orion_reg_fork(gen, gen.getWidth, dataInit, paInit, pbInit, pcInit))
//     
//     reg_fork.io.reset     := reset
//     in_ch.ack             := reg_fork.io.inA_ack
//     reg_fork.io.inA_req   := in_ch.req
//     reg_fork.io.inA_data  := in_ch.data
//     
//     reg_fork.io.outB_ack  := out_ch0.ack
//     out_ch0.req           := reg_fork.io.outB_req
//     out_ch0.data          := reg_fork.io.outB_data
//     
//     reg_fork.io.outC_ack  := out_ch1.ack
//     out_ch1.req           := reg_fork.io.outC_req
//     out_ch1.data          := reg_fork.io.outC_data
    
  }
}




class Fork[T <: Data](
  gen     : T, 
  pInit   : Int = 0
)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionNexusNode[T](
    sourceFn  = { seq => seq(0).copy() },
    sinkFn    = { seq => seq(0).copy() }
  )
  
  override lazy val module = new LazyModuleImp(this) {
    
    require(node.in.size  == 1, s"RegFork requires one input channel but saw ${node.in.size}")
    require(node.out.size == 2, s"RegFork requires two output channels but saw ${node.out.size}")
    
//     //-------------
//     class orion_fork[T <: Data](
//       pInit     : Int
//     ) extends BlackBox(Map(
//       "P_INIT" -> pInit 
//     )) with HasBlackBoxResource {
//       val io = IO(new Bundle{
//         val reset     = Input (Reset())
//         val inA_ack   = Output(Bool())
//         val inA_req   = Input (Bool())
//         
//         val outB_ack  = Input (Bool())
//         val outB_req  = Output(Bool())
//         
//         val outC_ack  = Input (Bool())
//         val outC_req  = Output(Bool())
//       })
//       
//       addResource("vsrc/orion_fork.v")
//     }
//     //-------------
    
    val in_ch  = node.in.head._1
    val (out_ch, edgesOut)   = node.out.unzip
    
    
    val phase         = Wire(Bool())
    val click         = Wire(Bool())
    
    click             := OrionDelay(((out_ch(1).ack & out_ch(0).ack & ~phase) | (~out_ch(1).ack & ~out_ch(0).ack & phase)), 10)
    
    val phase_pre     = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(~phase, init=pInit.U)}
    phase             := OrionDelay(phase_pre, 10)
    
    in_ch.ack         := phase
    
    out_ch(0).req     := in_ch.req
    out_ch(0).data    := in_ch.data
    
    out_ch(1).req     := in_ch.req
    out_ch(1).data    := in_ch.data
    
//     //OLD
//     val fork = Module(new orion_fork(pInit))
//     
//     fork.io.reset     := reset
//     in_ch.ack         := fork.io.inA_ack
//     fork.io.inA_req   := in_ch.req
//     
//     fork.io.outB_ack  := out_ch(0).ack
//     out_ch(0).req     := fork.io.outB_req
//     out_ch(0).data    := in_ch.data
//     
//     fork.io.outC_ack  := out_ch(1).ack
//     out_ch(1).req     := fork.io.outC_req
//     out_ch(1).data    := in_ch.data
    
  }
}
