package utopia.ambassador.model.post

import utopia.ambassador.model.enumeration.AuthCompletionType
import utopia.ambassador.model.enumeration.AuthCompletionType.Default
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.FromModelFactory
import utopia.metropolis.model.error.IllegalPostModelException

import scala.util.{Failure, Success}

object NewAuthPreparation extends FromModelFactory[NewAuthPreparation]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(model: Model[Property]) =
	{
		// task_id or non-empty task_ids is required
		val taskIds = model("task_id").int.map { Vector(_) }.getOrElse { model("task_ids").getVector.flatMap { _.int } }
		if (taskIds.isEmpty)
			Failure(new IllegalPostModelException("task_id or a non-empty task_ids array is required"))
		else
		{
			val state = model("state").getString
			// Redirect urls are expected to be within redirect_urls model
			val urlsModel = model("redirect_urls").getModel
			val redirectUrls = AuthCompletionType.values.flatMap { cType =>
				urlsModel(cType.keyName).string.map { cType -> _ }
			}.toMap
			Success(NewAuthPreparation(taskIds.toSet, state, redirectUrls))
		}
	}
}

/**
  * Posted when a new authentication attempt needs to be prepared.
  * Contains information concerning the upcoming attempt.
  * @author Mikko Hilpinen
  * @since 12.7.2021, v1.0
  */
case class NewAuthPreparation(taskIds: Set[Int], state: String = "",
                              redirectUrls: Map[AuthCompletionType, String] = Map())
{
	/**
	  * @return Whether this preparation is able to redirect the user regardless of authorization completion result
	  */
	def coversAllCompletionCases = redirectUrls.contains(Default) ||
		(redirectUrls.contains(AuthCompletionType.Success) && redirectUrls.contains(AuthCompletionType.Failure))
}
