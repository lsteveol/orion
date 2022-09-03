Orion Infrastructure
=========================
The Orion infrastructure utilizes Diplomacy to aid in implementation, particularly with regards to component connection. While this is the main
use-case currently, there are several reasons why Diplomacy was integrated. The following is a summary of the various features Orion wishes
to utilize and/or implement via the Diplomacy package.

* Component Connection
* Token/Bubble Optimization
* SDC Constraint Generation
* Process Specific Performace Estimations

.. note ::

  For more information regarding Diplomacy, please see the following:
  https://github.com/chipsalliance/rocket-chip/blob/master/docs/src/diplomacy/adder_tutorial.md

Bundled-Data Req/Ack Connections
---------------------------------
While the ``req``/``ack`` connections involved with self-timed circuits are fairly intuitive, the scheme can quickly become overwhelming for even
the smallest of designs. Particularly when fork/joins start becoming involved. Since Diplomacy allows a user to define ``Bundle``s which are used between
various diplomatic Nodes, we can utilize this feature for easing the connection burden.

``OrionBundle`` is the base ``Bundle`` class that is used for Node connections. This ``Bundle`` utilizes a type parameter so custom data elements can be used.

.. code-block :: scala 

  class OrionBundle[T <: Data](gen: T) extends Bundle{
    val req   = Output(Bool())
    val ack   = Input (Bool())
    val data  = Output(gen)
  }

.. note ::

  The default direction of the ``OrionBundle`` is from the perspective of a Source/Push component. Individual Nodes handle direction flipping
  where required.

During connections between nodes, the ``req`` and ``ack`` connections are handled as part of the connection framework.


Orion Node Parameters
-----------------------------
Orion Node Parameters defines information regarding each instance of a Node. Nodes are constructed utilizing these parameters. There are two
basic parameter types

* ``OrionPushParameters``
* ``OrionPullParameters``

The terms "Push" and "Pull" can also be thought of a Source/Sink. Currently, both parameter types have the same structure.

.. code-block :: scala

  case class OrionPushParameters[T <: Data](
    gen   : T,     
    name  : String,
    inst  : Option[LazyModule] = None
  )

  case class OrionPullParameters[T <: Data](
    gen   : T,
    name  : String,
    inst  : Option[LazyModule] = None
  )


``gen`` constitutes the data type that is used by this Node. For example, if the designer needs to operate on an 8bit value, ``gen`` would be ``UInt(8.W)``. Custom
``Bundles`` can be used.


``name`` is used for graphing and will also be used for future enhancements
``inst`` is reserved for future enhancements


These parameters are wrapped in PortParameters (``OrionPushPortParameters`` and ''OrionPullPortParameters``) which are used as the ``portParams`` for the 
base Diplomacy Node implementations.

.. code-block :: scala

  case class OrionPushPortParameters[T <: Data](
    pushes  :   Seq[OrionPushParameters[T]]   //Is it possible that we have more than one?
  )

  case class OrionPullPortParameters[T <: Data](
    pulls : Seq[OrionPullParameters[T]]
  )


``OrionEdgeParameters`` are what is sent between nodes. They meerly include the push/pull port parameters.

.. code-block :: scala

  case class OrionEdgeParameters[T <: Data](
    push        : OrionPushPortParameters[T],
    pull        : OrionPullPortParameters[T],
    params      : Parameters,
    sourceInfo  : SourceInfo
  )


.. note ::

  Currently the ``Orion[Push|Pull]PortParameters`` include a ``Seq`` of ``Orion[Push|Pull]Parameters``. A future revision may remove the ``Seq`` since
  we will likely only have one set of parameters per port.

Orion Nodes
-----------------------------
Various node types are defined for component implementations.

``OrionSourceNode`` and ``OrionSinkNode`` define source/push and sink/pull nodes.

.. code-block :: scala

  case class OrionSourceNode[T<: Data](portParams: Seq[OrionPushPortParameters[T]])(implicit valName: ValName) extends SourceNode(OrionImp[T])(portParams)

  case class OrionSinkNode  [T<: Data](portParams: Seq[OrionPullPortParameters[T]])(implicit valName: ValName) extends SinkNode(OrionImp[T])(portParams)

``OrionAdapterNode`` is used when the input and output data types are the same (e.g. ``Bool`` -> ``Bool`` or ``UInt(4.W)`` -> ``UInt(4.W)``) and there
is only a single connection in and out of the Node.

.. code-block :: scala

  case class OrionAdapterNode [T<: Data] (
    sourceFn: OrionPushPortParameters[T] => OrionPushPortParameters[T],
    sinkFn:   OrionPullPortParameters[T] => OrionPullPortParameters[T]  )(
    implicit valName: ValName)
    extends AdapterNode(OrionImp[T])(sourceFn, sinkFn)


``OrionMixedAdapterNode`` is used when the input and output data types differ (e.g. OR reduction of ``UInt(8.W)`` would result in ``UInt(8.W)`` -> ``Bool``) and there
is only a single connection in and out of the Node.

.. code-block :: scala

  case class OrionMixedAdapterNode [I <: Data, O <: Data] (
    sourceFn: OrionPushPortParameters[I] => OrionPushPortParameters[O],     
    sinkFn:   OrionPullPortParameters[O] => OrionPullPortParameters[I]  )(  
    implicit valName: ValName)
    extends MixedAdapterNode(OrionImp[I], OrionImp[O])(sourceFn, sinkFn)


``OrionNexusNode`` is used when the input and output data types are the same but there may be multiple connections on each input or output.

.. code-block :: scala

  case class OrionNexusNode [T<: Data] (
    sourceFn: Seq[OrionPushPortParameters[T]] => OrionPushPortParameters[T],
    sinkFn:   Seq[OrionPullPortParameters[T]] => OrionPullPortParameters[T]  )(
    implicit valName: ValName)
    extends NexusNode(OrionImp[T])(sourceFn, sinkFn)

``OrionMixedNexusNode`` is used when the input and output data types are different and there may be mutiple connections on each input or output

.. code-block :: scala

  case class OrionMixedNexusNode [I <: Data, O <: Data] (
    sourceFn: Seq[OrionPushPortParameters[I]] => OrionPushPortParameters[O],
    sinkFn:   Seq[OrionPullPortParameters[O]] => OrionPullPortParameters[I]  )(
    implicit valName: ValName)
    extends MixedNexusNode(OrionImp[I], OrionImp[O])(sourceFn, sinkFn)
