package utopia.vault.database

/**
  * Common trait for interfaces which are able to insert data to a database
  * @author Mikko Hilpinen
  * @since 4.8.2025, v2.0
  */
trait Inserter[-I, +S]
{
	// ABSTRACT ---------------------------
	
	/**
	  * Inserts a new item to the database
	  * @param data       Data to insert
	  * @param connection Implicit DB connection to use
	 * @return Inserted item, including its generated ID
	  */
	def insert(data: I)(implicit connection: Connection): S
	/**
	  * Inserts multiple new items to the database
	  * @param data       Data to insert
	  * @param connection Implicit DB connection to use
	 * @return Inserted items, including their generated IDs
	  */
	def insert(data: Seq[I])(implicit connection: Connection): Seq[S]
	/**
	  * Extracts the data to insert from the specified set of items, inserts the extracted data,
	  * and finally merges the inserted items with the original data.
	  * @param data Data that contains data to insert
	  * @param extractData Function that extracts the data to insert
	  * @param mergeBack Function that merges an inserted entry with the original data
	  * @param connection Implicit DB connection to use
	 * @tparam O Type of the original entries
	  * @tparam R Type of the merge results
	  * @return Merge results
	  */
	def insertFrom[O, R](data: Seq[O])(extractData: O => I)(mergeBack: (S, O) => R)
	                    (implicit connection: Connection): Seq[R]
}
