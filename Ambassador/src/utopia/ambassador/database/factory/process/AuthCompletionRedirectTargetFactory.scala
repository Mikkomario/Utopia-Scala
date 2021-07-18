package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.enumeration.AuthCompletionType.{Default, DenialOfAccess, Failure, Success}
import utopia.ambassador.model.partial.process.AuthCompletionRedirectTargetData
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading auth completion redirect urls from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthCompletionRedirectTargetFactory extends FromValidatedRowModelFactory[AuthCompletionRedirectTarget]
{
	override def table = AmbassadorTables.completionRedirectTarget
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		// Parses the result state filter from two parameters
		val resultStateFilter = model("resultStateFilter").boolean match
		{
			case Some(defined) =>
				if (defined)
					Success
				else if (model("isLimitedToDenials").getBoolean)
					DenialOfAccess
				else
					Failure
			case None => Default
		}
		AuthCompletionRedirectTarget(model("id"),
			AuthCompletionRedirectTargetData(model("preparationId"), model("url"), resultStateFilter))
	}
}
