package utopia.logos.database.access.text.statement.placement

import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.LogosTables
import utopia.logos.database.access.text.delimiter.AccessDelimiters
import utopia.logos.database.access.text.statement.{AccessStatementValues, FilterByStatement}
import utopia.logos.database.access.text.word.placement.{AccessWordPlacementValues, FilterByWordPlacement}
import utopia.logos.database.access.text.word.{AccessWordValues, FilterByWord}
import utopia.logos.database.access.url.link.placement.AccessLinkPlacements
import utopia.logos.database.factory.text.TextBuildingStatementDbReader
import utopia.logos.model.combined.text.TextBuildingStatement
import utopia.logos.model.partial.text.HasTextPlacementProps
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.many.{TargetingManyLike, TargetingManyRowsLike}

/**
 * Common trait for access points which yield some form of statement-placements
 *
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 * @author Mikko Hilpinen
 * @since 11.07.2025, v0.6
 */
trait AccessStatementPlacements[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends TargetingManyLike[A, Repr, One] with FilterStatementPlacements[Repr]
{
	// ATTRIBUTES   ------------------------
	
	lazy val joinedToStatements = join(LogosTables.statement)
	lazy val whereStatements = FilterByStatement(joinedToStatements)
	lazy val statements = AccessStatementValues(joinedToStatements)
	
	lazy val joinedToWordPlacements = joinedToStatements.join(LogosTables.wordPlacement)
	lazy val whereWordPlacements = FilterByWordPlacement(joinedToWordPlacements)
	lazy val wordPlacements = AccessWordPlacementValues(joinedToWordPlacements)
	
	lazy val joinedToWords = joinedToWordPlacements.join(LogosTables.word)
	lazy val whereWords = FilterByWord(joinedToWords)
	lazy val words = AccessWordValues(joinedToWords)
}

object AccessStatementPlacementRows
{
	// EXTENSIONS   -----------------------
	
	implicit class RichAccessStatementPlacementRows(val a: AccessStatementPlacementRows[HasTextPlacementProps, _, _])
		extends AnyVal
	{
		/**
		 * Converts all accessible statement placements to a full text mapping
		 * @param connection Implicit DB connection
		 * @return A map where keys are IDs of the text items and values are their full text content.
		 */
		def toTextMap(implicit connection: Connection) =
			a.pullWithText.groupBy { _._1.parentId }.view
				.mapValues { placements => placements.sortBy { _._1.orderIndex }.view.map { _._2 }.mkString }
				.toMap.withDefaultValue("")
	}
}
/**
 * Common trait for access points which yield some row-based forms of statement-placements
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 */
trait AccessStatementPlacementRows[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends AccessStatementPlacements[A, Repr, One] with TargetingManyRowsLike[A, Repr, One]
{
	/**
	 * Loads all accessible statement-placements and includes their text content
	 * @param connection Implicit DB connection
	 * @return All accessible statement placements, each accompanied by their full text content
	 */
	def pullWithText(implicit connection: Connection) = {
		// Pulls the items, including statement word data
		val placements = withTextBuildingStatements { (item, statement) => item -> statement }.pull
		val statementIds = placements.view.map { _._2.id }.toIntSet
		
		// Pulls delimiter and link data in order to complete the statements
		val delimiterIds = placements.view.flatMap { _._2.delimiterId }.toIntSet
		val delimiterPerId = AccessDelimiters(delimiterIds).idToText
		val linksPerStatement = AccessLinkPlacements.detailed.withinStatements(statementIds).pull
			.groupBy { _.statementId }.view
			.mapValues { _.map { placement => placement.orderIndex -> placement.link.toString } }
			.toMap
		
		// Combines the statement data in order to form the full text
		placements.map { case (placement, statement) =>
			// Combines the words, links and the delimiter
			val allWords = linksPerStatement.get(statement.id) match {
				case Some(links) => (statement.indexedWords ++ links).sortBy { _._1 }
				case None => statement.indexedWords
			}
			val text = s"${ allWords.iterator.map { _._2 }.mkString(" ") }${ delimiterPerId(statement.id) }"
			placement -> text
		}
	}
	
	/**
	 * Attaches text-building statements to accessible items
	 * @param f A function which combines the read items with the read text-building statements
	 * @tparam B Type of 'f' results
	 * @return Copy of this access which yields 'f' results
	 */
	def withTextBuildingStatements[B](f: (A, TextBuildingStatement) => B) =
		combineWithMany(TextBuildingStatementDbReader, neverEmpty = true) { (placement, statements) =>
			// Only links to one statement per link, because there's a one-to-one connection
			// Assumes that 'statements' always contains just that one element
			f(placement, statements.head)
		}
}