# Referential Integrity

## One-To-Many

[PLANT](plant.md) generates the entities so that you don't have to do anything else; whenever you show an entity in an `index`, `edit` or `add` view, the assignable entities 
are shown within a selection box for the assignment. beetRoot handles everything with one-to-many references - no code is required. The only important note here is to follow the naming conventions of beetRoot:

- The table that refers to `products` has the integer field `product_id`.

Further examples:

- Table `properties` -> reference table with integer field `property_id`.
- Table `tasks` -> reference table with integer field `task_id`.


## Many-To-Many

You have the option of using the built-in association mechanism to create many-to-many relationships. However, some code is required for this to work. To do this, overwrite 
the `extractCustomSingleInputDiv` method of the edit and add handlers to handle the corresponding column that refers to a relation table. The following examples show how this is done
(many-to-many user-role relationship):

- [ExtUsersAddHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/users/ExtUsersAddHandler.java)
- [ExtUsersEditHandler](https://github.com/autumoswitzerland/autumo-beetroot/blob/master/src/main/java/ch/autumo/beetroot/handler/users/ExtUsersEditHandler.java)

The methods already contain the correct IDs to handle the relationships. When you create a relationship table in the database, it only needs the
reference key, no ID is needed for this table, of course you can add more columns if they are needed for some other reason. E.g. user-role table:

```SQL
CREATE TABLE users_roles (
    "user_id" INT UNSIGNED NOT NULL,
    "role_id" INT UNSIGNED NOT NULL,
    "created" TIMESTAMP(3) DEFAULT NOW(),
    PRIMARY KEY ("user_id", "role_id"),
    FOREIGN KEY ("user_id") REFERENCES users("id") ON DELETE CASCADE,
    FOREIGN KEY ("role_id") REFERENCES roles("id") ON DELETE CASCADE
);
ALTER TABLE users_roles ADD INDEX "idx_user_id" ("user_id");
ALTER TABLE users_roles ADD INDEX "idx_role_id" ("role_id");
```


<br>
<br>
<a href="../README.md">[Main Page]</a>
