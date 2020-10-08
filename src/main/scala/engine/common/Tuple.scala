package engine.common


import engine.common.api.field.{IField, IntegerField, ListField}
import engine.common.api.schema.Schema

import collection.JavaConverters._
import scala.util.hashing.MurmurHash3

object Tuple{
  def apply(values: Any*): Tuple = new AmberTuple(values.toArray)
  def fromSeq(values: Seq[Any]): Tuple = new AmberTuple(values.toArray)
  def fromIterable(values: Iterable[Any]): Tuple = new AmberTuple(values.toArray)
  def fromJavaArray(values: Array[Any]) = new AmberTuple(values)
  def fromJavaList(values: java.util.List[Any]):Tuple = new AmberTuple(values.asScala.toArray)
  val empty = apply()


}

trait Tuple extends Serializable{
  def size: Int = length
  def length: Int
  def apply(i: Int): Any = get(i)
  def get(i: Int): Any
  def getAs[T](i: Int): T = get(i).asInstanceOf[T]
  def isNullAt(i: Int): Boolean = get(i) == null
  def getInstant(i: Int): java.time.Instant = getAs[java.time.Instant](i)
  def getSeq[T](i: Int): Seq[T] = getAs[Seq[T]](i)
  def getList[T](i: Int): List[T] = getSeq[T](i).toList
  def getMap[K, V](i: Int): scala.collection.Map[K, V] = getAs[Map[K, V]](i)
  def getLocalDate(i: Int): java.time.LocalDate = getAs[java.time.LocalDate](i)
  def getDate(i: Int): java.sql.Date = getAs[java.sql.Date](i)
  def getBigDecimal(i: Int): BigDecimal = getAs[BigDecimal](i)
  def getString(i: Int): String = getAs[String](i)
  def getDouble(i: Int): Double = getAnyValAs[Double](i)
  def getFloat(i: Int): Float = getAnyValAs[Float](i)
  def getLong(i: Int): Long = getAnyValAs[Long](i)
  def getInt(i: Int): Int = getAnyValAs[Int](i)
  def getShort(i: Int): Short = getAnyValAs[Short](i)
  def getByte(i: Int): Byte = getAnyValAs[Byte](i)
  def getBoolean(i: Int): Boolean = getAnyValAs[Boolean](i)

  def toArray():Array[Any]

  override def hashCode: Int = {
    var n = 0
    var h = MurmurHash3.seqSeed
    val len = length
    while (n < len) {
      h = MurmurHash3.mix(h, apply(n).##)
      n += 1
    }
    MurmurHash3.finalizeHash(h, n)
  }

  def toSeq: Seq[Any] = {
    val n = length
    val values = new Array[Any](n)
    var i = 0
    while (i < n) {
      values.update(i, get(i))
      i += 1
    }
    values.toSeq
  }


  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Tuple]) return false
    val other = o.asInstanceOf[Tuple]

    if (other eq null) return false

    if (length != other.length) {
      return false
    }

    var i = 0
    while (i < length) {
      if (isNullAt(i) != other.isNullAt(i)) {
        return false
      }
      if (!isNullAt(i)) {
        if(!get(i).equals(other.get(i)))
          return false
      }
      i += 1
    }
    true
  }

  def mkString:String = mkString(",")

  def mkString(sep: String): String = mkString("Tuple[", sep, "]")

  def mkString(start: String, sep: String, end: String): String = {
    val n = length
    val builder = new StringBuilder
    builder.append(start)
    if (n > 0) {
      builder.append(get(0))
      var i = 1
      while (i < n) {
        builder.append(sep)
        builder.append(get(i))
        i += 1
      }
    }
    builder.append(end)
    builder.toString()
  }

  override def toString: String = mkString

  private def getAnyValAs[T <: AnyVal](i: Int): T =
    if (isNullAt(i)) throw new NullPointerException(s"Value at index $i is null")
    else getAs[T](i)

}

