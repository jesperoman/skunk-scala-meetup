package se.oeman.meetup

final case class PostgresConfig(
  host: String,
  port: Int,
  user: String,
  database: String,
  password: String
)

object PostgresConfig:
  def default = PostgresConfig(
    host = "localhost",
    port = 5432,
    user = "skunk",
    database = "todos",
    password = "scalameetup"
  )
