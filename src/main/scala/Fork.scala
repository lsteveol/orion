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
    //val data_pre    = withClockAndReset(click.asClock, reset.asAsyncReset){RegNext(in_ch.data, init=dataInit.U.asTypeOf(gen))}
    
    val data_pre_dly  = Wire(gen)
    withClockAndReset(click.asClock, reset.asAsyncReset){
      val data_reg = RegInit(gen, init=dataInit.U.asTypeOf(gen))
      data_reg     := in_ch.data
      data_pre_dly := data_reg
    }
    
    pa              := OrionDelay(pa_pre, 10)
    pb              := OrionDelay(pb_pre, 10)
    pc              := OrionDelay(pc_pre, 10)
    //val data_reg    = OrionDelay(data_pre, 10)
    val data_reg    = OrionDelay(data_pre_dly, 10)
    
    in_ch.ack       := pa
    out_ch0.req     := pb
    out_ch0.data    := data_reg
    
    out_ch1.req     := pc
    out_ch1.data    := data_reg
   
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
    
  }
}
