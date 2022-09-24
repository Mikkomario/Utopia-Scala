package utopia.flow.generic.model.immutable

import utopia.flow.collection.immutable.Pair
import utopia.flow.error.DataTypeException
import utopia.flow.generic._
import utopia.flow.generic.casting.ConversionHandler
import utopia.flow.generic.model.mutable.{AnyType, BooleanType, DataType, DaysType, DoubleType, DurationType, FloatType, InstantType, IntType, LocalDateTimeType, LocalDateType, LocalTimeType, LongType, ModelType, PairType, StringType, VectorType}
import utopia.flow.parse.JsonValueConverter
import utopia.flow.parse.json.{JsonConvertible, JsonValueConverter}
import utopia.flow.time.{Days, Today}
import utopia.flow.util.CollectionExtensions._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

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
case class Value(content: Option[Any], dataType: DataType) extends JsonConvertible
{
    // INITIAL CODE    ---------
    
    // The content must be of correct type, if defined
    require(content.forall(dataType.isInstance), s"$content is not of type $dataType")
    
    
    // COMP. PROPERTIES    -----
    
    /**
     * The description of this value, describing both content and data type
     */
    def description = s"'$getString' ($dataType)"
    
    /**
      * @return Whether this value has a real (not null) object value associated with it
      */
    def nonEmpty = content.isDefined
    /**
     * Whether this value has a real object value associated with it
     */
    def isDefined = nonEmpty
    
    /**
     * Whether this value doesn't have a real object value associated with it
     */
    def isEmpty = content.isEmpty
    
    /**
      * @return None if this value is empty. This value otherwise.
      */
    def notEmpty = if (isEmpty) None else Some(this)
    
    
    // IMPLEMENTED METHODS    ---
    
    override def toJson = JsonValueConverter(this).getOrElse("null")
    
    /**
     * The contents of this value cast to a string
     */
    override def toString = string.getOrElse("")
    
    override def appendToJson(jsonBuilder: StringBuilder) = JsonValueConverter(this) match {
        case Some(json) => jsonBuilder ++= json
        case None => jsonBuilder ++= "null"
    }
    
    
    // OPERATORS    -------------
    
    /**
     * Finds a value from this value as if this value was a model
     * @param propertyName The name of the requested property
     * @return The value of the requested property 
     */
    def apply(propertyName: String): Value = model match {
        case Some(model) => model(propertyName)
        case None => Value.empty
    }
    /**
      * Finds a value from this value as if this value was a model
      * @param propName Name of the targeted property
      * @param alternative Alternative name of the property
      * @param more More alternative names
      * @return A value from under this (model) value
      */
    def apply(propName: String, alternative: String, more: String*): Value = model match {
        case Some(model) => model(propName, alternative, more: _*)
        case None => Value.empty
    }
    
    /**
     * Finds a value from this value as if this value was a vector
     * @param index The index at which the value is searched
     * @return The value from the provided index from a vector within this value or empty value if 
     * this value doesn't contain a vector or index was out of range
     */
    def apply(index: Int): Value = {
        if (index < 0)
            Value.empty
        else
            vector match {
                case Some(vector) => if (index >= vector.size) Value.empty else vector(index)
                case None => Value.empty
            }
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
     * Casts this value to a certain data type. Returns None if the casting failed
     */
    def castTo(dataType: DataType) = ConversionHandler.cast(this, dataType)
    /**
      * Casts this value to either of two data types. The resulting data type is based on comparing type conversions,
      * attempting to minimize unnecessary conversions and data loss.
      * @param leftType The primary (left) cast target type
      * @param rightType The secondary (right) cast target type
      * @return Either: Left: This value converted to primary target type or
      *         Right: This value converted to the secondary target type
      */
    def castTo(leftType: DataType, rightType: DataType): Either[Value, Value] = {
        // Checks which type would be more appropriate in this context
        // Case: Already of primary type => No conversion needed
        if (isOfType(leftType))
            Left(this)
        // Case: Already of secondary type => No conversion needed
        else if (isOfType(rightType))
            Right(this)
        // Case: Conversion required
        else {
            // Converts first to the type that is more close to this value's original data type
            val targetTypes = Pair(leftType, rightType)
            val conversions = targetTypes.map { ConversionHandler.conversionRouteBetween(dataType, _)
                .map { route => route.foldLeft(0) { _ + _.cost } } }
            val rightIsFirst = conversions.second.exists { cost => conversions.first.forall { _ > cost } }
    
            // If the first conversion fails, attempts with the other data type
            if (rightIsFirst)
                castTo(rightType) match {
                    case Some(v) => Right(v)
                    case None =>
                        castTo(leftType) match {
                            case Some(v) => Left(v)
                            case None => Right(Value.emptyWithType(rightType))
                        }
                }
            else
                castTo(leftType) match {
                    case Some(v) => Left(v)
                    case None =>
                        castTo(rightType) match {
                            case Some(v) => Right(v)
                            case None => Left(Value.emptyWithType(leftType))
                        }
                }
        }
    }
    
    /**
     * Casts this value to a new data type
     * @param dataType The target data type for the new value
     * @return This value casted to a new data type. If the value couldn't be casted, an empty 
     * value is returned instead
     */
    def withType(dataType: DataType) = ConversionHandler.cast(this, dataType)
        .getOrElse(Value.emptyWithType(dataType))
    
    /**
     * Returns the contents of this value, casted to the desired type range
     * @param ofType The targeted data type
     * @return The value's contents as an instance of the provided type
     */
    def objectValue(ofType: DataType): Option[Any] = withType(ofType).content
    
    def string: Option[String] = objectValue(StringType).map { _.asInstanceOf[String]}
    def int: Option[Int] = objectValue(IntType).map { _.asInstanceOf[Int] }
    def double: Option[Double] = objectValue(DoubleType).map { _.asInstanceOf[Double]}
    def float = objectValue(FloatType).map { _.asInstanceOf[Float]}
    def long = objectValue(LongType).map { _.asInstanceOf[Long]}
    def boolean = objectValue(BooleanType).map { _.asInstanceOf[Boolean]}
    def instant = objectValue(InstantType).map { _.asInstanceOf[Instant]}
    def localDate = objectValue(LocalDateType).map { _.asInstanceOf[LocalDate]}
    def localTime = objectValue(LocalTimeType).map { _.asInstanceOf[LocalTime]}
    def localDateTime = objectValue(LocalDateTimeType).map { _.asInstanceOf[LocalDateTime]}
    def duration = objectValue(DurationType).map { _.asInstanceOf[FiniteDuration] }
    def days = objectValue(DaysType).map { _.asInstanceOf[Days] }
    def vector = objectValue(VectorType).map { _.asInstanceOf[Vector[Value]]}
    def pair = objectValue(PairType).map { _.asInstanceOf[Pair[Value]] }
    def model = objectValue(ModelType).map { _.asInstanceOf[Model]}
    
    def stringOr(default: => String = "") = string.getOrElse(default)
    def intOr(default: => Int = 0) = int.getOrElse(default)
    def doubleOr(default: => Double = 0) = double.getOrElse(default)
    def floatOr(default: => Float = 0) = float.getOrElse(default)
    def longOr(default: => Long = 0) = long.getOrElse(default)
    def booleanOr(default: => Boolean = false) = boolean.getOrElse(default)
    def instantOr(default: => Instant = Instant.now()) = instant.getOrElse(default)
    def localDateOr(default: => LocalDate = Today) = localDate.getOrElse(default)
    def localTimeOr(default: => LocalTime = LocalTime.now()) = localTime.getOrElse(default)
    def localDateTimeOr(default: => LocalDateTime = LocalDateTime.now()) = localDateTime.getOrElse(default)
    def durationOr(default: => FiniteDuration = Duration.Zero) = duration.getOrElse(default)
    def daysOr(default: => Days = Days.zero) = days.getOrElse(default)
    def vectorOr(default: => Vector[Value] = Vector[Value]()) = vector.getOrElse(default)
    def pairOr(default: => Pair[Value] = Pair.twice(Value.empty)) = pair.getOrElse(default)
    def modelOr(default: => Model = Model.empty) = model.getOrElse(default)
    
    def getString = stringOr()
    def getInt = intOr()
    def getDouble = doubleOr()
    def getFloat = floatOr()
    def getLong = longOr()
    def getBoolean = booleanOr()
    def getInstant = instantOr()
    def getLocalDate = localDateOr()
    def getLocalTime = localTimeOr()
    def getLocalDateTime = localDateTimeOr()
    def getDuration = durationOr()
    def getDays = daysOr()
    def getVector = vectorOr()
    def getPair = pairOr()
    def getModel = modelOr()
    
    def trySting = getTry(string)("String")
    def tryInt = getTry(int)("Int")
    def tryDouble = getTry(double)("Double")
    def tryFloat = getTry(float)("Float")
    def tryLong = getTry(long)("Long")
    def tryBoolean = getTry(boolean)("Boolean")
    def tryInstant = getTry(instant)("Instant")
    def tryLocalDate = getTry(localDate)("LocalDate")
    def tryLocalTime = getTry(localTime)("LocalTime")
    def tryLocalDateTime = getTry(localDateTime)("LocalDateTime")
    def tryDuration = getTry(duration)("Duration")
    def tryDays = getTry(days)("Days")
    def tryVector = getTry(vector)("Vector[Value]")
    def tryPair = getTry(pair)("Pair")
    def tryModel = getTry(model)("Model")
    
    def tryVectorWith[A](f: Value => Try[A]) = tryVector.flatMap { _.tryMap(f) }
    def tryPairWith[A](f: Value => Try[A]) =
        tryPair.flatMap { p => f(p.first).flatMap { first => f(p.second).map { Pair(first, _) } } }
    def tryTupleWith[F, S](first: Value => Try[F])(second: Value => Try[S]): Try[(F, S)] =
        tryPair.flatMap { p => first(p.first).flatMap { f => second(p.second).map { f -> _ } } }
    
    private def getTry[A](value: Option[A])(dataType: => String) =
        value.toTry { DataTypeException(s"Can't cast $description to $dataType") }
}