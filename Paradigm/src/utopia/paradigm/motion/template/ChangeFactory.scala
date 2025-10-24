package utopia.paradigm.motion.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.operator.MayBeAboutZero
import utopia.flow.time.{Duration, TimeUnit}

import scala.util.{Failure, Success, Try}

/**
  * A common trait for factories which parse change data from model data
  * @author Mikko Hilpinen
  * @since 8.8.2022, v1.0
  */
trait ChangeFactory[+A, Amount <: MayBeAboutZero[Amount, _]] extends FromModelFactory[A]
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
	
	override def apply(model: HasProperties) =
		model.existingProperty("amount")
			.toTry { new IllegalArgumentException(s"Required property 'amount' is missing from $model") }
			.flatMap { p => amountFromValue(p.value) }
			.flatMap { amount =>
				model("duration").duration match {
					case Some(duration) => Success(apply(amount, duration))
					case None =>
						// Unspecified duration is only allowed for "zero" values
						if (amount.isAboutZero)
							Success(apply(amount, Duration.infinite))
						else
							Failure(new IllegalArgumentException(
								s"Required property 'duration' is missing from $model"))
				}
			}
			
	// OTHER    -------------------------
	
	/**
	 * @param amount The amount of change in a single time unit
	 * @param timeUnit Unit of time used (implicit)
	 * @return A new change over one time unit
	 */
	def of(amount: Amount)(implicit timeUnit: TimeUnit): A = apply(amount, timeUnit)
	/**
	 * @param amount The amount of change in a single time unit
	 * @param unit Unit of time used
	 * @return A new change over one time unit
	 */
	def apply(amount: Amount, unit: TimeUnit): A = apply(amount, unit.unit)
}
