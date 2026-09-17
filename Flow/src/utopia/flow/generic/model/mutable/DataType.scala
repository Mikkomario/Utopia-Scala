package utopia.flow.generic.model.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.tree.ValueTree
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.{ConversionHandler, SuperTypeCaster}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.time._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.language.existentials

object DataType
{
	// ATTRIBUTES   ---------------------------
	
	private var _typeTree: Seq[ValueTree[DataType]] = Empty
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return The data type hierarchy where the root nodes are the topmost super types of the types that appear
	  *         in the lower branches. Contains one tree for each type unique type hierarchy.
	  */
	def hierarchy = _typeTree
	
	/**
	  * @return All (currently introduced) known data types
	  */
	def values = _typeTree.flatMap { _.valuesIterator }
	
	
	// OTHER    -------------------------------
	
	/**
	  * Introduces a new data type hierarchy
	  * @param types A type tree or a branch to introduce.
	  *              Doesn't have to be exhaustive, but should at least
	  *              1. Start with a previously introduced type OR
	  *              1. Start with the actual root type.
	  *
	  *              For example, these cases would be valid:
	  *              1. Type Number has already been introduced as a subtype of Any.
	  *                 This method is then called with a tree where the root node is Number.
	  *              1. This method is called with a tree where the root node is Any (assuming that's the topmost type)
	  */
	def introduce(types: ValueTree[DataType]) = {
		val oldTypes = values.toSet
		_typeTree = _typeTree.mapOrAppend { _.mergeBranch(types).toOption }(types)
		// Adds super type casting
		val newTypes = types.valuesIterator.filterNot(oldTypes.contains).toSet
		if (newTypes.nonEmpty)
			ConversionHandler.addCaster(new SuperTypeCaster(newTypes))
	}
	/**
	  * Introduces a single data type to the type hierarchy
	  * @param dataType The data type to introduce to the common type hierarchy
	  */
	def introduce(dataType: DataType): Unit =
		introduce(ValueTree.branch(dataType.superTypesIterator.toOptimizedSeq.reverseIterator ++ Single(dataType)))
	
	
	// NESTED   --------------------------------
	
	/**
	  * Any type is the superType for all other types.
	  * Represents type [[Any]]
	  */
	case object AnyType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Any"
		override val supportedClass = classOf[Any]
		
		
		// INITIAL CODE ------------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  ------------------------
		
		override def superType = None
	}
	/**
	  * Represents type [[String]]
	  */
	case object StringType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "String"
		override val supportedClass = classOf[String]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[java.lang.Integer]] from Java (not Int because a reference type is required at this time)
	  */
	case object IntType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Int"
		override val supportedClass = classOf[Integer]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[java.lang.Double]] (not from Scala, because a reference type is required at this time)
	  */
	case object DoubleType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Double"
		override val supportedClass = classOf[java.lang.Double]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[java.lang.Long]] (not from Scala, because a reference type is required at this time)
	  */
	case object LongType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Long"
		override val supportedClass = classOf[java.lang.Long]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[java.lang.Float]] (not from Scala, because a reference type is required at this time)
	  */
	case object FloatType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Float"
		override val supportedClass = classOf[java.lang.Float]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[java.lang.Boolean]] (not from Scala, because a reference type is required at this time)
	  */
	case object BooleanType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Boolean"
		override val supportedClass = classOf[java.lang.Boolean]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[Instant]], which is used for representing a moment in time
	  */
	case object InstantType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Instant"
		override val supportedClass = classOf[Instant]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[LocalDate]], which represents a date
	  */
	case object LocalDateType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "LocalDate"
		override val supportedClass = classOf[LocalDate]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[LocalTime]], i.e. time of day
	  */
	case object LocalTimeType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "LocalTime"
		override val supportedClass = classOf[LocalTime]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[LocalDateTime]], i.e. local version of Instant
	  */
	case object LocalDateTimeType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "LocalDateTime"
		override val supportedClass = classOf[LocalDateTime]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[Duration]]
	  */
	case object DurationType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Duration"
		override val supportedClass = classOf[Duration]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[Days]], i.e. duration in days
	  */
	case object DaysType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Days"
		override val supportedClass = classOf[Days]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	case object YearType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name: String = "Year"
		override val supportedClass: Class[_] = classOf[Year]
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType: Option[DataType] = Some(AnyType)
	}
	case object MonthType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name: String = "Month"
		override val supportedClass: Class[_] = classOf[Month]
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType: Option[DataType] = Some(AnyType)
	}
	case object YearMonthType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name: String = "YearMonth"
		override val supportedClass: Class[_] = classOf[YearMonth]
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType: Option[DataType] = Some(AnyType)
	}
	/**
	  * Represents type [[Pair]] of [[Value]]s, i.e. two values together
	  */
	case object PairType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Pair"
		override val supportedClass = classOf[Pair[Value]]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[Vector]] of [[Value]]s, i.e. n values together
	  */
	case object VectorType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Vector"
		override val supportedClass = classOf[Vector[Value]]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
	/**
	  * Represents type [[Model]]
	  */
	case object ModelType extends DataType
	{
		// ATTRIBUTES   ------------------------
		
		override val name = "Model"
		override val supportedClass = classOf[Model]
		
		
		// INITIAL CODE -----------------------
		
		this.introduce()
		
		
		// IMPLEMENTED  -----------------------
		
		override def superType = Some(AnyType)
	}
}

trait DataType
{
	// ABSTRACT --------------------------------
	
	/**
	  * @return The name of this data type, in PascalCase.
	  */
	def name: String
	
	/**
	  * @return The class this data type represents.
	  *         Instances of this class may be treated as instances of this data type when wrapped
	  *         in instances of [[Value]].
	  */
	def supportedClass: Class[_]
	
	/**
	  * @return The data type that's the parent / supertype of this data type.
	  *         None if this is a topmost data type.
	  */
	def superType: Option[DataType]
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @return A data type hierarchy where this type is appears as the root and sub-types appear below.
	  *         Super types of this type are not included, obviously.
	  */
	def typeHierarchy =
		DataType.hierarchy.findMap { tree => tree.allNodesIterator.find { _.value == this } }
			.getOrElse(ValueTree(this).withoutChildren)
	
	/**
	  * @return The data types that are the subtypes of this data type
	  */
	def subtypes = subtypesIterator.toOptimizedSeq
	@deprecated("Renamed to subtypes", "v2.9")
	def subTypes = subtypes
	/**
	 * @return The data types that are the sub-types of this data type
	 */
	def subtypesIterator = typeHierarchy.allNodesIterator.drop(1).map { _.value }
	
	/**
	  * @return The data types that are the super types of this data type
	  */
	def superTypes = superTypesIterator.toOptimizedSeq
	/**
	 * @return An iterator that returns the super types of this data type in order from least to most abstract
	 *         (i.e. closest to furthest from this type)
	 */
	def superTypesIterator = OptionsIterator.iterate(superType) { _.superType }
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toString = name
	
	
	// OTHER    -------------------------------
	
	/**
	  * Checks whether this data type supports an instance
	  * @param instance An instance that may or may not be of the supported type
	  * @return Whether the provided value is an instance of this data type
	  */
	// NB: Only works with reference types
	def isInstance(instance: Any) = supportedClass.isInstance(instance)
	
	/**
	  * @param other Another data type
	  * @return Whether this data type is a sub-type of the specified data type (or is that type itself)
	  */
	def isOfType(other: DataType) = this == other || superTypesIterator.contains(other)
	
	/**
	  * Introduces this data type to the common type hierarchy
	  */
	protected def introduce() = DataType.introduce(this)
}