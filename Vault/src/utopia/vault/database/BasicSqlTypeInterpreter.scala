package utopia.vault.database

import utopia.flow.generic.model.mutable.DataType.{BooleanType, DoubleType, FloatType, InstantType, IntType, LocalDateType, LocalTimeType, LongType, StringType}
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._
import utopia.vault.database.columnlength.ColumnNumberLimit.{BigIntLimit, IntLimit, MediumIntLimit, SmallIntLimit, TinyIntLimit}
import utopia.vault.database.columnlength.ColumnTextLimit.{LongTextLimit, MediumTextLimit, TextLimit, TinyTextLimit, VarcharLimit}

import scala.util.Try

/**
 * This interpreter is able to interpret the basic sql type cases into the basic data types 
 * introduced in the Flow project. This interpreter is automatically included in the 
 * SqlTypeInterpreterManager.
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
object BasicSqlTypeInterpreter extends SqlTypeInterpreter
{
    // See type maximum lengths at:
    // https://dev.mysql.com/doc/refman/8.0/en/storage-requirements.html#data-types-storage-reqs-strings
    // Int types: https://dev.mysql.com/doc/refman/5.7/en/integer-types.html
    def apply(typeString: String) = {
        // TODO: Unsigned int should be read as long since it can have double as large a value
        // Doesn't include the text in parentheses '()', except in maximum length
        val (mainPart, parenthesisPart) = typeString.splitAtFirst("(").toTuple
        val maxLength = NotEmpty(parenthesisPart.untilFirst(")")).flatMap { s => Try { s.toInt }.toOption }
        mainPart.toLowerCase match {
            case "int" => Some(IntType) -> Some(IntLimit(maxLength))
            case "smallint" => Some(IntType) -> Some(SmallIntLimit(maxLength))
            case "mediumint" => Some(IntType) -> Some(MediumIntLimit(maxLength))
            case "bigint" => Some(LongType) -> Some(BigIntLimit(maxLength))
            case "float" => Some(FloatType) -> None
            case "double" | "decimal" => Some(DoubleType) -> None
            case "varchar" => Some(StringType) -> maxLength.map { VarcharLimit(_) }
            case "char" | "character" => Some(StringType) -> None
            case "tinyint" =>
                if (typeString.endsWith("(1)") || typeString.toLowerCase == "tinyint")
                    Some(BooleanType) -> None
                else
                    Some(IntType) -> Some(TinyIntLimit(maxLength))
            case "timestamp" | "datetime" => Some(InstantType) -> None
            case "date" => Some(LocalDateType) -> None
            case "time" => Some(LocalTimeType) -> None
            case "tinytext" => Some(StringType) -> Some(TinyTextLimit)
            case "text" => Some(StringType) -> Some(TextLimit)
            case "mediumtext" => Some(StringType) -> Some(MediumTextLimit)
            case "longtext" => Some(StringType) -> Some(LongTextLimit)
            case s if s.endsWith("text") || s.endsWith("blob") => Some(StringType) -> None
            case _ => None -> None
        }
    }
}