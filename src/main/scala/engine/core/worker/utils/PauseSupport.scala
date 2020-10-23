package engine.core.worker.utils

import engine.core.worker.PauseLevel._

import scala.util.control.Breaks


trait PauseSupport{

  @volatile private var dpThreadOuterPauseLevel: Int = No
  @volatile private var dPThreadInnerPauseLevel: Int = No
  @volatile private var isDpThreadRunning = false

  def trySetOuterPauseLevel(pauseLevel:Int):Boolean ={
    if(pauseLevel == No)return false
    if(pauseLevel > dpThreadOuterPauseLevel) {
      dpThreadOuterPauseLevel = pauseLevel
      true
    }else{
      false
    }
  }

  def isDpThreadReadyToStart: Boolean = dpThreadOuterPauseLevel == No && !isDpThreadRunning

  def isPauseLevelMatches:Boolean = dpThreadOuterPauseLevel == dPThreadInnerPauseLevel

  def waitDpThreadToPause(): Unit ={
    if(isDpThreadRunning){
      while(!isPauseLevelMatches){}
    }else{
      dPThreadInnerPauseLevel = dpThreadOuterPauseLevel
    }
  }

  def setDpThreadStarted(): Unit ={
    dPThreadInnerPauseLevel = No
    isDpThreadRunning = true
  }

  def setDpThreadExited(): Unit ={
    isDpThreadRunning = false
  }

  def waitDpThreadToStart(): Unit ={
    while(!isDpThreadRunning){}
  }

  def tryReleaseOuterPauseLevel(resumeLevel:Int):Boolean ={
    if(resumeLevel == No)return false
    if(resumeLevel >= dpThreadOuterPauseLevel) {
      dpThreadOuterPauseLevel = No
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
    dPThreadInnerPauseLevel = pauseLevel
    trySetOuterPauseLevel(pauseLevel)
    cleanupAndBreak()
  }


  def interruptOnOuterPaused(): Unit ={
    if(dpThreadOuterPauseLevel > dPThreadInnerPauseLevel){
      //Pause from outside: upgrade pause level
      dPThreadInnerPauseLevel = dpThreadOuterPauseLevel
      cleanupAndBreak()
    }
  }
}

