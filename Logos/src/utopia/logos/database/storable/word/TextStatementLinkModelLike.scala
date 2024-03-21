package utopia.logos.database.storable.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.word.{TextStatementLinkDbFactory, TextStatementLinkDbFactoryLike}
import utopia.logos.model.stored.word.TextStatementLink
import utopia.logos.model.template.StatementLinkFactory
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.{FromIdFactory, HasId}

/**
 * A DB model used for interacting with text statement links in a table
 *
 * @author Mikko Hilpinen
 * @since 14/03/2024, v1.0
 */
trait TextStatementLinkModelLike[+Repr]
	extends StorableWithFactory[TextStatementLink] with StatementLinkFactory[Repr]
		with FromIdFactory[Int, Repr] with HasId[Option[Int]]
{
	// ABSTRACT -----------------------------
	
	override def factory: TextStatementLinkDbFactory
	
	/**
	 * @return Targeted text id
	 */
	def textId: Option[Int]
	/**
	 * @return Targeted statement id
	 */
	def statementId: Option[Int]
	/**
	 * @return Targeted order index
	 */
	def orderIndex: Option[Int]
	
	/**
	 * @return Properties to assign to the DB row besides id, text id, statement id and order index
	 */
	protected def additionalValueProperties: Iterable[(String, Value)]
	
	/**
	 * @param id New id to assign (default = current)
	 * @param textId New text id to assign (default = current)
	 * @param statementId New statement id to assign (default = current)
	 * @param orderIndex New order index to assign (default = current)
	 * @return Copy of this item with the specified properties (default = current)
	 */
	protected def buildCopy(id: Option[Int] = id, textId: Option[Int] = textId, statementId: Option[Int] = statementId,
	                        orderIndex: Option[Int] = orderIndex): Repr
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return The configurations used when interacting with the database
	 */
	def config = factory.config
	
	
	// IMPLEMENTED  -------------------------
	
	override def valueProperties: Iterable[(String, Value)] = {
		val c = config
		Vector[(String, Value)]("id" -> id, c.textIdAttName -> textId, c.statementIdAttName -> statementId,
			c.orderIndexAttName -> orderIndex) ++ additionalValueProperties
	}
	
	override def withId(id: Int) = buildCopy(id = Some(id))
	override def withTextId(textId: Int) = buildCopy(textId = Some(textId))
	override def withStatementId(statementId: Int) = buildCopy(statementId = Some(statementId))
	override def at(orderIndex: Int) = buildCopy(orderIndex = Some(orderIndex))
}