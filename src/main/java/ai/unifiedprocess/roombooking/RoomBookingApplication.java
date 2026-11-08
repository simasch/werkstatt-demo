package ai.unifiedprocess.roombooking;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@StyleSheet(Aura.STYLESHEET)
@SpringBootApplication
public class RoomBookingApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(RoomBookingApplication.class, args);
    }

}
