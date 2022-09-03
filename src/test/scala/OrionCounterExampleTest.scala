package orion.test

import chisel3._
import chiseltest._
import chiseltest.internal._
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.ParallelTestExecution

import orion._
import orion.examples._
import freechips.rocketchip.config.{Parameters, Field, Config}
import freechips.rocketchip.diplomacy._

import scala.math._


import chisel3.experimental._
//class OrionBundleDriver[T <: Data, B <: OrionBundle[T]](bun: B){
// class OrionBundleDriver[B <: OrionBundle[_]](parent: Module, bun: B){
// 
//   require(DataMirror.directionOf(bun.req) == Direction.Input,  "OrionBundleDriver should be a source (req)!")
//   require(DataMirror.directionOf(bun.ack) == Direction.Output, "OrionBundleDriver should be a source (ack)!")
//   
//   def drive: Unit = {
//     bun.req.poke(if (bun.req.peek().litValue == 0) true.B else false.B)
//   }
//   
//   def finished: Unit = {
//     while(bun.ack.peek().litValue != bun.req.peek().litValue){
//       parent.clock.step(10)
//     }
//   }
//   
// }

class OrionCounterExampleTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "OrionCounterExample Test"
  
  val annos = Seq(IcarusBackendAnnotation, WriteVcdAnnotation)
  
  val iters  = 20
    
  it should s"check OrionCounterExample" in {
    implicit val p = Parameters.empty

    test(LazyModule(new OrionCounterExample()(p)).module).withAnnotations(annos) { dut =>
      var polarity = 1
      
      val enDriver    = new OrionBundleDriver(dut, dut.en)
      val clearDriver = new OrionBundleDriver(dut, dut.clear)
      
      dut.clock.setTimeout(9999)

      dut.reset.poke(true.B)
      dut.clock.step(10)
      dut.reset.poke(false.B)
      dut.clock.step(10)
      
      //dut.start.poke(true.B)
      
      for (i <- 1 until 256){        
        dut.en.data.poke(true.B)
        enDriver.drive
        
        dut.clear.data.poke(false.B)
        clearDriver.drive
        enDriver.finished
        clearDriver.finished


        while(dut.count.req.peek().litValue != polarity){
          dut.clock.step(10)
        }
        dut.count.ack.poke(polarity.B)
        dut.clock.step(1)
        polarity = if(polarity==1) 0 else 1
        
        dut.clock.step(50)

        dut.count.data.peek().litValue should be (i)
      }
      
      dut.clock.step(500)
      
      // Try not incrementing
      dut.en.data.poke(false.B)
      enDriver.drive

      dut.clear.data.poke(false.B)
      clearDriver.drive
      enDriver.finished
      clearDriver.finished
      

      while(dut.count.req.peek().litValue != polarity){
        dut.clock.step(10)
      }
      dut.count.ack.poke(polarity.B)
      dut.clock.step(1)
      polarity = if(polarity==1) 0 else 1

      dut.clock.step(50)

      dut.count.data.peek().litValue should be (255)
      
      
      dut.clock.step(500)
      
      // Try to Clear and run again
      dut.en.data.poke(false.B)
      enDriver.drive

      dut.clear.data.poke(true.B)
      clearDriver.drive
      enDriver.finished
      clearDriver.finished
      
      
      while(dut.count.req.peek().litValue != polarity){
        dut.clock.step(10)
      }
      dut.count.ack.poke(polarity.B)
      dut.clock.step(1)
      polarity = if(polarity==1) 0 else 1

      dut.clock.step(50)

      dut.count.data.peek().litValue should be (0)
      
      
      for (i <- 1 until 256){
        dut.en.data.poke(true.B)
        enDriver.drive
        
        dut.clear.data.poke(false.B)
        clearDriver.drive
        enDriver.finished
        clearDriver.finished


        while(dut.count.req.peek().litValue != polarity){
          dut.clock.step(10)
        }
        dut.count.ack.poke(polarity.B)
        dut.clock.step(1)
        polarity = if(polarity==1) 0 else 1
        
        dut.clock.step(50)

        dut.count.data.peek().litValue should be (i)
      }
      
    }
  }
}



// class OMuxTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
//   behavior of "Orion OMux Test"
//   
//   val annos = Seq(IcarusBackendAnnotation, WriteVcdAnnotation)
//   
//   val iters  = 20
//     
//   it should s"check OMux" in {
//     implicit val p = Parameters.empty
// 
//     test(LazyModule(new OMux()(p)).module).withAnnotations(annos) { dut =>
//       var polarity = 1
//       var first    = true
// 
//       dut.clock.setTimeout(9999)
// 
//       dut.reset.poke(true.B)
//       dut.clock.step(10)
//       dut.reset.poke(false.B)
//       dut.clock.step(10)
//       
//       for (i <- 0 until 10){
//         dut.a.req.poke(polarity.B)
//         dut.a.data.poke((255*polarity).U)
// 
//         dut.b.req.poke(polarity.B)
//         dut.b.data.poke(16.U)
// 
//         dut.sel.req.poke(polarity.B)
//         dut.sel.data.poke(false.B)
// 
//         while(dut.y.req.peek().litValue != polarity){
//           dut.clock.step(10)
//         }
//         dut.y.ack.poke(polarity.B)
//         dut.clock.step(1)
//         polarity = if(polarity==1) 0 else 1
//         
//         dut.clock.step(50)
//         first = false
// 
//         //0 should be (0)
//       }
//     }
//   }
// }
