[![pipeline status](https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2024/team/mondaywednesdaylecture/T_25/card-engine/badges/main/pipeline.svg)](https://git.doit.wisc.edu/cdis/cs/courses/cs506/sp2024/team/mondaywednesdaylecture/T_25/card-engine/-/commits/main)

# Card Engine

## How to develop using the CSL machines

```bash
ssh -L 8080:localhost:8080 -L 3000:localhost:3000 username@cs506-team-25.cs.wisc.edu
```

## How to use the docker compose

* Start the docker daemon
* Run the following to startup services

```bash
cd ./database
docker compose -f db_setup.yml up -d --build
```

To make edits to the database while the compose is running, run the following:

```bash
docker exec -it database-db2-1 mysql -u root -p
```

If you make edits to the backend API, you must restart the backend container in order for the changes to take effect in the running container.

## How to Use The Frontend Locally without the Compose

```bash
cd ./frontend
npm install
npm run dev
```

## How to Use The Backend Locally without the Compose

```bash
cd ./backend
./gradlew bootrun
```
