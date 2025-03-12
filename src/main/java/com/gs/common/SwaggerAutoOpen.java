package com.gs.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SwaggerAutoOpen implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port}")
    private String serverPort;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            String url = "http://localhost:" + serverPort + "/swagger-ui/index.html"; // 根据实际情况修改
            openUrlInBrowser(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openUrlInBrowser(String url) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        Runtime rt = Runtime.getRuntime();

        if (os.contains("mac")) {
            rt.exec("open " + url);
        } else if (os.contains("win")) {
            rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.contains("nix") || os.contains("nux")) {
            String[] browsers = {"xdg-open", "google-chrome", "firefox"};
            boolean success = false;
            for (String browser : browsers) {
                if (!success) {
                    try {
                        rt.exec(new String[]{browser, url});
                        success = true;
                    } catch (Exception e) {
                        // 尝试下一个浏览器
                    }
                }
            }
            if (!success) {
                throw new RuntimeException("未能在您的系统上找到可用的浏览器。");
            }
        } else {
            throw new RuntimeException("未知的操作系统：" + os);
        }
    }
}

