package com.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {
    // Jersey auto-discovers resources via package scanning in Main.java
}
