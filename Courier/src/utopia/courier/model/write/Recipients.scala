package utopia.courier.model.write

import javax.mail.Message.RecipientType

import scala.language.implicitConversions

object Recipients
{
	// ATTRIBUTES   ------------------------------
	
	/**
	  * An empty set of recipients
	  */
	val empty = new Recipients(Map())
	
	
	// IMPLICIT ----------------------------------
	
	implicit def emailToRecipients(email: String): Recipients = apply(email)
	implicit def addressListToRecipients(list: Vector[String]): Recipients = apply(list)
	implicit def mapToRecipients(map: Map[RecipientType, Vector[String]]): Recipients = apply(map)
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param recipientType Recipient type
	  * @param addresses Recipient email addresses
	  * @return A new recipients list
	  */
	def apply(recipientType: RecipientType, addresses: Vector[String]): Recipients =
		apply(Map(recipientType -> addresses))
	/**
	  * @param recipientType Recipient type
	  * @param firstAddress First recipient address
	  * @param moreAddresses More recipient addresses
	  * @return A new recipients list
	  */
	def apply(recipientType: RecipientType, firstAddress: String, moreAddresses: String*): Recipients =
		apply(recipientType, firstAddress +: moreAddresses.toVector)
	
	/**
	  * @param addresses Recipient addresses
	  * @return A new recipients list
	  */
	def apply(addresses: Vector[String]): Recipients = apply(RecipientType.TO, addresses)
	/**
	  * @param firstAddress First recipient email address
	  * @param moreAddresses More email addresses
	  * @return A new recipients list
	  */
	def apply(firstAddress: String, moreAddresses: String*): Recipients = apply(firstAddress +: moreAddresses.toVector)
}

/**
  * An object representing a set of mail recipients
  * @author Mikko Hilpinen
  * @since 13.9.2021, v0.1
  */
case class Recipients(addressesByType: Map[RecipientType, Vector[String]])
	extends Iterable[(String, RecipientType)]
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return All recipient addresses
	  */
	def all = addressesByType.valuesIterator.flatten.toVector
	
	/**
	  * @return Main recipients list
	  */
	def main = apply(RecipientType.TO)
	/**
	  * @return Copy recipients list
	  */
	def copies = apply(RecipientType.CC)
	/**
	  * @return Hidden copy recipients list
	  */
	def hiddenCopies = apply(RecipientType.BCC)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def iterator = addressesByType.iterator
		.flatMap { case (recipientType, addresses) => addresses.map { _ -> recipientType } }
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param recipientType Type of recipient
	  * @return Addresses linked with that recipient type
	  */
	def apply(recipientType: RecipientType) = addressesByType.getOrElse(recipientType, Vector())
	
	/**
	  * @param address A new email address + recipient type -pair
	  * @return A copy of this address list with that address included
	  */
	def +(address: (String, RecipientType)) =
	{
		val (newAddress, recipientType) = address
		Recipients(addressesByType + (recipientType -> (apply(recipientType) :+ newAddress)))
	}
	/**
	  * @param address A new email address to add
	  * @return A copy of this address list with that address added as 'TO'
	  */
	def +(address: String): Recipients = this + (address -> RecipientType.TO)
	
	/**
	  * @param addresses Addresses to add + recipient type to use
	  * @return A copy of this recipients list with those addresses added
	  */
	def ++(addresses: (IterableOnce[String], RecipientType)) =
	{
		val (newAddresses, recipientType) = addresses
		Recipients(addressesByType + (recipientType -> (apply(recipientType) ++ newAddresses)))
	}
	/**
	  * @param addresses Addresses to add
	  * @return A copy of this recipients list with those addresses added
	  */
	def ++(addresses: IterableOnce[String]): Recipients = this ++ (addresses -> RecipientType.TO)
	/**
	  * @param other Another set of recipients
	  * @return A combination of these recipients lists
	  */
	def ++(other: Recipients) = Recipients((addressesByType.keySet ++ other.addressesByType.keySet)
		.map { recipientType => recipientType -> (apply(recipientType) ++ other(recipientType)) }.toMap)
	
	/**
	  * @param addresses Email addresses
	  * @return A copy of this recipients list with those addresses included as copies
	  */
	def copiedTo(addresses: IterableOnce[String]) = this ++ (addresses -> RecipientType.CC)
	/**
	  * @param firstAddress Email address
	  * @param moreAddresses More email addresses
	  * @return A copy of this recipients list with those addresses included as copies
	  */
	def copiedTo(firstAddress: String, moreAddresses: String*): Recipients = copiedTo(firstAddress +: moreAddresses)
	
	/**
	  * @param addresses Email addresses
	  * @return A copy of this recipients list with those addresses included as hidden copies
	  */
	def hiddenCopiedTo(addresses: IterableOnce[String]) = this ++ (addresses -> RecipientType.BCC)
	/**
	  * @param firstAddress Email address
	  * @param moreAddresses More email addresses
	  * @return A copy of this recipients list with those addresses included as hidden copies
	  */
	def hiddenCopiedTo(firstAddress: String, moreAddresses: String*): Recipients =
		hiddenCopiedTo(firstAddress +: moreAddresses)
}