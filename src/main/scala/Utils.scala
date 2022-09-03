package freechips.rocketchip.diplomacy  
//!!! See below for why we label this as diplo package


import chisel3._
import chisel3.util._

import freechips.rocketchip.config.{Parameters, Field, Config}
import chisel3.internal.sourceinfo.SourceInfo
import freechips.rocketchip.util._

import java.io.{File, FileWriter}
object GenElabArts {
  def gen(prefix: String) {
    ElaborationArtefacts.files.foreach { case (extension, contents) =>
      //println(s"ext - ${extension} : cont - ${contents}")
      val f = new File(".", s"${prefix}.${extension}")
      val fw = new FileWriter(f)
      fw.write(contents())//note the ()
      fw.close
    }
  }
}

object OrionUtils {
  
  /**
    *   Get the LazyModule path name for SDC
    */
  def getPath(lazym: Option[LazyModule]): String = {
    lazym match {
      case None => "nothing!"
      case Some(lmodule) => {
        var path    = ""
        var lmod    = lmodule
        var top     = false
        var onlyTop = true

        while(!top){
          //this parent check is the reason we have to declare this as part of the diplomacy package
          //recursively goes through and captures all of the hier
          lmod.parent match {
            case None     => {
              top = true
            }
            case Some(lm) => {
              path = lmod.name + "." + path
              lmod = lm
              onlyTop = false
            }
          }
        }

        if(onlyTop) path = "top."
        path = path.toUpperCase()
        path = path.dropRight(1)    //remove the trailing '.'
        path
      }
    }
  }
}
