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
  * @param keyToIndex Each (primary) key mapped to its corresponding cell index
  * @param alternativeKeys Secondary keys **in lower case**, mapped to the primary key they correspond with
  */
case class Headers(keyToIndex: Map[String, Int], alternativeKeys: Map[String, String] = Map())
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
	def apply(key: String) = lift(key).getOrElse(-1)
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
	def lift(key: String): Option[Int] =
		lowerKeyToIndex.get(key.toLowerCase).orElse { alternativeKeys.get(key.toLowerCase).flatMap(lift) }
	/**
	  * @param index Header index
	  * @return Key associated with that index.
	  *         None if not a valid index.
	  */
	def lift(index: Int) = keyAtIndex.get(index)
	
	/**
	  * @param header A header
	  * @return Whether these headers contain the specified value in some form
	  */
	def contains(header: String): Boolean = {
		val lower = header.toLowerCase
		alternativeKeys.get(lower) match {
			case Some(primaryForm) => contains(primaryForm)
			case None => lowerKeyToIndex.contains(lower)
		}
	}
	/**
	  * @param header A header
	  * @param index The index at which that header should appear
	  * @return Whether that header appears (in some form) at that index
	  */
	def contains(header: String, index: Int) = lift(header).contains(index)
	
	/**
	  * Assigns an alternative name for a header
	  * @param primary The primary header name
	  * @param secondary Alternative header name
	  * @return Copy of these headers with the alternative name included
	  */
	def withAlternativeName(primary: String, secondary: String) =
		copy(alternativeKeys = alternativeKeys + (secondary -> primary))
	/**
	  * Assigns alternative names for a header
	  * @param primary The primary header name
	  * @param alternatives Alternative header names
	  * @return Copy of these headers with the alternative names included
	  */
	def withAlternativeNames(primary: String, alternatives: IterableOnce[String]) =
		copy(alternativeKeys = alternativeKeys ++ alternatives.iterator.map { _ -> primary })
	/**
	  * Assigns alternative names for a header
	  * @param primary The primary header name
	  * @param secondary An alternative name
	  * @param tertiary Another alternative name
	  * @param more Additional alternative names
	  * @return Copy of these headers with the alternative names included
	  */
	def withAlternativeNames(primary: String, secondary: String, tertiary: String, more: String*): Headers =
		withAlternativeNames(primary, Set(secondary, tertiary) ++ more)
}
