package io.github.nujanzh.yotsubato.dto.request;

import jakarta.validation.constraints.Size;

public record RejectJoinRequest(@Size(max = 250) String reason) {}
