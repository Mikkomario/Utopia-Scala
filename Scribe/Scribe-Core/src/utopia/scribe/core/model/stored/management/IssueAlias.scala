package utopia.scribe.core.model.stored.management

import utopia.scribe.core.model.factory.management.IssueAliasFactoryWrapper
import utopia.scribe.core.model.partial.management.IssueAliasData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}

object IssueAlias extends StandardStoredFactory[IssueAliasData, IssueAlias]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueAliasData
}

/**
  * Represents a issue alias that has already been stored in the database
  * @param id   id of this issue alias in the database
  * @param data Wrapped issue alias data
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class IssueAlias(id: Int, data: IssueAliasData) 
	extends StoredModelConvertible[IssueAliasData] with FromIdFactory[Int, IssueAlias] 
		with IssueAliasFactoryWrapper[IssueAliasData, IssueAlias]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: IssueAliasData) = copy(data = data)
}

