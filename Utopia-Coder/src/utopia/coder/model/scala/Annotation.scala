package utopia.coder.model.scala

import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.{Reference, ScalaType}
import utopia.coder.model.scala.template.ScalaConvertible
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.CombinedOrdering
import utopia.flow.util.Version

object Annotation
{
	// ATTRIBUTES   -------------------
	
	implicit val ordering: Ordering[Annotation] = CombinedOrdering[Annotation](
		Ordering.by[Annotation, String] { _.name },
		Ordering.by[Annotation, String] { _.genericTypes.mkString },
		Ordering.by[Annotation, String] { _.parameters.mkString }
	)
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new annotation
	  * @param name Annotation name (e.g. "throws")
	  * @param genericTypes Generic type parameters applied to this annotation (default = empty)
	  * @param parameters Parameters applied to this annotation, as values (default = empty)
	  * @return A new annotation
	  */
	def apply(name: String, genericTypes: Vector[ScalaType] = Vector(),
	          parameters: Vector[Value] = Vector()): Annotation =
		_Annotation(name, genericTypes, parameters)
	
	/**
	  * Merges two set of annotations together
	  * @param annotations Annotations to merge, where the first value is of higher priority than the other
	  * @return Merged annotations + cases which where conflicting (included in merge results, also)
	  */
	def merge(annotations: Pair[Seq[Annotation]]) = {
		val (unique, mergeCases) = annotations.separateMatchingWith { _ matches _ }
		val result = unique.first.toVector.sorted ++ mergeCases.map { _.merge { _ mergeWith _ } } ++
			unique.second.toVector.sorted
		result -> mergeCases.filter { _.merge { (a, b) => b.parameters.nonEmpty && a.parameters.nonEmpty } }
	}
	
	
	// NESTED   -----------------------
	
	/**
	  * A deprecation annotation
	  * @param description Description showing why the described item is deprecated
	  * @param sinceVersion The first version in which the described item is deprecated
	  */
	case class Deprecation(description: String, sinceVersion: Version) extends Annotation
	{
		override def name = "deprecated"
		override def genericTypes = Vector()
		override def parameters = Vector(description, sinceVersion.toString)
	}
	
	private case class _Annotation(name: String, genericTypes: Vector[ScalaType] = Vector(),
	                               parameters: Vector[Value] = Vector())
		extends Annotation
}

/**
  * Represents an annotation that appears within Scala code
  * @author Mikko Hilpinen
  * @since 8.10.2022, v1.7.1
  */
trait Annotation extends ScalaConvertible
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return The name of this annotation's type. E.g. "deprecated"
	  */
	def name: String
	/**
	  * @return Generic type parameters that apply to this annotation
	  */
	def genericTypes: Vector[ScalaType]
	/**
	  * @return Parameters that apply to this annotation
	  */
	def parameters: Vector[Value]
	
	
	// IMPLEMENTED  -----------------------
	
	override def toScala = {
		val s = new StringBuilder()
		var refs = Set[Reference]()
		s ++= s"@$name"
		if (genericTypes.nonEmpty) {
			val genericScalaParts = genericTypes.map { _.toScala }
			refs = genericScalaParts.flatMap { _.references }.toSet
			s ++= s"[${ genericScalaParts.mkString(", ") }]"
		}
		if (parameters.nonEmpty)
			s ++= s"(${ parameters.map { _.toJson }.mkString(", ") })"
		
		CodePiece(s.result(), refs)
	}
	
	
	// OTHER    -----------------------
	
	/**
	  * Checks whether these two annotations match each other in the sense that they serve the same purpose and
	  * should be merged instead of presented back to back
	  * @param other Another annotation
	  * @return Whether these two annotations match
	  */
	def matches(other: Annotation) = name == other.name && genericTypes == other.genericTypes
	/**
	  * Merges this annotation with another annotation.
	  * Assumes the other annotation matches this one.
	  * In practice, only modifies the parameters of this annotation.
	  * @param other Another annotation that matches this one (see: `.matches(Annotation)`)
	  * @return A merged copy of these annotations, where this annotation is the primary data source
	  */
	def mergeWith(other: Annotation) = {
		if (other.parameters.size <= parameters.size)
			this
		else
			Annotation(name, genericTypes, parameters ++ other.parameters.drop(parameters.size))
	}
}
