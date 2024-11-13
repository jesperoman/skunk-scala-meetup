package se.oeman.meetup

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.net.Network
import natchez.Trace
import skunk.Session

trait Postgres[F[_]]:
  def add(name: String): F[Int]

object Postgres:
  def default[F[_]: Async: Trace: Network: Console](
    config: PostgresConfig
  ): Resource[F, Postgres[F]] =
    Session
      .single[F](
        host = config.host,
        port = config.port,
        user = config.user,
        database = config.database,
        password = config.password.some
      )
      .evalMap(fromSession)

  def fromSession[F[_]: Async](session: Session[F]): F[Postgres[F]] =
    for insertQuery <- session.prepare(Todo.insert)
    yield new Postgres[F]:
      def add(name: String): F[Int] =
        insertQuery.unique(name)
