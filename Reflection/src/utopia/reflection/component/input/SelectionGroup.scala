package utopia.reflection.component.input

object SelectionGroup
{
	/**
	  * Creates a selection group for boolean selectable items
	  * @param options Selection options
	  * @tparam C Type of selection component
	  * @return A selection group for the components
	  */
	def apply[C <: InteractionWithPointer[Boolean]](options: Set[C]) = new SelectionGroup[Boolean, C](
		options, b => b, _.value = false)
	
	/**
	  * Creates a selection group for optional selectable items
	  * @param options Selection options
	  * @tparam A Type of selected item
	  * @tparam C Type of selection component
	  * @return A new selection group for the items
	  */
	def forOptions[A, C <: InteractionWithPointer[Option[A]]](options: Set[C]) = new SelectionGroup[Option[A], C](
		options, _.isDefined, _.value = None)
}

/**
  * Keeps track of multiple selectable items and deselects others when one becomes selected
  * @author Mikko Hilpinen
  * @since 2.8.2019, v1+
  */
case class SelectionGroup[A, C <: InteractionWithPointer[A]](options: Set[C], isSelected: A => Boolean, deselect: C => Unit)
{
	// ATTRIBUTES	--------------------
	
	private var lastSelected = options.find { o => isSelected(o.value) }
	
	
	// INITIAL CODE	--------------------
	
	// Deselects other options
	options.foreach { o => if (!lastSelected.contains(o) && isSelected(o.value)) deselect(o) }
	
	// Adds listening
	options.foreach { _.addValueListener { e => if (isSelected(e.newValue)) updateSelection() else lastSelected = None } }
	
	
	// OTHER	------------------------
	
	private def updateSelection() =
	{
		// Changes selection to newly selected item, if one is found
		val newSelected = options.find { o => !lastSelected.contains(o) && isSelected(o.value) }
		lastSelected.foreach(deselect)
		lastSelected = newSelected
	}
}
