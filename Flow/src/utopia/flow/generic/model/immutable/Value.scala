package utopia.flow.generic.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.error.DataTypeException
import utopia.flow.generic.casting.ConversionHandler
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.{ApproxSelfEquals, EqualsFunction}
import utopia.flow.parse.json.{JsonConvertible, JsonValueConverter}
import utopia.flow.time._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object Value
{
    // ATTRIBUTES   --------------------------
    
    /**
     * An empty value with no data type
     */
    val empty: Value = emptyWithType(AnyType)
    
    /**
      * Returns true if the two values are both empty or convert into an equal value.
      * I.e. The "surface" data type of the values may still differ.
      */
    implicit val convertToEqual: EqualsFunction[Value] = (a, b) => {
        if (a.isEmpty)
            b.isEmpty
        else if (b.isEmpty)
            false
        else {
            // Uses a more flexible check for certain data types
            // TODO: Create a way to inject support for other data types here as well
            a.dataType match {
                // Case: Comparing models => Compares the values using ~== instead of ==
                case ModelType => b.model.exists { _ ~== a.getModel }
                // Case: Comparing strings => Ignores case
                case StringType => b.string.exists { _ ~== a.getString }
                // Case: Comparing double numbers => Ignores some decimals
                case DoubleType => b.double.exists { _ ~== a.getDouble }
                // Case: Vector => Compares inner values using ~== instead of ==
                case VectorType =>
                    b.vector.exists { bVec =>
                        val vectors = Pair(a.getVector, bVec)
                        vectors.isSymmetricBy { _.size } && vectors.merge { (a, b) =>
                            a.iterator.zip(b.iterator).forall { case (a, b) => convertToEqual(a, b) }
                        }
                    }
                // Case: Pair => Compares inner values using ~== instead of ==
                case PairType => b.pair.exists { bPair => a.getPair.forallWith(bPair)(convertToEqual.apply) }
                // Case: Same data types => Compares values directly
                case b.dataType => a.content == b.content
                // Case: Different data types => Converts to the correct type first
                case _ => b.objectValue(a.dataType) == a.content
            }
        }
    }
    
    
    // OTHER    ------------------------------
    
    /**
     * Creates a new empty value that represents / mimics the provided data type
     */
    def emptyWithType(dataType: DataType) = Value(None, dataType)
}

/**
 * Values can wrap an object value and associate it with a certain data type. Values can be cast 
 * to different data types. They are immutable.
 */
case class Value(content: Option[Any], dataType: DataType)
    extends JsonConvertible with MaybeEmpty[Value] with ApproxSelfEquals[Value]
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
     * Whether this value has a real object value associated with it
     */
    def isDefined = nonEmpty
    
    
    // IMPLEMENTED METHODS    ---
    
    override def self = this
    
    /**
      * Whether this value doesn't have a real object value associated with it
      */
    override def isEmpty = content.isEmpty
    
    override def toJson = JsonValueConverter(this).getOrElse("null")
    
    /**
     * The contents of this value cast to a string
     */
    override def toString = string.getOrElse("")
    
    override implicit def approxEqualsFunction: EqualsFunction[Value] = Value.convertToEqual
    
    override def appendToJson(jsonBuilder: mutable.StringBuilder) = JsonValueConverter(this) match {
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
    def withType(dataType: DataType) = castTo(dataType).getOrElse(Value.emptyWithType(dataType))
    
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
    def duration = objectValue(DurationType).map { _.asInstanceOf[Duration] }
    def days = objectValue(DaysType).map { _.asInstanceOf[Days] }
    def year = objectValue(YearType).map { _.asInstanceOf[Year] }
    def month = objectValue(MonthType).map { _.asInstanceOf[Month] }
    def yearMonth = objectValue(YearMonthType).map { _.asInstanceOf[YearMonth] }
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
    def durationOr(default: => Duration = Duration.zero): Duration = duration.getOrElse(default)
    def daysOr(default: => Days = Days.zero) = days.getOrElse(default)
    def yearOr(default: => Year = Today.year) = year.getOrElse(default)
    def monthOr(default: => Month = Today.month) = month.getOrElse(default)
    def yearMonthOr(default: => YearMonth = Today.yearMonth) = yearMonth.getOrElse(default)
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
    def getYear = yearOr()
    def getMonth = monthOr()
    def getYearMonth = yearMonthOr()
    def getVector = vectorOr()
    def getPair = pairOr()
    def getModel = modelOr()
    
    def tryString = getTry(StringType) { _.getString }
    def tryInt = tryGetNonEmpty(int)("Int")
    def tryDouble = tryGetNonEmpty(double)("Double")
    def tryFloat = tryGetNonEmpty(float)("Float")
    def tryLong = tryGetNonEmpty(long)("Long")
    def tryBoolean = tryGetNonEmpty(boolean)("Boolean")
    def tryInstant = tryGetNonEmpty(instant)("Instant")
    def tryLocalDate = tryGetNonEmpty(localDate)("LocalDate")
    def tryLocalTime = tryGetNonEmpty(localTime)("LocalTime")
    def tryLocalDateTime = tryGetNonEmpty(localDateTime)("LocalDateTime")
    def tryDuration = tryGetNonEmpty(duration)("Duration")
    def tryDays = tryGetNonEmpty(days)("Days")
    def tryYear = tryGetNonEmpty(year)("Year")
    def tryMonth = tryGetNonEmpty(month)("Month")
    def tryYearMonth = tryGetNonEmpty(yearMonth)("YearMonth")
    def tryVector = getTry(VectorType) { _.getVector }
    def tryPair = tryGetNonEmpty(pair)("Pair")
    def tryModel = getTry(ModelType) { _.getModel }
    
    def tryVectorWith[A](f: Value => Try[A]) = tryVector.flatMap { _.tryMapAll(f) }
    def tryPairWith[A](f: Value => Try[A]) =
        tryPair.flatMap { p => f(p.first).flatMap { first => f(p.second).map { Pair(first, _) } } }
    def tryTupleWith[F, S](first: Value => Try[F])(second: Value => Try[S]): Try[(F, S)] =
        tryPair.flatMap { p => first(p.first).flatMap { f => second(p.second).map { f -> _ } } }
	
	/**
	 * @return Either:
	 *              1. Some(Left): This value as an Int, if possible
	 *              1. Some(Right): This value as a non-empty String
	 *              1. None: If this value was empty
	 */
	def intOrString = leftOrRight { _.int } { _.string }
	/**
	 * @return Either:
	 *              1. Left: This value as an Int, if possible
	 *              1. Right: This value as a String
	 */
	def getIntOrString = getLeftOrRight { _.int } { _.getString }
	/**
	 * @return Either:
	 *              1. Success(Left): This value as an Int, if possible
	 *              1. Success(Right): This value as a non-empty String
	 *              1. Failure: If this value was empty
	 */
	def tryIntOrString = tryLeftOrRight { _.tryInt } { _.string }
	
	/**
	 * Attempts to cast this value to a left value. And if that doesn't work, tries to cast this to a right value.
	 * @param toLeft A function for casting this value into a left type. Yields None if casting was not possible.
	 * @param toRight A function for casting this value into a right type. Yields None if casting was not possible.
	 * @tparam L Type of the left cast result, when successful
	 * @tparam R Type of the right cast result, when successful
	 * @return Either:
	 *              1. Some(Left): The result of 'toLeft', if successful
	 *              1. Some(Right): The result of 'toRight', if 'toLeft' failed.
	 *              1. None: If both of these functions yielded None.
	 */
	def leftOrRight[L, R](toLeft: Value => Option[L])(toRight: Value => Option[R]): Option[Either[L, R]] =
		toLeft(this) match {
			case Some(left) => Some(Left[L, R](left))
			case None => toRight(this).map(Right.apply[L, R])
		}
	/**
	 * Attempts to cast this value to a left value. And if that doesn't work, casts this to a right value.
	 * @param toLeft A function for casting this value into a left type. Yields None if casting was not possible.
	 * @param getRight A function for casting this value into a right type.
	 * @tparam L Type of the left cast result, when successful
	 * @tparam R Type of the right cast result
	 * @return Either
	 *              1. Left: The result of 'toLeft', if successful
	 *              1. Right: The result of 'getRight', if 'toLeft' failed.
	 */
	def getLeftOrRight[L, R](toLeft: Value => Option[L])(getRight: Value => R) = toLeft(this) match {
		case Some(left) => Left[L, R](left)
		case None => Right[L, R](getRight(this))
	}
	/**
	 * Attempts to cast this value to a left value. And if that doesn't work, tries to cast this to a right value.
	 * @param tryLeft A function for casting this value into a left type. Yields a failure if casting was not possible.
	 * @param toRight A function for casting this value into a right type. Yields None if casting was not possible.
	 * @tparam L Type of the left cast result, when successful
	 * @tparam R Type of the right cast result, when successful
	 * @return Either:
	 *              1. Success(Left): The result of 'toLeft', if successful
	 *              1. Success(Right): The result of 'toRight', if 'toLeft' failed.
	 *              1. Failure: If both of these functions failed
	 */
	def tryLeftOrRight[L, R](tryLeft: Value => Try[L])(toRight: Value => Option[R]) = tryLeft(this) match {
		case Success(left) => Success(Left[L, R](left))
		case Failure(error) =>
			toRight(this) match {
				case Some(right) => Success(Right[L, R](right))
				case None => Failure(error)
			}
	}
	
    private def getTry[A](targetType: DataType)(extract: Value => A) =
        castTo(targetType).toTry { new DataTypeException(s"Can't cast $description to $targetType") }.map(extract)
    
    private def tryGetNonEmpty[A](value: Option[A])(dataType: => String) =
        value.toTry { new DataTypeException(s"Can't cast $description to $dataType") }
}