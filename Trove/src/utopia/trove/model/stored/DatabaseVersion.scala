package utopia.trove.model.stored

import utopia.trove.model.partial.DatabaseVersionData
import utopia.vault.model.template.Stored

/**
  * A stored database version recording
  * @author Mikko Hilpinen
  * @since 19.9.2020, v1
  */
case class DatabaseVersion(id: Int, data: DatabaseVersionData) extends Stored[DatabaseVersionData, Int]
{
	override def toString = data.number.toString
}
