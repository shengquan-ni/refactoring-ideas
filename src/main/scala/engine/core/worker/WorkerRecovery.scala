package engine.core.worker

import java.io.{FileInputStream, FileOutputStream, PrintWriter}
import java.nio.file.{Files, Path, Paths}

import com.esotericsoftware.kryo.KryoException
import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import engine.core.InternalActor
import engine.core.control.ControlInputChannel
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.worker.WorkerRecovery.RecoveryPacket
import engine.message.ControlRecovery
import engine.message.ControlRecovery.RecoveryCompleted

import scala.collection.mutable

object WorkerRecovery{

  case class RecoveryPacket(cursor:Long, msg:InternalControlMessage)

}


trait WorkerRecovery extends ControlRecovery{
  this:InternalActor with ControlInputChannel with CoreProcessingUnit =>

  protected val messageToBeRecovered: mutable.Map[Long, Array[InternalControlMessage]] = mutable.LongMap[Array[InternalControlMessage]]()
  private val sentMessageIDs:mutable.HashSet[Long] = mutable.HashSet[Long]()
  private val stashedControlMessages:mutable.ArrayBuffer[InternalControlMessage] = mutable.ArrayBuffer[InternalControlMessage]()

  private val instantiator = new ScalaKryoInstantiator
  instantiator.setRegistrationRequired(false)
  private val kryo = instantiator.newKryo()

  private val outputFile: Path = Paths.get("D:\\"+amberID.toString+".out")
  private val inputFile: Path = Paths.get("D:\\"+amberID.toString+".in")

  private lazy val inputSerializer = new Output(new FileOutputStream("D:\\"+amberID.toString+".in",true))
  private lazy val outputSerializer = new PrintWriter(new FileOutputStream("D:\\"+amberID.toString+".out",true))

  override def saveInputControlMessage(msg: InternalControlMessage): Unit ={
    try {
      //println(s"Saved $msg when cursor = ${processedCount+generatedCount}")
      kryo.writeObject(inputSerializer,RecoveryPacket(processedCount+generatedCount,msg))
    }finally
      inputSerializer.flush()
  }

  def recoverInput(): Unit ={
    val input = new Input(new FileInputStream("D:\\"+amberID+".in"))
    var flag = true
    val messageSeq = mutable.ArrayBuffer[InternalControlMessage]()
    var prevCursor:Long = 0
    while(flag){
      try{
        val packet = kryo.readObject(input,classOf[RecoveryPacket])
        if(packet.cursor > 0){
          if(!messageToBeRecovered.contains(packet.cursor) && prevCursor != 0){
            messageToBeRecovered(prevCursor) = messageSeq.toArray
            messageSeq.clear()
          }
          messageSeq.addOne(packet.msg)
          prevCursor = packet.cursor
        }else{
          val msg = packet.msg
          //println(s"Recovered $msg when cursor = ${packet.cursor}")
          processControlMessageForRecovery(msg)
        }
      }catch{
        case e:KryoException =>
          input.close()
          flag = false
      }
    }
    if(messageSeq.nonEmpty){
      messageToBeRecovered(prevCursor) = messageSeq.toArray
      messageSeq.clear()
    }
    if(messageToBeRecovered.isEmpty){
      self ! RecoveryCompleted()
    }
  }


  def recoverOutput(): Unit ={
    val input = scala.io.Source.fromFile("D:\\"+amberID.toString+".out")
    val lines = input.getLines()
    sentMessageIDs ++= lines.map(i => i.toLong)
    input.close()
  }

  override def resetRecovery(): Unit ={
    if(Files.exists(outputFile)) {
      Files.delete(outputFile)
    }
    if(Files.exists(inputFile)) {
      Files.delete(inputFile)
    }
  }


  override def saveOutputControlMessageID(id: Long): Unit = {
    try{
      outputSerializer.write(id.toString)
      outputSerializer.write('\n')
    }finally
      outputSerializer.flush()
  }

  override def ifMessageHasSent(id: Long): Boolean = {
    val res = sentMessageIDs.contains(id)
    sentMessageIDs.remove(id)
    res
  }

  override def triggerRecovery(): Unit = {
    if(Files.exists(outputFile)) {
        recoverOutput()
    }
    if(Files.exists(inputFile)) {
        recoverInput()
    }
  }

}
