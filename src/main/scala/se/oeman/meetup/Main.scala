package se.oeman.meetup

import cats.effect.IO
import cats.effect.ResourceApp
import cats.effect.kernel.Resource
import natchez.Trace.Implicits.noop

object Main extends ResourceApp.Forever:
  override def run(args: List[String]): Resource[IO, Unit] =
    for
      postgres <- Postgres.sessionPool[IO](PostgresConfig.default)
      _ <- HttpServer(postgres)
    yield ()
