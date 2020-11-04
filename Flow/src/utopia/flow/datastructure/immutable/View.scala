package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.Viewable

/**
  * A very simple wrapper for an individual value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
case class View[+A](value: A) extends Viewable[A]
