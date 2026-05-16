package com.kfc.kfc_backend.controller;

import com.kfc.kfc_backend.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "http://64.90.16.27:*"})
public class AiAssistantController {
    @Autowired
    private AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> body) {
        return aiAssistantService.chat(body.get("question"));
    }
}

