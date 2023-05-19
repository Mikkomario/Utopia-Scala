package utopia.coder.controller.parsing.scala

import utopia.coder.model.scala.code.{Code, CodeLine}
import utopia.coder.model.scala.datatype.{Extension, GenericType, Reference}
import utopia.coder.model.scala.declaration.DeclarationPrefix.{Case, Sealed}
import utopia.coder.model.scala.declaration.InstanceDeclarationType.{ClassD, ObjectD, TraitD}
import utopia.coder.model.scala.declaration._
import utopia.coder.model.scala.doc.ScalaDoc
import utopia.coder.model.scala.{Annotation, DeclarationDate, Parameters, Visibility}

import scala.collection.immutable.VectorBuilder

/**
  * Used for building instance declarations based on the collected data
  * @author Mikko Hilpinen
  * @since 2.11.2021, v1.3
  */
class InstanceBuilder(visibility: Visibility, prefixes: Set[DeclarationPrefix], instanceType: InstanceDeclarationType,
                      name: String, genericTypes: Seq[GenericType], parameters: Option[Parameters],
                      extensions: Vector[Extension], scalaDoc: ScalaDoc, commentsBefore: Vector[String],
                      annotations: Seq[Annotation])
	extends InstanceBuilderLike
{
	// ATTRIBUTES   ----------------------------
	
	private val freeCodeBuilder = new VectorBuilder[CodeLine]()
	private val propertiesBuilder = new VectorBuilder[PropertyDeclaration]()
	private val methodsBuilder = new VectorBuilder[MethodDeclaration]()
	private val nestedBuilder = new VectorBuilder[InstanceDeclaration]()
	
	
	// OTHER    --------------------------------
	
	/**
	  * Adds free code to instance body
	  * @param lines Lines of code to add
	  */
	def addFreeCode(lines: IterableOnce[CodeLine]): Unit =
		freeCodeBuilder ++= lines
	/**
	  * Adds a new property to this instance
	  * @param property Property to add
	  */
	def addProperty(property: PropertyDeclaration): Unit = propertiesBuilder += property
	/**
	  * Adds a new method to this instance
	  * @param method Method to add
	  */
	def addMethod(method: MethodDeclaration): Unit = methodsBuilder += method
	/**
	  * Adds a new nested instance
	  * @param instance nested instance to add
	  */
	def addNested(instance: InstanceDeclaration): Unit = nestedBuilder += instance
	
	/**
	  * Finalizes the instance
	  * @param refMap Map that contains reference targets as keys and references as values
	  * @return Instance declaration based on collected data
	  */
	def result(refMap: Map[String, Reference]) =
	{
		val freeCodeLines = freeCodeBuilder.result()
		val freeCode = Code(freeCodeLines, refMap.keySet
			.filter { target => freeCodeLines.exists { _.code.contains(target) } }.map(refMap.apply))
		val since = scalaDoc.since.getOrElse(DeclarationDate.today)
		
		// TODO: WET WET
		instanceType match {
			case ObjectD => ObjectDeclaration(name, extensions, freeCode, propertiesBuilder.result(),
				methodsBuilder.result().toSet, nestedBuilder.result().toSet, visibility, annotations,
				scalaDoc.description, scalaDoc.author, commentsBefore, since, prefixes.contains(Case))
			case ClassD => ClassDeclaration(name, genericTypes, parameters.getOrElse(Parameters.empty), extensions,
				freeCode, propertiesBuilder.result(), methodsBuilder.result().toSet, nestedBuilder.result().toSet,
				visibility, annotations, scalaDoc.description, scalaDoc.author, commentsBefore, since,
				prefixes.contains(Case))
			case TraitD => TraitDeclaration(name, genericTypes, extensions, propertiesBuilder.result(),
				methodsBuilder.result().toSet, nestedBuilder.result().toSet, visibility, annotations,
				scalaDoc.description, scalaDoc.author, commentsBefore, since, prefixes.contains(Sealed))
		}
	}
}
