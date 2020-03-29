package utopia.vault.test

import utopia.flow.generic.DataType
import utopia.vault.database.SqlTypeInterpreterManager
import utopia.flow.generic.StringType
import utopia.flow.generic.IntType
import utopia.flow.generic.LongType
import utopia.flow.generic.BooleanType
import utopia.flow.generic.FloatType
import utopia.flow.generic.DoubleType
import utopia.flow.generic.InstantType

/**
 * This test makes sure the sql type interpretation is working correctly. No database connection 
 * is required for this one.
 * @author Mikko Hilpinen
 * @since 8.6.2017
 */
object SqlTypeInterpretationTest extends App
{
    DataType.setup()
    
    def test(typeString: String, expectedType: DataType) = 
            assert(SqlTypeInterpreterManager(typeString).contains(expectedType))
    
    test("varchar(255)", StringType)
    test("varchar(64)", StringType)
    test("TINYTEXT", StringType)
    test("MEDIUMTEXT", StringType)
    test("BLOB", StringType)
    test("CHAR(4)", StringType)
    test("CHAR", StringType)
    
    test("int(11)", IntType)
    test("bigint", LongType)
    test("tinyint(4)", IntType)
    test("tinyint(1)", BooleanType)
    test("tinyint", BooleanType)
    test("SMALLINT", IntType)
    test("MEDIUMINT", IntType)
    test("FLOAT(16, 4)", FloatType)
    test("double", DoubleType)
    test("DECIMAL(13, 3)", DoubleType)
    
    test("TIMESTAMP", InstantType)
    test("DATETIME", InstantType)
    
    println("Success!")
}