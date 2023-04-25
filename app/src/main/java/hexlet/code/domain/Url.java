package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.Instant;
import java.util.List;

@Entity
public final class Url extends Model {
    @Id
    private long id;

    private String name;

    @WhenCreated
    private Instant createdAt;

    @OneToMany
    private List<UrlCheck> urlChecks;

    public Url() {
    }

    public Url(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public String getName() {
        return name;
    }

    public List<UrlCheck> getUrlChecks() {
        return this.urlChecks;
    }

    public Instant lastDate() {
        final UrlCheck urlCheck = urlChecks.get(urlChecks.size() - 1);
        return urlCheck.getCreatedAt();
    }
}
