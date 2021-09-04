package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.ScalaDocKeyword.Since
import utopia.citadel.coder.model.scala.template.{CodeConvertible, ScalaDocConvertible}
import utopia.citadel.coder.model.scala.{Code, Extension, Parameters, ScalaDocPart}
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.CollectionExtensions._

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
	
	
	// IMPLEMENTED  --------------------------
	
	override def references = (constructorParams ++ extensions ++ creationCode ++ properties ++ methods ++ nested)
		.flatMap { _.references }.toSet
	
	override def documentation =
	{
		val builder = new VectorBuilder[ScalaDocPart]()
		val desc = description
		if (desc.nonEmpty)
			builder += ScalaDocPart.description(desc)
		constructorParams.foreach { builder ++= _.documentation }
		if (description.nonEmpty)
			builder += ScalaDocPart(Since, LocalDate.now().toString)
		builder.result()
	}
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]
		
		builder ++= scalaDoc
		// Writes the declaration and the extensions
		val paramsString = constructorParams match {
			case Some(params) => params.toScala
			case None => ""
		}
		val base = s"$baseString$paramsString"
		val ext = extensions
		if (ext.isEmpty)
			builder += base
		else {
			val extensionsString = s"extends ${ ext.map { _.toScala }.mkString(" with ") }"
			if (base.length + extensionsString.length < CodeConvertible.maxLineLength)
				builder += base + ' ' + extensionsString
			else {
				builder += base
				builder += "\t" + extensionsString
			}
		}
		
		// Starts writing the instance body
		/* Write order is as follows:
			1) Attributes
			2) Creation code
			3) Computed properties (non-implemented, first public, then private)
			4) Implemented properties, then implemented methods (first public, then protected)
			5) Other methods (first public)
			6) Nested objects, then nested classes (first public)
		*/
		val (attributes, computed) = properties.divideBy { _.isComputed }
		
		val visibilityOrdering: Ordering[Declaration] = (a, b) => -a.visibility.compareTo(b.visibility)
		val fullOrdering = new CombinedOrdering[Declaration](Vector(
			visibilityOrdering, Ordering.by[Declaration, String] { _.name }))
		
		val (newComputed, implementedComputed) = computed.divideBy { _.isOverridden }
		val (otherMethods, implementedMethods) = methods.divideBy { _.isOverridden }
		
		builder ++= bodyLinesFrom(Vector(
			attributes -> "ATTRIBUTES",
			creationCode.toVector -> "INITIAL CODE",
			newComputed.sorted(visibilityOrdering) -> "COMPUTED",
			(implementedComputed.sorted(fullOrdering) ++
				implementedMethods.toVector.sorted(fullOrdering)) -> "IMPLEMENTED",
			otherMethods.toVector.sorted(fullOrdering) -> "OTHER",
			nested.toVector.sorted(fullOrdering) -> "NESTED"
		))
		
		builder.result()
	}
	
	
	// OTHER    ---------------------------------
	
	private def bodyLinesFrom(segments: Seq[(Iterable[CodeConvertible], String)]) =
	{
		val segmentsToWrite = segments
			.map { case (code, header) => code.flatMap { _.toCodeLines } -> header }
			.filter { _._1.nonEmpty }
		if (segmentsToWrite.isEmpty)
			Vector()
		else
		{
			val builder = new VectorBuilder[String]
			builder += "{"
			segmentsToWrite.dropRight(1).foreach { case (lines, header) =>
				builder += s"\t// $header\t--------------------"
				builder += "\t"
				builder ++= lines.map { "\t" + _ }
				builder ++= Vector.fill(2)("\t")
			}
			val (lastLines, lastHeader) = segmentsToWrite.last
			builder += s"\t// $lastHeader\t--------------------"
			builder += "\t"
			builder ++= lastLines.map { "\t" + _ }
			builder += "}"
			builder.result()
		}
	}
}
