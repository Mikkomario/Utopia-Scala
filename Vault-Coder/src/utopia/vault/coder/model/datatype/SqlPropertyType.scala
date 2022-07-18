package utopia.vault.coder.model.datatype

/**
  * A property type for the SQL / database side of code
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  * @param baseTypeSql SQL segment matching the underlying data type (without any default or not null -portions).
  *                    E.g. "VARCHAR(32)"
  * @param defaultValue The default portion of this type. E.g. "2" or "CURRENT_TIMESTAMP".
  *                     Default = empty = no default or NULL.
  * @param columnNameSuffix Suffix applied to the end of automatically generated column names when using this data
  *                         type. Shouldn't include the possible separator (ie. '_'). Default = empty = no suffix.
  * @param isNullable Whether "NOT NULL" should be omitted (true) or included (false). Default = false.
  * @param indexByDefault Whether columns using this data type should be indexed by default. Default = false.
  */
case class SqlPropertyType(baseTypeSql: String, defaultValue: String = "", columnNameSuffix: String = "",
                           isNullable: Boolean = false, indexByDefault: Boolean = false)
{
	// COMPUTED --------------------------
	
	/**
	  * @return True if this type doesn't accept NULL values
	  */
	def isNotNullable = !isNullable
	
	/**
	  * @return An SQL segment based on this data type
	  */
	def toSql = {
		val builder = new StringBuilder()
		builder ++= baseTypeSql
		if (isNotNullable)
			builder ++= " NOT NULL"
		if (defaultValue.nonEmpty)
			builder ++= s" DEFAULT $defaultValue"
		builder.result()
	}
	/**
	  * @return If this type is nullable, returns "", otherwise returns "NOT NULL"
	  */
	def notNullPart = if (isNullable) "" else " NOT NULL"
	
	/**
	  * @return A copy of this type that accepts NULL values
	  */
	def nullable = if (isNullable) this else copy(isNullable = true)
	/**
	  * @return A copy of this type that rejects NULL values
	  */
	def notNullable = if (isNullable) copy(isNullable = false) else this
	
	
	// IMPLEMENTED  -----------------------
	
	override def toString = toSql
}
