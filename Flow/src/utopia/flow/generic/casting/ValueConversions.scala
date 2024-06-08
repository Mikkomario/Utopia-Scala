package utopia.flow.generic.casting

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.{BooleanType, DoubleType, DurationType, FloatType, InstantType, IntType, LocalDateTimeType, LocalDateType, LocalTimeType, LongType, PairType, StringType, VectorType}
import utopia.flow.generic.model.template.ValueConvertible

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * This object offers implicit conversions from basic data types to the valueConvertible trait
  * (and from there, Value)
  * @author Mikko Hilpinen
  * @since 19.6.2017
  */
object ValueConversions
{
	implicit def flattenValueOption[V](option: Option[V])(implicit f: V => Value): Value =
		option.map(f).getOrElse(Value.empty)
	
	implicit def unwrapConvertible[C1](c: C1)(implicit f: C1 => ValueConvertible): Value = c.toValue
	
	/*
    implicit def automapCollection[V, C <: IterableOnce[V], To](c: C)(implicit f: V => ValueConvertible, cbf: CanBuildFrom[_, Value, To]): To =
    {
        val builder = cbf()
        c.foreach { builder += f(_) }
        builder.result()
    }*/
	
	implicit class ValueOfString(val s: String) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(if (s.isEmpty) None else Some(s), StringType)
	}
	
	implicit class ValueOfInt(val i: Int) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(i), IntType)
	}
	
	implicit class ValueOfDouble(val d: Double) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(d), DoubleType)
	}
	
	implicit class ValueOfFloat(val f: Float) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(f), FloatType)
	}
	
	implicit class ValueOfLong(val l: Long) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(l), LongType)
	}
	
	implicit class ValueOfBoolean(val b: Boolean) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(b), BooleanType)
	}
	
	implicit class ValueOfInstant(val i: Instant) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(i), InstantType)
	}
	
	implicit class ValueOfLocalDate(val d: LocalDate) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(d), LocalDateType)
	}
	
	implicit class ValueOfLocalTime(val t: LocalTime) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(t), LocalTimeType)
	}
	
	implicit class ValueOfLocalDateTime(val d: LocalDateTime) extends AnyVal with ValueConvertible
	{
		def toValue = new Value(Some(d), LocalDateTimeType)
	}
	
	implicit class ValueOfDuration(val d: FiniteDuration) extends AnyVal with ValueConvertible
	{
		override implicit def toValue: Value = new Value(Some(d), DurationType)
	}
	
	implicit class ValueOfSeq[V](val v: Seq[V])(implicit f: V => Value) extends ValueConvertible
	{
		def toValue = v match {
			case p: Pair[V] => new Value(Some(p.map(f)), PairType)
			case v: Vector[V] => new Value(Some(v.map(f)), VectorType)
			case i: IterableOnce[V] =>
				if (i.knownSize == 2) {
					val iter = i.iterator.map(f)
					new Value(Some(Pair(iter.next(), iter.next())), PairType)
				}
				else
					new Value(Some(Vector.from(i.iterator.map(f))), VectorType)
		}
	}
	
	implicit class ValueOfOption[V](val v: Option[V])(implicit f: V => ValueConvertible) extends ValueConvertible
	{
		override implicit def toValue: Value = v match {
			case Some(v) => v.toValue
			case None => Value.empty
		}
	}
	
	/*
	implicit class ValueOfPair[V](val p: Pair[V])(implicit f: V => Value) extends ValueConvertible
	{
		override implicit def toValue: Value = new Value(Some(p.map(f)), PairType)
	}*/
	
	/*
    implicit class ValueOfVectorConvertible[V](val v: Vector[V])(implicit f: V => ValueConvertible) extends ValueConvertible
    {
        def toValue =
        {
            val valueVector: Vector[Value] = v.map(f).map(_.toValue)
            new Value(Some(valueVector), VectorType)
        }
    }*/
	
	/*
    implicit class ValueOfOption[C1](val option: Option[C1])(implicit f: C1 => ValueConvertible) extends ValueConvertible
    {
        def toValue = if (option.isDefined) option.get.toValue else Value.empty()
    }*/
}
