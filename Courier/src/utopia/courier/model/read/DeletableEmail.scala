package utopia.courier.model.read

import utopia.flow.view.mutable.eventful.Flag
import utopia.flow.view.template.Extender

/**
 * A wrapper class for email instances that allows the user to delete the email from the server
 * by using a mutable flag.
 * There may, however, be some limitations as to when this is possible.
 * @author Mikko Hilpinen
 * @since 19.10.2023, v1.1
 * @param deleteFlag A flag that contains true for deleted emails.
 *                   Setting this flag may (should) cause the represented email to be deleted.
 */
class DeletableEmail[+A](override val wrapped: A, val deleteFlag: Flag) extends Extender[A]
