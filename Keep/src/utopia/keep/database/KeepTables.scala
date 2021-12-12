package utopia.keep.database

import utopia.keep.util.KeepContext
import utopia.vault.model.immutable.Table

/**
  * Used for accessing the database tables introduced in this project
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object KeepTables
{
	// COMPUTED	--------------------
	
	/**
	  * Table that contains Problems (Represents a type of problem that may occur during a program's run)
	  */
	def problem = apply("problem")
	
	/**
	  * Table that contains ProblemCases (Represents a specific setting where a problem occurred)
	  */
	def problemCase = apply("problem_case")
	
	/**
	  * 
		Table that contains ProblemRepeats (Represents a case where a previously occurred problem repeats again)
	  */
	def problemRepeat = apply("problem_repeat")
	
	/**
	  * Table that contains SettingFields (Represents a field that specifies some program functionality)
	  */
	def settingField = apply("setting_field")
	
	/**
	  * Table that contains SettingValues (Represents a single setting value assignment)
	  */
	def settingValue = apply("setting_value")
	
	
	// OTHER	--------------------
	
	private def apply(tableName: String): Table = KeepContext.tables(KeepContext.databaseName, tableName)
}

