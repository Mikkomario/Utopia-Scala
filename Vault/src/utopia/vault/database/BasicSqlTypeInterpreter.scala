package utopia.vault.database

import utopia.flow.generic.IntType
import utopia.flow.generic.StringType
import utopia.flow.generic.BooleanType
import utopia.flow.generic.InstantType
import utopia.flow.generic.LongType
import utopia.flow.generic.FloatType
import utopia.flow.generic.DoubleType
import utopia.flow.generic.LocalDateType
import utopia.flow.generic.LocalTimeType

/**
 * This interpreter is able to interpret the basic sql type cases into the basic data types 
 * introduced in the Flow project. This interpreter is automatically included in the 
 * SqlTypeInterpreterManager.
 * @author Mikko Hilpinen
 * @since 4.6.2017
 */
object BasicSqlTypeInterpreter extends SqlTypeInterpreter
{
    def apply(typeString: String) = 
    {   
        // TODO: Unsigned int should be read as long since it can have double as large a value
        // Doesn't include the text in parentheses '()'
        typeString.toLowerCase.span { _ != '(' }._1 match 
        {
            case "int" | "smallint" | "mediumint" => Some(IntType)
            case "bigint" => Some(LongType)
            case "float" => Some(FloatType)
            case "double" | "decimal" => Some(DoubleType)
            case "varchar" | "char" | "character" => Some(StringType)
            case "tinyint" => Some(if (typeString.endsWith("(1)") || typeString.toLowerCase == "tinyint") BooleanType else IntType)
            case "timestamp" | "datetime" => Some(InstantType)
            case "date" => Some(LocalDateType)
            case "time" => Some(LocalTimeType)
            case s if s.endsWith("text") || s.endsWith("blob") => Some(StringType)
            case _ => None
        }
    }
}