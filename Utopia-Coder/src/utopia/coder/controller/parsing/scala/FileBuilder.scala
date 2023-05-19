package utopia.coder.controller.parsing.scala

import utopia.coder.model.scala.Package
import utopia.coder.model.scala.code.CodeLine
import utopia.coder.model.scala.datatype.Reference
import utopia.coder.model.scala.declaration.{File, InstanceDeclaration, MethodDeclaration, PropertyDeclaration}

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

/**
  * Used for building file declarations (+ extra import statements)
  * @author Mikko Hilpinen
  * @since 2.11.2021
  */
class FileBuilder(filePackage: Package, extraReferences: Set[Reference])
	extends mutable.Builder[InstanceDeclaration, File] with InstanceBuilderLike
{
	// ATTRIBUTES   -------------------------
	
	private val instancesBuilder = new VectorBuilder[InstanceDeclaration]()
	
	
	// IMPLEMENTED --------------------------
	
	override def clear() = instancesBuilder.clear()
	
	override def result() = File(filePackage, instancesBuilder.result(), extraReferences)
	
	override def addOne(elem: InstanceDeclaration) = {
		instancesBuilder += elem
		this
	}
	
	// Ignores most of the incoming data types
	override def addFreeCode(lines: IterableOnce[CodeLine]) = ()
	override def addProperty(property: PropertyDeclaration) = ()
	override def addMethod(method: MethodDeclaration) = ()
	
	override def addNested(instance: InstanceDeclaration) = instancesBuilder += instance
}
