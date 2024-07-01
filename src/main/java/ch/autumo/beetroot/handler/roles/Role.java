/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package ch.autumo.beetroot.handler.roles;

import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.annotations.Column;
import ch.autumo.beetroot.annotations.Nullable;
import ch.autumo.beetroot.annotations.Unique;

/**
 * Role. 
 */
public class Role extends Model {

    private static final long serialVersionUID = 1L;
	
    @Nullable
    @Column (name = "permissions")
    private String permissions;

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    @Nullable
    @Column (name = "created")
    private java.sql.Timestamp created;

    public java.sql.Timestamp getCreated() {
        return created;
    }

    public void setCreated(java.sql.Timestamp created) {
        this.created = created;
    }

    @Unique
    @Column (name = "name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    @Column (name = "description")
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Nullable
    @Column (name = "modified")
    private java.sql.Timestamp modified;

    public java.sql.Timestamp getModified() {
        return modified;
    }

    public void setModified(java.sql.Timestamp modified) {
        this.modified = modified;
    }

    @Override
    public String getDisplayField() {
        return "name";
    }

    @Override
    public Class<?> modelClass() {
        return Role.class;
    }	
	
}
