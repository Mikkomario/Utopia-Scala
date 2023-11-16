package utopia.scribe.core.model.combined.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.template.Extender
import utopia.scribe.core.model.partial.logging.IssueData
import utopia.scribe.core.model.stored.logging.Issue

object IssueInstances
{
	/**
	  * Ascending ordering that is based on issue severity (1), version (2) and latest occurrence time (3)
	  */
	implicit val ordering: Ordering[IssueInstances] = CombinedOrdering(
		Ordering.by { i: IssueInstances => i.severity },
		Ordering.by { i: IssueInstances => i.variants.iterator.map { _.version }.maxOption },
		Ordering.by { i: IssueInstances => i.latestOccurrence.map { _.lastOccurrence } }
	)
}

/**
  * Lists variants and occurrences of an issue, along with the base issue data
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
case class IssueInstances(issue: Issue, variants: Vector[IssueVariantInstances] = Vector()) extends Extender[IssueData]
{
	// ATTRIBUTES   ------------------
	
	/**
	  * The earliest recorded occurrence of this issue.
	  * None if no occurrences are recorded
	  */
	lazy val earliestOccurrence = variants.iterator.flatMap { _.earliestOccurrence }.minByOption { _.firstOccurrence }
	/**
	  * @return The latest occurrence of this issue.
	  *         None if no occurrences are recorded.
	  */
	lazy val latestOccurrence =
		variants.iterator.flatMap { _.latestOccurrence }.maxByOption { _.lastOccurrence }
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return The DB id of this issue
	  */
	def id = issue.id
	/**
	  * @return Ids of the included variants of this issue
	  */
	def variantIds = variants.map { _.id }
	
	/**
	  * @return The latest version in which this issue appeared.
	  *         None if there are no variants recorded.
	  */
	def latestVersion = variants.iterator.map { _.version }.maxOption
	
	/**
	  * @return Whether there are recorded occurrences for this issue
	  */
	def hasOccurred = variants.exists { _.occurrences.nonEmpty }
	/**
	  * @return Whether there are no recorded occurrences for this issue
	  */
	def hasNotOccurred = !hasOccurred
	
	/**
	  * @return Number of occurrences represented by this instance
	  */
	def numberOfOccurrences = variants.iterator.map { _.numberOfOccurrences }.sum
	
	/**
	  * @return The time period within which the recorded occurrences have occurred
	  */
	def occurrencePeriod =
		latestOccurrence.flatMap { latest => earliestOccurrence.map { earliest => Span(earliest, latest) } }
	/**
	  * @return The average duration between issue occurrences.
	  *         None if only 0-1 occurrences have been recorded.
	  */
	def averageOccurrenceInterval = {
		val count = numberOfOccurrences
		if (count > 1)
			Some((latestOccurrence.get.lastOccurrence - earliestOccurrence.get.firstOccurrence) / count)
		else
			None
	}
	
	/**
	  * @return Copy of this model without issue occurrence information
	  */
	def withoutOccurrences = VaryingIssue(issue, variants.map { _.variant })
	
	
	// IMPLEMENTED  ------------------
	
	override def wrapped: IssueData = issue.data
	
	override def toString = {
		val sb = new StringBuilder()
		sb ++= s"${issue.severity}: ${issue.context}"
		variants.oneOrMany match {
			case Left(variant) =>
				variant.details.notEmpty.foreach { d => sb ++= s"/$d" }
				sb ++= s"(${variant.version})"
			case Right(variants) =>
				sb ++= s"(${variants.size} variants, "
				val versions = variants.iterator.map { _.version }.minMax
				if (versions.isSymmetric)
					sb ++= versions.first.toString
				else
					sb ++= versions.mkString(" - ")
				sb += ')'
		}
		val count = numberOfOccurrences
		if (count > 1) {
			sb ++= s" - $count occurrences"
			averageOccurrenceInterval.foreach { interval => sb ++= s", once every ${interval.description}" }
		}
		latestOccurrence.foreach { latest =>
			sb ++= s" - Latest (${(Now - latest.lastOccurrence).description} ago): ${
				latest.errorMessages.headOption.getOrElse("No message")}"
		}
		
		sb.result()
	}
}
