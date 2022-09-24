package utopia.metropolis.model.post

import utopia.flow.collection.template.typeless
import utopia.flow.datastructure.template
import utopia.flow.generic.model
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, Property}
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.enumeration.ModelStyle

import scala.concurrent.duration.FiniteDuration

object NewSessionRequest extends SureFromModelFactory[NewSessionRequest]
{
	// ATTRIBUTES   ------------------------------
	
	/**
	  * The default new session request instance
	  */
	lazy val default = apply()
	
	
	// IMPLEMENTED  ------------------------------
	
	override def parseFrom(model: model.template.Model[Property]) =
		NewSessionRequest(model("model_style", "style").string.flatMap(ModelStyle.findForKey),
			model("duration_minutes", "duration").int.map { _.minutes },
			model("request_refresh_token"), model("revoke_previous"))
}

/**
  * Used for requesting / opening new sessions
  * @author Mikko Hilpinen
  * @since 19.2.2022, v2.0.2
  */
case class NewSessionRequest(modelStyle: Option[ModelStyle] = None, customDuration: Option[FiniteDuration] = None,
                             requestRefreshToken: Boolean = false, revokePrevious: Boolean = false)
	extends ModelConvertible
{
	override def toModel = Model(Vector("model_style" -> modelStyle.map { _.key },
		"duration_minutes" -> customDuration.map { _.toMinutes },
		"request_refresh_token" -> requestRefreshToken, "revoke_previous" -> revokePrevious))
}
