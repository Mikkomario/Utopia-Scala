package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.Node
import utopia.flow.generic.BooleanType
import utopia.flow.generic.ConversionHandler
import utopia.flow.generic.DataType
import utopia.flow.generic.DoubleType
import utopia.flow.generic.FloatType
import utopia.flow.generic.IntType
import utopia.flow.generic.LongType
import utopia.flow.generic.StringType
import java.time.Instant
import utopia.flow.generic.InstantType
import utopia.flow.generic.VectorType
import utopia.flow.generic.ModelType
import utopia.flow.generic.AnyType
import utopia.flow.parse.JsonValueConverter
import utopia.flow.parse.JsonConvertible
import utopia.flow.generic.LocalDateType
import java.time.LocalDate
import utopia.flow.generic.LocalTimeType
import java.time.LocalTime
import utopia.flow.generic.LocalDateTimeType
import java.time.LocalDateTime

object Value
{
    /**
     * An empty value with no data type
     */
    val empty: Value = emptyWithType(AnyType)
    
    /**
     * Creates a new empty value that represents / mimics the provided data type
     */
    def emptyWithType(dataType: DataType) = Value(None, dataType)
}

/**
 * Values can wrap an object value and associate it with a certain data type. Values can be cast 
 * to different data types. They are immutable.
 */
case class Value(content: Option[Any], dataType: DataType) extends Node[Option[Any]] with JsonConvertible
{
    // INITIAL CODE    ---------
    
    // The content must be of correct type, if defined
    require(content.forall(dataType.isInstance), s"$content is not of type $dataType")
    
    
    // COMP. PROPERTIES    -----
    
    override def toJson = JsonValueConverter(this).getOrElse("null")
    
    /**
     * The description of this value, describing both content and data type
     */
    def description = s"'${stringOr()}' ($dataType)"
    
    /**
     * Whether this value has a real object value associated with it
     */
    def isDefined = content.isDefined
    
    /**
     * Whether this value doesn't have a real object value associated with it
     */
    def isEmpty = content.isEmpty
    
    /**
      * @return None if this value is empty. This value otherwise.
      */
    def notEmpty = if (isEmpty) None else Some(this)
    
    
    // IMPLEMENTED METHODS    ---
    
    /**
     * The contents of this value cast to a string
     */
    override def toString = string.getOrElse("")
    
    
    // OPERATORS    -------------
    
    /**
     * Finds a value from this value as if this value was a model
     * @param propertyName The name of the requested property
     * @return The value of the requested property 
     */
    def apply(propertyName: String): Value = getModel(propertyName)
    
    /**
     * Finds a value from this value as if this value was a vector
     * @param index The index at which the value is searched
     * @return The value from the provided index from a vector within this value or empty value if 
     * this value doesn't contain a vector or index was out of range
     */
    def apply(index: Int): Value =
    {
        val vector = getVector
        if (index < 0 || index >= vector.length) Value.empty else vector(index)
    }
    
    
    // OTHER METHODS    ---------
    
    /**
     * If this value is empty, returns the default value. If this value is defined, returns this value.
     */
    def orElse(default: => Value) = if (isDefined) this else default
    
    /**
     * Checks whether this value is of the specified data type
     */
    def isOfType(dataType: DataType) = this.dataType.isOfType(dataType)
    
    /**
     * Casts the value to a certain data type. Returns None if the casting failed
     */
    def castTo(dataType: DataType) = ConversionHandler.cast(this, dataType)
    
    /**
     * Casts this value to a new data type
     * @param dataType The target data type for the new value
     * @return This value casted to a new data type. If the value couldn't be casted, an empty 
     * value is returned instead
     */
    def withType(dataType: DataType) = ConversionHandler.cast(this, dataType).getOrElse(Value.emptyWithType(dataType))
    
    /**
     * Returns the contents of this value, casted to the desired type range
     * @param ofType The targeted data type
     * @return The value's contents as an instance of the provided type
     */
    def objectValue(ofType: DataType) = withType(ofType).content
    
    /**
     * The string value of this value or None if the value can't be casted
     */
    def string = objectValue(StringType).map { _.asInstanceOf[String]}
    
    /**
     * The integer value of this value or None if the value can't be casted
     */
    def int = objectValue(IntType).map { _.asInstanceOf[Int] }
    
    /**
     * The double value of this value or None if the value can't be casted
     */
    def double = objectValue(DoubleType).map { _.asInstanceOf[Double]}
    
    /**
     * The float value of this value or None if the value can't be casted
     */
    def float = objectValue(FloatType).map { _.asInstanceOf[Float]}
    
    /**
     * The long value of this value or None if the value can't be casted
     */
    def long = objectValue(LongType).map { _.asInstanceOf[Long]}
    
    /**
     * The boolean value of this value or None if the value can't be casted
     */
    def boolean = objectValue(BooleanType).map { _.asInstanceOf[Boolean]}
    
    /**
     * The instant value of this value or None if the value can't be casted
     */
    def instant = objectValue(InstantType).map { _.asInstanceOf[Instant]}
    
    /**
     * The local date value of this value or None if the value can't be casted
     */
    def localDate = objectValue(LocalDateType).map { _.asInstanceOf[LocalDate]}
    
    /**
     * The local time value of this value or None if the value can't be casted
     */
    def localTime = objectValue(LocalTimeType).map { _.asInstanceOf[LocalTime]}
    
    /**
     * The local date time value of this value or None if the value can't be casted
     */
    def localDateTime = objectValue(LocalDateTimeType).map { _.asInstanceOf[LocalDateTime]}
    
    /**
     * The vector value of this value or None if the value can't be casted
     */
    def vector = objectValue(VectorType).map { _.asInstanceOf[Vector[Value]]}
    
    /**
     * The model value of this value or None if the value can't be casted
     */
    def model = objectValue(ModelType).map { _.asInstanceOf[Model[Constant]]}
    
    /**
     * The contents of this value casted to a string, or if that fails, the default value
     */
    def stringOr(default: => String = "") = string.getOrElse(default)
    
    /**
     * The contents of this value casted to an integer, or if that fails, the default value 0
     */
    def intOr(default: => Int = 0) = int.getOrElse(default)
    
    /**
     * The contents of this value casted to a double, or if that fails, the default value 0
     */
    def doubleOr(default: => Double = 0) = double.getOrElse(default)
    
    /**
     * The contents of this value casted to a float, or if that fails, the default value 0
     */
    def floatOr(default: => Float = 0) = float.getOrElse(default)
    
    /**
     * The contents of this value casted to a long, or if that fails, the default value 0
     */
    def longOr(default: => Long = 0) = long.getOrElse(default)
    
    /**
     * The contents of this value casted to a boolean, or if that fails, the default value false
     */
    def booleanOr(default: => Boolean = false) = boolean.getOrElse(default)
    
    /**
     * The contents of this value casted to an instant, or if that fails, the default value
     * (current instant)
     */
    def instantOr(default: => Instant = Instant.now()) = instant.getOrElse(default)
    
    /**
     * The current contents of this value as a local date or the default value (current date)
     */
    def localDateOr(default: => LocalDate = LocalDate.now()) = localDate.getOrElse(default)
    
    /**
     * The current contents of this value as a local time or the default value (current time)
     */
    def localTimeOr(default: => LocalTime = LocalTime.now()) = localTime.getOrElse(default)
    
    /**
     * The current contents of this value as a local date time or the default value (current time)
     */
    def localDateTimeOr(default: => LocalDateTime = LocalDateTime.now()) = localDateTime.getOrElse(default)
    
    /**
     * The contents of this value casted to a vector, or if that fails, the default value (empty
     * vector)
     */
    def vectorOr(default: => Vector[Value] = Vector[Value]()) = vector.getOrElse(default)
    
    /**
     * The contents of this value casted to a model, or if that fails, the default value (empty
     * model)
     */
    def modelOr(default: => Model[Constant] = Model.empty) = model.getOrElse(default)
    
    /**
      * The contents of this value casted to a string, or if that fails, an empty string
      */
    def getString = stringOr()
    
    /**
      * The contents of this value casted to an integer, or if that fails, 0
      */
    def getInt = intOr()
    
    /**
      * The contents of this value casted to a double, or if that fails, 0.0
      */
    def getDouble = doubleOr()
    
    /**
      * The contents of this value casted to a float, or if that fails, 0
      */
    def getFloat = floatOr()
    
    /**
      * The contents of this value casted to a long, or if that fails, 0
      */
    def getLong = longOr()
    
    /**
      * The contents of this value casted to a boolean, or if that fails, false
      */
    def getBoolean = booleanOr()
    
    /**
      * The contents of this value casted to an instant, or if that fails, current instant
      */
    def getInstant = instantOr()
    
    /**
      * The current contents of this value as a local date or current date
      */
    def getLocalDate = localDateOr()
    
    /**
      * The current contents of this value as a local time or current time
      */
    def getLocalTime = localTimeOr()
    
    /**
      * The current contents of this value as a local date time or current time
      */
    def getLocalDateTime = localDateTimeOr()
    
    /**
      * The contents of this value casted to a vector, or if that fails, empty value vector
      */
    def getVector = vectorOr()
    
    /**
      * The contents of this value casted to a model, or if that fails, empty model
      */
    def getModel = modelOr()
}