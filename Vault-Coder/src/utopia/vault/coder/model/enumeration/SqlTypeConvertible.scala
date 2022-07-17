package utopia.vault.coder.model.enumeration

/**
  * A common trait for data types which may be represented by sql data types
  * @author Mikko Hilpinen
  * @since 17.7.2022, v1.5.1
  */
trait SqlTypeConvertible
{
	/**
	  * @return an SQL representation of this type
	  */
	def toSql: String
	/**
	  * @return Suffix to add to generated column names for this type. Only contains the end portion, not the
	  *         separator (e.g. '_'). None if no suffix should be added.
	  */
	def columnNameSuffix: Option[String]
	/**
	  * @return Default value for this type in the SQL document when no other default has been specified
	  */
	def baseSqlDefault: String
	/**
	  * @return Whether properties of this type act as database indices by default
	  */
	def createsIndexByDefault: Boolean
}
