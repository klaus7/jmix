/*
 * Copyright 2022 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ${project_rootPackage}.screen.login;

import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.flowui.screen.StandardScreen;
import io.jmix.flowui.screen.Subscribe;
import io.jmix.flowui.screen.UiController;
import io.jmix.flowui.screen.UiDescriptor;
import io.jmix.securityflowui.authentication.AuthDetails;
import io.jmix.securityflowui.authentication.LoginScreenSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

@UiController("${normalizedPrefix_underscore}login-screen")
@UiDescriptor("login-screen.xml")
@Route(value = "login")
public class LoginView extends StandardScreen {

    private final Logger log = LoggerFactory.getLogger(LoginView.class);

    @Autowired
    private LoginScreenSupport loginScreenSupport;

    @Subscribe("login")
    public void onLogin(LoginEvent event) {
        try {
            loginScreenSupport.authenticate(
                    AuthDetails.of(event.getUsername(), event.getPassword())
            );
        } catch (BadCredentialsException | DisabledException | LockedException | AccessDeniedException e) {
            log.info("Login failed", e);
            event.getSource().setError(true);
        }
    }
}
