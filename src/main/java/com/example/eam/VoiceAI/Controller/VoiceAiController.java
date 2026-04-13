package com.example.eam.VoiceAI.Controller;

import com.example.eam.Common.ApiResponse;
import com.example.eam.VoiceAI.Dto.VoiceIntakeRequest;
import com.example.eam.VoiceAI.Dto.VoiceIntakeResponse;
import com.example.eam.VoiceAI.Service.VoiceAiIntakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice-ai")
@RequiredArgsConstructor
public class VoiceAiController {

    private final VoiceAiIntakeService voiceAiIntakeService;

    @PostMapping("/intake")
    public ResponseEntity<ApiResponse<VoiceIntakeResponse>> intake(@RequestBody VoiceIntakeRequest request) {
        VoiceIntakeResponse response = voiceAiIntakeService.processIntake(request);
        return ResponseEntity.ok(
                ApiResponse.successResponse(
                        HttpStatus.OK.value(),
                        "Voice intake processed successfully",
                        response
                )
        );
    }
}
