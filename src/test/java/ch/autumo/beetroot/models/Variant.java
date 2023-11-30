/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package ch.autumo.beetroot.models;

import ch.autumo.beetroot.Model;
import ch.autumo.beetroot.annotations.Column;
import ch.autumo.beetroot.annotations.Nullable;

/**
 * Variant. 
 */
public class Variant extends Model {

    private static final long serialVersionUID = 1L;
	
    @Column (name = "identifier")
    private String identifier;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
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

    @Column (name = "product_id")
    private int productId;

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
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

    @Column (name = "license_rt_type")
    private String licenseRtType;

    public String getLicenseRtType() {
        return licenseRtType;
    }

    public void setLicenseRtType(String licenseRtType) {
        this.licenseRtType = licenseRtType;
    }
    
    @Override
    public String getDisplayField() {
        return "description";
    }

    @Override
    public java.util.Map<String, Class<?>> getForeignReferences() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("product_id", ch.autumo.beetroot.models.Product.class)
            );
    }

    @Override
    public Class<?> modelClass() {
        return Variant.class;
    }	
	
}