package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.text.statement.DbSingleStatement
import utopia.logos.model.factory.text.StatementFactoryWrapper
import utopia.logos.model.partial.text.StatementData
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object StoredStatement extends StoredFromModelFactory[StatementData, StoredStatement]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = StatementData
	
	override protected def complete(model: AnyModel, data: StatementData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a statement that has already been stored in the database
  * @param id id of this statement in the database
  * @param data Wrapped statement data
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class StoredStatement(id: Int, data: StatementData)
	extends StoredModelConvertible[StatementData] with FromIdFactory[Int, StoredStatement]
		with StatementFactoryWrapper[StatementData, StoredStatement]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this statement in the database
	  */
	def access = DbSingleStatement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: StatementData) = copy(data = data)
}

