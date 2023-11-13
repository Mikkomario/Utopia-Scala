package utopia.coder.model.scala

import utopia.coder.model.merging.MergeConflict
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.ScalaType
import utopia.coder.model.scala.template.{Documented, ScalaConvertible}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsFunction

import scala.language.implicitConversions

object Parameters
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * An empty parameters list
	  */
	val empty = apply(Vector(), Vector())
	
	private val parameterEqualsByType: EqualsFunction[Parameter] =
		EqualsFunction.by { p: Parameter => p.dataType }(ScalaType.matchFunction)
	
	/**
	  * A function that matches two sets of parameters based on their base types listed.
	  * E.g. (first: String, second: Option[Int]) matches (a: String, b: Option[Double]).
	  */
	implicit val matchFunction: EqualsFunction[Parameters] = EqualsFunction { (a, b) =>
		(a.lists.flatten ++ a.implicits).~==(b.lists.flatten ++ b.implicits)(parameterEqualsByType)
	}
	
	
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
	extends ScalaConvertible with Documented with MaybeEmpty[Parameters]
{
	// COMPUTED -----------------------------------
	
	/**
	  * @return Whether these parameter lists contain at least one list that is not implicit
	  */
	def containsExplicits = lists.nonEmpty
	/**
	  * @return Whether this set of parameter lists only consists of implicit parameters
	  */
	def isImplicitOnly = lists.isEmpty && implicits.nonEmpty
	
	/**
	  * @return The first parameter in this list
	  */
	def head = apply(0)
	
	
	// IMPLEMENTED  -------------------------------
	
	override def self = this
	
	/**
	  * @return Whether this parameters list is empty
	  */
	override def isEmpty = lists.isEmpty && implicits.isEmpty
	
	override def documentation = (lists.flatten ++ implicits).flatMap { _.documentation }
	
	override def toScala = {
		if (isEmpty)
			"()"
		else {
			val basePart = lists.map(parameterListFrom).reduceLeftOption { _ + _ }.getOrElse(CodePiece.empty)
			if (implicits.isEmpty)
				basePart
			else
				basePart + implicits.map { _.toScala }
					.reduceLeft { _.append(_, ", ") }.withPrefix("implicit ").withinParenthesis
		}
	}
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param listIndex Index of the targeted parameter list
	  * @param paramIndex Index of the targeted parameter in that list
	  * @return Targeted parameter
	  */
	def apply(listIndex: Int, paramIndex: Int) = lists(listIndex)(paramIndex)
	/**
	  * @param paramIndex Index of the targeted parameter (in the first parameter list)
	  * @return Targeted parameter
	  */
	def apply(paramIndex: Int): Parameter = apply(0, paramIndex)
	
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
	
	/**
	  * @param other Another set of parameters
	  * @param description Description to attach to the conflict, if one is found
	  * @return A possible conflict between these parameter sets
	  */
	def conflictWith(other: Parameters, description: => String = "") = {
		val my = toString
		val their = other.toString
		if (my == their)
			None
		else
			Some(MergeConflict.line(their, my, description))
	}
	
	private def parameterListFrom(list: Seq[Parameter]) = {
		if (list.isEmpty)
			CodePiece("()")
		else {
			// Doesn't specify default parameters if there are non-default parameters remaining on the right
			val modifiedList = list.findLastIndexWhere { _.hasNoDefault } match {
				case Some(lastNonDefaultIndex) =>
					if (lastNonDefaultIndex > 0)
						list.take(lastNonDefaultIndex).map { _.withoutDefault } ++ list.drop(lastNonDefaultIndex)
					else
						list
				case None => list
			}
			modifiedList.map { _.toScala }.reduceLeft { _.append(_, ", ") }.withinParenthesis
		}
	}
}
