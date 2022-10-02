package utopia.metropolis.model

import scala.language.implicitConversions
import utopia.flow.util
import utopia.flow.view.template.Extender

object DeepExtender
{
	implicit def deepAutoAccess[Surface <: Extender[Deep], Deep](e: DeepExtender[Surface, Deep]): Deep = e.wrapped.wrapped
}

/**
  * An extender that has two or more layers
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  * @tparam Surface Surface level object type
  * @tparam Deep deeper wrapped object type
  */
trait DeepExtender[+Surface <: Extender[Deep], +Deep] extends Extender[Surface]
