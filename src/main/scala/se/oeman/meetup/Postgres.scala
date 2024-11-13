package se.oeman.meetup

import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace
import skunk.*
import skunk.data.Completion.Delete
import skunk.data.Completion.Update
import fs2.io.net.SocketGroup
import skunk.net.protocol.Parse
import skunk.net.protocol.Describe
import skunk.util.Pool
trait Postgres[F[_]]:
  def add(name: String): F[Todo]
  def list: Stream[F, Todo]
  def get(id: Int): F[Option[Todo]]
  def update(id: Int, todo: Todo): F[Boolean]
  def delete(id: Int): F[Boolean]
  def getSession: Session[F]
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

  def sessionPool[F[_]: Async: Trace: Console: Network](
    config: PostgresConfig
  ): Resource[F, Resource[F, Postgres[F]]] = {
    def session(
      socketGroup: SocketGroup[F]
    ): Resource[F, Postgres[F]] =
      for {
        pc <- Resource.eval(Parse.Cache.empty[F](1024))
        dc <- Resource.eval(Describe.Cache.empty[F](1024, 1024))
        s <- Session.fromSocketGroup[F](
          socketGroup = socketGroup,
          host = config.host,
          port = config.port,
          user = config.user,
          database = config.database,
          password = config.password.some,
          socketOptions = Session.DefaultSocketOptions,
          sslOptions = None,
          parameters = Session.DefaultConnectionParameters,
          describeCache = dc,
          parseCache = pc
        )
        pg <- Resource.eval(fromSession(s))
      } yield pg

    for {
      pool <- Pool.of(session(Network[F]), 10)(
        Session.Recyclers.minimal[F].contramap(_.getSession)
      )
    } yield pool
  }

  def fromSession[F[_]: Async](session: Session[F]): F[Postgres[F]] =
    for
      insertQuery <- session.prepare(Todo.insert)
      listQuery <- session.prepare(Todo.list)
      getQuery <- session.prepare(Todo.get)
      updateQuery <- session.prepare(Todo.update)
      deleteQuery <- session.prepare(Todo.delete)
    yield new Postgres[F]:
      override def add(name: String): F[Todo] =
        insertQuery.unique(name)

      override def list: Stream[F, Todo] =
        listQuery.stream(Void, 1024)

      override def get(id: Int): F[Option[Todo]] =
        getQuery.option(id)

      override def update(id: Int, todo: Todo): F[Boolean] =
        updateQuery.execute((todo.name, todo.completed, id)).map {
          case Update(x) => x > 0
          case _ => false
        }

      override def delete(id: Int): F[Boolean] =
        deleteQuery.execute(id).map {
          case Delete(x) => x > 0
          case _ => false
        }

      override def getSession: Session[F] = session
