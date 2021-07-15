package utopia.ambassador.model.post

import utopia.ambassador.model.enumeration.AuthCompletionType
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory

import scala.util.Success

object NewAuthPreparation extends FromModelFactory[NewAuthPreparation]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(model: Model[Property]) = Success(from(model))
	
	
	// OTHER    --------------------------
	
	/**
	  * Parses a new authentication preparation from the specified model (won't fail)
	  * @param model A model to parse
	  * @return Authentication preparation based on that model
	  */
	def from(model: Model[Property]) =
	{
		val state = model("state").getString
		val redirectUrls = AuthCompletionType.values.flatMap { cType =>
			model(cType.keyName).string.map { cType -> _ }
		}.toMap
		NewAuthPreparation(state, redirectUrls)
	}
}

/**
  * Posted when a new authentication attempt needs to be prepared.
  * Contains information concerning the upcoming attempt.
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class NewAuthPreparation(state: String = "", redirectUrls: Map[AuthCompletionType, String] = Map())
{
	/**
	  * @return Whether this preparation is able to redirect the user regardless of authorization completion result
	  */
	def coversAllCompletionCases = redirectUrls.contains(Default) ||
		(redirectUrls.contains(AuthCompletionType.Success) && redirectUrls.contains(AuthCompletionType.Failure))
}
