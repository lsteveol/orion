package orion

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

/** [[FunctionBlock]] defines a node in which the data needs to be modified, usually through some combinaitional
  * logic. The input and output Data types can differ
  *
  * @tparam I The type of input Data
  * @tparam O The type of output Data
  * @param itype
  * @param otype
  *
  * 
  *
  *
  *
  *
  */

class FunctionBlock[I <: Data, O <: Data](itype : I, otype : O)(body: FunctionBlockImp[I,O] => Unit)(implicit p: Parameters) extends LazyModule{
  
  val node = OrionMixedAdapterNode[I,O](
    sourceFn  = { seq => 
      OrionPushPortParameters[O](Seq(OrionPushParameters(otype, "fbOut")))
    },
    sinkFn    = { seq => 
      OrionPullPortParameters[I](Seq(OrionPullParameters(itype, "fbIn")))
    },
  )
  
  //var cpBody = body
      
  override lazy val module = new FunctionBlockImp[I,O](this, body/*, cpBody*/)
}

class FunctionBlockImp[I <: Data, O <: Data](override val wrapper: FunctionBlock[I,O], body: FunctionBlockImp[I,O] => Unit/*, cpBody: FunctionBlockImp[I,O] => Unit*/)(implicit p: Parameters) extends LazyModuleImp(wrapper){
  val in  = wrapper.node.in.head._1
  val out = wrapper.node.out.head._1
  
  in.ack    := out.ack
  out.req   := in.req 
  
  //cpBody(this)
  body(this)
}



object FunctionBlock{
  
  /**
    *   I **REALLY** want to see about getting this to work
    */
//   def apply[I <: Data, O <: Data, IN, ON](in: IN, out: ON)(body: FunctionBlockImp[I,O] => Unit)(implicit p: Parameters, 
//     ev: IN <:< MixedNode[OrionPushPortParameters[I], OrionPullPortParameters[I], OrionEdgeParameters[I], OrionBundle[I],
//                          OrionPushPortParameters[I], OrionPullPortParameters[I], OrionEdgeParameters[I], OrionBundle[I]] with OrionNodeTrait[I],
//     dv: ON <:< MixedNode[OrionPushPortParameters[O], OrionPullPortParameters[O], OrionEdgeParameters[O], OrionBundle[O],
//                          OrionPushPortParameters[O], OrionPullPortParameters[O], OrionEdgeParameters[O], OrionBundle[O]] with OrionNodeTrait[O]): Unit = {
//     
//     val ofb = LazyModule(new FunctionBlock[I,O](in.getBundleType, out.getBundleType)(body)(p))
//     ofb.node  := in
//     out       := ofb.node
//     Unit
//   }
  
  
  def apply[I <: Data, O <: Data](itype : I, otype : O)(body: FunctionBlockImp[I,O] => Unit)(implicit p: Parameters): OrionMixedAdapterNode[I,O] = {
    val ofb = LazyModule(new FunctionBlock[I,O](itype, otype)(body)(p))
    ofb.node
  }
  
}


