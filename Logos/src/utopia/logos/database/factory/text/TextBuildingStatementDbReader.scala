package utopia.logos.database.factory.text

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.storable.text.{WordDbModel, WordPlacementDbModel}
import utopia.logos.model.combined.text.TextBuildingStatement
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.model.template.HasTablesAsTarget
import utopia.vault.nosql.read.DbReader
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, SqlTarget}

/**
 * An interface for reading text-building statements from the DB
 *
 * @author Mikko Hilpinen
 * @since 10.07.2025, v0.6
 */
object TextBuildingStatementDbReader extends DbReader[Seq[TextBuildingStatement]] with HasTablesAsTarget
{
	// ATTRIBUTES   -------------------
	
	private val delegate = StatementDbFactory
	
	private val wordPlacementModel = WordPlacementDbModel
	private val wordModel = WordDbModel
	
	override lazy val joinedTables: Seq[Table] = Pair(wordPlacementModel.table, wordModel.table)
	override val joinType: JoinType = Inner
	
	override lazy val target: SqlTarget = super.target
	override lazy val selectTarget: SelectTarget = StatementDbFactory.selectTarget +
		Vector(wordPlacementModel.style, wordPlacementModel.orderIndex, wordModel.text)
	
	
	// IMPLEMENTED  -------------------
	
	override def table: Table = delegate.table
	
	override def apply(stream: ResultStream): Seq[TextBuildingStatement] =
		stream.withLinkedRowsIterator(delegate)
			// Parses and combines the text for each statement
			.map { case (statement, rows) =>
				val text = rows
					.map { row =>
						val style = DisplayStyle.fromValue(row(wordPlacementModel.style))
						style(row(wordModel.text).getString) -> row(wordPlacementModel.orderIndex).getInt
					}
					.sortBy { _._2 }.iterator.map { _._1 }.mkString(" ")
				TextBuildingStatement(statement, text)
			}
			.toOptimizedSeq
}
