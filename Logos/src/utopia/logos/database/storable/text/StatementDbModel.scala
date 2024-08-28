package utopia.logos.database.storable.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.text.StatementFactory
import utopia.logos.model.partial.text.StatementData
import utopia.logos.model.stored.text.Statement
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing StatementDbModel instances and for inserting statements to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object StatementDbModel 
	extends StorableFactory[StatementDbModel, Statement, StatementData] 
		with FromIdFactory[Int, StatementDbModel] with HasIdProperty with StatementFactory[StatementDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with delimiter ids
	  */
	lazy val delimiterId = property("delimiterId")
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = LogosTables.statement
	
	override def apply(data: StatementData) = apply(None, data.delimiterId, Some(data.created))
	
	/**
	  * @param created Time when this statement was first made
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated
	  * with any character.
	  * @return A model containing only the specified delimiter id
	  */
	override def withDelimiterId(delimiterId: Int) = apply(delimiterId = Some(delimiterId))
	override def withId(id: Int) = apply(id = Some(id))
	
	override protected def complete(id: Value, data: StatementData) = Statement(id.getInt, data)
}

/**
  * Used for interacting with Statements in the database
  * @param id statement database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class StatementDbModel(id: Option[Int] = None, delimiterId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, StatementDbModel] 
		with StatementFactory[StatementDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = StatementDbModel.table
	
	override def valueProperties = 
		Vector(StatementDbModel.id.name -> id, StatementDbModel.delimiterId.name -> delimiterId, 
			StatementDbModel.created.name -> created)
	
	/**
	  * @param created Time when this statement was first made
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	/**
	  * @param delimiterId Id of the delimiter that terminates this sentence. None if this sentence is not terminated
	  * with any character.
	  * @return A new copy of this model with the specified delimiter id
	  */
	override def withDelimiterId(delimiterId: Int) = copy(delimiterId = Some(delimiterId))
	override def withId(id: Int) = copy(id = Some(id))
}

