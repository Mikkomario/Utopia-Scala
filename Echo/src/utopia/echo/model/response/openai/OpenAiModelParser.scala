package utopia.echo.model.response.openai

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.{Alive, Dead, Flux}
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.generic.model.template.{ModelLike, Property}

import scala.util.Try

object OpenAiModelParser
{
	// OTHER    --------------------------
	
	/**
	  * @param left Left side model parser (primary)
	  * @param right Right side model parser (secondary)
	  * @tparam L Type of parsed left items
	  * @tparam R Type of parsed right items
	  * @return A parser that checks the model type for which parser to use
	  */
	def either[L, R](left: OpenAiModelParser[L], right: OpenAiModelParser[R]): OpenAiModelParser[Either[L, R]] =
		new EitherParser[L, R](left, right)
	
	/**
	  * Parses status value from an Open AI response model.
	  * Supports the following status values:
	  *     - "completed" => Alive
	  *     - "incomplete" | "failed" => Dead
	  *     - "in_progress" => Flux
	  *
	  * Defaults to Alive
	  *
	  * @param model Model to parse
	  * @return Interpreted status.
	  */
	def parseStatusFrom(model: AnyModel): SchrodingerState = model("status").getString match {
		case "incomplete" | "failed" => Dead
		case "in_progress" => Flux
		case _ => Alive
	}
	/**
	  * Interprets a status value from an Open AI response.
	  * Supports the following status values:
	  *     - "completed" => Alive
	  *     - "incomplete" | "failed" => Dead
	  *     - "in_progress" => Flux
	  *
	  * Defaults to Alive
	  *
	  * @param status Status string to interpret
	  * @return Interpreted status.
	  */
	def interpretStatus(status: String): SchrodingerState = status match {
		case "incomplete" | "failed" => Dead
		case "in_progress" => Flux
		case _ => Alive
	}
		
	
	// NESTED   --------------------------
	
	private class EitherParser[+L, +R](left: OpenAiModelParser[L], right: OpenAiModelParser[R])
		extends OpenAiModelParser[Either[L, R]]
	{
		override lazy val typeIdentifiers: Set[String] = left.typeIdentifiers ++ right.typeIdentifiers
		
		override def apply(model: ModelLike[Property]): Try[Either[L, R]] = {
			val typeIdentifier = model("type").getString
			if (right.typeIdentifiers.contains(typeIdentifier))
				right(model).map(Right.apply)
			else
				left(model).map(Left.apply)
		}
	}
}

/**
  * Common trait for interfaces that parse Open AI response models
  * @author Mikko Hilpinen
  * @since 04.04.2025, v1.3
  */
trait OpenAiModelParser[+A] extends FromModelFactory[A] with HasTypeIdentifiers
{
	// OTHER    --------------------------
	
	/**
	  * @param other Another parser
	  * @tparam R Type of parse results from the other parser
	  * @return A parser that parses using 'other' if the model's type property matches it.
	  */
	def ||[R](other: OpenAiModelParser[R]) = OpenAiModelParser.either(this, other)
	
	/**
	  * Parses status value from an Open AI response model.
	  * Supports the following status values:
	  *     - "completed" => Alive
	  *     - "incomplete" | "failed" => Dead
	  *     - "in_progress" => Flux
	  *
	  * Defaults to Alive
	  *
	  * @param model Model to parse
	  * @return Interpreted status.
	  */
	protected def parseStatusFrom(model: AnyModel): SchrodingerState = OpenAiModelParser.parseStatusFrom(model)
	/**
	  * Interprets a status value from an Open AI response.
	  * Supports the following status values:
	  *     - "completed" => Alive
	  *     - "incomplete" | "failed" => Dead
	  *     - "in_progress" => Flux
	  *
	  * Defaults to Alive
	  *
	  * @param status Status string to interpret
	  * @return Interpreted status.
	  */
	protected def interpretStatus(status: String): SchrodingerState = OpenAiModelParser.interpretStatus(status)
}
