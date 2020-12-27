## Running application locally

The application server expects a PostgresQL database to run Flyway migrations. If it can't find one it'll retry forever until successful. This way it doesn't matter what is started first - the application or the database.

### Database server

Run the local development database with docker. Replace values for `POSTGRES_DB` and `POSTGRES_PASSWORD` according to your settings.
```bash
docker run --name devdb --network host -e POSTGRES_DB=items -e POSTGRES_PASSWORD=12345 -d postgres
```

Connect with `psql`:
```bash
psql -h localhost -U postgres -d items
```

Connect with docker:
```bash
docker exec -it testdb psql -U postgres -d items
```

### Application server

To run the application locally:
```bash
sbt run
```

After you run the application Flyway will run the migrations.

By default the server is started at `http://localhost:8080`. Expects the databse at `localhost:5432` with database name `items` and user/password `postgres/12345`.

You can override the defaults with the following environment variables:

- API_HOST
- API_PORT
- DB_HOST
- DB_PORT
- DB_NAME
- DB_USER
- DB_PASSWORD
