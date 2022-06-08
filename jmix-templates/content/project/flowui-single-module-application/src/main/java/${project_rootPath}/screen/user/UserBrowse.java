package ${project_rootPackage}.screen.user;

import ${project_rootPackage}.entity.User;
import ${project_rootPackage}.screen.main.MainScreen;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.screen.*;

@UiController("${normalizedPrefix_underscore}User.browse")
@UiDescriptor("user-browse.xml")
@LookupComponent("usersTable")
@Route(value = UserBrowse.ROUTE, layout = MainScreen.class)
public class UserBrowse extends StandardLookup<User> {
    public static final String ROUTE = "users";
}
