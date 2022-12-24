package utopia.flow.generic.model.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Tree}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.{ConversionHandler, SuperTypeCaster}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.time.Days

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.duration.FiniteDuration
import scala.language.existentials

object DataType
{
    // ATTRIBUTES   ---------------------------
    
    private var _typeTree = Vector[Tree[DataType]]()
    
    
    // COMPUTED -------------------------------
    
    /**
      * @return The data type hierarchy where the root nodes are the topmost super types of the types that appear
      *         in the lower branches. Contains one tree for each type unique type hierarchy.
      */
    def hierarchy = _typeTree
    
    /**
      * @return All (currently introduced) known data types
      */
    def values = _typeTree.flatMap { _.allNodesIterator.map { _.nav } }
    
    
    // OTHER    -------------------------------
    
    /**
      * Introduces a new data type hierarchy
      * @param types A type tree or a branch to introduce.
      *              Doesn't have to be exhaustive, but should at least
      *              a) Start with a previously introduced type OR
      *              b) Start with the actual root type.
      *
      *              For example, these cases would be valid:
      *              a) Type Number has already been introduced as a sub-type of Any.
      *              This method is then called with a tree where the root node is Number.
      *              b) This method is called with a tree where the root node is Any (assuming that's the topmost type)
      */
    def introduce(types: Tree[DataType]) = {
        val oldTypes = values.toSet
        _typeTree = _typeTree.mapOrAppend { _.mergeBranch(types).toOption }(types)
        // Adds super type casting
        val newTypes = types.allNavsIterator.filterNot(oldTypes.contains).toSet
        if (newTypes.nonEmpty)
            ConversionHandler.addCaster(new SuperTypeCaster(newTypes))
    }
    /**
      * Introduces a single data type to the type hierarchy
      * @param dataType The data type to introduce to the common type hierarchy
      */
    def introduce(dataType: DataType): Unit =
        introduce(Tree.branch(dataType.superTypesIterator.toVector.reverse :+ dataType))
    
    /**
      * Sets up the basic data type information. This method should be called before using any of the
      * data types
      */
    @deprecated("Not needed anymore", "v2.0")
    def setup() = ()
    
    
    // NESTED   --------------------------------
    
    /**
      * Any type is the superType for all other types.
      * Represents type [[Any]]
      */
    case object AnyType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Any]
        
        
        // INITIAL CODE ------------------------
        
        introduce()
        
        
        // IMPLEMENTED  ------------------------
        
        override def name = "Any"
        override def superType = None
    }
    /**
      * Represents type [[String]]
      */
    case object StringType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[String]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "String"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Integer]] from Java (not Int because a reference type is required at this time)
      */
    case object IntType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Integer]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Int"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[java.lang.Double]] (not from Scala, because a reference type is required at this time)
      */
    case object DoubleType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[java.lang.Double]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Double"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[java.lang.Long]] (not from Scala, because a reference type is required at this time)
      */
    case object LongType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[java.lang.Long]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Long"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[java.lang.Float]] (not from Scala, because a reference type is required at this time)
      */
    case object FloatType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[java.lang.Float]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Float"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[java.lang.Boolean]] (not from Scala, because a reference type is required at this time)
      */
    case object BooleanType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[java.lang.Boolean]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Boolean"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Instant]], which is used for representing a moment in time
      */
    case object InstantType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Instant]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Instant"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[LocalDate]], which represents a date
      */
    case object LocalDateType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[LocalDate]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "LocalDate"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[LocalTime]], i.e. time of day
      */
    case object LocalTimeType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[LocalTime]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "LocalTime"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[LocalDateTime]], i.e. local version of Instant
      */
    case object LocalDateTimeType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[LocalDateTime]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "LocalDateTime"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[FiniteDuration]]
      */
    case object DurationType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[FiniteDuration]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Duration"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Days]], i.e. duration in days
      */
    case object DaysType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Days]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Days"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Pair]] of [[Value]]s, i.e. two values together
      */
    case object PairType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Pair[Value]]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Pair"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Vector]] of [[Value]]s, i.e. n values together
      */
    case object VectorType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Vector[Value]]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Vector"
        override def superType = Some(AnyType)
    }
    /**
      * Represents type [[Model]]
      */
    case object ModelType extends DataType
    {
        // ATTRIBUTES   ------------------------
        
        override lazy val supportedClass = classOf[Model]
        
        
        // INITIAL CODE -----------------------
        
        introduce()
        
        
        // IMPLEMENTED  -----------------------
        
        override def name = "Model"
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
      * @return The data type that's the parent / super type of this data type.
      *         None if this is a topmost data type.
      */
    def superType: Option[DataType]
    
    
    // COMPUTED --------------------------------
    
    /**
      * @return An iterator that returns the super types of this data type in order from least to most abstract
      *         (i.e. closest to furthest from this type)
      */
    def superTypesIterator = OptionsIterator.iterate(superType) { _.superType }
    
    /**
      * @return A data type hierarchy where this type is appears as the root and sub-types appear below.
      *         Super types of this type are not included, obviously.
      */
    def typeHierarchy =
        DataType.hierarchy.findMap { tree => tree.allNodesIterator.find { _.nav == this } }.getOrElse(Tree(this))
    
    /**
      * @return The data types that are the sub-types of this data type
      */
    def subTypes = typeHierarchy.allNodesIterator.drop(1).map { _.nav }.toVector
    /**
      * @return The data types that are the super types of this data type
      */
    def superTypes = superTypesIterator.toVector
    
    
    // IMPLEMENTED  ---------------------------
    
    override def toString = name
    
    
    // OTHER    -------------------------------
    
    /**
      * Checks whether this data type supports an instance
      * @param instance An instance that may or may not be of the supported type
      * @return Whether the provided value is an instance of this data type
      */
    // TODO: Only works on reference types. Use classtags with value types
    def isInstance(instance: Any) = supportedClass.isInstance(instance)
    // def isInstance(a: Any) = classTag == ClassTag(a.getClass)
    /*
    def isInstance(instance: Any) =
    {
        val B = ClassTag(supportedClass)
    			ClassTag(element.getClass) match {
    				case B => true
    				case _ => false
    	}
    }*/
    
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