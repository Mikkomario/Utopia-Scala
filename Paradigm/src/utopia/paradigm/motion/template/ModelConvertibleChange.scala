package utopia.paradigm.motion.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ValueConvertible}
import utopia.flow.operator.MayBeZero
import utopia.flow.time.TimeExtensions._

/**
  * A common trait for change representation that can be converted to simple "amount" + "duration" -models
  * @author Mikko Hilpinen
  * @since 8.8.2022, v1.0
  */
trait ModelConvertibleChange[+A <: ValueConvertible with MayBeZero[A], +Repr <: Change[A, _]]
	extends Change[A, Repr] with ModelConvertible with MayBeZero[Repr]
{
	// COMPUTED --------------------------
	
	/**
	  * @return A zero representation of the amount used by this change
	  */
	def zeroAmount = amount.zero
	
	
	// IMPLEMENTED  ----------------------
	
	override def isZero = amount.isZero || duration.isInfinite
	
	override def toModel = duration.finite match {
		case Some(duration) => Model.from("amount" -> amount, "duration" -> duration)
		case None => Model.from("amount" -> zeroAmount)
	}
}
