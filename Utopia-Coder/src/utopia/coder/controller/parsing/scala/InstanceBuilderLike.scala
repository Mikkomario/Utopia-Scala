package utopia.coder.controller.parsing.scala

import utopia.coder.model.scala.code.CodeLine
import utopia.coder.model.scala.declaration.{InstanceDeclaration, MethodDeclaration, PropertyDeclaration}

/**
  * Common trait for builders that collect read scala data
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  */
trait InstanceBuilderLike
{
	/**
	  * Adds free code to instance body
	  * @param lines Lines of code to add
	  */
	def addFreeCode(lines: IterableOnce[CodeLine]): Unit
	/**
	  * Adds a new property to this instance
	  * @param property Property to add
	  */
	def addProperty(property: PropertyDeclaration): Unit
	/**
	  * Adds a new method to this instance
	  * @param method Method to add
	  */
	def addMethod(method: MethodDeclaration): Unit
	/**
	  * Adds a new nested instance
	  * @param instance nested instance to add
	  */
	def addNested(instance: InstanceDeclaration): Unit
}
