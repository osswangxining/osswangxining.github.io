---
title: Mongo
date: 2017-1-9 20:46:25
---
### 1. mongo docker image
Start the Database
```
docker run --name mymongo -d mongo:<label> --auth
```

Add the Initial Admin User
```
$ docker exec -it mymongo mongo admin
connecting to: admin
> db.createUser({ user: 'jsmith', pwd: 'some-initial-password', roles: [ { role: "userAdminAnyDatabase", db: "admin" } ] });
Successfully added user: {
    "user" : "jsmith",
    "roles" : [
        {
            "role" : "userAdminAnyDatabase",
            "db" : "admin"
        }
    ]
}
```
### 2. mongo backup and restore
