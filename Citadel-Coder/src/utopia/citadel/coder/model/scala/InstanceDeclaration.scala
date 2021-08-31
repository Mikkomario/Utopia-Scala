package utopia.citadel.coder.model.scala

import scala.collection.immutable.VectorBuilder

import utopia.flow.util.CollectionExtensions._

/**
  * Declares an object or a class
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait InstanceDeclaration extends Declaration with CodeConvertible
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Code portion generated from this instance's constructor, if present
	  */
	def parametersString: String
	
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
	
	
	// IMPLEMENTED  --------------------------
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]
		
		// Writes the declaration and the extensions
		val base = s"$baseString$parametersString"
		val ext = extensions
		if (ext.isEmpty)
			builder += base
		else
		{
			val extensionsString = s"extends ${ext.map { _.toScala }.mkString(" with ")}"
			if (base.length + extensionsString.length < CodeConvertible.maxLineLength)
				builder += base + ' ' + extensionsString
			else
			{
				builder += base
				builder += "\t" + extensionsString
			}
		}
		
		// Starts writing the instance body
		builder += "{"
		def writeBodySegment(parts: Seq[CodeConvertible], header: => String) =
		{
			if (parts.nonEmpty)
			{
				builder += s"\t// $header\t----------"
				builder += "\t"
				parts.foreach { att =>
					att.toCodeLines.foreach { builder += "\t" + _ }
					builder += "\t"
				}
			}
		}
		/* Write order is as follows:
			1) Attributes
			2) Creation code
			3) Computed properties (non-implemented, first public, then private)
			4) Implemented properties, then implemented methods (first public, then protected)
			5) Other methods (first public)
			6) Nested objects, then nested classes (first public)
		*/
		val (attributes, computed) = properties.divideBy { _.isComputed }
		writeBodySegment(attributes, "ATTRIBUTES")
		
		creationCode.foreach { code =>
			builder += "\t// INITIAL CODE\t----------"
			builder += "\t"
			code.lines.foreach { builder += "\t" + _ }
			builder += "\t"
		}
		
		val (newComputed, implementedComputed) = computed.divideBy { _.isOverridden }
		writeBodySegment(newComputed.sortWith { _.visibility > _.visibility }, "COMPUTED")
		
		// TODO: Continue
		
		???
	}
}
