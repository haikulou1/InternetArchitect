package com.staffboard.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class WhitelistRequest {
    @NotEmpty(message = "员工ID列表不能为空")
    private List<Long> employeeIds;

    @NotNull(message = "白名单状态不能为空")
    private Boolean isWhitelist;

    private String whitelistType;  // CORE_TALENT / EXECUTIVE / SPECIAL
}
