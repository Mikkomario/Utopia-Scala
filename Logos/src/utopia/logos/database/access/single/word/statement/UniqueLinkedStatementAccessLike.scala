package utopia.logos.database.access.single.word.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.single.text.statement.UniqueStatementAccessLike
import utopia.logos.model.cached.StatementLinkDbConfig
import utopia.logos.model.template.PlacedFactory
import utopia.vault.database.Connection
import utopia.vault.nosql.view.FilterableView

/**
  * Common trait for access points which return linked statements
  * @author Mikko Hilpinen
  * @since 31.07.2024
  */
@deprecated("Replaced with UniquePlacedStatementAccessLike", "v0.3")
trait UniqueLinkedStatementAccessLike[+A, +Repr] 
	extends UniqueStatementAccessLike[A, Repr] with FilterableView[Repr] with PlacedFactory[Repr]
{
	// ABSTRACT	--------------------
	
	/**
	  * Configurations used when interacting with the text statement links in the DB
	  */
	protected def linkConfig: StatementLinkDbConfig
	
	
	// COMPUTED	--------------------
	
	/**
	  * Id of the text linked to the accessible statement. None if no statement was accessible.
	  * @param connection Implicit DB connection
	  */
	def textId(implicit connection: Connection) = pullColumn(linkConfig.textIdColumn).int
	
	/**
	  * Position index of the accessible statement within the linked text. None if no statement was accessible.
	  * @param connection Implicit DB connection
	  */
	def orderIndex(implicit connection: Connection) = pullColumn(linkConfig.orderIndexColumn).int
	
	
	// IMPLEMENTED	--------------------
	
	override def at(orderIndex: Int): Repr = filter(linkConfig.orderIndexColumn <=> orderIndex)
	
	
	// OTHER	--------------------
	
	/**
	  * @param textId Id of the targeted text
	  * @return Conditional copy of this access point, 
	  * which only returns the item if it is linked to the specific text
	  */
	def linkedToText(textId: Int) = filter(linkConfig.textIdColumn <=> textId)
}

