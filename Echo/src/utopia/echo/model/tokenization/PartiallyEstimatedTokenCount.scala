package utopia.echo.model.tokenization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.{FromModelFactory, FromValueFactory}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.Subtractable
import utopia.flow.operator.{MayBeZero, Reversible}
import utopia.flow.util.Mutate

import scala.language.implicitConversions
import scala.util.{Success, Try}

object PartiallyEstimatedTokenCount
	extends FromModelFactory[PartiallyEstimatedTokenCount] with FromValueFactory[PartiallyEstimatedTokenCount]
{
	// ATTRIBUTES   --------------------
	
	/**
	 * A zero token count
	 */
	lazy val zero = confirmed(0)
	
	
	// IMPLICIT ------------------------
	
	/**
	 * Wraps a token count estimate
	 * @param estimate Estimate to wrap
	 * @return A count that only consists of an estimate
	 */
	implicit def wrapEstimate(estimate: EstimatedTokenCount): PartiallyEstimatedTokenCount = apply(estimate, 0)
	
	
	// IMPLEMENTED  -------------------
	
	override def default: PartiallyEstimatedTokenCount = zero
	
	override def apply(model: ModelLike[Property]): Try[PartiallyEstimatedTokenCount] = {
		if (model.contains("estimate"))
			Success(apply(EstimatedTokenCount.getFromValue(model("estimate")), model("confirmed").getInt))
		else if (model.contains("confirmed"))
			model.tryGet("confirmed") { _.tryInt }.map(confirmed)
		else
			EstimatedTokenCount(model).map(wrapEstimate)
	}
	override def fromValue(value: Value): Option[PartiallyEstimatedTokenCount] = value.castTo(ModelType, IntType) match {
		case Left(modelV) => modelV.model.flatMap { apply(_).toOption }
		case Right(intV) => intV.int.map(confirmed)
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * Wraps a fully confirmed token count
	 * @param tokenCount Token count to wrap. Should originate from an LLM / be 100% correct.
	 * @return A new token count consisting only of a confirmed part
	 */
	def confirmed(tokenCount: Int) = apply(EstimatedTokenCount.zero, tokenCount)
}

/**
 * A token count that consists of an estimated part, plus a measured (i.e. confirmed) part.
 * @author Mikko Hilpinen
 * @since 23.03.2025, v1.3
 * @param estimatePart Part of this token count that consists of an estimate
 * @param confirmedPart Part of this token count that has been confirmed (i.e. originates from an LLM)
 */
case class PartiallyEstimatedTokenCount(estimatePart: EstimatedTokenCount, confirmedPart: Int)
	extends MayBeZero[PartiallyEstimatedTokenCount] with SelfCombinable[PartiallyEstimatedTokenCount]
		with Subtractable[PartiallyEstimatedTokenCount, PartiallyEstimatedTokenCount]
		with Reversible[PartiallyEstimatedTokenCount] with ModelConvertible
{
	// COMPUTED ----------------------------------
	
	/**
	 * @return Token count which includes a correction factor in the estimate part
	 */
	def corrected = estimatePart.corrected + confirmedPart
	
	/**
	 * @return True if this token count only consists of a confirmed part
	 */
	def isFullyConfirmed = estimatePart.isZero
	/**
	 * @return True if this time count contains at least some estimates / is not 100% confirmed
	 */
	def containsEstimate = estimatePart.nonZero
	
	
	// IMPLEMENTED  ------------------------------
	
	override def self: PartiallyEstimatedTokenCount = this
	
	override def zero: PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount.zero
	override def isZero: Boolean = confirmedPart == 0 && estimatePart.isZero
	
	override def unary_- : PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount(-estimatePart, -confirmedPart)
	
	override def toModel: Model = Model.from("estimate" -> estimatePart, "confirmed" -> confirmedPart)
	override def toString = if (isFullyConfirmed) confirmedPart.toString else s"~$corrected"
	
	override def +(other: PartiallyEstimatedTokenCount): PartiallyEstimatedTokenCount =
		PartiallyEstimatedTokenCount(estimatePart + other.estimatePart, confirmedPart + other.confirmedPart)
	override def -(other: PartiallyEstimatedTokenCount): PartiallyEstimatedTokenCount =
		PartiallyEstimatedTokenCount(estimatePart - other.estimatePart, confirmedPart - other.confirmedPart)
		
	
	// OTHER    ---------------------------------
	
	/**
	 * @param estimate Estimate part to assign to this count
	 * @return Copy of this count with the specified estimate part
	 */
	def withEstimatePart(estimate: EstimatedTokenCount) = copy(estimatePart = estimate)
	/**
	 * @param f A mapping function applied to this count's estimate part
	 * @return Copy of this count with mapped estimate part
	 */
	def mapEstimatePart(f: Mutate[EstimatedTokenCount]) =
		withEstimatePart(f(estimatePart))
}