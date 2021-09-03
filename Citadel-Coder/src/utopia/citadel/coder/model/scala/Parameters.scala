package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.template.{Referencing, ScalaConvertible}

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
	// Implicitly converts from a single parameter
	implicit def oneParamToMany(param: Parameter): Parameters = apply(Vector(Vector(param)))
	
	/**
	  * Creates a new parameter list
	  * @param firstParam First parameter to include
	  * @param moreParams More parameters to include
	  * @return A new parameters list
	  */
	def apply(firstParam: Parameter, moreParams: Parameter*): Parameters =
		apply(Vector(firstParam +: moreParams.toVector))
	
	/**
	  * Creates a new parameter list consisting of implicit parameters
	  * @param firstParam First parameter
	  * @param moreParams More parameters
	  * @return A parameters list
	  */
	def implicits(firstParam: Parameter, moreParams: Parameter*) =
		apply(Vector(), firstParam +: moreParams.toVector)
}

/**
  * Lists parameters, either in a single list or multiple lists
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
case class Parameters(lists: Vector[Vector[Parameter]] = Vector(), implicits: Vector[Parameter] = Vector())
	extends ScalaConvertible with Referencing
{
	// COMPUTED -----------------------------------
	
	/**
	  * @return Whether this parameters list is empty
	  */
	def isEmpty = lists.isEmpty && implicits.isEmpty
	/**
	  * @return Whether this parameters list is nonempty
	  */
	def nonEmpty = !isEmpty
	
	
	// IMPLEMENTED  -------------------------------
	
	override def references = (lists.flatten ++ implicits).flatMap { _.references }.toSet
	
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
