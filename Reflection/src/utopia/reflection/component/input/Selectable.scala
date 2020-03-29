package utopia.reflection.component.input

import scala.collection.generic.CanBuildFrom

object Selectable
{
	implicit class OptionalSelectable[A, CP <: Traversable[A]](val s: Selectable[Option[A], CP]) extends AnyVal
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
	
	implicit class MultiSelectable[A, CS <: Traversable[A], CP <: Traversable[A]](val s: Selectable[CS, CP]) extends AnyVal
	{
		/**
		  * If no item is selected, selects the first item
		  * @param cbf Can build from (implicit)
		  */
		def selectAny()(implicit cbf: CanBuildFrom[Vector[A], A, CS]) = if (!s.isSelected) selectFirst()
		
		/**
		  * Selects the first item in this selectable
		  * @param cbf Can build from (implicit)
		  */
		def selectFirst()(implicit cbf: CanBuildFrom[Vector[A], A, CS]) = s.selected.headOption.foreach(selectOne)
		
		/**
		  * Selects all currently available items
		  * @param cbf Can build from (implicit)
		  */
		def selectAll()(implicit cbf: CanBuildFrom[CP, A, CS]) = selectMany(s.content)
		
		/**
		  * Selects no items
		  * @param cbf Can build from (implicit)
		  */
		def clearSelection()(implicit cbf: CanBuildFrom[Vector[A], A, CS]) = selectMany(Vector[A]())
		
		/**
		  * Selects exactly one item
		  * @param item Target item
		  * @param cbf Can build from (implicit)
		  */
		def selectOne(item: A)(implicit cbf: CanBuildFrom[Vector[A], A, CS]) = selectMany(Vector(item))
		
		/**
		 * Selects the items that satisfy the specified search condition
		 * @param f A search condition
		 * @param cbf Can build from (implicit)
		 */
		def selectWhere(f: A => Boolean)(implicit cbf: CanBuildFrom[Traversable[A], A, CS]) = selectMany(s.content.filter(f))
		
		/**
		  * Selects multiple items
		  * @param many Items
		  * @param cbf Can build from (implicit)
		  * @tparam C The type of item collection
		  */
		def selectMany[C <: TraversableOnce[A]](many: C)(implicit cbf: CanBuildFrom[C, A, CS]) = select(many)
		
		private def select[C <: TraversableOnce[A]](many: C)(implicit cbf: CanBuildFrom[C, A, CS]) =
		{
			val builder = cbf(many)
			builder ++= many
			s.select(builder.result())
		}
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
