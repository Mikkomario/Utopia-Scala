package utopia.trove.database

import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.trove.model.VersionNumber
import utopia.trove.model.partial.DatabaseVersionData
import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.factory.{FromRowFactoryWithTimestamps, FromValidatedRowModelFactory}

/**
  * Used for reading database version recordings from the database
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
case class DatabaseVersionFactory(table: Table) extends FromValidatedRowModelFactory[DatabaseVersion]
	with FromRowFactoryWithTimestamps[DatabaseVersion]
{
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model[Constant]) = DatabaseVersion(model("id"),
		DatabaseVersionData(VersionNumber.parse(model("version")), model(creationTimePropertyName)))
}

