package utopia.bunnymunch.jawn

import org.typelevel.jawn.Facade.SimpleFacade
import utopia.flow.collection.value.typeless.{Constant, Value}
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{DoubleType, IntType, LongType}
import utopia.flow.generic.ValueConversions._

/**
  * A facade for producing flow values
  * @author Mikko Hilpinen
  * @since 12.5.2020, v1
  */
object ValueFacade extends SimpleFacade[Value]
{
	override def jarray(vs: List[Value]) = vs.toVector
	
	override def jobject(vs: Map[String, Value]) = Model.withConstants(vs.map { case (k, v) => Constant(k, v) })
	
	override def jnull = Value.empty
	
	override def jfalse = false
	
	override def jtrue = true
	
	override def jnum(s: CharSequence, decIndex: Int, expIndex: Int) =
	{
		if (decIndex >= 0)
			s.toString withType DoubleType
		else if (expIndex > 0 || s.length() >= 10)
			s.toString withType LongType
		else
			s.toString withType IntType
	}
	
	override def jstring(s: CharSequence) = s.toString
}
