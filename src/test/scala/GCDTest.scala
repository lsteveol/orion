package orion.test

import chisel3._
import chiseltest._
//import chiseltest.experimental.TestOptionBuilder._
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


class GCDTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Orion GCD Test"
  
  val annos = Seq(IcarusBackendAnnotation, WriteVcdAnnotation)
  
  val widths = List(8, 10, 12, 16, 20, 24)  //FIX Int
  val iters  = 20
  
  for(width <- widths){
  
    it should s"check GCD${width}" in {
      implicit val p = Parameters.empty



      test(LazyModule(new GCD(new GCDBundle(width), true)(p)).module).withAnnotations(annos) { dut =>
        var polarity = 1
        
        dut.clock.setTimeout(9999999)
        
        dut.reset.poke(true.B)
        dut.clock.step(10)
        dut.reset.poke(false.B)
        dut.clock.step(10)


        for(i <- 0 until iters){
          
          val ar = new scala.util.Random
          val br = new scala.util.Random
          var a  = 1 + ar.nextInt(scala.math.pow(2,width).toInt-1)
          var b  = 1 + br.nextInt(scala.math.pow(2,width).toInt-1)
          
          while(a == 0){ a = 1 + ar.nextInt(scala.math.pow(2,width).toInt-1) }
          while(b == 0){ b = 1 + br.nextInt(scala.math.pow(2,width).toInt-1) }
          
          dut.in.get.req.poke(polarity.B)
          dut.in.get.data.a.poke(a.U)
          dut.in.get.data.b.poke(b.U)
          while(dut.out.get.req.peek().litValue != polarity){
            dut.clock.step(10)
          }
          dut.clock.step(1)
          dut.out.get.ack.poke(polarity.B)
          
          val gcd = (BigInt(a) gcd BigInt(b)).toInt
          
          dut.out.get.data.a.peek().litValue should be (gcd)
          dut.out.get.data.b.peek().litValue should be (gcd)

          polarity = if (polarity==1) 0 else 1

          dut.clock.step(1000)
        }
      }
    }
  }
}
