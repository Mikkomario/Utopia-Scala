package utopia.echo.model.tokenization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.{FromModelFactoryWithSchema, FromValueFactory}
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.{IntType, ModelType}
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.Subtractable
import utopia.flow.operator.{MayBeZero, Reversible}

import scala.language.implicitConversions

object EstimatedTokenCount extends FromModelFactoryWithSchema[EstimatedTokenCount] with FromValueFactory[EstimatedTokenCount]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val schema = ModelDeclaration("raw" -> IntType, "corrected" -> IntType)
	
	/**
	 * A zero tokens -estimate
	 */
	lazy val zero = apply(0)
	
	
	// IMPLICIT -----------------------------
	
	implicit def autoExtract(c: EstimatedTokenCount): Int = c.corrected
	
	
	// IMPLEMENTED  ------------------------
	
	override def default: EstimatedTokenCount = zero
	
	override protected def fromValidatedModel(model: Model): EstimatedTokenCount =
		apply(model("raw").getInt, model("corrected").getInt)
	
	override def fromValue(value: Value): Option[EstimatedTokenCount] = value.castTo(ModelType, IntType) match {
		case Left(modelV) => modelV.model.flatMap { apply(_).toOption }
		case Right(intV) => intV.int.map(apply)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Creates a new token count without applying a corrective modifier
	 * @param noCorrection Value to wrap without applying corrective modifications
	 * @return A new estimated token count wrapping the specified value
	 */
	def apply(noCorrection: Int): EstimatedTokenCount = apply(noCorrection, noCorrection)
	
	/**
	 * @param raw Raw estimate
	 * @param correctionMod Correction modifier to apply
	 * @return A new estimate wrapping the specified raw value & applying the specified correction modifier
	 */
	def withCorrectionMod(raw: Int, correctionMod: Double) =
		apply(raw, (raw * correctionMod).round.toInt)
}

/**
 * Contains 2 token count values:
 *      1. Heuristically calculated token count
 *      1. A value that has been corrected based on historical calculations & feedback
 * @author Mikko Hilpinen
 * @since 23.03.2025, v1.3
 */
case class EstimatedTokenCount(raw: Int, corrected: Int)
	extends MayBeZero[EstimatedTokenCount] with SelfCombinable[EstimatedTokenCount]
		with Reversible[EstimatedTokenCount] with Subtractable[EstimatedTokenCount, EstimatedTokenCount]
		with ModelConvertible
{
	// IMPLEMENTED  ---------------------------
	
	override def self: EstimatedTokenCount = this
	
	override def zero: EstimatedTokenCount = EstimatedTokenCount.zero
	override def isZero: Boolean = raw == 0
	
	override def unary_- : EstimatedTokenCount = EstimatedTokenCount(-raw, -corrected)
	
	override def toModel: Model = Model.from("raw" -> raw, "corrected" -> corrected)
	
	override def +(other: EstimatedTokenCount): EstimatedTokenCount =
		EstimatedTokenCount(raw + other.raw, corrected + other.corrected)
	override def -(other: EstimatedTokenCount): EstimatedTokenCount =
		EstimatedTokenCount(raw - other.raw, corrected - other.corrected)
	
	
	// IMPLEMENTED  --------------------------
	
	override def toString = if (raw == 0) "0" else s"~$corrected"
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param corrected A new corrected token count
	 * @return Copy of this token count with the specified corrected value
	 */
	def withCorrected(corrected: Int) = copy(corrected = corrected)
	
	/**
	 * @param correctionMod A new correction modifier to apply
	 * @return A copy of this token count that applies the specified correction modifier
	 */
	def withCorrectionModifier(correctionMod: Double) =
		withCorrected((raw * correctionMod).round.toInt)
}