package engine.core.worker.utils

import engine.breakpoints.LocalBreakpoint.LocalBreakpoint
import engine.common.Tuple
import engine.core.worker.ControlPriority._
import engine.core.worker.components.WorkerSendLayerComponent

import scala.util.control.Breaks

trait BreakpointComponent {
  this: WorkerMetadata with PauseComponent with WorkerSendLayerComponent =>

  val breakpointSupport:BreakpointSupport = new BreakpointSupport

  class BreakpointSupport{
    var userAttachedBreakpoints = new Array[LocalBreakpoint](0)

    def attachBreakpoint(breakpoint: LocalBreakpoint): Unit ={
      var i = 0
      Breaks.breakable {
        while (i < userAttachedBreakpoints.length) {
          if (userAttachedBreakpoints(i).id == breakpoint.id) {
            userAttachedBreakpoints(i) = breakpoint
            Breaks.break()
          }
          i += 1
        }
        userAttachedBreakpoints = userAttachedBreakpoints :+ breakpoint
      }
    }

    def removeBreakpoint(breakpointID:String): Unit ={
      val idx = userAttachedBreakpoints.indexWhere(_.id == breakpointID)
      if(idx != -1){
        userAttachedBreakpoints = userAttachedBreakpoints.take(idx)
      }
    }

    def validateOneTuple(tuple:Tuple):Unit ={
      var triggered = false
      for(i <- userAttachedBreakpoints){
        i.accept(tuple)
        if(i.isTriggered){
          triggered = true
          // sendLayer.sendTo(controller, BreakpointTriggered(i))
        }
      }
      if(triggered){
        pauseSupport.setPauseLevelAndBreak(Breakpoint)
      }
    }
  }

}
