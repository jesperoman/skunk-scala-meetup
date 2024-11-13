const docker_name = 'meetup-postgres'
const postgres_password = 'scalameetup'
const postgres_user = 'skunk'
const postgres_db = 'todos'

# Script for restart/reinitialize postgres docker container script
# Written for nushell (https://www.nushell.sh/)
# Initialize with 'use scripts/restart-postgres.nu'
# Run with 'restart-postgres'
export def main [] {
  # Kill and remove present containers if any
  docker ps -a
    | detect columns
    | where NAMES =~ $docker_name
    | each { |container|
      try { docker kill $container.NAMES }
      try { docker rm $container.NAMES }
    }

  # Start postgres
  (docker run
    --name $docker_name
    --env $'POSTGRES_PASSWORD=($postgres_password)'
    --env $'POSTGRES_USER=($postgres_user)'
    --env $'POSTGRES_DB=($postgres_db)'
    --publish 5432:5432
    --detach postgres:17)

  # Wait for postgres to start
  sleep 2sec

  # Set up todos table
  (pgroll
    init
    --postgres-url
    $'postgres://($postgres_user):($postgres_password)@localhost/($postgres_db)?sslmode=disable'
  )

  (pgroll
    start
    --postgres-url
    $'postgres://($postgres_user):($postgres_password)@localhost/($postgres_db)?sslmode=disable'
    src/main/resources/pgroll/01_initial_migration.json
  )

  (pgroll
    complete
    --postgres-url
    $'postgres://($postgres_user):($postgres_password)@localhost/($postgres_db)?sslmode=disable'
  )

  # Show running containers
  docker ps | detect columns
}
