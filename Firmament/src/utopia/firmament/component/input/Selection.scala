package utopia.firmament.component.input

import utopia.firmament.component.display.Pool

object Selection
{
	implicit class OptionalSelect(val s: Selection[Option[_], _]) extends AnyVal
	{
		/**
		  * @return Whether there is currently an item selected
		  */
		def isDefined = s.selected.isDefined
	}
	
	implicit class MultiSelect(val s: Selection[Iterable[_], _]) extends AnyVal
	{
		/**
		  * @return Whether there is currently an item selected
		  */
		def isSelected = s.selected.nonEmpty
	}
}

/**
  * Selection is an input that has a base pool of value(s), from which some are selected
  * @author Mikko Hilpinen
  * @since 23.4.2019, Reflection v1+
  * @tparam S the type of selection
  * @tparam C The type of selection pool
  */
trait Selection[+S, +C] extends Input[S] with Pool[C]
{
	// COMPUTED	-----------------
	
	/**
	  * @return The currently selected value (same as value)
	  */
	def selected = value
}
