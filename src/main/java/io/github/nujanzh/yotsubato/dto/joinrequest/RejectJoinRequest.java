package io.github.nujanzh.yotsubato.dto.joinrequest;

import jakarta.validation.constraints.Size;

public record RejectJoinRequest(@Size(max = 250) String reason) {}
