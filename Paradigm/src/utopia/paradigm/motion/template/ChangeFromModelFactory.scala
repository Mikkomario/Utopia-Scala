package utopia.paradigm.motion.template

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{ModelValidationFailedException, Value}
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.operator.CanBeAboutZero
import utopia.flow.collection.CollectionExtensions._

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * A common trait for factories which parse change data from model data
  * @author Mikko Hilpinen
  * @since 8.8.2022, v1.0
  */
trait ChangeFromModelFactory[+A, Amount <: CanBeAboutZero[Amount, _]] extends FromModelFactory[A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param value An input value
	  * @return Amount from that value. Failure if value couldn't be parsed.
	  */
	protected def amountFromValue(value: Value): Try[Amount]
	
	/**
	  * @param amount Amount of change
	  * @param duration Duration of change
	  * @return A new change instance
	  */
	def apply(amount: Amount, duration: Duration): A
	
	
	// IMPLEMENTED  ----------------------
	
	override def apply(model: ModelLike[Property]) =
		model.findExisting("amount")
			.toTry { new ModelValidationFailedException(s"Required property 'amount' is missing from $model") }
			.flatMap { p => amountFromValue(p.value) }
			.flatMap { amount =>
				model("duration").duration match {
					case Some(duration) => Success(apply(amount, duration))
					case None =>
						// Unspecified duration is only allowed for "zero" values
						if (amount.isAboutZero)
							Success(apply(amount, Duration.Inf))
						else
							Failure(new ModelValidationFailedException(
								s"Required property 'duration' is missing from $model"))
				}
			}
}
