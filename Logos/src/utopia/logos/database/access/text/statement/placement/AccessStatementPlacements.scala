package utopia.logos.database.access.text.statement.placement

import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.LogosTables
import utopia.logos.database.access.text.delimiter.AccessDelimiters
import utopia.logos.database.access.text.statement.{AccessStatementValues, FilterByStatement}
import utopia.logos.database.access.text.word.placement.{AccessWordPlacementValues, FilterByWordPlacement}
import utopia.logos.database.access.text.word.{AccessWordValues, FilterByWord}
import utopia.logos.database.factory.text.TextBuildingStatementDbReader
import utopia.logos.model.combined.text.TextBuildingStatement
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

/**
 * Common trait for access points which yield some row-based forms of statement-placements
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 */
trait AccessStatementPlacementRows[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends AccessStatementPlacements[A, Repr, One] with TargetingManyRowsLike[A, Repr, One]
{
	def pullWithText(implicit connection: Connection) = {
		val items = withTextBuildingStatements { (item, statements) => item -> statements }.pull
		// Pulls delimiter and link data in order to complete the statements
		val delimiterIds = items.iterator.flatMap { _._2 }.flatMap { _.delimiterId }.toIntSet
		val delimiterPerId = AccessDelimiters(delimiterIds).idToText
		
		// TODO: Pull links
	}
	
	/**
	 * Attaches text-building statements to accessible items
	 * @param f A function which combines the read items with the read text-building statements
	 * @tparam B Type of 'f' results
	 * @return Copy of this access which yields 'f' results
	 */
	def withTextBuildingStatements[B](f: (A, Seq[TextBuildingStatement]) => B) =
		combineWithMany(TextBuildingStatementDbReader, neverEmpty = true)(f)
}