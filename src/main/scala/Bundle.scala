package orion

import chisel3._
import chisel3.util._

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._
import chisel3.internal.sourceinfo.SourceInfo


//===================================
// Main Channel Bundle
//===================================



class OrionBundle[T <: Data](gen: T) extends Bundle{
  val req   = Output(Bool())
  val ack   = Input (Bool())
  val data  = Output(gen)
}

object OrionBundle{
  def apply[T <: Data](gen: T) = new OrionBundle(gen)
}



case class OrionPushParameters[T <: Data](
  gen   : T,     
  name  : String
)

case class OrionPushPortParameters[T <: Data](
  pushes  :   Seq[OrionPushParameters[T]]   //Is it possible that we have more than one?
)

case class OrionPullParameters[T <: Data](
  gen   : T,
  name  : String
)

case class OrionPullPortParameters[T <: Data](
  pulls : Seq[OrionPullParameters[T]]
)

case class OrionEdgeParameters[T <: Data](
  push        : OrionPushPortParameters[T],
  pull        : OrionPullPortParameters[T],
  params      : Parameters,
  sourceInfo  : SourceInfo
){
  //val bundle = 
}



//===================================
// Node Imp
//===================================

class OrionImp [T<: Data] extends NodeImp[OrionPushPortParameters[T], OrionPullPortParameters[T], OrionEdgeParameters[T], OrionEdgeParameters[T], OrionBundle[T]] {
  def edgeO(pd: OrionPushPortParameters[T], pu: OrionPullPortParameters[T], p: Parameters, sourceInfo: SourceInfo) =  {
    OrionEdgeParameters(pd, pu, p, sourceInfo)
  }
  def edgeI(pd: OrionPushPortParameters[T], pu: OrionPullPortParameters[T], p: Parameters, sourceInfo: SourceInfo) =  {
    OrionEdgeParameters(pd, pu, p, sourceInfo)
  }
  
  def bundleO(eo: OrionEdgeParameters[T]) = OrionBundle[T](eo.push.pushes(0).gen)
  def bundleI(ei: OrionEdgeParameters[T]) = Flipped(OrionBundle[T](ei.pull.pulls(0).gen))
  
  def render(ei: OrionEdgeParameters[T]) = RenderedEdge(colour = "#ff00ff", label = s"blah")

  
}

object OrionImp{
  def apply[T <: Data]() = new OrionImp[T]()
}


case class OrionSourceNode[T<: Data](portParams: Seq[OrionPushPortParameters[T]])(implicit valName: ValName) extends SourceNode(OrionImp[T])(portParams)

case class OrionSinkNode  [T<: Data](portParams: Seq[OrionPullPortParameters[T]])(implicit valName: ValName) extends SinkNode(OrionImp[T])(portParams)


/**
  *   Adapter Node that requires the input/output bundles to be the same
  */
case class OrionAdapterNode [T<: Data] (
  sourceFn: OrionPushPortParameters[T] => OrionPushPortParameters[T],
  sinkFn:   OrionPullPortParameters[T] => OrionPullPortParameters[T]  )(
  implicit valName: ValName)
  extends AdapterNode(OrionImp[T])(sourceFn, sinkFn)

/**
  *   Adapter Node that allows in the Input and Output Bundles to be different
  *   I - Represents the input node type
  *   O - Represents the output node type
  */
case class OrionMixedAdapterNode [I <: Data, O <: Data] (
  sourceFn: OrionPushPortParameters[I] => OrionPushPortParameters[O],
  sinkFn:   OrionPullPortParameters[O] => OrionPullPortParameters[I]  )(
  implicit valName: ValName)
  extends MixedAdapterNode(OrionImp[I], OrionImp[O])(sourceFn, sinkFn)


case class OrionNexusNode [T<: Data] (
  sourceFn: Seq[OrionPushPortParameters[T]] => OrionPushPortParameters[T],
  sinkFn:   Seq[OrionPullPortParameters[T]] => OrionPullPortParameters[T]  )(
  implicit valName: ValName)
  extends NexusNode(OrionImp[T])(sourceFn, sinkFn)


