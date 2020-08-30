package utopia.reflection.component.template.input

import scala.collection.BuildFrom

object Selectable
{
	implicit class OptionalSelectable[A, CP <: Iterable[A]](val s: Selectable[Option[A], CP]) extends AnyVal
	{
		/**
		  * @param item The item to be selected
		  */
		def selectOne(item: A) = s.select(Some(item))
		
		/**
		  * Selects the first available item
		  */
		def selectFirst() = s.select(s.content.headOption)
		
		/**
		  * If there isn't a value selected, selects the first one available
		  */
		def selectAny() = if (!s.isDefined) selectFirst()
		
		/**
		  * Clears any selection
		  */
		def selectNone() = s.select(None)
		
		/**
		 * Selects the first item that satisfies specified condition
		 * @param f A search condition
		 */
		def selectFirstWhere(f: A => Boolean) = s.content.find(f).foreach { selectOne }
	}
	
	implicit class MultiSelectable[A, CS <: Iterable[A], CP <: Iterable[A]](val s: Selectable[CS, CP]) extends AnyVal
	{
		/**
		  * If no item is selected, selects the first item
		  * @param bf Build from (implicit)
		  */
		def selectAny()(implicit bf: BuildFrom[Vector[A], A, CS]) = if (!s.isSelected) selectFirst()
		
		/**
		  * Selects the first item in this selectable
		  * @param bf Build from (implicit)
		  */
		def selectFirst()(implicit bf: BuildFrom[Vector[A], A, CS]) = s.selected.headOption.foreach(selectOne)
		
		/**
		  * Selects all currently available items
		  * @param bf Build from (implicit)
		  */
		def selectAll()(implicit bf: BuildFrom[CP, A, CS]) = selectMany(s.content)
		
		/**
		  * Selects no items
		  * @param bf Build from (implicit)
		  */
		def clearSelection()(implicit bf: BuildFrom[Vector[A], A, CS]) = selectMany(Vector[A]())
		
		/**
		  * Selects exactly one item
		  * @param item Target item
		  * @param bf Build from (implicit)
		  */
		def selectOne(item: A)(implicit bf: BuildFrom[Vector[A], A, CS]) = selectMany(Vector(item))
		
		/**
		 * Selects the items that satisfy the specified search condition
		 * @param f A search condition
		 * @param bf Build from (implicit)
		 */
		def selectWhere(f: A => Boolean)(implicit bf: BuildFrom[Iterable[A], A, CS]) = selectMany(s.content.filter(f))
		
		/**
		  * Selects multiple items
		  * @param many Items
		  * @param bf Build from (implicit)
		  * @tparam C The type of item collection
		  */
		def selectMany[C <: IterableOnce[A]](many: C)(implicit bf: BuildFrom[C, A, CS]) = select(many)
		
		private def select[C <: IterableOnce[A]](many: C)(implicit bf: BuildFrom[C, A, CS]) =
			s.select(bf.fromSpecific(many)(many))
	}
}

/**
  * Selectable components are selections that can be interacted with from the program side
  * @author Mikko Hilpinen
  * @since 23.4.2019, v1+
  */
trait Selectable[S, C] extends Selection[S, C] with Interaction[S]
{
	// COMPUTED	------------------
	
	/**
	  * Updates the currently selected value, also generating events (same as calling value = ...)
	  * @param newValue The new value for this selection
	  */
	def selected_=(newValue: S) = value = newValue
	
	
	// OTHER	------------------
	
	/**
	  * Updates the currently selected value (same as calling setValue(...))
	  * @param newValue The new value for this selection
	  */
	def select(newValue: S) = value = newValue
}
