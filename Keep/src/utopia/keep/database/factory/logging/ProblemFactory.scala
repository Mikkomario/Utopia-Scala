package utopia.keep.database.factory.logging

import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.keep.database.KeepTables
import utopia.keep.model.enumeration.Severity
import utopia.keep.model.partial.logging.ProblemData
import utopia.keep.model.stored.logging.Problem
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
	
	override def table = KeepTables.problem
	
	override def apply(model: template.Model[Property]) = {
		table.validate(model).flatMap{ valid => 
			Severity.forId(valid("severity").getInt).map { severity => 
				Problem(valid("id").getInt, ProblemData(valid("context").getString, severity, 
					valid("created").getInstant))
			}
		}
	}
}

