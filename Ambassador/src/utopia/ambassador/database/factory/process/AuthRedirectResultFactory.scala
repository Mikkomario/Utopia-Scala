package utopia.ambassador.database.factory.process

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.enumeration.GrantLevel.{AccessDenied, AccessFailed, FullAccess, PartialAccess}
import utopia.ambassador.model.partial.process.AuthRedirectResultData
import utopia.ambassador.model.stored.process.AuthRedirectResult
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading authentication redirection results from the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
object AuthRedirectResultFactory extends FromValidatedRowModelFactory[AuthRedirectResult]
{
	override def table = AmbassadorTables.authRedirectResult
	
	override protected def fromValidatedModel(model: Model[Constant]) =
	{
		// Parses the grant level based on 3 parameters
		val grantLevel = {
			if (model("didReceiveCode").getBoolean)
			{
				if (model("didReceiveToken").getBoolean)
				{
					if (model("didReceiveFullScope").getBoolean)
						FullAccess
					else
						PartialAccess
				}
				else
					AccessFailed
			}
			else
				AccessDenied
		}
		AuthRedirectResult(model("id"),
			AuthRedirectResultData(model("redirectId"), grantLevel, model("created")))
	}
}
