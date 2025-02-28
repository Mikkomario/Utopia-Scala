package utopia.logos.database.access.many.text.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.access.many.text.delimiter.DbDelimiters
import utopia.logos.database.access.many.text.statement.DbStatements
import utopia.logos.database.factory.text.WordInTextDbFactory
import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.logos.model.combined.text.WordInText
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.ViewManyByIntIds
import utopia.vault.sql.Condition

object ManyWordsInTextsAccess
{
	// OTHER    --------------------------
	
	/**
	 * @param linkModel Model used for interacting with text links
	 * @param condition Applied search condition
	 * @return Access to words in texts linked using the specified model, applying the specified condition
	 */
	def apply(linkModel: StatementPlacementDbProps, condition: Condition): ManyWordsInTextsAccess =
		apply(linkModel, Some(condition))
	/**
	 * @param linkModel Model used for interacting with text links
	 * @param condition Applied search condition (optional)
	 * @return Access to words in texts linked using the specified model, applying the specified condition
	 */
	def apply(linkModel: StatementPlacementDbProps, condition: Option[Condition]): ManyWordsInTextsAccess =
		apply(WordInTextDbFactory(linkModel), condition)
	/**
	 * @param factory Factory used for pulling the word data
	 * @param condition Applied access condition
	 * @return Access to words using that factory & condition combination
	 */
	def apply(factory: WordInTextDbFactory, condition: Condition): ManyWordsInTextsAccess =
		apply(factory, Some(condition))
	/**
	 * @param factory Factory used for pulling the word data
	 * @param condition Applied access condition. None if targeting all texts.
	 * @return Access to words using that factory & condition combination
	 */
	def apply(factory: WordInTextDbFactory, condition: Option[Condition]): ManyWordsInTextsAccess =
		_Access(factory, condition)
	
	
	// NESTED   --------------------------
	
	private case class _Access(factory: WordInTextDbFactory, accessCondition: Option[Condition])
		extends ManyWordsInTextsAccess
}

/**
 * Common trait for access points which yield words in their correct context
 *
 * @author Mikko Hilpinen
 * @since 28.02.2025, v0.5
 */
trait ManyWordsInTextsAccess
	extends ManyWordsAccessLike[WordInText, ManyWordsInTextsAccess] with ManyRowModelAccess[WordInText]
		with ViewManyByIntIds[ManyWordsInTextsAccess]
{
	// ABSTRACT ------------------------
	
	override def factory: WordInTextDbFactory
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return Model used for interacting with textual links
	 */
	protected def textLinkModel = factory.linkProps
	
	/**
	 * @param connection Implicit DB connection
	 * @return A map where keys are text ids and values are their contents as strings.
	 *         Defines an empty string as the default value.
	 */
	def textPerId(implicit connection: Connection) = {
		// Pulls the targeted words
		val words = pull
		// Pulls the referenced delimiters
		val statementDelimiterIds = DbStatements(words.view.map { _.statementId }.toIntSet).delimiterIdMap
		val delimiters = DbDelimiters(statementDelimiterIds.valuesIterator.toIntSet).idToTextMap
		
		// Forms the text content for each targeted text
		words.groupBy { _.textId }.view
			.mapValues { words =>
				// Forms text content for each statement
				words.groupBy { _.indexOfStatement }.view
					.mapValues { words =>
						val delimiter = statementDelimiterIds.get(words.head.statementId).flatMap(delimiters.get)
							.getOrElse("")
						s"${ words.sortBy { _.indexInStatement }.mkString(" ") }$delimiter"
					}
					// Orders and combines the text content within the text
					.toVector.sortBy { _._1 }.iterator.map { _._2 }.mkString
			}
			.toMap.withDefaultValue("")
	}
	
	
	// IMPLEMENTED  --------------------
	
	override protected def self: ManyWordsInTextsAccess = this
	
	override def apply(condition: Condition): ManyWordsInTextsAccess = ManyWordsInTextsAccess(factory, condition)
	
	
	// OTHER    ------------------------
	
	/**
	 * @param textId Id of the targeted text
	 * @return Access to that text's word content
	 */
	def withinText(textId: Int) = filter(textLinkModel.parentId <=> textId)
	/**
	 * @param textIds Ids of the targeted texts
	 * @return Access to the text/word content of those texts
	 */
	def withinTexts(textIds: IterableOnce[Int]) =
		filter(textLinkModel.parentId.in(textIds.toIntSet))
}
