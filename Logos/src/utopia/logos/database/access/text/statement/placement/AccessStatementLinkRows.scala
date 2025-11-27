package utopia.logos.database.access.text.statement.placement

import utopia.flow.collection.CollectionExtensions._
import utopia.logos.database.access.text.delimiter.AccessDelimiters
import utopia.logos.database.access.url.link.placement.AccessLinkPlacements
import utopia.logos.database.factory.text.TextBuildingStatementDbReader
import utopia.logos.model.combined.text.TextBuildingStatement
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.many.{TargetingManyLike, TargetingManyRowsLike}

/**
 * Common trait for access points which yield some row-based items that contain direct (one-to-one) statement links
 * @tparam A Type of pulled items
 * @tparam Repr Type of this access point
 * @tparam One Type of the access point which targets only one item at a time
 */
trait AccessStatementLinkRows[+A, +Repr <: TargetingManyLike[_, Repr, _], +One]
	extends TargetingManyRowsLike[A, Repr, One]
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
			val delimiter = statement.delimiterId.flatMap(delimiterPerId.get).getOrElse("")
			val text = s"${ allWords.iterator.map { _._2 }.mkString(" ") }$delimiter"
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