package com.visitor.management;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return """
                <html>
                <head>
                    <title>Visitor Management System</title>
                </head>
                <body style="font-family: Arial; text-align: center; margin-top: 100px;">
                    <h1>Visitor Management System</h1>
                    <h2>Welcome!</h2>
                    <p>QR-Based Visitor Management System</p>
                    <p>Spring Boot and MySQL Connected Successfully!</p>
                </body>
                </html>
                """;
    }
}