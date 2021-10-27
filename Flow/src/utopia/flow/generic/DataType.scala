package utopia.flow.generic

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.mutable.Tree
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.Model

import scala.language.existentials

/**
 * Any type is the superType for all other types
 * @see Any
 */
object AnyType extends DataType("Any", classOf[Any], None)
/**
 * String type stands for strings
 * @see String
 */
object StringType extends DataType("String", classOf[String])
/**
 * Int type stands for all integer numbers
 */
object IntType extends DataType("Int", classOf[java.lang.Integer])
/**
 * Double type stands for Double numbers
 */
object DoubleType extends DataType("Double", classOf[java.lang.Double])
/**
 * Float type stands for floating point numbers
 */
object FloatType extends DataType("Float", classOf[java.lang.Float])
/**
 * Long type stands for large long numbers
 */
object LongType extends DataType("Long", classOf[java.lang.Long])
/**
 * Boolean type stands for boolean values
 */
object BooleanType extends DataType("Boolean", classOf[java.lang.Boolean])
/**
 * Instant type stands for instant time type, which can represent any other time type as well
 * @see java.time
 */
object InstantType extends DataType("Instant", classOf[java.time.Instant])
/**
 * The local date type is used for representing dates on a local clock / time zone
 */
object LocalDateType extends DataType("LocalDate", classOf[java.time.LocalDate])
/**
 * Time type represents a time (hour, minute, second) portion of a specific date + time in 
 * the local clock / time zone
 */
object LocalTimeType extends DataType("LocalTime", classOf[java.time.LocalTime])
/**
 * The local date time type represents a combination of date and time components in a local 
 * clock / time zone
 */
object LocalDateTimeType extends DataType("LocalDateTime", classOf[java.time.LocalDateTime])
/**
 * Vector type stands for a vector of values. Only Vectors with exact parameter type of Value
 * are accepted
 */
object VectorType extends DataType("Vector", classOf[Vector[Value]])
/**
 * Model type only accepts immutable models that contain basic constants
 */
object ModelType extends DataType("Model", classOf[Model])


object DataType
{
    // ATTRIBUTES    ----------
    
    private var isSetup = false
    private var _values = HashSet[DataType]()
    /**
     * Each data type ever created
     */
    def values = _values
    
    
    // OTHER METHODS    -------
    
    /**
     * Sets up the basic data type information. This method should be called before using any of the
     * data types
     */
    def setup() = 
    {
        if (!isSetup)
        {
            isSetup = true
            introduceTypes(AnyType, StringType, IntType, DoubleType, FloatType, LongType, 
                    BooleanType, InstantType, LocalDateType, LocalTimeType, LocalDateTimeType, 
                    VectorType, ModelType)
            ConversionHandler.addCaster(BasicValueCaster)
        }
    }
    
    /**
     * Introduces a number of new data types to the data type interface. Each data type created
     * outside the Flow project should be introduced this way.
     */
    def introduceTypes(types: DataType*) = 
    {
        _values ++= types
        // Also introduces super type handling
        ConversionHandler.addCaster(new SuperTypeCaster(types.toSet))
    }
}

/**
 * A data types specifies the internal type of a value
 * @author Mikko Hilpinen
 * @since 4.11.2016
 * @param name The name of the data type. Used when printing the type information. Should start 
 * with a upper case letter. Eg. "String"
 * @param superType The type above this data type. All DataTypes should have a super type, except 
 * Any, which is the highest data type. The value defaults to Any but in some cases you want to 
 * specify a different type like Number, for example.
 */
case class DataType(name: String, supportedClass: Class[_], superType: Option[DataType] = Some(AnyType))
{
    private val tree = new Tree(this)
    
    // INITIAL CODE    -----------
    
    // The datatype interface must be set up at this point
    if (!DataType.isSetup)
        throw EnvironmentNotSetupException("DataType.setup() must be called before instantiating any DataType instances")
    
    // The super type should be aware of a new child
    superType.foreach { _.tree += this.tree }
    
    
    // COMPUTED PROPERTIES    ----
    
    /**
     * The type hierarchy of this data type where the data type is the root node and the subtypes
     * are below that
     */
    def typeHierarchy = tree.immutableCopy
    
    /**
     * All types deriving from this data type
     */
    def subTypes = typeHierarchy.nodesBelow.map { _.content }
    
    
    // IMPLEMENTED METHODS    ----
    
    override def toString: String = name
    
    
    // OTHER METHODS    ----------
    
    /**
     * Finds out whether this data type is a subType of another data type
     */
    def isOfType(other: DataType): Boolean = {this == other || superType.contains(other) || 
            (superType.isDefined && superType.get.isOfType(other))}
    
    /**
     * Checks whether this data type supports an instance
     * @param instance An instance that may or may not be of the supported type
     * @return Whether the provided value is an instance of this data type
     */
    // TODO: Only works on reference types. Use classtags with value types
    def isInstance(instance: Any) = supportedClass.isInstance(instance)
    
    /*
    def isInstance(instance: Any) = 
    {
        val B = ClassTag(supportedClass)
    			ClassTag(element.getClass) match {
    				case B => true
    				case _ => false
    	}
    }*/
}