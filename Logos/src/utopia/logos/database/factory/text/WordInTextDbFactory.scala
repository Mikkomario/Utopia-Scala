package utopia.logos.database.factory.text

import utopia.flow.generic.casting.ValueUnwraps._
import utopia.logos.database.props.text.StatementPlacementDbProps
import utopia.logos.database.storable.text.{StatementDbModel, WordDbModel, WordPlacementDbModel}
import utopia.logos.model.combined.text.WordInText
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Row, Table}
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, OrderBy}

import scala.util.{Success, Try}

/**
 * A factory used for reading text content by pulling words with placement information included
 *
 * @author Mikko Hilpinen
 * @since 28.02.2025, v0.5
 */
case class WordInTextDbFactory(linkProps: StatementPlacementDbProps)
	extends FromRowFactory[WordInText]
{
	// ATTRIBUTES   -------------------------
	
	private val wordModel = WordDbModel
	private val wordPlacementModel = WordPlacementDbModel
	private val statementModel = StatementDbModel
	
	override val joinedTables: Seq[Table] = Vector(wordPlacementModel.table, statementModel.table, linkProps.table)
	override val joinType: JoinType = Inner
	
	override lazy val selectTarget: SelectTarget = Vector[Column](
		wordModel.id, wordModel.text,
		wordPlacementModel.style, wordPlacementModel.orderIndex,
		statementModel.id,
		linkProps.parentId, linkProps.orderIndex)
	
	override val defaultOrdering: Option[OrderBy] = None
	
	
	// IMPLEMENTED  -------------------------
	
	override def table: Table = wordModel.table
	
	override def apply(row: Row): Try[WordInText] = {
		val word = row(wordModel.table)
		val wordPlacement = row(wordPlacementModel.table)
		val link = row(linkProps.table)
		
		// NB: Doesn't perform any validation
		Success(WordInText(word(wordModel.id), word(wordModel.text),
			DisplayStyle.fromValue(wordPlacement(wordPlacementModel.style)),
			row(statementModel.table)(statementModel.id), wordPlacement(wordPlacementModel.orderIndex),
			link(linkProps.parentId), link(linkProps.orderIndex)))
	}
}