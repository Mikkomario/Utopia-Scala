package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template.LazyLike

/**
  * A common trait for lazy container implementations which allow outside manipulation
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  */
trait MutableLazyLike[A] extends LazyLike[A]
{
	// ABSTRACT	----------------------
	
	/**
	  * Overrides the current value of this lazy container
	  * @param newValue New value, which may be empty (uninitialized)
	  */
	protected def updateValue(newValue: Option[A]): Unit
	
	
	// OTHER	----------------------
	
	/**
	  * @param newValue New value for this lazy container
	  */
	def set(newValue: A) = updateValue(Some(newValue))
	
	/**
	  * Resets this lazy container so that a new value is generated the next time get is called
	  */
	def reset() = updateValue(None)
}
