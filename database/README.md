# Running the compose

To spin up the containers for the db, backend, and phpmyadmin, run the following command from within the /database folder:

```bash
docker compose -f db_setup.yml up -d --build
```

Running this command will spin up the frontend, backend, and database as well as PHPMyAdmin to view/edit the db.

## Ports for each system within the running compose

DB: db2:3306/full_house_badger

PHPMyAdmin: http://localhost:50080/

Frontend: http://localhost:3000/

Backend: http://localhost:8080/

To get into the mysql DB from your terminal, run the following command:

```bash
docker exec -it database-db2-1 mysql -u root -p
```
