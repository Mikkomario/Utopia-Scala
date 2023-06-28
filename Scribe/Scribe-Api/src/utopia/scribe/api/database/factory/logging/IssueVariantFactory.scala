package utopia.scribe.api.database.factory.logging

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading issue variant data from the DB
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueVariantFactory 
	extends FromValidatedRowModelFactory[IssueVariant] with FromRowFactoryWithTimestamps[IssueVariant]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.issueVariant
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueVariant(valid("id").getInt, IssueVariantData(valid("issueId").getInt, 
			Version(valid("version").getString), valid("errorId").int, 
			valid("details").notEmpty match { case Some(v) => JsonBunny.sureMunch(v.getString).getModel; case None => Model.empty },
			valid("created").getInstant))
}

