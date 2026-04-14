package utopia.scribe.api.app.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Version
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.template.eventful.Changing
import utopia.scribe.api.database.access.logging.issue.{AccessIssue, AccessIssues}
import utopia.scribe.api.database.access.management.aliasing.AccessIssueAlias
import utopia.scribe.api.database.storable.management.{CommentDbModel, IssueAliasDbModel, ResolutionDbModel}
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.management.{CommentData, IssueAliasData, ResolutionData}
import utopia.vault.database.Connection

import java.time.Instant
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
 * Provides commands for managing the currently open issue
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.1
 */
class ManageIssueCommands(openIssueP: Changing[Option[Int]])(implicit log: Logger)
{
	// ATTRIBUTES   ---------------------------
	
	private lazy val multiLineIndicator = "\"\"\""
	
	private val commentModel = CommentDbModel
	private val resolutionModel = ResolutionDbModel
	private val aliasModel = IssueAliasDbModel
	
	/**
	 * A console command that shows the currently targeted database name
	 */
	val showDb = Command.withoutArguments("database", help = "Tells which database is currently targeted") {
		ScribeConsoleSettings.dbName match {
			case Success(dbName) => println(s"Currently targeting \"$dbName\"")
			case Failure(error) => log(error, "Failed to access app configuration")
		}
	}
	/**
	 * A console command for changing the used database
	 */
	val changeDb = Command("database:set", help = "Changes the targeted database")(
		ArgumentSchema("database", "db", defaultDescription = "Requested")) {
		args =>
			args("database").string
				.orElse {
					StdIn.readNonEmptyLine("Specify the name of the database to target from now on (empty cancels)")
				}
				.foreach { dbName =>
					val oldDbName = ScribeConsoleSettings.dbName
					(ScribeConsoleSettings.dbName = dbName).logWithMessage("Failed to change the targeted database")
					
					connectionPool.tryWith { implicit c => AccessIssues.nonEmpty }.failure.foreach { error =>
						log(error, s"Failed to access the specified database: $dbName")
						oldDbName match {
							case Success(oldDbName) =>
								ScribeConsoleSettings.dbName = oldDbName
								println(s"Set the database back to \"$oldDbName\", because \"$dbName\" couldn't be accessed")
							
							case _ => println("The database is currently inaccessible")
						}
					}
				}
	}
	
	/**
	 * A console command for leaving a comment on the open issue
	 */
	val comment = Command.withoutArguments("comment", help = "Comments on the currently open issue") {
		openIssueP.value.foreach { issueId =>
			readMultiLine("Please write the comment.").foreach { comment =>
				connectionPool.logging { implicit c =>
					commentModel.insert(CommentData(issueId, comment))
					println("Comment posted!")
				}
			}
		}
	}
	/**
	 * A console command for requesting a notification to be generated
	 */
	val follow = Command.withoutArguments("follow", "track",
		help = "Flags the currently open issue, so that a notification will be generated once it appears the next time") {
		openIssueP.value.foreach { issueId =>
			connectionPool.logging { implicit c =>
				resolutionModel.insert(ResolutionData(issueId, notifies = true))
				println("You will receive a notification when this issue occurs the next time")
			}
		}
	}
	/**
	 * A console command for marking an issue fixed
	 */
	val fixed = Command("fixed", "resolve", help = "Marks the currently open issue as fixed / resolved")(
		ArgumentSchema("in", "version", help = "Version on which this fix is released. Default = fix has already been released."),
		ArgumentSchema.flag("keep", "K", help = "Whether to keep displaying the issue normally")) {
		args =>
			openIssueP.value.foreach { issueId =>
				val version = args("in").string.flatMap(Version.findFrom)
				val comment = readMultiLine("Comment on this fix (optional).")
				connectionPool.logging { implicit c =>
					val commentId = insertComment(comment, issueId)
					resolutionModel.insert(ResolutionData(issueId, commentId, version,
						silences = !args("keep").getBoolean, notifies = true))
					
					println("Fix recorded")
					version match {
						case Some(version) =>
							println(s"You will receive a notification if this issue still appears after $version")
						case None => println("You will receive a notification if this issue still appears")
					}
				}
			}
	}
	/**
	 * A console command for silencing an issue
	 */
	val silence = Command("silence", "mute",
		help = "Silences the currently open issue, so that it will not be reported anymore")(
		ArgumentSchema("until", help = "Date, duration or version, until which the issue should remain silenced. Default = forever.")) {
		args =>
			openIssueP.value.foreach { issueId =>
				val untilV = args("until")
				val until: Option[Option[Either[Version, Instant]]] = {
					if (untilV.isEmpty) {
						if (StdIn.ask("Are you sure you want to silence this issue forever?"))
							Some(None)
						else
							None
					}
					else {
						val untilStr = untilV.getString
						if (untilStr.headOption.contains('v'))
							Version.findFrom(untilStr).map { v => Some(Left(v)) }
						else {
							val until = untilV.castTo(InstantType, DurationType) match {
								case Left(timeV) => timeV.instant
								case Right(durationV) => Some(Now + durationV.getDuration)
							}
							until.filter { _.isFuture }.map { t => Some(Right(t)) }
						}
					}
				}
				until match {
					case Some(until) =>
						val (versionThreshold, expires) = until match {
							case Some(Left(version)) => Some(version) -> None
							case Some(Right(time)) => None -> Some(time)
							case None => None -> None
						}
						val comment = readMultiLine("Comment on this action (optional).")
						connectionPool.logging { implicit c =>
							val commentId = insertComment(comment, issueId)
							resolutionModel.insert(
								ResolutionData(issueId, commentId, versionThreshold = versionThreshold,
									deprecates = expires, silences = true))
							println("This issue has now been silenced")
						}
					
					case None => println(s"$untilV was not recognized as a valid value")
				}
			}
	}
	/**
	 * A console command for giving an issue a new name
	 */
	val alias = Command("alias", "name", help = "Gives an issue a new name")(
		ArgumentSchema("issue", help = "ID of the issue to name. Defaults to the currently open issue.")) {
		args =>
			args("issue").int.orElse(openIssueP.value) match {
				case Some(issueId) =>
					connectionPool.logging { implicit c =>
						val context = AccessIssue(issueId).context.pull
						if (context.isEmpty)
							println(s"$issueId is not a valid issue ID")
						else {
							val existingAlias = AccessIssueAlias.ofIssue(issueId).pull
							existingAlias.map { _.alias }.filter { _.nonEmpty } match {
								case Some(alias) => println(s"Please write a new name for $alias ($context)")
								case None => println(s"Please write a new name for $context")
							}
							StdIn.readNonEmptyLine().foreach { newAlias =>
								aliasModel.insert(IssueAliasData(issueId, newAlias,
									existingAlias.flatMap { _.newSeverity }))
								existingAlias.foreach { old => AccessIssueAlias(old.id).delete() }
								println("New alias assigned!")
							}
						}
					}
				
				case None => println("No issue was targeted")
			}
	}
	/**
	 * A console command for adjusting issue severity
	 */
	// WET WET (from alias)
	val changeSeverity = Command("severity", "adjust", help = "Assigns a new severity for an issue")(
		ArgumentSchema("issue", help = "ID of the issue to name. Defaults to the currently open issue.")) {
		args =>
			args("issue").int.orElse(openIssueP.value) match {
				case Some(issueId) =>
					connectionPool.logging { implicit c =>
						AccessIssue(issueId).pull match {
							case Some(issue) =>
								val existingAlias = AccessIssueAlias.ofIssue(issueId).pull
								val existingSeverity = existingAlias.flatMap { _.newSeverity }
									.getOrElse(issue.severity)
								val issueName = existingAlias.map { _.alias }.filter { _.nonEmpty }
									.getOrElse(issue.context)
								
								println(s"Select a new severity for $issueName")
								StdIn.selectFrom(Severity.values.map { s => s -> s.toString })
									.filterNot { _ == existingSeverity } match
								{
									case Some(newSeverity) =>
										val alias = existingAlias match {
											case Some(alias) => alias.alias
											case None => ""
										}
										aliasModel.insert(IssueAliasData(issueId, alias, Some(newSeverity)))
										existingAlias.foreach { old => AccessIssueAlias(old.id).delete() }
										println(s"$issueName severity changed to $newSeverity")
									
									case None => println("Issue severity was not changed")
								}
								
							case None => println(s"$issueId is not a valid issue ID")
						}
					}
				
				case None => println("No issue was targeted")
			}
	}
	
	private val staticCommands = Vector(showDb, changeDb, alias, changeSeverity)
	private val conditionalCommands = Vector(comment, fixed, silence, follow)
	
	/**
	 * A pointer that contains the currently available console commands
	 */
	val pointer = openIssueP.lightMap { _.isDefined }
		.lightMap { if (_) staticCommands ++ conditionalCommands else staticCommands }
	
	
	// COMPUTED --------------------------------
	
	/**
	 * @return The currently available console commands
	 */
	def currently = pointer.value
	
	
	// OTHER    --------------------------------
	
	private def insertComment(comment: Option[String], issueId: Int)(implicit connection: Connection) =
		comment.map { comment => commentModel.insert(CommentData(issueId, comment)).id }
	
	private def readMultiLine(prompt: String) = {
		StdIn.readNonEmptyLine(s"$prompt\nTo write multiple lines, start and end with $multiLineIndicator")
			.map { firstLine =>
				if (firstLine.startsWith(multiLineIndicator)) {
					val moreLines = Iterator.continually { StdIn.readLine() }.takeTo { _.contains(multiLineIndicator) }
						.mkString("\n")
					s"${ firstLine.drop(multiLineIndicator.length) }\n$moreLines"
				}
				else
					firstLine
			}
	}
}
