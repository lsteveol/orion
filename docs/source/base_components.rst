Orion Base Components
================================
The Orion library defines several base components for building self-timed circuits. These base components should constitute 
95% or more of the general components you need for building self-timed designs. However, if there is a block you need that is
not covered, it should be fairly straightforward to spin your own implementation.

The majority of these designs (currently) leverage the phase-decoupled click-based elements developed and explained by Jens Sparso and team. The
basic building blocks can be found in the `Async-Click-Library <https://github.com/zuzkajelcicova/Async-Click-Library>`_. 

.. note ::

  This document will not describe the operation of self-timed circuits in detail. For more information on self-timed circuits, it is suggested
  to start with `Introduction to Asynchronous Circuit Design <https://www.amazon.com/Introduction-Asynchronous-Circuit-Design-Spars%C3%B8/dp/B08BF2PFLN>`_
  by Jens Sparso.


The following list of components provides a general overview of each base component and will only expand funtionality that differs from
what is presented/implemented in the Async-Click-Library. Designers are encouraged to read
`Design and FPGA-implementation of Asynchronous Circuits Using Two-Phase Handshaking <https://ieeexplore.ieee.org/document/8850673>`_ in 
conjunction with the Async-Click-Library for more information regarding each component specifically.

DecoupledReg
--------------
The ``DecoupledReg`` implements the phase-decoupled register stage. 

.. danger ::
  
  ``DecoupledReg`` utilizes two node implementations, an ``OrionSourceNode`` and ``OrionSinkNode`` to drive/receive data. This is due to
  the restriction that Diplomacy operates on acyclic graphs. Self-timed circuit loops will naturally break this requirement, so for now
  Orion cuts these by implementing this as two nodes in lieu of something such as as ``OrionAdapterNode``.
  
  A future revision of Orion aims to spin a different diplomacy implementation and/or add an option to the current Diplomacy package to
  break this check, if possible.


FunctionBlock
---------------
The ``FunctionBlock`` defines a custom function implementation for a particular instance in which there is one input channel and one output channel. 
Compared to the Async-Click-Library, instead of creating a separate Module, the ``FunctionBlock`` defines a "container" which handles 
the ``req`/``ack`` connections but leaves the functional implementation to the designer, but without the need to necessarily extend a ``FunctionBlock`` class.

Let's look at an example of how this works with the GCD example circuit.

We have a situation where we want to determine if two ``UInt``s are equal. In the GCD we have defined the two input signals in a ``GCDBundle``:

.. code-block ::

  class GCDBundle(width: Int = 8) extends Bundle{
    val a = UInt(width.W)
    val b = UInt(width.W)
  }

We will want to define a ``FunctionBlock`` where the input is both of the ``UInt`` signals (via ``GCDBundle``), and the output would be
a ``Bool`` since we just want true/false if they are equal. This is how we could define this ``FunctionBlock`` for our use.

.. code-block :: scala

  val CL0     = LazyModule(new FunctionBlock(GCDBundle(UInt(8.W), Bool())(f => {
    f.out.data := (f.in.data.a =/= f.in.data.b)
  }))

Breaking down each piece, we have the following

.. code-block :: scala
  
  //            Input DataType       Output DataType
  FunctionBlock(GCDBundle(UInt(8.W), Bool())

Here we are defining a ``FunctionBlock`` in which the input data type is that of a ``GCDBundle`` with ``UInt(8.W)`` and an output data type
that is of type ``Bool``.

After the type declarations, we define the actual implementation of the ``FunctionBlock``. 

.. code-block :: scala

  FunctionBlock(GCDBundle(UInt(8.W), Bool())(f => { //the "f" here is arbitrary
    f.out.data := (f.in.data.a =/= f.in.data.b)
  }))

``f.in`` and ``f.out`` correspond to the input and output Bundled-Data channels, respectfully. Even though we have defined the input/output
types as ``GCDBundle`` and ``Bool``, recall that the channels between Nodes is actually an :ref:`OrionBundle<Bundled-Data Req/Ack Connections>`
and the ``data`` object represents our data type. Because of this, in the case of the ``GCDBundle`` input, we can access the ``a`` and ``b``
signals via ``f.in.data.a`` and ``f.in.data.b``. For the output, the ``f.out.data`` is just the ``Bool`` type since we don't have any 
sub bundles.

Inside the code block (the ``f => {...}``) you are free to implement whatever logic is required between the input and output channels. So this
could be something as simple as an equality check as shown here, or even a more complex logic function. The only requirement is that the operations
must be performed using only the input and output channels.

.. note ::

  A designer is free to extend the FunctionBlock and/or spin their own version for specific use cases, however this is beyond the scope of
  this document.
  
.. todo ::

  Want to investigate a way to infer the type via the node connections in a future version of the project to remove the need to explicitly
  define the input/output data types


Join
-----
The ``Join`` component defines a node in which two incomming channels are joined into a single output channel. While the use case 
is different compared to a ``FunctionBlock`` the ability to provide a custom implementation without the need to extend the base ``Join`` class
is available.

In the case of the ``Join``, since there are two input channels, the two channels have the identifier as ``ina`` and ``inb``. ``ina`` is channel 0 
or the first channel to be connected, which ``inb`` represents channel 1 or the second channel to be connected.

An example is the ``Fib`` circuit in which we want to represent two signals together to send to an adder block. We created a
``FibAdderBundle`` to describe the two ``UInt`` signals going out of the block.

.. code-block :: scala

  class FibAdderBundle(gen : UInt) extends Bundle{
    val a = Output(gen)
    val b = Output(gen)
  }


Inside the ``Join`` we simply want to "combine" the separate input ``UInt`` channels into one output channel. And we do so by the following

.. code-block ::

  val J0      = LazyModule(new Join(gen, new FibAdderBundle(gen))({j => 
    j.out.data.a := j.ina.data
    j.out.data.b := j.inb.data
  }))  


Here the two input channels are combined into the respective signals on the output channel ``Bundle``.

Fork
--------------
``Fork`` implements a fanout from one input channel into two output channels.

RegFork
--------------
``RegFork`` implements the handshake register + fork. Similarly to the ``DecoupledReg``, since loops are often used with this node, the single
input channel and dual output channels are represented by an ``OrionSinkNode`` and two ``OrionSourceNodes``.

OrionMux
--------------
The ``OrionMux`` implements a Mux node where there is a select, two input channels and a single output channel. This block is named ``OrionMux`` to avoid
a naming collision with the native Chisel ``Mux``.

Demux
--------------
``Demux`` implements a demux node where an input channel is steered between two output channels based on a select channel.

Merge
--------------
``Merge`` implementes a merge node where two mutually exclusive input channels are converted to a single output channel.


Barrier
--------------
The ``Barrier`` implments a barrier which blocks a ``req`` from propogating and can be used to stablize the system for initialization.

.. note ::

  The ``Barrier`` includes a ``start`` signal which will require wiring to any top level logic through the use of a ``LazyModuleImp``.



