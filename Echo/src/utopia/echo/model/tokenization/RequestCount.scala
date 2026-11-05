package utopia.echo.model.tokenization

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.combine.Combinable.SelfCombinable
import utopia.flow.operator.combine.{LinearScalable, Subtractable}
import utopia.flow.operator.sign.{SignOrZero, SignedOrZero}

object RequestCount extends FromModelFactoryWithSchema[RequestCount]
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Represents zero requests
	 */
	lazy val zero = RequestCount(0, TokenCount.zero)
	
	override lazy val schema: ModelDeclaration = ModelDeclaration("requests" -> IntType, "tokens" -> IntType)
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def fromValidatedModel(model: Model): RequestCount =
		apply(model("requests").getInt, model("tokens").getInt)
		
	
	// OTHER    ----------------------------
	
	/**
	 * @param tokens Number of tokens in a single request (input + estimated output)
	 * @return Request count matching a single request of that size
	 */
	def unit(tokens: TokenCount) = apply(1, tokens)
}

/**
 * Counts the number of LLM requests and tokens involved
 * @param requests The number of requests involved
 * @param tokens The number of tokens involved
 * @author Mikko Hilpinen
 * @since 05.11.2026, v1.6
 */
case class RequestCount(requests: Int, tokens: TokenCount)
	extends LinearScalable[RequestCount] with SelfCombinable[RequestCount] with Subtractable[RequestCount, RequestCount]
		with ModelConvertible with SignedOrZero[RequestCount]
{
	// IMPLEMENTED  ------------------------
	
	override def self: RequestCount = this
	
	override def sign: SignOrZero = tokens.sign
	override def zero: RequestCount = RequestCount.zero
	
	override def unary_- : RequestCount = RequestCount(-requests, -tokens)
	
	override def toModel: Model = Model.from("requests" -> requests, "tokens" -> tokens.value)
	override def toString: String = s"$tokens tokens across $requests requests"
	
	override def +(other: RequestCount): RequestCount = RequestCount(requests + other.requests, tokens + other.tokens)
	override def -(other: RequestCount): RequestCount = this + (-other)
	override def *(mod: Double): RequestCount = RequestCount((requests * mod).round.toInt, tokens * mod)
	
	
	// OTHER    ---------------------------
	
	def *(mod: Int) = RequestCount(requests * mod, tokens * mod)
}
