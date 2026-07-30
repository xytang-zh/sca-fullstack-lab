package com.xytang.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 踢人下线入参（US2 完善具体逻辑：Redis Pub/Sub + 多端选择）
 */
@Data
@Schema(description = "踢人下线参数")
public class KickoutDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标用户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Long userId;
}
