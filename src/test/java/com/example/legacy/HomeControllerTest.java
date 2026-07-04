package com.example.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;

public class HomeControllerTest {

    @Test
    public void homeReturnsHomeView() {
        HomeController controller = new HomeController();
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.home(model);

        assertEquals("home", viewName);
        assertNotNull(model.get("serverTime"));
    }

    @Test
    public void healthReturnsOk() {
        HomeController controller = new HomeController();

        assertEquals("OK", controller.health());
    }
}
