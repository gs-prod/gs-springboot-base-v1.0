package com.gs.controller;

import com.gs.utils.CreateValidateCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.io.IOException;

@Tag(name = "captcha")
@RequestMapping("api/")
@RestController
public class CaptchaController {

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CreateValidateCode validateCode = new CreateValidateCode();
        request.getSession().setAttribute("captcha", validateCode.getCode());
        response.setContentType("image/png");
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(validateCode.getImage(), "PNG", out);
        out.flush();
        out.close();
    }
}
