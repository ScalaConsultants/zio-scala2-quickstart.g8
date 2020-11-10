package $package$.infrastructure.flyway

import org.flywaydb.core.api.output.{ MigrateOutput => RawMigrateOutput, MigrateResult => RawMigrateResult }

import scala.jdk.CollectionConverters._

final case class MigrateResult(
  flywayVersion: String,
  database: String,
  warnings: List[String],
  operation: String,
  initialSchemaVersion: Option[String],
  targetSchemaVersion: Option[String],
  schemaName: Option[String],
  migrations: List[MigrateOutput],
  migrationsExecuted: Int
)

object MigrateResult {
  def apply(raw: RawMigrateResult): MigrateResult =
    MigrateResult(
      raw.flywayVersion,
      raw.database,
      raw.warnings.asScala.toList,
      raw.operation,
      Option(raw.initialSchemaVersion),
      Option(raw.targetSchemaVersion),
      Option.when(raw.schemaName.nonEmpty)(raw.schemaName),
      raw.migrations.asScala.toList.map(MigrateOutput(_)),
      raw.migrationsExecuted
    )
}

final case class MigrateOutput(
  category: String,
  version: String,
  description: String,
  migrationType: String,
  filepath: String,
  executionTime: Int
)

object MigrateOutput {
  def apply(raw: RawMigrateOutput): MigrateOutput =
    MigrateOutput(
      raw.category,
      raw.version,
      raw.description,
      raw.`type`,
      raw.filepath,
      raw.executionTime
    )
}
