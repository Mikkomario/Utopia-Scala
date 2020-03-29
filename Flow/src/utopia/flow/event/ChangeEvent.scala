package utopia.flow.event

/**
  * Change events are generated when a value changes
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1+
  * @tparam A the type of changed item
  * @param oldValue The previous value
  * @param newValue The new value
  */
case class ChangeEvent[+A](oldValue: A, newValue: A)
{
	override def toString = s"Change from $oldValue to $newValue"
}
