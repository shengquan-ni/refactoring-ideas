package engine.core.worker.utils

import engine.core.worker.ControlPriority._

import scala.util.control.Breaks

trait PauseComponent {

  val pauseSupport:PauseSupport = new PauseSupport

  class PauseSupport{
    @volatile var dpThreadOuterControlLevel: Int = No
    @volatile var dPThreadInnerControlLevel: Int = No
    @volatile var isDpThreadRunning = false

    def trySetOuterPauseLevel(pauseLevel:Int):Boolean ={
      if(pauseLevel == No)return false
      if(pauseLevel > dpThreadOuterControlLevel) {
        dpThreadOuterControlLevel = pauseLevel
        true
      }else{
        false
      }
    }


    def tryReleaseOuterPauseLevel(resumeLevel:Int):Boolean ={
      if(resumeLevel == No)return false
      if(resumeLevel >= dpThreadOuterControlLevel) {
        dpThreadOuterControlLevel = No
        true
      }else{
        false
      }
    }

    def cleanupAndBreak(): Unit ={
      isDpThreadRunning = false
      Breaks.break()
    }

    def setPauseLevelAndBreak(pauseLevel: Int): Unit ={
      dPThreadInnerControlLevel = pauseLevel
      trySetOuterPauseLevel(pauseLevel)
      cleanupAndBreak()
    }


    def interruptOnOuterPaused(): Unit ={
      if(dpThreadOuterControlLevel > dPThreadInnerControlLevel){
        //Pause from outside: upgrade pause level
        dPThreadInnerControlLevel = dpThreadOuterControlLevel
        cleanupAndBreak()
      }
    }
  }

}
