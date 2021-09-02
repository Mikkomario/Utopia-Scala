package utopia.citadel.coder.model.scala

import scala.language.implicitConversions

object Parameters
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * An empty parameters list
	  */
	val empty = apply(Vector(), Vector())
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly converts from parameter vectors
	implicit def parametersFromVector(params: Vector[Parameter]): Parameters = apply(Vector(params))
	implicit def parametersFromVectors(paramLists: Vector[Vector[Parameter]]): Parameters = apply(paramLists)
	
	/**
	  * Creates a new parameter list
	  * @param firstParam First parameter to include
	  * @param moreParams More parameters to include
	  * @return A new parameters list
	  */
	def apply(firstParam: Parameter, moreParams: Parameter*): Parameters =
		apply(Vector(firstParam +: moreParams.toVector))
}

/**
  * Lists parameters, either in a single list or multiple lists
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
case class Parameters(lists: Vector[Vector[Parameter]] = Vector(), implicits: Vector[Parameter] = Vector())
	extends ScalaConvertible
{
	// IMPLEMENTED  -------------------------------
	
	override def toScala =
	{
		if (lists.isEmpty && implicits.isEmpty)
			"()"
		else
		{
			val base = lists.map { list => s"(${list.map { _.toScala }.mkString(", ")})" }.mkString
			val implicitString = if (implicits.isEmpty) "" else
				s"(implicit ${implicits.map { _.toScala }.mkString(", ")})"
			base + implicitString
		}
	}
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Combines two parameter groups
	  * @param moreParameters More parameters
	  * @return A combination of these lists
	  */
	def ++(moreParameters: Parameters) =
		Parameters(lists ++ moreParameters.lists, implicits ++ moreParameters.implicits)
	
	/**
	  * @param firstParam First implicit parameter
	  * @param moreParams More implicit parameters
	  * @return A copy of this parameters list with the specified implicit parameters
	  */
	def withImplicits(firstParam: Parameter, moreParams: Parameter*) =
		copy(implicits = firstParam +: moreParams.toVector)
}
