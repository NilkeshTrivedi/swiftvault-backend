package com.swiftvault.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SetPinRequest {
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
    private String pin;

    @NotBlank(message = "Confirm PIN is required")
    private String confirmPin;

    public String getPin()        { return pin; }
    public String getConfirmPin() { return confirmPin; }
    public void setPin(String v)        { this.pin = v; }
    public void setConfirmPin(String v) { this.confirmPin = v; }
}