package utopia.annex.model.manifest

import utopia.flow.view.template.Extender

/**
  * Manifests are forms in which Schrödinger items appear, covering both the Flux, Alive and Dead states.
  * Providing a Manifest will allow the client to operate with some appearance of an item,
  * regardless of the Schrödinger's state.
  * @author Mikko Hilpinen
  * @since 20.11.2022, v1.4
  */
case class Manifest[+A](wrapped: A, state: SchrodingerState) extends Extender[A] with HasSchrodingerState
{
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function to apply to the wrapped item within this Manifest
	  * @tparam B Type of mapping results
	  * @return A mapped copy of this Manifest
	  */
	def mapItem[B](f: A => B) = copy(wrapped = f(wrapped))
}