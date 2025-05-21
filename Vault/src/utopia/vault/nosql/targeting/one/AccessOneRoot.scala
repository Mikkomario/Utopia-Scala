package utopia.vault.nosql.targeting.one

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ViewFactory

import scala.language.implicitConversions

object AccessOneRoot
{
	// IMPLICIT --------------------------
	
	implicit def autoAccessRoot[A <: ViewFactory[A] with Indexed](a: AccessOneRoot[A]): A = a.root
}

/**
  * Interface for factories which provide root level access to some individual database items
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait AccessOneRoot[+A <: ViewFactory[A] with Indexed]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The root access point
	  */
	def root: A
	
	
	// OTHER    -------------------------
	
	/**
	  * @param id ID of the targeted item
	  * @return Access to that item
	  */
	def apply(id: Int) = root(root.index <=> id)
}
