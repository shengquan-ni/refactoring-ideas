package engine.core.worker

object PauseLevel{
  final val No = 0
  final val BackPressure = 1
  final val Breakpoint = 2
  final val CoreException = 3
  final val Internal = 4
  final val User = 1024
  final val Recovery = 2048
  final val Forced = 9999
}
