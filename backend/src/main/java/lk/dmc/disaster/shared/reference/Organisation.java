package lk.dmc.disaster.shared.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lk.dmc.disaster.shared.domain.BaseEntity;

/** Organisation entity. */
@Entity
@Table(name = "organisations")
public class Organisation extends BaseEntity {
    private String name;

    @Enumerated(EnumType.STRING)
    private OrganisationType type;

    private String contactPhone;

    protected Organisation() {}

    public String getName() { return name; }
    public OrganisationType getType() { return type; }
    public String getContactPhone() { return contactPhone; }
}
