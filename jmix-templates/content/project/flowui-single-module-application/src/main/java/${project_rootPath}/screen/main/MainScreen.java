package ${project_rootPackage}.screen.main;

import com.vaadin.flow.router.Route;
import io.jmix.flowui.app.main.StandardMainScreen;
import io.jmix.flowui.screen.UiController;
import io.jmix.flowui.screen.UiDescriptor;

@UiController("${normalizedPrefix_underscore}MainScreen")
@UiDescriptor("main-screen.xml")
@Route("")
public class MainScreen extends StandardMainScreen {
}
