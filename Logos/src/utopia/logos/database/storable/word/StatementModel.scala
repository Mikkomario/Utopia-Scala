package utopia.logos.database.storable.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.word.StatementDbFactory
import utopia.logos.model.factory.word.StatementFactory
import utopia.logos.model.partial.word.StatementData
import utopia.logos.model.stored.word.Statement
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing StatementModel instances and for inserting statements to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with StatementDbModel", "v0.3")
object StatementModel 
	extends StorableFactory[StatementModel, Statement, StatementData] with StatementFactory[StatementModel]
		with FromIdFactory[Int, StatementModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val delimiterId = property("delimiterId")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains statement delimiter id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val delimiterIdAttName = "delimiterId"
	/**
	  * Name of the property that contains statement created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = StatementDbFactory
	
	/**
	  * Column that contains statement delimiter id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def delimiterIdColumn = table(delimiterIdAttName)
	/**
	  * Column that contains statement created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: StatementData) = apply(None, data.delimiterId, Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: StatementData) = Statement(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this statement was first made
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated
	  * with any character.
	  * @return A model containing only the specified delimiter id
	  */
	def withDelimiterId(delimiterId: Int) = apply(delimiterId = Some(delimiterId))
}

/**
  * Used for interacting with Statements in the database
  * @param id statement database id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with StatementDbModel", "v0.3")
case class StatementModel(id: Option[Int] = None, delimiterId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[Statement] with StatementFactory[StatementModel] with FromIdFactory[Int, StatementModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = StatementModel.factory
	
	override def valueProperties =
		Vector("id" -> id, StatementModel.delimiterId.name -> delimiterId, StatementModel.created.name -> created)
	
	override def withId(id: Int): StatementModel = copy(id = Some(id))
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this statement was first made
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated
	  * with any character.
	  * @return A new copy of this model with the specified delimiter id
	  */
	def withDelimiterId(delimiterId: Int) = copy(delimiterId = Some(delimiterId))
}

