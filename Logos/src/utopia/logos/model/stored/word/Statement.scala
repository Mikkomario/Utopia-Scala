package utopia.logos.model.stored.word

import utopia.logos.database.access.single.word.statement.DbSingleStatement
import utopia.logos.model.factory.word.StatementFactory
import utopia.logos.model.partial.word.StatementData
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

import java.time.Instant

/**
  * Represents a statement that has already been stored in the database
  * @param id id of this statement in the database
  * @param data Wrapped statement data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class Statement(id: Int, data: StatementData) 
	extends StoredModelConvertible[StatementData] with StatementFactory[Statement] with FromIdFactory[Int, Statement]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this statement in the database
	  */
	def access = DbSingleStatement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withCreated(created: Instant) = copy(data = data.withCreated(created))
	
	override def withDelimiterId(delimiterId: Int) = copy(data = data.withDelimiterId(delimiterId))
	
	override def withId(id: Int) = copy(id = id)
}

