package utopia.scribe.database.factory.logging

import utopia.flow.datastructure.template
import utopia.flow.generic.model.template.{Model, Property}
import utopia.scribe.database.ScribeTables
import utopia.scribe.model.enumeration.Severity
import utopia.scribe.model.partial.logging.ProblemData
import utopia.scribe.model.stored.logging.Problem
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromRowModelFactory

/**
  * Used for reading Problem data from the DB
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object ProblemFactory extends FromRowModelFactory[Problem] with FromRowFactoryWithTimestamps[Problem]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.problem
	
	override def apply(model: Model[Property]) = {
		table.validate(model).flatMap{ valid => 
			Severity.forId(valid("severity").getInt).map { severity => 
				Problem(valid("id").getInt, ProblemData(valid("context").getString, severity, 
					valid("created").getInstant))
			}
		}
	}
}

