package utopia.manuscript.excel

object Headers
{
	/**
	  * @param headers All headers, starting from index 0
	  * @return A set of headers wrapping that sequence
	  */
	def apply(headers: Seq[String]): Headers = apply(headers.zipWithIndex.toMap)
}

/**
  * Represents a headers-row or a headers-column
  * @author Mikko Hilpinen
  * @since 31/01/2024, v1.0
  */
case class Headers(keyToIndex: Map[String, Int])
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * A map that maps each lower-cased key to its matching (cell) index
	  */
	lazy val lowerKeyToIndex = keyToIndex.map { case (k, i) => k.toLowerCase -> i }.withDefaultValue(-1)
	/**
	  * A map that maps each index to its original header / key
	  */
	lazy val keyAtIndex = keyToIndex.map { case (k, i) => i -> k }.withDefaultValue("")
	
	
	// OTHER    ------------------------
	
	/**
	  * @param key Key to find
	  * @return Index of that key in these headers.
	  *         -1 if that key was not present in these headers.
	  */
	def apply(key: String) = lowerKeyToIndex(key.toLowerCase)
	/**
	  * @param index Header index
	  * @return Key associated with that index.
	  *         Empty string if not a valid index.
	  */
	def apply(index: Int) = keyAtIndex(index)
	
	/**
	  * @param key Key to find
	  * @return Index of that key in these headers.
	  *         None if that key was not present in these headers.
	  */
	def lift(key: String) = lowerKeyToIndex.get(key.toLowerCase)
	/**
	  * @param index Header index
	  * @return Key associated with that index.
	  *         None if not a valid index.
	  */
	def lift(index: Int) = keyAtIndex.get(index)
}
