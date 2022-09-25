package utopia.trove.database

import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.trove.model.partial.DatabaseVersionData
import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading database version recordings from the database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
case class DatabaseVersionFactory(table: Table) extends FromValidatedRowModelFactory[DatabaseVersion]
	with FromRowFactoryWithTimestamps[DatabaseVersion]
{
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model) = DatabaseVersion(model("id"),
		DatabaseVersionData(Version(model("version").getString), model(creationTimePropertyName)))
}

