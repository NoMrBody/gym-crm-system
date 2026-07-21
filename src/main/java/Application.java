import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication(scanBasePackages = {
        "config", "controller", "dao", "exception", "facade", "mapper", "service", "util"
})
@EntityScan("model")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
