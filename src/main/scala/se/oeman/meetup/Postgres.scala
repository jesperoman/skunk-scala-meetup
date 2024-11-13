package se.oeman.meetup

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace
import skunk.*

trait Postgres[F[_]]:
  def add(name: String): F[Todo]
  def list: Stream[F, Todo]
  def get(id: Int): F[Option[Todo]]

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
    for
      insertQuery <- session.prepare(Todo.insert)
      listQuery <- session.prepare(Todo.list)
      getQuery <- session.prepare(Todo.get)
    yield new Postgres[F]:
      override def add(name: String): F[Todo] =
        insertQuery.unique(name)

      override def list: Stream[F, Todo] =
        listQuery.stream(Void, 1024)

      override def get(id: Int): F[Option[Todo]] =
        getQuery.option(id)
