package org.example.fleet.delivery;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MainApp {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private MainApp() {
    }

    public static void main(String[] args) throws Exception {
        log.info("Starting Delivery Tracking service");
        Main main = new Main();
        main.configure().withBasePackageScan("org.example.fleet.delivery");
        main.run(args);
    }
}
