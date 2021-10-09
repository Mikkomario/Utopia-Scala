package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.ScalaDocKeyword.{Author, Since}
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.CollectionExtensions._
import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.{Extension, Parameters, ScalaDocPart}
import utopia.vault.coder.model.scala.template.{CodeConvertible, ScalaDocConvertible}

import java.time.LocalDate
import scala.collection.immutable.VectorBuilder

/**
  * Declares an object or a class
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait InstanceDeclaration extends Declaration with CodeConvertible with ScalaDocConvertible
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return parameters accepted by this instance's constructor, if it has one.
	  */
	protected def constructorParams: Option[Parameters]
	
	/**
	  * @return Classes & traits this instance extends, including possible construction parameters etc.
	  */
	def extensions: Vector[Extension]
	
	/**
	  * @return Code executed every time an instance is created (optional)
	  */
	def creationCode: Option[Code]
	
	/**
	  * @return Properties defined in this instance
	  */
	def properties: Vector[PropertyDeclaration]
	
	/**
	  * @return Methods defined for this instance
	  */
	def methods: Set[MethodDeclaration]
	
	/**
	  * @return Nested classes & objects
	  */
	def nested: Set[InstanceDeclaration]
	
	/**
	  * @return Description of this instance (may be empty)
	  */
	def description: String
	
	/**
	  * @return Author that wrote this declaration (may be empty)
	  */
	def author: String
	
	
	// IMPLEMENTED  --------------------------
	
	override def documentation =
	{
		val builder = new VectorBuilder[ScalaDocPart]()
		val desc = description
		if (desc.nonEmpty)
			builder += ScalaDocPart.description(desc)
		constructorParams.foreach { builder ++= _.documentation }
		// If there are other scaladocs, adds author and since -tags
		if (description.nonEmpty)
		{
			if (author.nonEmpty)
				builder += ScalaDocPart(Author, author)
			builder += ScalaDocPart(Since, LocalDate.now().toString)
		}
		builder.result()
	}
	
	override def toCode =
	{
		val builder = new CodeBuilder()
		
		// Writes the scaladoc
		builder ++= scalaDoc
		// Writes the declaration and the extensions
		builder += basePart
		constructorParams.foreach { builder += _.toScala }
		
		val ext = extensions
		if (ext.nonEmpty)
			builder.appendPartial(ext.map { _.toScala }.reduceLeft { _.append(_, " with ") }
				.withPrefix("extends "), " ", allowLineSplit = true)
		
		// Starts writing the instance body
		/* Write order is as follows:
			1) Attributes
			2) Creation code
			3) Abstract properties
			4) Abstract methods
			5) Computed properties (non-implemented, first public, then private)
			6) Implemented properties, then implemented methods (first public, then protected)
			7) Other methods (first public)
			8) Nested objects, then nested classes (first public)
		*/
		val (attributes, computed) = properties.divideBy { _.isComputed }
		
		val (concreteComputed, abstractComputed) = computed.divideBy { _.isAbstract }
		val (concreteMethods, abstractMethods) = methods.divideBy { _.isAbstract }
		
		val (newComputed, implementedComputed) = concreteComputed.divideBy { _.isOverridden }
		val (otherMethods, implementedMethods) = concreteMethods.divideBy { _.isOverridden }
		
		val visibilityOrdering: Ordering[Declaration] = (a, b) => -a.visibility.compareTo(b.visibility)
		val fullOrdering = new CombinedOrdering[Declaration](Vector(
			visibilityOrdering, Ordering.by[Declaration, String] { _.name }))
		
		appendSegments(builder, Vector[(Iterable[CodeConvertible], String)](
			attributes -> "ATTRIBUTES",
			creationCode.toVector -> "INITIAL CODE",
			(abstractComputed.sorted(visibilityOrdering) ++
				abstractMethods.toVector.sorted(fullOrdering)) -> "ABSTRACT",
			newComputed.sorted(visibilityOrdering) -> "COMPUTED",
			(implementedComputed.sorted(fullOrdering) ++
				implementedMethods.toVector.sorted(fullOrdering)) -> "IMPLEMENTED",
			otherMethods.toVector.sorted(fullOrdering) -> "OTHER",
			nested.toVector.sorted(fullOrdering) -> "NESTED"
		))
		
		builder.result()
	}
	
	
	// OTHER    ---------------------------------
	
	private def appendSegments(builder: CodeBuilder, segments: Seq[(Iterable[CodeConvertible], String)]) =
	{
		val segmentsToWrite = segments
			.map { case (code, header) => code.map { _.toCode } -> header }
			.filter { _._1.nonEmpty }
		if (segmentsToWrite.nonEmpty)
			builder.block { builder =>
				segmentsToWrite.dropRight(1).foreach { case (codes, header) =>
					builder += s"// $header\t--------------------"
					builder.addEmptyLine()
					codes.foreach { code =>
						builder ++= code
						builder.addEmptyLine()
					}
					builder.addEmptyLine()
				}
				// Writes the last portion separately because the separators are different at the end
				val (lastCodes, lastHeader) = segmentsToWrite.last
				builder += s"// $lastHeader\t--------------------"
				lastCodes.foreach { code =>
					builder.addEmptyLine()
					builder ++= code
				}
			}
	}
}
