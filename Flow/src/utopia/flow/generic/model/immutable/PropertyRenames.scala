package utopia.flow.generic.model.immutable

import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.generic.model.immutable.PropertyRenames.renamesWith
import utopia.flow.operator.MaybeEmpty

import scala.collection.View

object PropertyRenames
{
	// OTHER    ------------------------
	
	/**
	 * @param fromTo Original property name, plus the renamed version
	 * @return A set containing only that mapping
	 */
	def apply(fromTo: (String, String)) = new PropertyRenames(Single(fromTo))
	/**
	 * @param first First renaming to apply (old -> new)
	 * @param second Second renaming toa apply
	 * @param more More renamings
	 * @return A combination of the specified renamings
	 */
	def apply(first: (String, String), second: (String, String), more: (String, String)*): PropertyRenames =
		new PropertyRenames(Pair[Pair[String]](first, second) ++ more.iterator.map(Pair.tupleToPair))
	
	/**
	 * @param renames Renamings to apply, where the first values are the original property names and the
	 *                second values are the renamed versions.
	 * @return Renames based on the specified values
	 */
	def apply(renames: IterableOnce[Pair[String]]) = new PropertyRenames(OptimizedIndexedSeq.from(renames))
	
	private def renamesWith(renames: Seq[Pair[String]], newRename: Pair[String]) = {
		renames.iterator.zipWithIndex.find { _._1.second == newRename.first } match {
			case Some((original, index)) =>
				val remainingRenamesView = View.concat(renames.view.take(index), renames.view.drop(index + 1))
					.filterNot { _.second == newRename.second }
				OptimizedIndexedSeq.concat(remainingRenamesView, Single(Pair(original.first, newRename.second)))
			
			case None => renames.filterNot { _.second == newRename.second } :+ newRename
		}
	}
}

/**
 * Used for renaming model properties without modifying the underlying model
 * @author Mikko Hilpinen
 * @since 24.10.2025, v2.7
 */
class PropertyRenames(private val renames: Seq[Pair[String]]) extends MaybeEmpty[PropertyRenames]
{
	// ATTRIBUTES   ----------------------
	
	private lazy val newToOld = renames.iterator.map { p => p.second.toLowerCase -> p.first }.toMap
	private lazy val oldToNew = renames.iterator.map { p => p.first.toLowerCase -> p.second }.toMap
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: PropertyRenames = this
	override def isEmpty: Boolean = renames.isEmpty
	
	
	// OTHER    -------------------------
	
	/**
	 * @param renamed A potentially renamed (external) property name
	 * @return The matching original property name
	 */
	def original(renamed: String) = originalOption(renamed).getOrElse(renamed)
	/**
	 * @param renamed A potentially renamed (external) property name
	 * @return The matching original property name. None if there was no rename with the specified value.
	 */
	def originalOption(renamed: String) = newToOld.get(renamed.toLowerCase)
	/**
	 * @param original An original (internal) property name
	 * @return A potentially renamed version of that name
	 */
	def renamed(original: String) = renamedOption(original).getOrElse(original)
	/**
	 * @param original An original (internal) property name
	 * @return A potentially renamed version of that name. None if that property has no renaming.
	 */
	def renamedOption(original: String) = oldToNew.get(original.toLowerCase)
	
	/**
	 * @param renamed A renamed property
	 * @return Whether these renames yield that rename for some of the original properties
	 */
	def yields(renamed: String) = newToOld.contains(renamed.toLowerCase)
	/**
	 * @param original An original property name
	 * @return Whether these renames refer to that property
	 */
	def refersTo(original: String) = oldToNew.contains(original.toLowerCase)
	
	/**
	 * @param renaming A new renaming to add to this set.
	 *                 The first value is the original (internal) property name.
	 *                 The second value is the new (external) property name.
	 * @return A copy of these renames with the specified one added
	 */
	def +(renaming: Pair[String]) = new PropertyRenames(renamesWith(renames, renaming))
	/**
	 * @param renaming A new renaming to add to this set.
	 *                 The first value is the original (internal) property name.
	 *                 The second value is the new (external) property name.
	 * @return A copy of these renames with the specified one added
	 */
	def +(renaming: (String, String)): PropertyRenames = this + Pair.tupleToPair(renaming)
	
	/**
	 * @param newRenames New mappings to add to this set.
	 *                   The first values are the original (internal) property names.
	 *                   The second values are the new (external) property names.
	 * @return A copy of these renames with the specified renames included
	 */
	def ++(newRenames: IterableOnce[Pair[String]]) =
		new PropertyRenames(newRenames.iterator.foldLeft(renames)(renamesWith))
	/**
	 * @param other New mappings to add to this set.
	 * @return A copy of these renames with the specified renames included
	 */
	def ++(other: PropertyRenames): PropertyRenames = this ++ other.renames
}
