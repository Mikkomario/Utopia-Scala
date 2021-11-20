package utopia.vault.database

/**
 * Connection settings specify, how to connect to the database. A settings instance has value
 * semantics
 * @param connectionTarget
 * The new MariaDB / MySQL server to be used. Should not include 
 * the name of the database. Default: "jdbc:mysql://localhost:3306/"
 * @param user The user name used for accessing the database. Default: "root"
 * @param password the password used for accessing the database. Default: ""
 * @param driver The driver used when connecting to the server. Eg. "org.gjt.mm.mysql.Driver.". 
 * The mariaDB driver is used by default if nothing else is provided
 * @param debugPrintsEnabled Whether connection debug printing should be enabled
 * @param maximumAmountOfRowsCached The maximum number of rows requested on queries that map or operate on multiple rows
 * @param charsetName Name of the charset used when reading from DB (E.g. "utf8").
 *                    Please refer to MySql character set names in
 *                    https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-charsets.html
 * @param charsetCollationName Character set collation option. E.g. "utf8_general_ci"
 * @author Mikko Hilpinen
 * @since 16.4.2017
 */
case class ConnectionSettings(connectionTarget: String = "jdbc:mysql://localhost:3306/", user: String = "root",
							  password: String = "", defaultDBName: Option[String] = None,
							  driver: Option[String] = None, debugPrintsEnabled: Boolean = false,
							  maximumAmountOfRowsCached: Int = 10000, charsetName: String = "",
							  charsetCollationName: String = "")
{
	/**
	 * @return String inserted to driver manager connection in order to specify charset and collation
	 */
	def charsetString =
	{
		if (charsetName.isEmpty && charsetCollationName.isEmpty)
			""
		else
		{
			val charsetPart = if (charsetName.isEmpty) "" else s"&characterEncoding=$charsetName"
			val collationPart = if (charsetCollationName.isEmpty) "" else s"&connectionCollation=$charsetCollationName"
			s"?useUnicode=true$charsetPart$collationPart"
		}
	}
}