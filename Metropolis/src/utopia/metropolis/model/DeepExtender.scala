package utopia.metropolis.model

import scala.language.implicitConversions

object DeepExtender
{
	implicit def deepAutoAccess[Surface <: Extender[Deep], Deep](e: DeepExtender[Surface, Deep]): Deep = e.wrapped.wrapped
}

/**
  * An extender that has two or more layers
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  * @tparam Surface Surface level object type
  * @tparam Deep deeper wrapped object type
  */
trait DeepExtender[+Surface <: Extender[Deep], +Deep] extends Extender[Surface]
