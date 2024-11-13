# Skunk todo

Basic demo of postgresql library Skunk in a super simple todo application with a small http4s REST api

## Database

We're using [pgroll](https://github.com/xataio/pgroll) for db migrations, and run postgres in docker to be able to easily clean up and restart the database.

There is a [script](scripts/restart-postgres.nu) written in [nushell](https://www.nushell.sh/) to facilitate starting (or restarting) db, when in nushell, just:

```
❯ use scripts/restart-postgres.nu *
❯ restart-postgres
```

What it's basically doing is:
1. Looking for any running containers with name `meetup-postgres` and kill/remove them
2. Starting a postgres docker container with the name `meetup-postgres`
3. Run the [pgroll](https://github.com/xataio/pgroll) migrations under [src/main/resources/pgroll](src/main/resources/pgroll)

## Run

To be able to automatically restart on code changes, we use the [sbt-revolver](https://github.com/spray/sbt-revolver) sbt plugin, to run the project:

```
sbt ~reStart
```

## Dependencies

* [Skunk](https://typelevel.org/skunk/)
* [Cats Effect](https://typelevel.org/cats-effect/)
* [http4s](https://http4s.org/)
* [FS2](https://fs2.io/)
* [sbt-tpolecat](https://github.com/typelevel/sbt-tpolecat)
* [sbt-revolver](https://github.com/spray/sbt-revolver)
* [pgroll](https://github.com/xataio/pgroll)
* [Nushell](https://www.nushell.sh/)