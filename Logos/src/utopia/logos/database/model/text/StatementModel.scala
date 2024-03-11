package utopia.logos.database.model.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.text.StatementFactory
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.Statement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing StatementModel instances and for inserting statements to the database
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object StatementModel extends DataInserter[StatementModel, Statement, StatementData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains statement delimiter id
	  */
	val delimiterIdAttName = "delimiterId"
	
	/**
	  * Name of the property that contains statement created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains statement delimiter id
	  */
	def delimiterIdColumn = table(delimiterIdAttName)
	
	/**
	  * Column that contains statement created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = StatementFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: StatementData) = apply(None, data.delimiterId, Some(data.created))
	
	override protected def complete(id: Value, data: StatementData) = Statement(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this statement was first made
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * 
		@param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character.
	  * @return A model containing only the specified delimiter id
	  */
	def withDelimiterId(delimiterId: Int) = apply(delimiterId = Some(delimiterId))
	
	/**
	  * @param id A statement id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with Statements in the database
  * @param id statement database id
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class StatementModel(id: Option[Int] = None, delimiterId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Statement]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StatementModel.factory
	
	override def valueProperties = {
		import StatementModel._
		Vector("id" -> id, delimiterIdAttName -> delimiterId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this statement was first made
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * 
		@param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated 
	  * with any character.
	  * @return A new copy of this model with the specified delimiter id
	  */
	def withDelimiterId(delimiterId: Int) = copy(delimiterId = Some(delimiterId))
}

