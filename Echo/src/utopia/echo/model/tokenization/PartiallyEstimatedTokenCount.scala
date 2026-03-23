package utopia.echo.model.tokenization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.{FromModelFactory, FromValueFactory}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.flow.generic.model.template.ModelConvertible
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
	lazy val zero = confirmed(TokenCount.zero)
	
	
	// IMPLICIT ------------------------
	
	/**
	 * Wraps a token count estimate
	 * @param estimate Estimate to wrap
	 * @return A count that only consists of an estimate
	 */
	implicit def estimate(estimate: EstimatedTokenCount): PartiallyEstimatedTokenCount =
		apply(estimate, TokenCount.zero)
	
	
	// IMPLEMENTED  -------------------
	
	override def default: PartiallyEstimatedTokenCount = zero
	
	override def apply(model: HasProperties): Try[PartiallyEstimatedTokenCount] = {
		if (model.contains("estimate"))
			Success(apply(EstimatedTokenCount.getFromValue(model("estimate")), TokenCount(model("confirmed").getInt)))
		else if (model.contains("confirmed"))
			model.tryGet("confirmed") { _.tryInt }.map { tokens => confirmed(TokenCount(tokens)) }
		else
			EstimatedTokenCount(model).map(estimate)
	}
	override def fromValue(value: Value): Option[PartiallyEstimatedTokenCount] = value.castTo(ModelType, IntType) match {
		case Left(modelV) => modelV.model.flatMap { apply(_).toOption }
		case Right(intV) => intV.int.map { tokens => confirmed(TokenCount(tokens)) }
	}
	
	
	// OTHER    ------------------------
	
	/**
	 * @param tokenCount A token count counted as an estimate
	 * @return That token count as an partial estimate
	 */
	def estimate(tokenCount: TokenCount): PartiallyEstimatedTokenCount = estimate(EstimatedTokenCount.from(tokenCount))
	/**
	 * Wraps a fully confirmed token count
	 * @param tokenCount Token count to wrap. Should originate from an LLM / be 100% correct.
	 * @return A new token count consisting only of a confirmed part
	 */
	def confirmed(tokenCount: TokenCount) = apply(EstimatedTokenCount.zero, tokenCount)
	
	/**
	 * @param estimate Part of this token count that consists of an estimate
	 * @param confirmed Part of this token count that has been confirmed (i.e. originates from an LLM)
	 * @return A new partially estimated token count
	 */
	def apply(estimate: EstimatedTokenCount, confirmed: TokenCount) =
		new PartiallyEstimatedTokenCount(estimate, confirmed)
}

/**
 * A token count that consists of an estimated part, plus a measured (i.e. confirmed) part.
 * @author Mikko Hilpinen
 * @since 23.03.2025, v1.3
 * @param estimatePart Part of this token count that consists of an estimate
 * @param confirmedPart Part of this token count that has been confirmed (i.e. originates from an LLM)
 */
class PartiallyEstimatedTokenCount(val estimatePart: EstimatedTokenCount, val confirmedPart: TokenCount)
	extends TokenCount with TokenCountLike[PartiallyEstimatedTokenCount] with ModelConvertible
{
	// COMPUTED ----------------------------------
	
	/**
	 * @return Token count which includes a correction factor in the estimate part
	 */
	def corrected = confirmedPart + estimatePart.corrected
	
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
	
	override def value: Int = corrected.value
	
	override def zero: PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount.zero
	override def isZero: Boolean = confirmedPart.isZero && estimatePart.isZero
	
	override def unary_- : PartiallyEstimatedTokenCount = PartiallyEstimatedTokenCount(-estimatePart, -confirmedPart)
	
	override def toModel: Model = Model.from("estimate" -> estimatePart, "confirmed" -> confirmedPart.value)
	override def toString = if (isFullyConfirmed) confirmedPart.toString else s"~$corrected"
	
	override def +(other: TokenCount) = other match {
		case p: PartiallyEstimatedTokenCount =>
			PartiallyEstimatedTokenCount(estimatePart + p.estimatePart, confirmedPart + p.confirmedPart)
		case e: EstimatedTokenCount => mapEstimatePart { _ + e }
		case o => mapConfirmedPart { _ + o }
	}
	override def +(amount: Int) = mapConfirmedPart { _ + amount }
	override def *(mod: Double) = PartiallyEstimatedTokenCount(estimatePart * mod, confirmedPart * mod)
	
	override protected def withValue(value: Int): PartiallyEstimatedTokenCount =
		PartiallyEstimatedTokenCount.confirmed(TokenCount(value))
		
	
	// OTHER    ---------------------------------
	
	/**
	 * @param estimate Estimate part to assign to this count
	 * @return Copy of this count with the specified estimate part
	 */
	def withEstimatePart(estimate: EstimatedTokenCount) = PartiallyEstimatedTokenCount(estimate, confirmedPart)
	/**
	 * @param f A mapping function applied to this count's estimate part
	 * @return Copy of this count with mapped estimate part
	 */
	def mapEstimatePart(f: Mutate[EstimatedTokenCount]) =
		withEstimatePart(f(estimatePart))
		
	def withConfirmedPart(confirmed: TokenCount) = PartiallyEstimatedTokenCount(estimatePart, confirmed)
	def mapConfirmedPart(f: Mutate[TokenCount]) = withConfirmedPart(f(confirmedPart))
}