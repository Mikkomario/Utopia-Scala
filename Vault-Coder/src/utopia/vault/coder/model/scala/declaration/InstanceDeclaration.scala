package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.ScalaDocKeyword.{Author, Since}
import utopia.flow.util.CombinedOrdering
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.StringExtensions._
import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.merging.{MergeConflict, Mergeable}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.{Extension, Parameters, ScalaDocPart, Visibility}
import utopia.vault.coder.model.scala.template.{CodeConvertible, ScalaDocConvertible}

import java.time.LocalDate
import scala.collection.immutable.VectorBuilder

/**
  * Declares an object or a class
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait InstanceDeclaration
	extends Declaration with CodeConvertible with ScalaDocConvertible
		with Mergeable[InstanceDeclaration, InstanceDeclaration]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Comments presented before the main declaration, but not included in the scaladoc
	  */
	def headerComments: Vector[String]
	/**
	  * @return parameters accepted by this instance's constructor, if it has one.
	  */
	protected def constructorParams: Option[Parameters]
	/**
	  * @return Classes & traits this instance extends, including possible construction parameters etc.
	  */
	def extensions: Vector[Extension]
	/**
	  * @return Code executed every time an instance is created
	  */
	def creationCode: Code
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
	
	/**
	  * Creates a copy of this instance, with altered information
	  * @param visibility New visibility
	  * @param extensions New extensions
	  * @param creationCode New creation code
	  * @param properties New properties
	  * @param methods New methods
	  * @param nested New nested instances
	  * @param description New description
	  * @param author New author
	  * @param headerComments New header comments
	  * @return A modified copy of this instance
	  */
	protected def makeCopy(visibility: Visibility, extensions: Vector[Extension], creationCode: Code,
	                       properties: Vector[PropertyDeclaration], methods: Set[MethodDeclaration],
	                       nested: Set[InstanceDeclaration], description: String, author: String,
	                       headerComments: Vector[String]): InstanceDeclaration
	
	
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
		// Writes possible comments
		headerComments.foreach { c => builder += s"// $c" }
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
			creationCode.notEmpty.toVector -> "INITIAL CODE",
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
	
	override def mergeWith(other: InstanceDeclaration) =
	{
		val conflictsBuilder = new VectorBuilder[MergeConflict]()
		
		val myDeclaration = basePart
		val theirDeclaration = other.basePart
		if (myDeclaration != theirDeclaration)
			conflictsBuilder += MergeConflict.line(theirDeclaration.toString, myDeclaration.toString,
				s"$name declarations differ")
		val mySuperConstructor = extensions.find { _.hasConstructor }
		val theirSuperConstructor = other.extensions.find { _.hasConstructor}
		if (mySuperConstructor.exists { my => theirSuperConstructor.exists { _ != my } })
			conflictsBuilder += MergeConflict.line(theirSuperConstructor.get.toString,
				mySuperConstructor.get.toString, s"$name versions specify different super constructors")
		
		def _mergeDeclarations[A <: Mergeable[A, A] with Declaration](my: Vector[A], their: Vector[A]): Vector[A] =
		{
			my.map { declaration =>
				their.find { _.name == declaration.name } match
				{
					case Some(otherVersion) =>
						val (merged, conflicts) = declaration.mergeWith(otherVersion)
						conflictsBuilder ++= conflicts
						merged
					case None => declaration
				}
			} ++ their.filterNot { prop => properties.exists { _.name == prop.name } }
		}
		
		val newProperties = _mergeDeclarations(properties, other.properties)
		val newMethods = _mergeDeclarations(methods.toVector, other.methods.toVector).toSet
		val newNested = _mergeDeclarations(nested.toVector, other.nested.toVector).toSet
		
		val (comparableExtensions, addedExtensions) = other.extensions.dividedWith { ext =>
			extensions.find { _.parentType.data == ext.parentType.data } match {
				case Some(myVersion) => Left(myVersion -> ext)
				case None => Right(ext)
			}
		}
		comparableExtensions.foreach { case (my, their) =>
			if (my != their)
				conflictsBuilder += MergeConflict.line(their.toString, my.toString,
					s"$name extension differs")
		}
		val newExtensions = mySuperConstructor.orElse(theirSuperConstructor).toVector ++
			extensions.filterNot { _.hasConstructor } ++ addedExtensions.filterNot { _.hasConstructor }
		
		makeCopy(visibility min other.visibility, newExtensions,
			creationCode ++ Code(other.creationCode.lines.filterNot(creationCode.lines.contains),
				other.creationCode.references),
			newProperties, newMethods, newNested, description.notEmpty.getOrElse(other.description),
			author.notEmpty.getOrElse(other.author),
			headerComments ++ other.headerComments.filterNot(headerComments.contains)) -> conflictsBuilder.result()
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
