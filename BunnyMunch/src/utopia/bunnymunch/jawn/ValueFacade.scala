package utopia.bunnymunch.jawn

import org.typelevel.jawn.Facade.SimpleFacade
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DoubleType, IntType, LongType}

/**
  * A facade for producing flow values
  * @author Mikko Hilpinen
  * @since 12.5.2020, v1
  */
object ValueFacade extends SimpleFacade[Value]
{
	// ATTRIBUTES   -------------------
	
	override val jnull = Value.empty
	
	override val jfalse = false
	override val jtrue = true
	
	
	// IMPLEMENTED  -------------------
	
	override def jarray(vs: List[Value]) = vs.toVector
	override def jobject(vs: Map[String, Value]) = Model.withConstants(vs.map { case (k, v) => Constant(k, v) })
	
	override def jnum(s: CharSequence, decIndex: Int, expIndex: Int) = {
		if (decIndex >= 0)
			s.toString withType DoubleType
		else if (expIndex > 0 || s.length() >= 10)
			s.toString withType LongType
		else
			s.toString withType IntType
	}
	
	override def jstring(s: CharSequence) = unescape(s)
	
	
	// OTHER    -----------------------
	
	private def unescape(chars: CharSequence): String = {
		val len = chars.length()
		val resultBuilder = new StringBuilder()
		var nextIndex = 0
		while (nextIndex < len) {
			val c = chars.charAt(nextIndex)
			nextIndex += 1
			
			if (nextIndex < len && c == '\\') {
				val c2 = chars.charAt(nextIndex)
				nextIndex += 1
				c2 match {
					case 'n' | 'r' => resultBuilder += '\n'
					case 't' => resultBuilder += '\t'
					case other =>
						resultBuilder += c
						resultBuilder += other
				}
			}
			else
				resultBuilder += c
		}
		resultBuilder.result()
	}
}
